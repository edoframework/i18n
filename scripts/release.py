#!/usr/bin/env python3
"""Release helpers for the Makefile (ADR 0003: one signed bundle, two registries).

Run as `python3 -I scripts/release.py <command> ...`, so that nothing from the current
directory or the environment is imported. Every command exits non-zero, with one line per
problem on stderr, when anything is not exactly as expected.

  bundle   --source DIR --dest DIR --group G --version V --artifacts "A B" --name NAME [--pom-only "A"]
           Check the signed tree Mill wrote (DIR, Maven layout), copy it to DEST/repository,
           and write the ledger DEST/SHA256SUMS, the deterministic zip DEST/NAME.zip and its
           SHA-256 in DEST/NAME.zip.sha256. DEST must not exist.
  verify   --dest DIR
           Check that DEST/repository holds exactly the files in DEST/SHA256SUMS, with those
           hashes, and that the zip still matches DEST/NAME.zip.sha256. Prints the ledger's
           paths, POMs last, which is the upload order.
  pins     [--own "A B"] POM...
           Print "artifactId version" for every dev.taihei dependency in the POMs. A dependency
           on one of the --own artifacts (this library's other modules, released in the same
           bundle) is not listed; it must have the depending POM's own version.
  no-repos JSON
           Check that Mill's `show {modules}.repositories` output names no repository.
"""

import argparse
import hashlib
import json
import os
import re
import stat
import sys
import xml.etree.ElementTree as ET
import zipfile

# A path in the ledger is a Maven path: no "..", no leading slash, nothing a shell or a URL
# would read differently.
SAFE_PATH = re.compile(r"^[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*$")
# What Mill's SonatypeCentralPublishModule writes per module: these four files, each with a
# signature and two checksums. Checksums of a signature are allowed, nothing else.
MAIN_SUFFIXES = (".jar", "-sources.jar", "-javadoc.jar", ".pom")
SIDE_SUFFIXES = ("", ".asc", ".md5", ".sha1")
OPTIONAL_SUFFIXES = (".asc.md5", ".asc.sha1")
ZIP_TIME = (1980, 1, 1, 0, 0, 0)
POM_NS = "{http://maven.apache.org/POM/4.0.0}"


def fail(problems):
    for p in problems:
        print(f"[release] {p}", file=sys.stderr)
    sys.exit(1)


def digest(path, algorithm):
    h = hashlib.new(algorithm)
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


def tree(root):
    """Every regular file under root, as sorted relative paths; refuses anything else."""
    if os.path.islink(root) or not os.path.isdir(root):
        fail([f"{root}: not a directory"])
    found, problems = [], []
    for dirpath, dirnames, filenames in os.walk(root, followlinks=False):
        for name in dirnames:
            if os.path.islink(os.path.join(dirpath, name)):
                problems.append(f"{os.path.join(dirpath, name)}: a symlink")
        for name in filenames:
            full = os.path.join(dirpath, name)
            mode = os.lstat(full).st_mode
            if not stat.S_ISREG(mode):
                problems.append(f"{full}: not a regular file")
                continue
            rel = os.path.relpath(full, root).replace(os.sep, "/")
            if not SAFE_PATH.match(rel) or ".." in rel.split("/"):
                problems.append(f"{rel}: not a plain Maven path")
            found.append(rel)
    if problems:
        fail(problems)
    return sorted(found)


def expected_files(group, version, artifacts, pom_only=()):
    required, optional = set(), set()
    for a in artifacts:
        base = f"{group.replace('.', '/')}/{a}/{version}/{a}-{version}"
        # A POM-only module (a relocation, a bill of materials) publishes its POM alone.
        for main in (".pom",) if a in pom_only else MAIN_SUFFIXES:
            for side in SIDE_SUFFIXES:
                required.add(base + main + side)
            for side in OPTIONAL_SUFFIXES:
                optional.add(base + main + side)
    return required, optional


def check_pom(path, group, artifact, version):
    problems = []
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as e:
        return [f"{path}: not XML ({e})"]

    def text(*names):
        node = root
        for n in names:
            node = node.find(POM_NS + n) if node is not None else None
        return (node.text or "").strip() if node is not None else ""

    for field, want in (("groupId", group), ("artifactId", artifact), ("version", version)):
        if text(field) != want:
            problems.append(f"{path}: {field} is '{text(field)}', not '{want}'")
    # What Central refuses a component without.
    for names in (("name",), ("description",), ("url",), ("scm", "url"), ("scm", "connection")):
        if not text(*names):
            problems.append(f"{path}: no {'/'.join(names)}")
    licenses = root.find(POM_NS + "licenses")
    if licenses is None or not any(
        "Apache" in ((l.findtext(POM_NS + "name") or "") + (l.findtext(POM_NS + "url") or ""))
        for l in licenses.findall(POM_NS + "license")
    ):
        problems.append(f"{path}: no Apache licence")
    developers = root.find(POM_NS + "developers")
    if developers is None or not developers.findall(POM_NS + "developer"):
        problems.append(f"{path}: no developer")
    return problems


def write_ledger(repo, files, ledger):
    with open(ledger, "w", encoding="ascii", newline="\n") as f:
        for rel in files:
            f.write(f"{digest(os.path.join(repo, rel), 'sha256')}  {rel}\n")


def write_zip(repo, files, target):
    # Stored, not deflated: the jars are compressed already, and stored entries do not depend
    # on the zlib build, so the same tree gives the same bytes on any machine.
    with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_STORED) as z:
        for rel in files:
            info = zipfile.ZipInfo(rel, date_time=ZIP_TIME)
            info.create_system = 3
            info.external_attr = (stat.S_IFREG | 0o644) << 16
            info.compress_type = zipfile.ZIP_STORED
            with open(os.path.join(repo, rel), "rb") as f:
                z.writestr(info, f.read())


def read_ledger(ledger):
    entries, problems = {}, []
    with open(ledger, encoding="ascii") as f:
        for n, line in enumerate(f, 1):
            m = re.match(r"^([0-9a-f]{64})  (\S+)\n$", line)
            if not m or not SAFE_PATH.match(m.group(2)) or ".." in m.group(2).split("/"):
                problems.append(f"{ledger}:{n}: not a ledger line")
                continue
            if m.group(2) in entries:
                problems.append(f"{ledger}:{n}: {m.group(2)} twice")
            entries[m.group(2)] = m.group(1)
    if problems:
        fail(problems)
    if list(entries) != sorted(entries):
        fail([f"{ledger}: not sorted by path"])
    return entries


def bundle(args):
    artifacts = args.artifacts.split()
    if os.path.lexists(args.dest):
        fail([f"{args.dest} exists already: a bundle is built once"])
    files = tree(args.source)
    pom_only = args.pom_only.split()
    if any(a not in artifacts for a in pom_only):
        fail([f"--pom-only names an artifact that is not in --artifacts: {args.pom_only}"])
    required, optional = expected_files(args.group, args.version, artifacts, pom_only)
    problems = [f"missing from the bundle: {p}" for p in sorted(required - set(files))]
    problems += [f"not expected in the bundle: {p}" for p in files if p not in required | optional]
    for rel in files:
        full = os.path.join(args.source, rel)
        for algorithm in ("md5", "sha1"):
            side = f"{full}.{algorithm}"
            if os.path.isfile(side):
                with open(side, encoding="ascii", errors="replace") as f:
                    recorded = f.read().split()
                if not recorded or recorded[0].lower() != digest(full, algorithm):
                    problems.append(f"{rel}.{algorithm} does not match {rel}")
    for a in artifacts:
        pom = os.path.join(args.source, args.group.replace(".", "/"), a, args.version, f"{a}-{args.version}.pom")
        if os.path.isfile(pom):
            problems += check_pom(pom, args.group, a, args.version)
    if problems:
        fail(problems)

    repo = os.path.join(args.dest, "repository")
    os.makedirs(args.dest, mode=0o755)
    for rel in files:
        target = os.path.join(repo, rel)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(os.path.join(args.source, rel), "rb") as src, open(target, "xb") as dst:
            dst.write(src.read())
        os.chmod(target, 0o644)
    if tree(repo) != files:
        fail([f"{repo}: the copy differs from {args.source}"])
    write_ledger(repo, files, os.path.join(args.dest, "SHA256SUMS"))
    zip_path = os.path.join(args.dest, f"{args.name}.zip")
    write_zip(repo, files, zip_path)
    with open(f"{zip_path}.sha256", "w", encoding="ascii", newline="\n") as f:
        f.write(f"{digest(zip_path, 'sha256')}  {args.name}.zip\n")
    print(f"{len(files)} files, {sum(1 for p in files if p.endswith('.asc'))} signatures")


def verify(args):
    ledger = read_ledger(os.path.join(args.dest, "SHA256SUMS"))
    repo = os.path.join(args.dest, "repository")
    files = tree(repo)
    problems = [f"not in the ledger: {p}" for p in files if p not in ledger]
    problems += [f"missing: {p}" for p in ledger if p not in files]
    problems += [f"changed: {p}" for p in ledger if p in files and digest(os.path.join(repo, p), "sha256") != ledger[p]]
    sums = [n for n in os.listdir(args.dest) if n.endswith(".zip.sha256")]
    if len(sums) != 1:
        problems.append(f"{args.dest}: needs exactly one .zip.sha256")
    else:
        with open(os.path.join(args.dest, sums[0]), encoding="ascii") as f:
            m = re.match(r"^([0-9a-f]{64})  (\S+\.zip)\n$", f.read())
        if not m or m.group(2) != sums[0][: -len(".sha256")]:
            problems.append(f"{sums[0]}: not a checksum line for its zip")
        else:
            z = os.path.join(args.dest, m.group(2))
            if not os.path.isfile(z) or os.path.islink(z) or digest(z, "sha256") != m.group(1):
                problems.append(f"{m.group(2)} does not match {sums[0]}")
            else:
                with zipfile.ZipFile(z) as zf:
                    names = zf.namelist()
                    if names != list(ledger):
                        problems.append(f"{m.group(2)}: entries differ from the ledger")
                    else:
                        for n in names:
                            if hashlib.sha256(zf.read(n)).hexdigest() != ledger[n]:
                                problems.append(f"{m.group(2)}: {n} differs from the ledger")
    if problems:
        fail(problems)
    # The upload order: every file first, each module's POM last, so a registry never sees a
    # POM whose files are not there yet.
    for p in [p for p in ledger if not p.endswith(".pom")] + [p for p in ledger if p.endswith(".pom")]:
        print(p)


def pins(args):
    own = set(args.own.split())
    for path in args.poms:
        root = ET.parse(path).getroot()
        for dep in root.iter(POM_NS + "dependency"):
            if (dep.findtext(POM_NS + "groupId") or "").strip() == "dev.taihei":
                artifact = (dep.findtext(POM_NS + "artifactId") or "").strip()
                version = (dep.findtext(POM_NS + "version") or "").strip()
                if artifact in own:
                    # Another module of this library: it is released with this one, in the same
                    # bundle, so it is never a pin on an earlier release.
                    if version != (root.findtext(POM_NS + "version") or "").strip():
                        fail([f"{path}: dev.taihei dependency '{artifact}' '{version}' is not this release's own version"])
                    continue
                if not re.match(r"^[a-z0-9_.-]+$", artifact) or not re.match(r"^[0-9]+\.[0-9]+\.[0-9]+$", version):
                    fail([f"{path}: dev.taihei dependency '{artifact}' '{version}' is not a released version"])
                print(artifact, version)


def no_repos(args):
    with open(args.json) as f:
        shown = json.load(f)
    if not isinstance(shown, dict) or not shown:
        fail([f"{args.json}: no modules shown"])
    named = {k: v for k, v in shown.items() if v}
    if named:
        fail([f"{k} names {v}: an open repository resolves from Maven Central only" for k, v in named.items()])


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    b = sub.add_parser("bundle")
    for opt in ("--source", "--dest", "--group", "--version", "--artifacts", "--name"):
        b.add_argument(opt, required=True)
    b.add_argument("--pom-only", default="")
    v = sub.add_parser("verify")
    v.add_argument("--dest", required=True)
    p = sub.add_parser("pins")
    p.add_argument("--own", default="")
    p.add_argument("poms", nargs="+")
    n = sub.add_parser("no-repos")
    n.add_argument("json")
    args = parser.parse_args()
    {"bundle": bundle, "verify": verify, "pins": pins, "no-repos": no_repos}[args.command](args)


if __name__ == "__main__":
    main()
