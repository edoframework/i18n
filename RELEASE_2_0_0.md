# Release 2.0.0

**Release Date:** 2026-10-20

genius-i18n 2.0.0 opens `Language` to any BCP 47 language. `Language` used to be a closed enum of English, Dutch and French. It is now a final class identified by its canonical tag (`en`, `nl-BE`, `de`, `zh-Hant-TW`). `English`, `Dutch` and `French` are still available as `val`s in the companion, so `case Language.English =>` still matches and their JSON form (`"English"`) does not change. This is a major release: `ordinal` and `fromOrdinal` are gone, the compiler no longer checks a `match` on `Language` for exhaustiveness (such a match now needs a `case _`), and the class is not binary compatible with the enum, so every consumer recompiles against 2.0.0.

The number and date formatters on JVM and Scala.js now accept any language tag. A bare `en`, `nl` or `fr` still formats with the same region as before (US, NL, FR). Message lookup now falls back to the base language: a reader with `nl-BE` gets the Dutch text when no `nl-BE` text was written. French plural rules apply to every French region.

## Highlights

- **Any BCP 47 language**: `Language("de")`, `Language.parse("nl-BE")`, with tags canonicalized and invalid tags rejected.
- **Source compatible built-ins**: `Language.English`, `Dutch`, `French`, `values`, `valueOf`, `of`, `isoCode` and `isDefault` all keep working. Equality and hashing go by tag, so `Language("en") == Language.English`.
- **Language names from the platform**: any language that is not built in gets a `label` in English, Dutch, French and in its own language, from the JDK locale data or `Intl.DisplayNames`.
- **Base-language fallback in `Message`**: the exact tag first, then the base language, then English.
- **Locale-aware formatting for any tag**: on JVM and Scala.js, an unknown or invalid tag now falls back to US English formatting.

## New Features

### Open `Language` type

A language is built from a BCP 47 tag (RFC 5646). The tag is canonicalized: the language in lower case, the script in title case, the region in upper case, and `_` accepted as a separator. The primary language subtag must be two or three letters.

```scala
import genius.i18n.*

Language.parse("NL_be")       // Right(Language("nl-BE"))
Language.parse("invalid")     // Left("Not a BCP 47 language tag: 'invalid'")
Language("de")                // throws IllegalArgumentException on an invalid tag
Language.of("xx-!!")          // English: falls back to the default language

val belgianDutch = Language("nl-BE")
belgianDutch.tag              // "nl-BE"
belgianDutch.isoCode          // "nl-BE" (the full tag, not just "nl")
belgianDutch.base             // Language.Dutch
belgianDutch.locale           // java.util.Locale for nl-BE

Language("en") == Language.English  // true
```

Matching on a built-in still compiles, but the match now needs a wildcard case:

```scala
def greeting(l: Language): String = l match
  case Language.English => "Hello"
  case Language.Dutch   => "Hallo"
  case Language.French  => "Bonjour"
  case _                => "Hello"   // now required
```

### Platform language names

Languages that are not built in are named by the platform: `PlatformLanguageNames` uses the JDK on JVM and `Intl.DisplayNames` on Scala.js. If the platform does not know a language, its label falls back to the tag.

```scala
Language("de").label.translate(Language.Dutch)   // "Duits"
Language("de").label.translate(Language("de"))   // "Deutsch"
```

### Base-language fallback for messages

```scala
val hello = Message.with0Args(
  Language.English -> "Hello",
  Language.Dutch   -> "Hallo",
)

hello.translate(Language("nl-BE"))  // "Hallo": falls back from nl-BE to nl
hello.translate(Language("de"))     // "Hello": falls back to English
```

### JSON

The three built-ins are still written by name (`"English"`), so existing stored data keeps reading and older readers keep reading what 2.0.0 writes for them. Any other language is written as its tag, which a reader on 1.0.0 cannot decode: JSON holding `"de"` fails on 1.x. Both forms are read. `JsonEncoder`, `JsonDecoder`, `JsonCodec`, `JsonFieldEncoder` and `JsonFieldDecoder` instances are provided, so a `Map[Language, _]` still round-trips.

## Changelog

### Added
- `Language.parse(tag): Either[String, Language]` and `Language(tag)`, which accept any canonicalized BCP 47 tag.
- `Language#tag`, `Language#base` and `Language#locale`.
- `PlatformLanguageNames.displayName(tag, in)` on JVM (JDK locale data) and on Scala.js (`Intl.DisplayNames`).
- `PlatformLocales.javaLocale` (JVM) and `PlatformLocales.jsLocale` (JS): one shared mapping from a language tag to the formatter locale.
- JSON codecs that read both a built-in's name and any language tag.
- Tests: `OpenLanguageSpec` (parsing, canonicalization, equality, JSON, labels, fallback) and `PlatformFormatterSpec` (formatter locales for built-in, regional and invalid tags).

### Changed
- **Breaking:** `Language` is now a `final class` instead of an `enum`. `ordinal` and `fromOrdinal` are gone. A `match` on it is no longer checked for exhaustiveness and needs a `case _`. The change is not binary compatible: consumers recompile against 2.0.0.
- **Breaking for mixed versions:** a reader on 1.0.0 cannot decode a non-built-in tag such as `"de"` written by 2.0.0.
- `Language#isoCode` now returns the full tag (`"nl-BE"`). Use `base.isoCode` to get the language alone.
- `Language.of` now returns the language for any valid tag (`of("de")` was English, is now German). It returns English only for input that is not a valid tag.
- On JVM, the number and date formatters format an unrecognized tag as US English instead of using the JVM default locale. On Scala.js, any tag that `Intl` accepts is now used as is; before, every tag other than `en`, `nl` and `fr` became `en-US`.
- In `MessageFormat2`, the French rule that `one` covers 0 and 1 now applies to every French region (`fr-BE`, `fr-CA`, …).
- `make release-prepare` now refuses to run on a dirty tree. If `validate` reformats files, it stops instead of staging them silently (GFSDK-2).

### Fixed
- On Scala.js, a tag that is valid under RFC 5646 but that `Intl` rejects (for example `zh-cmn-Hans`) no longer throws a `RangeError`. It falls back to `en-US`.
- `Language` is `Serializable`.

## Module Changes

- `i18n.jvm` (`genius-i18n`): `Language` rewritten; new `PlatformLocales` and `PlatformLanguageNames`; `PlatformNumberFormatter`, `PlatformDateTimeFormatter`, `Message` and `MessageFormat2` updated.
- `i18n.js` (`genius-i18n`, Scala.js): the same changes, backed by `Intl.NumberFormat` and `Intl.DisplayNames`.
- Build and tooling: CI authenticates to `repo.genius.fish` to fetch the `mill-release` plugin; the `release-prepare` target is stricter; the library skill docs (`docs/skill`) and the README describe the open `Language`.

## Compatibility

- Scala 3.8.3
- Scala.js 1.21.0
- zio-json 0.9.2
- ZIO 2.1.26 (tests)
- scala-java-time 2.6.0 (Scala.js)
- Build: Mill with `fish.genius::mill-release` 0.4.0 and `mill-scalafix` 0.6.0

Upgrading from 1.x is source compatible apart from `match` expressions on `Language`, which need a `case _`, and any use of enum-only members; it is not binary compatible, so recompile. Serialized JSON for English, Dutch and French does not change; a 1.x reader cannot decode any other language.
