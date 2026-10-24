# CLAUDE.md

Guidance for Claude Code in this repository: **Taihei Tsuji** (`dev.taihei::taihei-tsuji` and the
JVM-only `dev.taihei::taihei-tsuji-tools`; taihei-i18n up to 4.x, on branch `4.0.x`): languages,
typed message families with per-language catalogues, ICU MessageFormat 2.0 and locale-aware
formatting for Scala 3 on the JVM and Scala.js. Part of the Taihei Framework; the family's rules
come from the parent folder's `CLAUDE.md` when this repo is checked out under `~/Projects/Taihei`.

## Commands

```bash
make validate          # repo-guard, release-test, scalafix, scalafmt, compile, test and MiMa
make repo-guard        # Maven Central only: no other repository, every dev.taihei pin on Central
make mima              # binary compatibility with the newest 5.x release tag on Maven Central
make test              # ./mill __.test.testForked (JS tests run under Node)
TAIHEI_TEST_JAVA_HOME=<a JDK 17> ./mill tsuji.jvm.test.testForked   # the JVM tests on Java 17
make publish-dry-run   # the bundle Maven Central would receive, written to out/
make release-prepare TYPE=patch|minor|major EMBARGO=0|1   # then review RELEASE_x_y_z.md;
                       # EMBARGO=1 when the ticket has the label embargo (read it with fude first)
make release-finalize  # GitHub Packages and the read-back: only with the maintainer's yes
make release-github    # after the push: the bundle on an immutable GitHub Release
```

Releases follow ADR 0003: one signed bundle, uploaded unchanged to this repository's GitHub
Packages registry, read back, tagged and pushed, kept on an immutable GitHub Release, and taken
to Maven Central by the next release train in nakama. A security fix (`EMBARGO=1`) goes to
Central first with `make publish`. The publish token lives in `~/.taihei-packages-publish.env`,
the read token in Coursier's credentials file; never read or print either.

## Layout

- `shared/src/dev/taihei/tsuji/`: `Language`, `MessageFamily` and `MessageDefinition`, the macro
  (`MessageMacros`), `Message`/`Text`/`Displayable`, `MessageArg`, `MessageKey`, `Catalogue`,
  `Catalogues`, `MessageFormat2`, `CoreMessages`; `legacy/` the deprecated 4.x `Message`.
- `shared/resources/META-INF/taihei-tsuji/`: CoreMessages' Dutch and French catalogues (JVM jar).
- `jvm/src/…` and `js/src/…`: the private platform formatters, `ClasspathCatalogues` (JVM) and
  `CatalogueLoader` (Scala.js).
- `tools/src/…`: `taihei-tsuji-tools`, `ExportCatalogues`.
- `shared/test/src/…`: zio-test specs, run on both platforms; `jvm/test` and `js/test` for what
  exists on one platform only.
- `docs/skill/`: the API reference for the library's Claude Code skill.

## Rules

- Whatever is in `shared/` compiles on Scala.js: no JVM-only API there.
- Java 17 is the JVM floor: the JVM modules compile with `-java-output-version:17`, and CI runs
  the JVM tests on Java 17 too.
- Catalogue files and JSON messages are input the application did not compile: bound sizes and
  nesting, never throw, test with adversarial input.
- MiMa compares within the current major line (`release.majorLine` in `build.mill`).
- The public API follows semantic versioning; a break is a major release with a migration section
  in the README.
- Every dependency comes from Maven Central under an Apache-compatible licence; the build needs no
  credentials.
- Work is tracked in Jira project TAIHEI; commits are `TAIHEI-12: subject` with a body that explains the
  change to a reader without access to the ticket.
