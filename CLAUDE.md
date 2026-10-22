# CLAUDE.md

Guidance for Claude Code in this repository: **Taihei Framework i18n** (`dev.taihei::taihei-i18n`),
languages, translatable messages, ICU MessageFormat 2.0 and locale-aware formatting for Scala 3 on
the JVM and Scala.js. Part of the Taihei Framework; the family's rules come from the parent folder's
`CLAUDE.md` when this repo is checked out under `~/Projects/Taihei`.

## Commands

```bash
make validate          # scalafix, scalafmt, compile, test and MiMa, JVM and Scala.js
make mima              # binary compatibility with the newest release tag only
make test              # ./mill __.test.testForked (JS tests run under Node)
make publish-dry-run   # the bundle Maven Central would receive, written to out/
make release-prepare TYPE=patch|minor|major   # then review RELEASE_x_y_z.md
make release-finalize  # publishes to Maven Central: only with the maintainer's yes
```

## Layout

- `shared/src/dev/taihei/i18n/`: `Language`, `Message`, `MessageFormat2`, `CoreMessages`.
- `jvm/src/…` and `js/src/…`: the platform formatters behind `PlatformNumberFormatter`,
  `PlatformDateTimeFormatter`, `PlatformLocales`, `PlatformLanguageNames`.
- `shared/test/src/…`: zio-test specs, run on both platforms.
- `docs/skill/`: the API reference for the library's Claude Code skill.

## Rules

- Whatever is in `shared/` compiles on Scala.js: no JVM-only API there.
- The public API follows semantic versioning; a break is a major release with a migration section
  in the README.
- Every dependency comes from Maven Central under an Apache-compatible licence; the build needs no
  credentials.
- Work is tracked in Jira project TAIHEI; commits are `TAIHEI-12: subject` with a body that explains the
  change to a reader without access to the ticket.
