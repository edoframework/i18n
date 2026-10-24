# Release 5.0.1

**Release Date:** 2026-10-24

This patch release makes Taihei Tsuji work on Java 17, the oldest Java it supports. The public API
is unchanged and the release is binary compatible with 5.0.0 (MiMa checks it).

## The bug

On the JVM, the formatters built the locale for the bare tag `nl` (`Language.Dutch`) with
`java.util.Locale.of("nl", "NL")`, an API that exists only from Java 19, while the jar is Java 17
bytecode. On Java 17 and 18 the first `:number`, `:integer`, `:date`, `:time` or `:datetime`
formatted for `Language.Dutch` threw `java.lang.NoSuchMethodError`. That is a `LinkageError`, which
`scala.util.control.NonFatal` does not catch, although showing a message is meant never to throw.

- **Affected versions:** `dev.taihei::taihei-tsuji` 5.0.0, and `dev.taihei::taihei-i18n` 4.0.0 to
  4.0.2 (`dev.taihei.i18n.PlatformLocales`).
- **Not affected:** Scala.js, and every JVM from Java 19 on.
- **Workaround** for a version you cannot upgrade: run on Java 19 or later.
- **Fix:** upgrade to 5.0.1. The locale is now built with `Locale.forLanguageTag("nl-NL")`.

## How Java 17 stays supported

- The JVM modules (`taihei-tsuji` and `taihei-tsuji-tools`) compile with `-java-output-version:17`:
  scalac checks every call against the Java 17 API, so a newer JDK API fails the build instead of
  failing at run time on a user's Java 17.
- CI runs the JVM tests on Java 17 as well as on Java 25.
- The README states the requirement: JVM, Java 17 or later.

Formats come from the platform's locale data, which differs between JDKs: a medium Dutch date is
`20 okt. 2026` on Java 17 and `20 okt 2026` on Java 21 and later and in Scala.js.

## Where to get it

5.0.1 is published to this repository's GitHub Packages registry first
(`https://maven.pkg.github.com/taiheiframework/tsuji`), and reaches Maven Central, as the same
signed files, with the next release train. A build that resolves from Maven Central only gets
5.0.1 once it is there. The GitHub Release of `v5.0.1` holds the signed bundle and its
`SHA256SUMS`, the SHA-256 of every file both registries serve.

```scala
// Mill
mvn"dev.taihei::taihei-tsuji:5.0.1"
// sbt
"dev.taihei" %% "taihei-tsuji" % "5.0.1"
```

## Changelog

### Fixed
- `Language.Dutch` on Java 17 and 18: `:number`, `:integer`, `:date`, `:time` and `:datetime` no
  longer throw `NoSuchMethodError`.

### Added
- Tests: a Dutch message with `:number`, `:integer`, `:date`, `:time` and `:datetime`; the JVM
  locales for `nl` and for a tag with a region; per-platform expectations for locale data.
- A CI job that runs the JVM tests on Java 17 (`TAIHEI_TEST_JAVA_HOME` points the test modules at
  another JDK; it affects only the tests).

### Changed (build and release, no effect on the published API)
- Releases follow the GitHub Packages route: `release-finalize` builds and signs one bundle, uploads
  it unchanged to GitHub Packages and reads every file back; `release-github` keeps it on an
  immutable GitHub Release; Maven Central follows with the release train. `make publish` is only
  for a security fix under embargo, which goes to Central first (`release-prepare EMBARGO=1`).
- `make validate` also runs `repo-guard` (the build resolves from Maven Central only, and every
  `dev.taihei` pin is on Central) and `release-test` (the release machinery against a fake registry
  on localhost).
- The MiMa baseline is the newest tag whose version is on Maven Central.
- The release guards refuse tracked symlinks, submodules, files with a git filter and git attribute
  files, look for hidden edits in the whole repository, and cover `.mill-opts`; a fresh Mill daemon
  runs every step that sees release credentials.
- CI runs the Scala.js tests on Node 24.

## Compatibility

- Scala 3.9.0 (LTS), Scala.js 1.22.0
- JVM: Java 17 or later
- ZIO 2.1.27, zio-json 1.1.1, scala-java-time 2.8.0 on Scala.js
- Binary compatible with 5.0.0
