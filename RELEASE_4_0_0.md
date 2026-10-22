# Release 4.0.0

**Release Date:** 2026-10-22

This is the first release of the library as Taihei Framework i18n. The library is now published as `dev.taihei::taihei-i18n`, and its package is `dev.taihei.i18n`. The API is the same as `com.edoframework::edo-i18n:3.0.0`: no type, method or behaviour changed. The new coordinates and package are a source and binary break, so this is a major release. Migrating means changing one dependency line and one import prefix.

The build now checks binary compatibility with MiMa in `make validate` and in CI. 4.0.0 is the first release under `dev.taihei`, so it has no baseline yet. From 4.0.1 on, every change will be checked against the newest release, and a binary break will fail the build unless it ships as a major.

## Highlights

- **New coordinates** - `dev.taihei::taihei-i18n` on Maven Central, published as `taihei-i18n_3` (JVM) and `taihei-i18n_sjs1_3` (Scala.js).
- **New package** - `com.edoframework.i18n` is now `dev.taihei.i18n`.
- **Same API** - `Language`, `Message`, `MessageFormat2`, `CoreMessages` and the platform formatters are unchanged.
- **Binary compatibility checks** - MiMa runs in `make validate` and in CI. It will compare future releases against 4.0.0 and later.

## Breaking Changes

### Dependency and package rename

Replace the dependency:

```scala
// before
mvn"com.edoframework::edo-i18n:3.0.0"       // Mill, JVM
mvn"com.edoframework::edo-i18n::3.0.0"      // Mill, Scala.js
"com.edoframework" %%% "edo-i18n" % "3.0.0" // sbt

// after
mvn"dev.taihei::taihei-i18n:4.0.0"          // Mill, JVM
mvn"dev.taihei::taihei-i18n::4.0.0"         // Mill, Scala.js
"dev.taihei" %%% "taihei-i18n" % "4.0.0"    // sbt
```

Replace the imports:

```scala
// before
import com.edoframework.i18n.*

// after
import dev.taihei.i18n.*
```

If you are coming from `fish.genius::genius-i18n:2.0.0`, replace `genius.i18n` with `dev.taihei.i18n`. The changes that came with 3.0.0 also apply to you: Scala 3.9 or later, `zio-json` 1.1 instead of 0.9, and the Apache License 2.0.

The JSON form of `Language` and `Message` does not change. JSON written by edo-i18n 3.0.0 reads back with 4.0.0.

The `IllegalArgumentException` from `Language.valueOf` for an unknown name now says `enum dev.taihei.i18n.Language has no case with name: …`. Code that matched on the old message text needs updating.

## Changelog

### Added
- MiMa binary compatibility check (`make mima`), part of `make validate` and run in CI. It compares against the newest `v`-tag at or after 4.0.0. You can set the baseline with `MIMA_BASELINE`, and `make release-prepare TYPE=major` turns the check off for a major release.

### Changed
- Maven coordinates: `com.edoframework::edo-i18n` is now `dev.taihei::taihei-i18n`.
- Package: `com.edoframework.i18n` is now `dev.taihei.i18n`, in shared, JVM and Scala.js sources.
- The `Language.valueOf` error message uses the new package name.
- The POM points to `https://github.com/taiheiframework/i18n` and `https://taihei.dev`.
- README, the Claude Code skill docs (`docs/skill/`) and the scalafix import grouping use the new name and package.
- The README migration section covers coming from both edo-i18n 3.0.0 and genius-i18n 2.0.0.
- `NOTICE` records that 3.0.0 was published as `com.edoframework::edo-i18n` and that versions up to 2.0.0 were published as `fish.genius::genius-i18n` under the MIT License.

## Module Changes

- `i18n.jvm`: artifact `taihei-i18n_3`, package `dev.taihei.i18n`
- `i18n.js`: artifact `taihei-i18n_sjs1_3`, package `dev.taihei.i18n`

## Compatibility

- Scala 3.9.0 (LTS). Your build needs Scala 3.9 or later.
- Scala.js 1.22.0
- zio-json 1.1.1
- scala-java-time 2.8.0 (Scala.js only, with tzdb)
- zio-test 2.1.27 (tests only)
- Built with Mill. Build plugins: mill-scalafix 0.6.2 and mill-mima 0.2.2.
- Licence: Apache 2.0. Not binary or source compatible with `com.edoframework::edo-i18n:3.0.0` because of the package change. Apart from that, the API is identical.
