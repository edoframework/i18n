# genius-i18n

A composable, type-safe internationalization library for Scala 3 — locales,
translatable messages, ICU MessageFormat 2.0, and locale-aware number/date
formatting. Cross-compiled for the **JVM and Scala.js**.

Extracted from the Genius Fish platform as a standalone library; the package is
`genius.i18n`.

## Modules

| Artifact         | Platform | Purpose                                              |
| ---------------- | -------- | ---------------------------------------------------- |
| `genius-i18n`    | JVM      | The full library on `java.time` / `java.text`.       |
| `genius-i18n_sjs1` | Scala.js | The same API, formatting via the browser `Intl` API. |

The `shared/` sources compile on both platforms; `jvm/` and `js/` hold only the
platform-specific number/date formatters.

## Stack

- Mill 1.1.5
- Scala 3.8.3 · Scala.js 1.21.0
- ZIO 2.1.26 (`zio-json` for codecs, `zio-test`)
- scala-java-time (JS `java.time` support)

## Build

```sh
make compile        # ./mill __.compile           (JVM + JS)
make test           # ./mill __.test.testForked    (JVM + JS; JS runs on Node)
make fmt            # ./mill __.reformat
make fix            # ./mill __.fix                 (scalafix)
make validate       # fix + fmt + compile + test
make publishLocal   # to ~/.ivy2/local
```

## Quick start

```scala
import genius.i18n.*

// A translatable message with named placeholders:
val greeting = Message.withNamedArgs(
  Language.English -> "Hello {$name}, you have {$points} points",
  Language.Dutch   -> "Hallo {$name}, u hebt {$points} punten",
  Language.French  -> "Bonjour {$name}, vous avez {$points} points",
)("name" -> "Alice", "points" -> 150)

greeting.translate(Language.English)  // Hello Alice, you have 150 points
greeting.translate(Language.French)   // Bonjour Alice, vous avez 150 points
```

`Language` is an enum (`English`, `Dutch`, `French`) with ISO codes;
`Language.of("nl")` parses a code (falling back to English). Both `Language` and
`Message` derive `zio-json` codecs, so messages serialize cleanly across a wire.

### ICU MessageFormat 2.0

```scala
val tmpl =
  """.match {$count :plural}
    |when one   {You have one item}
    |when other {You have {$count} items}""".stripMargin

MessageFormat2.format(tmpl, Map("count" -> 1), "en")  // You have one item
MessageFormat2.format(tmpl, Map("count" -> 5), "en")  // You have 5 items
```

Placeholders support functions — `{$value :number style=currency}`,
`{$date :datetime dateStyle=long}` — which route through the cross-platform
`PlatformNumberFormatter` / `PlatformDateTimeFormatter`.

### Pre-built messages

`CoreMessages` ships a tri-lingual library of common messages grouped under
`Authentication`, `Authorization`, `Validation`, `Exceptions`, and `General` —
e.g. `CoreMessages.Validation.stringMinLength(8)`.

## Testing

```scala
import genius.i18n.*
import zio.test.*

assertTrue(
  greeting.translate(Language.Dutch).contains("Alice"),
  Language.of("fr") == Language.French,
)
```

The suite runs on both platforms (`make test`); JS tests execute under Node.

## License

MIT — see [LICENSE](LICENSE).
