# Release 1.0.0

**Release Date:** 2026-06-09

This is the first stable release of **genius-i18n**, a composable, type-safe internationalization library for Scala 3. Extracted from the Genius Fish platform as a standalone library under the `genius.i18n` package, it provides locale modeling, translatable messages, an ICU MessageFormat 2.0 engine, and locale-aware number/date formatting. The library is cross-compiled for both the **JVM** (on `java.time` / `java.text`) and **Scala.js** (formatting via the browser `Intl` API) behind a single shared API.

Version 1.0.0 establishes the public API: a `Language` enum, an immutable `Message` type with `zio-json` codecs, a hand-written MessageFormat 2.0 parser/evaluator, and a tri-lingual `CoreMessages` catalog of common application strings. From this release onward, positional message constructors (`with1Arg` … `with10Args`) are deprecated in favor of named placeholders via `withNamedArgs`.

## Highlights

- **Cross-platform by construction** — one API, shared sources, with only the number/date formatters specialized per platform (`java.text` on the JVM, `Intl` on Scala.js).
- **Type-safe translatable messages** — `Message` carries its translations per `Language` and renders against a target locale, with `zio-json` codecs so messages serialize cleanly across the wire.
- **ICU MessageFormat 2.0 engine** — a self-contained recursive-descent parser and evaluator supporting `.input`/`.let` declarations, `.match`/`when` selection, plurals, and `:number` / `:datetime` function annotations.
- **Batteries-included message catalog** — `CoreMessages` ships ready-made, partially tri-lingual (EN/NL/FR) strings for authentication, authorization, validation, and exceptions.

## New Features

### Language modeling

A `Language` enum (`English`, `Dutch`, `French`) carrying ISO codes and a default flag, with parsing, sampling, and a self-describing `label` message. It derives a `zio-json` codec and provides `JsonFieldEncoder`/`JsonFieldDecoder` so languages can be used as map keys in JSON.

```scala
import genius.i18n.*

Language.of("nl")          // Language.Dutch
Language.of("xx")          // Language.English (fallback)
Language.defaultLanguage   // English
Language.French.label.translate(Language.Dutch)  // "Frans"
```

### Translatable messages

`Message` holds per-`Language` templates plus positional and named arguments, and renders against a chosen locale (falling back to the default language when a translation is missing). Supported argument types are `String`, `Int`, `Long`, `Double`, `Float`, `Boolean`, and `java.time` `LocalDate` / `LocalDateTime` / `LocalTime`.

```scala
val greeting = Message.withNamedArgs(
  Language.English -> "Hello {$name}, you have {$points} points",
  Language.Dutch   -> "Hallo {$name}, u hebt {$points} punten",
  Language.French  -> "Bonjour {$name}, vous avez {$points} points",
)("name" -> "Alice", "points" -> 150)

greeting.translate(Language.English)  // Hello Alice, you have 150 points
greeting.translate(Language.French)   // Bonjour Alice, vous avez 150 points
```

### ICU MessageFormat 2.0

`MessageFormat2.format` parses and evaluates MessageFormat 2.0 templates, including `.input`/`.let` declarations, `.match` selection with `when` cases, locale-aware plural selection (with French treating 0 and 1 as `one`), and function annotations.

```scala
val tmpl =
  """.match {$count :plural}
    |when one   {You have one item}
    |when other {You have {$count} items}""".stripMargin

MessageFormat2.format(tmpl, Map("count" -> 1), "en")  // You have one item
MessageFormat2.format(tmpl, Map("count" -> 5), "en")  // You have 5 items
```

Placeholders route through cross-platform formatters — `{$value :number style=currency}` and `{$date :datetime}` resolve to `PlatformNumberFormatter` / `PlatformDateTimeFormatter`, backed by `java.text` on the JVM and `Intl` on Scala.js.

### CoreMessages catalog

A pre-built library of common messages grouped under `General`, `Authentication`, `Authorization`, `Validation`, and `Exceptions`. Validation and exception messages are largely translated into English, Dutch, and French.

```scala
CoreMessages.Validation.stringMinLength(8).translate(Language.French)
CoreMessages.Exceptions.unauthorized.translate(Language.Dutch)  // "Geen toegang"
```

## Changelog

### Added
- `Language` enum (English, Dutch, French) with ISO codes, default-language fallback, `of`/`sample`/`empty` helpers, self-describing `label`, and `zio-json` codecs plus field encoders/decoders.
- `Message` type with per-language templates, positional and named arguments over a `MessageArg` union, `translate`/`render`, and a derived `zio-json` codec.
- `MessageFormat2`: a recursive-descent parser and evaluator for ICU MessageFormat 2.0 (`.input`, `.let`, `.match`/`when`, placeholders, function annotations, locale-aware plural matching).
- `PlatformNumberFormatter` and `PlatformDateTimeFormatter` with separate JVM (`java.text`/`java.time`) and Scala.js (`Intl`) implementations.
- `CoreMessages` catalog under `General`, `Authentication`, `Authorization`, `Validation`, and `Exceptions`, with EN/NL/FR translations where applicable.
- Cross-build setup for JVM and Scala.js publishing the `genius-i18n` artifact.

### Changed
- Established named-placeholder message construction (`withNamedArgs`, `with0Args`) as the primary API.

### Deprecated
- Positional message constructors `Message.with1Arg` through `with10Args` — use `withNamedArgs` with named placeholders (`{$name}`) instead.

## Module Changes

- **`i18n.jvm`** (`genius-i18n`, JVM) — shared sources plus the `java.text`/`java.time` formatters; depends on `zio-json`.
- **`i18n.js`** (`genius-i18n`, Scala.js) — shared sources plus the `Intl`-based formatters; depends on `zio-json` and `scala-java-time` (+ tzdb) for `java.time` support.
- **`shared/`** — platform-independent `Language`, `Message`, `MessageFormat2`, and `CoreMessages`, with a test suite running on both platforms.

## Compatibility

- **Scala:** 3.8.3
- **Scala.js:** 1.21.0
- **Platforms:** JVM and Scala.js (JS tests run on Node)
- **Key dependencies:**
  - ZIO 2.1.26 (`zio-test`, `zio-test-sbt`)
  - zio-json 0.9.2
  - scala-java-time 2.6.0 (Scala.js `java.time` + tzdb)
- **License:** MIT
