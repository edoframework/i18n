---
name: library-genius-i18n
description: How to use genius-i18n — Genius Fish's composable, type-safe internationalization library for Scala 3 (JVM + Scala.js). Apply when writing or reviewing Scala that handles locales, translatable messages, ICU MessageFormat 2.0, or locale-aware number/date formatting with the genius.i18n.* API (Language, Message, MessageFormat2, CoreMessages, PlatformNumberFormatter/PlatformDateTimeFormatter). Covers the model, the message API, the formatter syntax, the pre-built message library, cross-platform notes, and the build.
---

# genius-i18n

A composable, type-safe i18n library for Scala 3, cross-compiled for the **JVM and
Scala.js**. It gives you a small set of value types — a `Language` enum and a
translatable `Message` — plus an ICU MessageFormat 2.0 engine and locale-aware
number/date formatting that works the same on both platforms.

The package is `genius.i18n` (note: **not** `fish.genius.i18n` — it was extracted
verbatim from the platform). Artifacts: `fish.genius::genius-i18n` (JVM) and
`fish.genius::genius-i18n_sjs1` (Scala.js). Source: <https://github.com/genius-fish/i18n>.

## When to use this skill

Read this whenever you write or review Scala that imports `genius.i18n.*` —
building translatable messages, parsing/selecting a locale, formatting numbers or
dates per-locale, using the `CoreMessages` library, or working with the ICU
MessageFormat 2.0 syntax. It also flags the cross-platform (JVM/JS) considerations.

## The model

Unlike the sibling DSL libraries (genius-latex, genius-uml), this is **plain value
types + functions**, not a context-function DSL — there are no markers or AST.
Four pieces:

- **`Language`** — an enum of supported locales (`English`, `Dutch`, `French`),
  each with an ISO code. The unit of "which translation".
- **`Message`** — a bundle of per-language templates plus arguments. You build one
  with translations for each language, then `.translate(language)` renders it. This
  is the core value you pass around instead of a bare `String`.
- **`MessageFormat2`** — a standalone ICU MessageFormat 2.0 engine
  (`format(template, args, locale)`): variable substitution, `:number` / `:datetime`
  functions, and locale-aware `.match`/plural selection.
- **`CoreMessages`** — a ready-made tri-lingual library of common messages
  (auth, authorization, validation, exceptions, general).

`Message` template placeholders use `{$name}`; the same `:number` / `:datetime`
function annotations as MessageFormat2 route through the cross-platform
`PlatformNumberFormatter` / `PlatformDateTimeFormatter` (JVM: `java.text` /
`java.time`; JS: the browser `Intl` API).

```scala
import genius.i18n.*

val greeting = Message.withNamedArgs(
  Language.English -> "Hello {$name}, you have {$points} points",
  Language.Dutch   -> "Hallo {$name}, u hebt {$points} punten",
  Language.French  -> "Bonjour {$name}, vous avez {$points} points",
)("name" -> "Alice", "points" -> 150)

greeting.translate(Language.French)   // Bonjour Alice, vous avez 150 points
```

Both `Language` and `Message` derive `zio-json` codecs, so messages serialize over
a wire and survive round-trips (templates + args are preserved, not just the
rendered string).

## Reference index — read the file that matches the task

- **`reference/core.md`** — `Language` (cases, ISO codes, parsing, codecs) and
  `Message` (factories, `translate`, `MessageArg`, JSON codecs).
- **`reference/messageformat2.md`** — the ICU MessageFormat 2.0 engine: template
  syntax, `:number`/`:datetime` functions, declarations, `.match`/plurals.
- **`reference/coremessages.md`** — the pre-built `CoreMessages` library grouped by
  domain (Authentication, Authorization, Validation, Exceptions, General).
- **`reference/formatting.md`** — the cross-platform formatter contract
  (`PlatformNumberFormatter` / `PlatformDateTimeFormatter`), JVM vs JS behavior,
  and the build/test layout.

## Top gotchas

- The package is **`genius.i18n`**, not `fish.genius.i18n`.
- Prefer **`Message.withNamedArgs(...)(...)`** and `{$name}` placeholders; the
  positional `with1Arg`…`with10Args` factories are deprecated.
- Pass a `Message`, not a pre-rendered `String`, through your domain — render at the
  edge with `.translate(userLanguage)` so the same value works in any locale.
- It's cross-platform: anything in `shared/` must compile on Scala.js too (no
  JVM-only APIs there — platform specifics live in `jvm/` and `js/`). JS tests run
  under Node (`make test`).
- French pluralization treats 0 and 1 as `one`; English/Dutch treat only 1 as `one`.
