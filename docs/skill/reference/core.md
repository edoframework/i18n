# core — Language & Message

Package `genius.i18n`. The two value types you work with directly.

## Language

```scala
enum Language(val isoCode: String, val isDefault: Boolean) derives JsonCodec:
  case English extends Language("en", true)
  case Dutch   extends Language("nl", false)
  case French  extends Language("fr", false)
```

- `.isoCode: String` — ISO 639-1 code (`"en"`, `"nl"`, `"fr"`).
- `.isDefault: Boolean` — true for `English`.
- `.label: Message` — the language's own name as a translatable `Message`.

Companion:

| Member | Result |
| --- | --- |
| `Language.of(isoCode: String): Language` | parse a code; falls back to `English` on an unknown code |
| `Language.defaultLanguage: Language` | `English` |
| `Language.empty: Language` | alias for `English` |
| `Language.sample: Language` | a random case (test/sample data) |

Codecs (given): `JsonCodec[Language]` (as the ISO string), plus
`JsonFieldEncoder`/`JsonFieldDecoder` so `Language` works as a JSON **map key**
(that's how `Message` stores its per-language templates).

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
