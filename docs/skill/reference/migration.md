# migration - from taihei-i18n 4.x to 5.x

## Step 1: imports only

The 4.x `Message` is `dev.taihei.i18n.legacy.Message` in 5.x, unchanged and deprecated, until
6.0.0:

```scala
import dev.taihei.i18n.*
import dev.taihei.i18n.legacy.Message   // explicit, so it wins over the wildcard's Message
```

Code that only builds and translates 4.x messages then compiles with deprecation warnings; code
that uses `Message.of`, `Language.label`, `CoreMessages`, the `MessageArg` union or the MF2
internals needs the changes of step 2. To silence them while migrating:
`-Wconf:cat=deprecation&origin=dev\.taihei\.i18n\.legacy\..*:s`. Legacy messages are
`Displayable` and show next to new ones; `legacy.MessageArg` is the 4.x union type.

## Step 2: families, one at a time

| 4.x | 5.x |
| --- | --- |
| `Message.with0Args(English -> "Saved", Dutch -> "Opgeslagen")` | `val saved = message("Saved", Dutch -> "Opgeslagen")` in a `MessageFamily` |
| `Message.withNamedArgs(English -> "Hello {$name}")("name" -> n)` | `val hello = message[(name: String)]("Hello {$name}")`; `hello(name = n)` |
| `with1Arg` ... `with10Args`, `{$0}` | named arguments in the named tuple |
| `translate(l)`, `render(l)` | `localize(l)` with a `given Catalogues` |
| `toString` (English) | `toString` (the family's source language) |
| `Message.of(throwable)` | `Text.of(throwable)` |
| `Message.of(string)` | `Text(string, Language.English)` |
| `MessageFormat2.Parser`, AST, `Context` | internal; `format` and `validate` |
| `PlatformNumberFormatter` and the other `Platform*` objects | internal; format in a template |
| `Language.label` (legacy) | `Language.label: Message` |
| `CoreMessages.Exceptions.revisionMismatch(expected, actual)` | `revisionMismatchBetween(expected, actual)` |
| `CoreMessages.Validation.formatErrors(Map[String, List[Message]])` | `formatErrors(Map[String, List[Displayable]])` |
| `!!unknown message!!` | gone: the source language always has the key |

## JSON during a rollout

5.x reads 4.x JSON (as a legacy message with the key `legacy`). A 4.x reader cannot read a 5.x
message: upgrade readers (clients) first, or keep writing legacy messages until they have.

## Libraries built on 4.x

`Language` is binary compatible with 4.x: a library that uses only `Language` (taihei-lorem 4.x)
keeps working with 5.0.0 on the classpath.
