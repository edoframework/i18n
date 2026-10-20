# core — Language & Message

Package `genius.i18n`. The two value types you work with directly.

## Language

`Language` is a final class identified by its canonical BCP 47 tag. The three
built-ins are `val`s in the companion; any other language is `Language(tag)`.
(Until 2.0.0 it was a closed enum of the three built-ins.)

```scala
import genius.i18n.Language
import genius.i18n.Language.*

English                    // the default; also Dutch, French
Language("de")             // any valid tag
Language("nl-BE").base     // Dutch
Language("en") == English  // equality and hash by tag
```

- `.tag` / `.isoCode: String` — the canonical tag (`"en"`, `"nl-BE"`, `"zh-Hant-TW"`).
- `.base: Language` — without script, region or variant (`nl-BE` gives `nl`).
- `.locale: java.util.Locale` — `Locale.forLanguageTag(tag)`.
- `.isDefault: Boolean` — true for `English`.
- `.label: Message` — the language's name: hand translations for the built-ins;
  for any other language the platform's names (JDK locale data, `Intl.DisplayNames`
  in JS) in English, Dutch, French and in the language itself.
- `.toString` — `"English"`, `"Dutch"`, `"French"` for the built-ins, else the tag.

Companion:

| Member | Result |
| --- | --- |
| `Language(tag: String): Language` | any valid BCP 47 tag, canonicalized; `IllegalArgumentException` otherwise |
| `Language.parse(tag: String): Either[String, Language]` | the same without throwing |
| `Language.of(isoCode: String): Language` | the language for a valid tag; `English` only for invalid input |
| `Language.values: Array[Language]` | the built-ins (`English`, `Dutch`, `French`) |
| `Language.valueOf(name: String): Language` | a built-in by name (`"Dutch"`) |
| `Language.defaultLanguage: Language` | `English` |
| `Language.empty: Language` | alias for `English` |
| `Language.sample: Language` | a random built-in (test/sample data) |

A `match` on `Language` is not checked for exhaustiveness: `case English =>` still
works, but add `case _` for every other language. `ordinal` and `fromOrdinal` are
gone.

Codecs (given): `JsonEncoder`/`JsonDecoder[Language]` and
`JsonFieldEncoder`/`JsonFieldDecoder` (so `Language` works as a JSON **map key**,
which is how `Message` stores its per-language templates). A built-in is written by
its name (`"English"`), any other language by its tag (`"de"`); both forms are read.

## Message

A `Message` is a bundle of per-language templates plus the arguments to splice in
— a translatable value you pass instead of a bare `String`.

```scala
final case class Message(
  args: List[MessageArg],
  namedArgs: Map[String, MessageArg] = Map.empty,
  translationTemplates: Map[Language, String])
```

`MessageArg` is the union of substitutable types:

```scala
type MessageArg = String | Int | Long | Double | Float | Boolean
                | java.time.LocalDate | java.time.LocalDateTime | java.time.LocalTime
```

### Building a Message

| Factory | Use |
| --- | --- |
| `Message.with0Args(translations: (Language, String)*)` | a static message, no substitution |
| `Message.withNamedArgs(translations: (Language, String)*)(arguments: (String, MessageArg)*)` | **preferred** — `{$name}` placeholders bound to named args |
| `Message.of(value: String)` | wrap a plain string as an English-only message |
| `Message.of(t: Throwable)` | a message from an exception's text |

```scala
Message.with0Args(
  Language.English -> "Saved",
  Language.Dutch   -> "Opgeslagen",
  Language.French  -> "Enregistré",
)

Message.withNamedArgs(
  Language.English -> "{$count} results for {$query}",
)("count" -> 12, "query" -> "fish")
```

> The positional `with1Arg` … `with10Args` factories return curried
> `(A, B, …) => Message` builders and are **deprecated** — use `withNamedArgs`.

### Rendering & inspecting

| Method | Result |
| --- | --- |
| `translate(language: Language): String` | render in that language |
| `render(language: Language): String` | alias for `translate` |
| `nonEmpty: Boolean` | true if the rendered translation is non-empty |
| `toString: String` | the English translation (for logging) |

Placeholders are `{$name}`; `:number` / `:datetime` function annotations on a
placeholder route through the platform formatters (see `formatting.md`). Missing a
template for a requested language falls back to the default (English).

Codecs (given): `JsonCodec[Message]` (serializes `args` + `namedArgs` +
`translationTemplates`, so the message stays translatable after a round-trip), and
`JsonEncoder`/`JsonDecoder[MessageArg]` (args coerced to/from strings).
