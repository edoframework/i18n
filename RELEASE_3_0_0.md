# Release 3.0.0

**Release Date:** 2026-10-22

3.0.0 is the first release of the library as Edo Framework i18n, published on Maven Central as `com.edoframework::edo-i18n`. The API is the same as `fish.genius::genius-i18n:2.0.0`, with three differences. The package is now `com.edoframework.i18n`, the library is compiled with Scala 3.9 (LTS), and the licence is now Apache 2.0 instead of MIT. Most users need only a new dependency coordinate and new imports.

The build now resolves everything from Maven Central and needs no credentials, so anyone can clone and build it. Releases are versioned from git tags and run from the Makefile, and they are signed by the Edo Framework release key. CI runs with read-only permissions and pins its actions to commit hashes, and a DCO check now runs on pull requests.

## Highlights

- **New coordinates:** `com.edoframework::edo-i18n` on Maven Central, for the JVM and for Scala.js (`edo-i18n_sjs1`).
- **New package:** `genius.i18n` is now `com.edoframework.i18n`.
- **Scala 3.9 LTS:** compiled with Scala 3.9.0 and Scala.js 1.22.0.
- **Apache License 2.0:** with a `NOTICE`. Versions up to and including 2.0.0 stay available under the MIT License.
- **Builds without credentials:** the build no longer uses a private Maven repository or a private Mill plugin.

## Breaking Changes

- The Maven coordinates changed from `fish.genius::genius-i18n` to `com.edoframework::edo-i18n`.
- The package changed from `genius.i18n` to `com.edoframework.i18n`. Every import needs updating.
- The library is compiled with Scala 3.9.0, so your build needs Scala 3.9 or later.
- The transitive `zio-json` dependency moved from 0.9.2 to 1.1.1.
- The `IllegalArgumentException` from `Language.valueOf` for an unknown name now has the new package in its message: `enum com.edoframework.i18n.Language has no case with name: …`.

## New Features

### Migrating from genius-i18n

Change the dependency:

```scala
mvn"com.edoframework::edo-i18n:3.0.0"       // Mill, JVM
mvn"com.edoframework::edo-i18n::3.0.0"      // Mill, Scala.js
"com.edoframework" %%% "edo-i18n" % "3.0.0" // sbt
```

Then change your imports:

```scala
// before
import genius.i18n.*
// after
import com.edoframework.i18n.*
```

`Language`, `Message`, `MessageFormat2`, `CoreMessages` and the platform formatters are otherwise unchanged.

### Releases from git tags

The build now takes its version from git. An exact `v`-tag on a clean tree is the release version, and anything else is a SNAPSHOT of the last tag. Releases run from the Makefile with `make release-prepare TYPE=patch|minor|major`, `make release-finalize` and `make publish-dry-run`. They are published through Mill's `SonatypeCentralPublishModule` and signed by `Edo Framework Releases <releases@edoframework.com>`, fingerprint `5026 B299 6DDB 697E C463 0BCF 14A7 33E2 A507 702F`.

## Changelog

### Added
- `NOTICE` file with the Innovenso BV copyright and the licence history of earlier versions.
- A DCO workflow that checks every commit in a pull request has a `Signed-off-by` line matching its author.
- `make release-prepare`, `make release-finalize`, `make release-abort` and `make publish-dry-run` for releasing to Maven Central.
- A migration section in the README for users of `genius-i18n`.

### Changed
- The artifact is now `com.edoframework::edo-i18n` and the package `com.edoframework.i18n`.
- The licence changed from MIT to Apache 2.0, copyright Innovenso BV.
- Scala 3.8.3 to 3.9.0, Scala.js 1.21.0 to 1.22.0, ZIO 2.1.26 to 2.1.27, zio-json 0.9.2 to 1.1.1, scala-java-time 2.6.0 to 2.8.0.
- Mill 1.1.5 to 1.3.1, mill-scalafix 0.6.0 to 0.6.2.
- The POM now points to `github.com/edoframework/i18n`, with Innovenso BV as developer.
- CI runs with `contents: read` permissions and pins every GitHub Action to a commit hash.
- The `docs/skill/` API reference was updated for the new package and coordinates.

### Fixed
- The build no longer needs credentials for a private repository. It resolves only from Maven Central.

## Module Changes

- `edo-i18n` (JVM): renamed from `genius-i18n` and moved to `com.edoframework.i18n`. The `java.time` / `java.text` platform formatters did not change otherwise.
- `edo-i18n_sjs1` (Scala.js): renamed from `genius-i18n_sjs1` and moved to `com.edoframework.i18n`. The `Intl`-based platform formatters did not change otherwise.

## Compatibility

- Scala 3.9.0 (LTS), so it needs Scala 3.9 or later
- Scala.js 1.22.0
- zio-json 1.1.1
- scala-java-time 2.8.0 and scala-java-time-tzdb 2.8.0 (Scala.js only)
- ZIO 2.1.27 (zio-test, tests only)
- Built with Mill 1.3.1
