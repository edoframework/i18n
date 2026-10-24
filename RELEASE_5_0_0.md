# Release 5.0.0

**Release Date:** 2026-10-23

taihei-i18n is now **Taihei Tsuji**. The new coordinates are `dev.taihei::taihei-tsuji` and the new package is `dev.taihei.tsuji`. This is a major release. It replaces the 4.x `Message`, which carried every translation inside each value, with **typed message families**. A macro checks those families when the code compiles. Their translations live inline or in per-language **catalogue files**, and a `Catalogues` registry looks them up through a defined fallback chain. On the wire, a message is now only its key and typed arguments, plus by default its text in the source language.

The release also adds a JVM-only `taihei-tsuji-tools` artifact, which exports catalogues for translators and browsers. Scala.js applications load catalogues at runtime with `CatalogueLoader`. `MessageFormat2` gains `validate` and more functions. All input read from JSON or catalogue files is bounded in size, nesting and hash collisions. The 4.x `Message` remains as the deprecated `dev.taihei.tsuji.legacy.Message` until 6.0.0, so a first migration step can be a change of imports. Stored 4.x JSON stays readable.

## Highlights

- **New name and coordinates**: `dev.taihei::taihei-tsuji` and the package `dev.taihei.tsuji`. The 4.x line keeps its name on branch `4.0.x` and receives security fixes for 12 months.
- **Typed message families**: `message[(count: Int, on: LocalDate)](...)` checks the MF2 syntax, variables, function and argument types, and `.match` defaults at compile time. No build plugin is needed.
- **Catalogues and fallback chain**: one JSON file per language. The chain tries the exact tag, then the tag with subtags removed, then configured fallbacks, then the source language. Showing a message never throws.
- **Classpath and browser loading**: catalogues load from `META-INF/taihei-tsuji/` on the JVM and through `CatalogueLoader.load` over `fetch` on Scala.js.
- **`taihei-tsuji-tools`**: `ExportCatalogues` writes one merged catalogue per language for the families found on the classpath.
- **Compact wire format**: `{"key":...,"args":{...},"text":...}`, with typed JSON arguments. The 4.x shape still reads.
- **Bounded input**: limits on size, depth, element counts, output length and shared hash codes for catalogues and JSON messages.

## New Features

### Message families

A family groups messages under a key prefix and declares a source language (English by default). Each message is a `val`, and its key is `prefix.valName`.

```scala
import java.time.LocalDate
import dev.taihei.tsuji.*
import dev.taihei.tsuji.Language.*

object Orders extends MessageFamily("order"):
  val cancelled = message("Your order was cancelled", Dutch -> "Uw bestelling werd geannuleerd")
  val shipped   = message[(count: Int, on: LocalDate)](
    ".match {$count :plural} when one {One item shipped on {$on :date dateStyle=long}} " +
      "when * {{$count} items shipped on {$on :date dateStyle=long}}",
  )

given Catalogues = Catalogues(Seq(Orders))
Orders.shipped(count = 3, on = LocalDate.of(2026, 10, 23)).localize(Language("de"))
// 3 items shipped on 23. Oktober 2026
```

The macro checks the template and every literal inline translation:

- the template is valid MF2;
- every variable is an argument or an earlier `.local`;
- `:number`, `:integer` and `:plural` apply only to numbers;
- `:datetime`, `:date` and `:time` apply only to matching `java.time` types;
- every `.match` has a `*` variant;
- argument names are valid.

An unused argument produces a warning. `message` compiles only as a `val` in the body of a `MessageFamily` object. Arguments use the `MessageArg[A]` type class. Instances exist for primitives, `String`, `LocalDate`, `LocalDateTime`, `LocalTime`, `Language`, nested `Message`, `Text` and `Displayable`, and for `Option` and `List` of these. Your own types go through `MessageArg.as`.

### Catalogues, fallback chain and verification

```scala
given catalogues: Catalogues = Catalogues(
  families  = Seq(Orders) ++ CoreMessages.families,
  fallbacks = Map(Language("lb") -> Seq(Language("de"))),
)
catalogues.add(Catalogue(Language("de"), Map(Orders.cancelled.key -> "Storniert")))
```

- **File format**: `{"format":1,"language":"de","messages":{...}}`. `Catalogue.parse` never throws, and it lists bad entries in `problems`.
- **Precedence**: in a target language, a catalogue file wins over an inline translation. In the source language, the template in the code wins.
- **Number and date formatting**: always uses the reader's language, even when the template comes from a fallback.
- **Registry API**: `onAdded` notifies listeners when a catalogue is added, `problems` collects issues, and `fallbackChain` shows the lookup order.
- **Thread safety**: the registry is safe to use from several threads.
- **Verification in tests**: `Catalogues.verify(families, catalogues, required)` checks every template against its definition. It also reports unknown keys, missing keys in required languages, and duplicate prefixes and keys. On the JVM, `ClasspathCatalogues.verify` runs the same check over the files on the classpath.

### Loading catalogues on JVM and Scala.js

- **JVM**: a family's catalogue for a language is read once, from `META-INF/taihei-tsuji/<prefix>/<tag>.json`, the first time that language is needed. Files are limited to 16 MB and must be UTF-8. The registry remembers only the latest 4096 misses.
- **Scala.js**: `CatalogueLoader.load(catalogues, language, url)` fetches a file and returns a `Future[List[CatalogueProblem]]` that never fails. It refuses an oversized declared length before reading and also counts the body as it reads it. It does not depend on Laminar or an effect library.

### `taihei-tsuji-tools`: ExportCatalogues

```
dev.taihei.tsuji.tools.ExportCatalogues --out <dir> [--scan <package>]... [--family <object>]...
```

- **Discovery**: finds families on the classpath, including library families such as `CoreMessages`.
- **Output**: writes one `<dir>/<tag>.json` per language. The source-language file holds the source templates; other languages use classpath files over inline translations.
- **Bad templates**: templates that fail their checks are left out of the files and printed.
- **Exit status**: 1 when there are problems, 2 for usage errors. `--scan` accepts only a package name.

### JSON wire format

```json
{"key":"order.shipped","args":{"count":3,"on":"2026-10-23"},"text":"3 items shipped on October 23, 2026"}
```

- **Writing without text**: `Message.encoder(withText = false)` omits `text`.
- **Decoding needs a registry**: the decoders need a `given Catalogues` to look up argument types by key.
- **Unknown keys**: a key the registry does not know reads as its `text`, with its arguments kept as they came.
- **Text values**: a `Text` travels as `{"text":...,"language":...}`.
- **4.x compatibility**: 4.x JSON (`args`, `namedArgs`, `translationTemplates`) reads as a legacy message under the key `legacy`.

### MessageFormat2 additions

- **Validation**: `MessageFormat2.validate(template): List[TemplateError]` reports errors with their positions, escaped and shortened so they are safe to log.
- **New functions**: `:integer`, `:date`, `:time` and `:string`.
- **New declarations**: `.local` (both `.local {$x = ...}` and `.local $x = {...}`) alongside `.let`.
- **Languages as values**: a `Language` argument shows as its name in the reader's language, and a `.match` on a `Language` compares its tag.

## Changelog

### Added
- `MessageFamily`, `MessageDefinition`, the compile-time `message` macro (`MessageMacros`), `MessageKey`, and the `MessageArg[A]` type class with `MessageArg.as`.
- `Displayable`, the common supertype of `Message`, `Text` and `legacy.Message`, shown with `localize(language)(using Catalogues)`.
- `Text` and `Text.of(throwable)` for text in a single language.
- `Catalogue`, `CatalogueProblem`, the `Catalogues` registry with configurable fallbacks, `register`, `add`, `onAdded`, `problems` and `fallbackChain`, and `Catalogues.verify`.
- `ClasspathCatalogues.read` and `ClasspathCatalogues.verify` (JVM).
- `CatalogueLoader.load` (Scala.js).
- The `taihei-tsuji-tools` artifact with `ExportCatalogues`.
- `MessageFormat2.validate` and `TemplateError`.
- MF2 functions `:integer`, `:date`, `:time` and `:string`, and the `.local` declaration.
- New `CoreMessages` members:
  - the `CoreMessages.Languages` family (`name(language)`);
  - `Validation.failed` and `Validation.fieldError`;
  - `Exceptions.revisionMismatchBetween`;
  - `CoreMessages.families`.
- Dutch and French catalogue files for `CoreMessages`, shipped in the JVM jar under `META-INF/taihei-tsuji/taihei.*`.
- Input bounds:
  - nested messages at most 8 deep;
  - lists at most 10,000 elements;
  - catalogues at most 4M characters, 100,000 messages and 16 levels of nesting;
  - templates at most 64K characters;
  - one template formats to at most 1M characters, and one message tree to at most 4M;
  - arguments kept for an unknown key at most 16 levels and 100,000 values;
  - catalogues and kept argument objects in which more than 16 keys share a hash code are refused.
- Migration guide in the README and a new `docs/skill/reference/migration.md`, plus a new `catalogues.md` skill reference.

### Changed
- **Breaking: renamed** from `dev.taihei::taihei-i18n` to `dev.taihei::taihei-tsuji`, and from the package `dev.taihei.i18n` to `dev.taihei.tsuji`. The catalogue resource path is `META-INF/taihei-tsuji/`. There is no relocation, so both lines can share a classpath, but their `Language` types are different types.
- **Breaking:** the 4.x `Message` moved to `dev.taihei.tsuji.legacy.Message` (with `legacy.MessageArg`). Both are unchanged in behaviour and deprecated until 6.0.0.
- **Breaking:** `Message` is now a key plus typed arguments. The decoder and `JsonCodec[Message]` need a `given Catalogues`. `toString` shows the family's source language.
- **Breaking:** `CoreMessages` groups are now `MessageFamily` objects with named, typed arguments.
  - `Exceptions.revisionMismatch(expected, actual)` is now `revisionMismatchBetween(expected, actual)`.
  - `Validation.formatErrors` takes `Map[String, List[Displayable]]` and shows each field error in the reader's language.
  - `Validation.dateMinimum` and `Validation.dateMaximum` format their date with `:date`.
  - Dutch and French are now catalogue files, so a browser shows English until it loads them.
- **Breaking:** `Language.label` returns the new `Message`, shown in any reader's language. Other `Language` APIs are unchanged.
- **Breaking:** internals that were public are now package-private:
  - `PlatformNumberFormatter`, `PlatformDateTimeFormatter`, `PlatformLocales` and `PlatformLanguageNames`;
  - the `Intl` facades;
  - the `MessageFormat2` parser, AST and `Context`.
- `CatalogueProblem` and `TemplateError` can no longer be constructed outside the library: read their fields, or match on them.
- `legacy.Message` JSON decoding enforces the 4.0.x bounds: at most 1000 templates and 1000 arguments, and no template, argument or argument name longer than 65,536 characters. Entries are counted before they are hashed.
- MiMa compares binary compatibility only within the current major line (`release.majorLine = 5.0.0`). The first baseline is taihei-tsuji 5.0.0.
- Published jars hold resources only from folders the build names. There are no default resource folders.
- The compiler options `-WunstableInlineAccessors` and `-Wconf:msg=Unstable inline accessor:e` make an unstable inline accessor a compile error. This keeps the macro's expansion stable in the bytecode.
- POM description, URL and SCM now point to `taiheiframework/tsuji`.
- Release process:
  - `publish`, `publish-dry-run` and `release-preflight` refuse stray, ignored or hidden-change files in source and build folders;
  - they refuse to publish any modules other than the three named ones;
  - they refuse versions that are not `X.Y.Z`;
  - the upload is one named bundle, and the Mill daemon is shut down afterwards;
  - preflight checks all three artifacts on Central with a 30 s timeout and stops on any answer other than 200 or 404;
  - `release-finalize` publishes only the commit `release-prepare` validated, plus its notes;
  - patch and minor releases require the previous release as the MiMa baseline.
- CI workflows check out with `persist-credentials: false`.

### Fixed
- Functions inside `.match` variant patterns (for example `when * {{$n} on {$d :date}}`) are now formatted. Previously only plain `{$var}` placeholders worked there.
- The MF2 parser no longer creates a nested parser per variant. It records every recovery as an error and never throws while formatting.
- When the classpath sees requests for many language tags that have no files, a supported language still loads.
- `CatalogueLoader` catches rejected promises from the body and stream cancellations it starts.

## Module Changes

- `tsuji.jvm`, published as `taihei-tsuji` (`taihei-tsuji_3`): the Mill module was renamed from `i18n.jvm`. It now ships `shared/resources` (the CoreMessages catalogues) and adds `ClasspathCatalogues`.
- `tsuji.js`, published as `taihei-tsuji` (`taihei-tsuji_sjs1_3`): the Mill module was renamed from `i18n.js`. It adds `CatalogueLoader`.
- `tsuji.tools`, published as `taihei-tsuji-tools` (`taihei-tsuji-tools_3`): new and JVM-only. It depends on `tsuji.jvm`.
- Test modules gain platform-specific sources (`jvm/test`, `js/test`) and JVM test resources.

## Compatibility

- **Scala:** 3.9.0 (LTS line). Scala.js 1.22.0.
- **Dependencies:**
  - zio-json 1.1.1;
  - scala-java-time and scala-java-time-tzdb 2.8.0 (Scala.js only);
  - ZIO / zio-test 2.1.27 (tests only).
- **Build plugins:** mill-scalafix 0.6.2 and mill-mima 0.2.2.
- **Binary compatibility:** 5.0.0 starts a new artifact and a new major line, so there is no MiMa baseline for it. 5.x patch and minor releases are checked against the current 5.x release.
- **Migration from taihei-i18n 4.x:**
  1. Change the dependency and replace `dev.taihei.i18n` with `dev.taihei.tsuji`.
  2. Import `dev.taihei.tsuji.legacy.Message` explicitly.
  3. Move messages to families one family at a time. To silence the legacy deprecation warnings meanwhile, add `-Wconf:cat=deprecation&origin=dev\.taihei\.tsuji\.legacy\..*:s`.
- **JSON during a rollout:** 5.x reads 4.x messages, but 4.x readers cannot read 5.x messages. Upgrade readers first, or keep writing legacy messages until they are upgraded.
