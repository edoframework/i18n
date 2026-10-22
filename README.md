# Taihei Framework i18n

A composable, type-safe internationalization library for Scala 3 — locales,
translatable messages, ICU MessageFormat 2.0, and locale-aware number/date
formatting. Cross-compiled for the **JVM and Scala.js**.

Part of the [Taihei Framework](https://taihei.dev), the open-source Scala 3
libraries from Innovenso. The package is `dev.taihei.i18n`.

## Modules

Published on Maven Central under `dev.taihei`:

| Artifact                | Platform | Purpose                                              |
| ----------------------- | -------- | ---------------------------------------------------- |
| `taihei-i18n`           | JVM      | The full library on `java.time` / `java.text`.       |
| `taihei-i18n_sjs1`      | Scala.js | The same API, formatting via the browser `Intl` API. |

```scala
mvn"dev.taihei::taihei-i18n:4.0.0"     // Mill, JVM
mvn"dev.taihei::taihei-i18n::4.0.0"    // Mill, Scala.js
"dev.taihei" %%% "taihei-i18n" % "4.0.0" // sbt
```

The `shared/` sources compile on both platforms; `jvm/` and `js/` hold only the
platform-specific number/date formatters.

## Stack

- Mill 1.3.1
- Scala 3.9.0 (LTS) · Scala.js 1.22.0
- ZIO 2.1.27 (`zio-json` 1.1 for codecs, `zio-test`)
- scala-java-time 2.8 (JS `java.time` support)

Everything resolves from Maven Central; the build needs no credentials.

## Build

```sh
make compile        # ./mill __.compile           (JVM + JS)
make test           # ./mill __.test.testForked    (JVM + JS; JS runs on Node)
make fmt            # ./mill __.reformat
make fix            # ./mill __.fix                 (scalafix)
make validate       # fix + fmt + compile + test + mima
make mima           # binary compatibility with the newest release tag
make publishLocal   # to ~/.ivy2/local (a SNAPSHOT only)
```

## Releasing

The version is the git tag: an exact `v`-tag on a clean tree is the release,
anything else a SNAPSHOT of the last tag.

```sh
make release-prepare TYPE=patch|minor|major   # validate, write RELEASE_x_y_z.md
make release-finalize                          # commit the notes, tag locally,
                                               # check Central, publish to it
git push origin main && git push origin vX.Y.Z # only after the publish succeeded
make publish-dry-run                           # the signed bundle, written to out/
```

Publishing reads the Central Portal user token and the release key from
`~/.edo-release.env` or the environment (`MILL_SONATYPE_USERNAME`,
`MILL_SONATYPE_PASSWORD`, `MILL_PGP_SECRET_BASE64`, `MILL_PGP_PASSPHRASE`).
Releases are signed by the Taihei Framework release key,
fingerprint `5026 B299 6DDB 697E C463  0BCF 14A7 33E2 A507 702F`.

## Coming from edo-i18n 3.0.0 or genius-i18n

4.0.0 has the API of `com.edoframework::edo-i18n:3.0.0` under the library's new name:

- the dependency is `dev.taihei::taihei-i18n`;
- the package is `dev.taihei.i18n`: replace `com.edoframework.i18n` in your imports.

Coming from `fish.genius::genius-i18n:2.0.0`, replace `genius.i18n` instead. 3.0.0 also
moved to Scala 3.9 (LTS), so your build needs Scala 3.9 or later, to `zio-json` 1.1
instead of 0.9, and to the Apache License 2.0.

## Quick start

```scala
import dev.taihei.i18n.*

// A translatable message with named placeholders:
val greeting = Message.withNamedArgs(
  Language.English -> "Hello {$name}, you have {$points} points",
  Language.Dutch   -> "Hallo {$name}, u hebt {$points} punten",
  Language.French  -> "Bonjour {$name}, vous avez {$points} points",
)("name" -> "Alice", "points" -> 150)

greeting.translate(Language.English)  // Hello Alice, you have 150 points
greeting.translate(Language.French)   // Bonjour Alice, vous avez 150 points
```

`Language` is identified by its BCP 47 tag. `English`, `Dutch` and `French` are
built in; any other language is `Language("de")` or `Language("nl-BE")`, and
`Language.of(tag)` parses a tag, falling back to English only for invalid input. A
message without a translation for a language falls back to its base language
(`nl-BE` to `nl`), then to English; numbers and dates format in the language's
locale. Both `Language` and `Message` have `zio-json` codecs, so messages serialize
cleanly across a wire (a built-in as `"English"`, any other language as its tag).

Since 2.0.0 `Language` is a class, not an enum: `case English =>` still matches,
but a `match` on a language needs a `case _`, and `ordinal`/`fromOrdinal` are gone.
`isoCode` is the full tag, so it is `"nl-BE"` for Belgian Dutch: compare
`base.isoCode` where only the language matters. `Language.of` reads tags, not the
names `"English"`/`"Dutch"`/`"French"` (that is `valueOf`).

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
import dev.taihei.i18n.*
import zio.test.*

assertTrue(
  greeting.translate(Language.Dutch).contains("Alice"),
  Language.of("fr") == Language.French,
)
```

The suite runs on both platforms (`make test`); JS tests execute under Node.

## License

Apache License 2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE). Copyright
Innovenso BV. Versions up to and including 2.0.0 were published as
`fish.genius::genius-i18n` under the MIT License and remain available under it.
