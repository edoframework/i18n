# CLAUDE.md

Guidance for Claude Code in this repository: **Taihei Tsuji** (`dev.taihei::taihei-tsuji` and the
JVM-only `dev.taihei::taihei-tsuji-tools`; taihei-i18n up to 4.x, on branch `4.0.x`): languages,
typed message families with per-language catalogues, ICU MessageFormat 2.0 and locale-aware
formatting for Scala 3 on the JVM and Scala.js. Part of the Taihei Framework; the family's rules
come from the parent folder's `CLAUDE.md` when this repo is checked out under `~/Projects/Taihei`.

## Commands

```bash
make validate          # scalafix, scalafmt, compile, test and MiMa, JVM and Scala.js
make mima              # binary compatibility with the newest release of the current major
make test              # ./mill __.test.testForked (JS tests run under Node)
make publish-dry-run   # the bundle Maven Central would receive, written to out/
make release-prepare TYPE=patch|minor|major   # then review RELEASE_x_y_z.md
make release-finalize  # publishes to Maven Central: only with the maintainer's yes
```

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
- Catalogue files and JSON messages are input the application did not compile: bound sizes and
  nesting, never throw, test with adversarial input.
- MiMa compares within the current major line (`release.majorLine` in `build.mill`).
- The public API follows semantic versioning; a break is a major release with a migration section
  in the README.
- Every dependency comes from Maven Central under an Apache-compatible licence; the build needs no
  credentials.
- Work is tracked in Jira project TAIHEI; commits are `TAIHEI-12: subject` with a body that explains the
  change to a reader without access to the ticket.
