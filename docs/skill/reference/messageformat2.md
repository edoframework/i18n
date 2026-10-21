# messageformat2 — ICU MessageFormat 2.0

Package `com.edoframework.i18n`. A standalone implementation of the ICU MessageFormat 2.0
template language — variable substitution, formatting functions, and locale-aware
plural/select matching.

## Entry point

```scala
object MessageFormat2:
  def format(
    template: String,
    args: Map[String, Any] = Map.empty,
    locale: String = "en",   // "en" | "nl" | "fr"
  ): String
```

`Message` uses the same placeholder/function syntax internally, so most of the
time you write templates inside `Message.withNamedArgs(...)` rather than calling
`MessageFormat2.format` directly. Reach for `format` when you have a raw template
string and an argument map.

## Placeholders

| Syntax | Effect |
| --- | --- |
| `{$name}` | substitute the variable `name` |
| `{$value :number}` | format as a locale number (→ `PlatformNumberFormatter`) |
| `{$value :number style=currency}` | currency formatting |
| `{$value :number style=percent}` | percent formatting |
| `{$date :datetime}` | format as date/time (→ `PlatformDateTimeFormatter`) |
| `{$date :datetime dateStyle=short}` | date style: `short` \| `medium` \| `long` \| `full` |

## Declarations

| Syntax | Effect |
| --- | --- |
| `.input {$var}` | declare an input variable |
| `.input {$var :function options}` | declare with a function annotation |
| `.let {$local = (literal)}` | bind a local variable |

## Matching (plural / select)

```
.match {$count :plural}
when one   {You have one item}
when other {You have {$count} items}
```

- `.match {$selector}` — single selector; `.match {$a} {$b}` — multiple.
- `when key { pattern }` — a case; `when * { pattern }` — wildcard (matches all).
- Plural keys: `one`, `other`, and exact matches `=0`, `=1`, ….
- **Locale-aware plurals:** French treats `0` and `1` as `one`; English and Dutch
  treat only `1` as `one`. Always pass the right `locale` so selection is correct.

```scala
val tmpl =
  """.match {$count :plural}
    |when =0    {No messages}
    |when one   {One message}
    |when other {{$count} messages}""".stripMargin

MessageFormat2.format(tmpl, Map("count" -> 0), "en")  // No messages
MessageFormat2.format(tmpl, Map("count" -> 1), "fr")  // One message  (fr: 1 → one)
MessageFormat2.format(tmpl, Map("count" -> 3), "en")  // 3 messages
```
