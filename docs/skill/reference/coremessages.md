# coremessages — the pre-built message library

Package `com.edoframework.i18n`. `CoreMessages` is a ready-made, tri-lingual (English, Dutch,
French) library of common application messages, grouped into nested objects.
Static members are `Message` values; parameterized ones are factory methods
returning a `Message`.

```scala
import com.edoframework.i18n.*

CoreMessages.Validation.stringMinLength(8).translate(Language.English)
CoreMessages.Authentication.invalidCredentials.translate(Language.French)
```

## Groups

| Group | Representative members |
| --- | --- |
| `CoreMessages.General` | `helloWorld(name: String)` |
| `CoreMessages.Authentication` | `success`, `accountNotFound`, `invalidCredentials`, `emailNotVerified`, `emailAlreadyRegistered`, `weakPassword`, `passwordMismatch`, `expiredToken`, `tokenAlreadyUsed`, `wrongCurrentPassword`, `verificationSent`, `passwordResetSent`, `locked`, `cannotRemoveLastCredential` |
| `CoreMessages.Authorization` | `accountNotActive`, `accountNotSystem`, `insufficientRole`, `insufficientGrant`, `personAccountMismatch`, `registerPersonForDifferentAccount` |
| `CoreMessages.Validation` | `invalid`, `stringShouldNotBeBlank`, `stringValidEmail`, `stringValidUrl`, `stringValidPhoneNumber`, `stringLengthBetween(min, max)`, `stringMinLength(min)`, `stringMaxLength(max)`, `stringShouldEqual(otherName)`, `intMinimum/intMaximum/intBetween`, `longMinimum/longMaximum`, `dateMinimum/dateMaximum/dateFuture/datePast`, `nonEmpty`, `invalidDate`, `invalidDateTime`, `dateRangeInvalid`, `formatErrors(errors)`, `missingTranslations(languages)` |
| `CoreMessages.Exceptions` | `unsupportedCommand`, `invalidCommand`, `commandVeto(command, state)`, `notFound`, `imageNotFound`, `unauthorized`, `forbidden`, `unknownError`, `revisionMismatch` (static and `(expected, actual)`), `eventFailure`, `stateFailure`, `invalidQuery`, `queryFailure`, `personAlreadyLinked`, `mediaProcessingFailed` |

## Formatting a bag of validation errors

`CoreMessages.Validation.formatErrors` folds a map of field → messages into one
human-readable `Message`:

```scala
val errors: Map[String, List[Message]] = Map(
  "email"    -> List(CoreMessages.Validation.stringValidEmail),
  "password" -> List(CoreMessages.Authentication.weakPassword),
)
CoreMessages.Validation.formatErrors(errors).translate(Language.English)
// Validation failed: email (…), password (…)
```

> Note: in the platform, `Map[String, List[Message]]` is the expansion of the
> `genius.validation.ValidationErrors` alias. This library inlines the plain `Map`
> type so it carries no dependency on the validation package — `formatErrors` still
> accepts a platform `ValidationErrors` value unchanged (it's a transparent alias).
