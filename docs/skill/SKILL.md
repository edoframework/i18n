---
name: taihei-i18n
description: How to use Taihei Framework i18n 5.x (dev.taihei::taihei-i18n, formerly edo-i18n and genius-i18n), the type-safe internationalization library for Scala 3 on the JVM and Scala.js. Apply when writing or reviewing Scala that uses dev.taihei.i18n.* - message families (MessageFamily, message[...]), Message, Text, Displayable, MessageArg, Catalogue and Catalogues, the fallback chain, catalogue files on the classpath or fetched with CatalogueLoader, Catalogues.verify in tests, the taihei-i18n-tools export, the JSON wire format, ICU MessageFormat 2.0 templates, CoreMessages, Language, or migrating from the 4.x Message (dev.taihei.i18n.legacy.Message).
---

# Taihei Framework i18n 5.x

A type-safe i18n library for Scala 3, cross-compiled for the JVM and Scala.js. Messages are
defined in Scala as typed **message families**, checked by a macro when the code compiles; their
translations live inline or in per-language **catalogue files**; a `Message` is only a key and
typed arguments, so only the reader's language travels and is loaded.

Package `dev.taihei.i18n`. Artifacts on Maven Central: `dev.taihei::taihei-i18n` (`taihei-i18n_3`
on the JVM, `taihei-i18n_sjs1_3` on Scala.js) and the JVM-only `dev.taihei::taihei-i18n-tools`,
version 5.0.0, Scala 3.9 (LTS), Apache 2.0. Source: <https://github.com/taiheiframework/i18n>.
4.0.x gets security fixes for 12 months after 5.0.0; use 4.0.1 or later of 4.x.

## When to use this skill

Whenever Scala code imports `dev.taihei.i18n.*`: defining or showing messages, adding
translations, loading catalogues in a browser, sending messages over JSON, testing catalogues,
formatting numbers and dates per locale, or moving code from 4.x.

## The model in one example

```scala
import java.time.LocalDate
import dev.taihei.i18n.*
import dev.taihei.i18n.Language.*

object Orders extends MessageFamily("order"):          // key prefix, English source
  val cancelled = message("Your order was cancelled", Dutch -> "Uw bestelling werd geannuleerd")
  val shipped   = message[(count: Int, on: LocalDate)](
    ".match {$count :plural} when one {One item shipped on {$on :date dateStyle=long}} " +
      "when * {{$count} items shipped on {$on :date dateStyle=long}}"
  )

given Catalogues = Catalogues(Seq(Orders))             // the app's registry
Orders.shipped(count = 3, on = today).localize(Dutch)  // falls back to English, formats in Dutch
```

- `MessageFamily(prefix, sourceLanguage = English)`; each message a `val` = `message(...)`.
- No type argument: a `Message`. A named tuple of argument types: a definition applied by name or
  position, checked by the compiler.
- `Displayable#localize(language)(using Catalogues)` shows a `Message`, a `Text` or a legacy
  message. Fallback: exact, subtags removed, configured fallbacks, source language. Never throws.
- Translations: inline pairs (in the Scala.js bundle!) or JSON catalogue files.

## Reference index - read the file that matches the task

- **`reference/core.md`**: `Language`, `MessageFamily`, `message`, the macro's checks,
  `MessageArg`, `Message`, `Text`, `Displayable`, `MessageKey`, the JSON format and key rules.
- **`reference/catalogues.md`**: catalogue files, the `Catalogues` registry and its chain, the
  classpath (JVM), `CatalogueLoader` (Scala.js), `verify`, the export tool.
- **`reference/messageformat2.md`**: the template syntax, functions, `.match`, `validate`.
- **`reference/coremessages.md`**: the ready-made families.
- **`reference/formatting.md`**: platform behaviour, cross-platform rules, build and layout.
- **`reference/migration.md`**: moving from 4.x, constructor by constructor.

## Top gotchas

- `message` works only as the right-hand side of a `val` in the family's body, with a literal
  template; a `def`, a nested object or a class is a compile error. Use a plain `val`, not a
  `lazy val` (it joins `definitions` only when first used, unseen by verify, the export and
  JSON decoding until then). The key is the val's name:
  renaming the val changes the key, which is a wire contract.
- A key is a contract: never reuse it for other arguments. Adding an argument needs a new key
  unless it is an `Option`.
- Decoding a `Message` from JSON needs a `given Catalogues` that knows the family (`Catalogues(Seq(...))`
  or `register`). An unknown key reads as its `text`.
- Inline translations are code and end up in the Scala.js bundle; a browser app with many
  languages serves exported files and loads them with `CatalogueLoader`.
- Completeness of files is checked by `Catalogues.verify` (or `ClasspathCatalogues.verify`) in
  tests, not by the compiler. Mark the languages that must be complete as `required`.
- The functions of a user `MessageArg.as` type are checked by `verify`, not the macro.
- `legacy.Message` and `dev.taihei.i18n.Message` share a simple name: an explicit
  `import dev.taihei.i18n.legacy.Message` wins over `dev.taihei.i18n.*`.
- `shared/` compiles on Scala.js too: no JVM-only API there.
- French plural `one` is 0 and 1; English and Dutch only 1.
