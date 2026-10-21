package com.edoframework.i18n

import java.time.LocalDate

import com.edoframework.i18n.Language.*

object CoreMessages:

  object General:

    def helloWorld(name: String) =
      Message.withNamedArgs(
        English -> "Hello {$name}",
        Dutch -> "Dag {$name}",
        French -> "Bonjour {$name}",
      )("name" -> name)

  object Authentication:

    val success = Message.with0Args(
      English -> "Authentication successful"
    )

    val accountNotFound = Message.with0Args(
      English -> "Account not found"
    )

    val invalidCredentials = Message.with0Args(
      English -> "Invalid email or password"
    )

    val emailNotVerified = Message.with0Args(
      English -> "Please verify your email address before signing in"
    )

    val emailAlreadyRegistered = Message.with0Args(
      English -> "An account with this email already exists"
    )

    val weakPassword = Message.with0Args(
      English -> "Password is too weak; choose a stronger one"
    )

    val passwordMismatch = Message.with0Args(
      English -> "Passwords do not match"
    )

    val expiredToken = Message.with0Args(
      English -> "This link has expired"
    )

    val tokenAlreadyUsed = Message.with0Args(
      English -> "This link has already been used"
    )

    val cannotRemoveLastCredential = Message.with0Args(
      English -> "Cannot remove the only sign-in method for this account"
    )

    val wrongCurrentPassword = Message.with0Args(
      English -> "Current password is incorrect"
    )

    val verificationSent = Message.with0Args(
      English -> "Check your email to confirm your account"
    )

    val passwordResetSent = Message.with0Args(
      English -> "If that email is registered, a reset link has been sent"
    )

    val locked = Message.with0Args(
      English -> "Too many failed attempts; please try again later"
    )

  end Authentication

  object Exceptions:

    val unsupportedCommand = Message.with0Args(
      English -> "Unsupported command",
      Dutch -> "Operatie niet ondersteund",
      French -> "commande non supportée",
    )

    val invalidCommand = Message.with0Args(
      English -> "Invalid command",
      Dutch -> "Ongeldige opdracht",
      French -> "commande non valide",
    )

    def commandVeto(command: String, state: String) =
      Message.withNamedArgs(
        English -> "Cannot execute command {$command} on state {$state}",
        Dutch -> "Kan opdracht {$command} niet uitvoeren op status {$state}",
        French -> "Impossible d'exécuter la commande {$command} sur l'état {$state}",
      )("command" -> command, "state" -> state)

    val personAlreadyLinked = Message.with0Args(
      English -> "Cannot link: person is already linked to an account",
      Dutch -> "Kan niet koppelen: persoon is al gekoppeld aan een account",
      French -> "Impossible de lier: la personne est déjà liée à un compte",
    )

    val mediaProcessingFailed = Message.with0Args(
      English -> "Media processing failed",
      Dutch -> "Mediaverwerking mislukt",
      French -> "Le traitement du média a échoué",
    )

    val eventFailure = Message.with0Args(
      English -> "Event failure"
    )

    val stateFailure = Message.with0Args(
      English -> "State failure"
    )

    val invalidQuery = Message.with0Args(
      English -> "Invalid query",
      Dutch -> "Ongeldige opdracht",
      French -> "commande non valide",
    )

    val queryFailure = Message.with0Args(
      English -> "Query failure"
    )

    val unknownError = Message.with0Args(
      English -> "Unknown error",
      Dutch -> "Onbekende fout",
      French -> "erreur inconnue",
    )

    val notFound = Message.with0Args(
      English -> "Not found"
    )

    val imageNotFound = Message.with0Args(
      English -> "Image not found",
      Dutch -> "Afbeelding niet gevonden",
      French -> "Image non trouvé",
    )

    val unauthorized = Message.with0Args(
      English -> "Unauthorized",
      Dutch -> "Geen toegang",
      French -> "non autorisé",
    )

    val forbidden = Message.with0Args(
      English -> "Forbidden: Access denied",
      Dutch -> "Verboden: Toegang geweigerd",
      French -> "Interdit: Accès refusé",
    )

    val revisionMismatch = Message.with0Args(
      English -> "Revision mismatch: the resource was modified by another user",
      Dutch -> "Revisie mismatch: de resource werd gewijzigd door een andere gebruiker",
      French -> "Conflit de révision: la ressource a été modifiée par un autre utilisateur",
    )

    def revisionMismatch(expected: Long, actual: Long) =
      Message.withNamedArgs(
        English ->
        "Revision mismatch: expected revision {$expected}, but current revision is {$actual}",
        Dutch ->
        "Revisie mismatch: verwachte revisie {$expected}, maar huidige revisie is {$actual}",
        French ->
        "Conflit de révision: révision attendue {$expected}, mais révision actuelle est {$actual}",
      )("expected" -> expected, "actual" -> actual)

  end Exceptions

  object Validation:
    val invalid = Message.with0Args(English -> "validation failed")

    val stringShouldNotBeBlank = Message.with0Args(
      English -> "should not be blank",
      Dutch -> "mag niet leeg zijn",
      French -> "ne doit pas être vide",
    )

    val stringValidEmail = Message.with0Args(
      English -> "should be a valid email address",
      Dutch -> "moet een geldig e-mailadres zijn",
      French -> "Doit être une adresse e-mail valide",
    )

    val stringValidUrl = Message.with0Args(
      English -> "should be a valid URL",
      Dutch -> "moet een geldige URL zijn",
      French -> "Doit être un URL valide",
    )

    val stringValidPhoneNumber = Message.with0Args(
      English -> "should be a valid phone number",
      Dutch -> "moet een geldig telefoonnummer zijn",
      French -> "doit être un numéro de téléphone valide",
    )

    def stringLengthBetween(min: Int, max: Int) =
      Message.withNamedArgs(
        English -> "length should be between {$min} and {$max}",
        Dutch -> "lengte moet tussen {$min} en {$max} zijn",
        French -> "la longueur doit se situer entre {$min} et {$max}",
      )("min" -> min, "max" -> max)

    def stringMinLength(min: Int) =
      Message.withNamedArgs(
        English -> "length should be greater than {$min}",
        Dutch -> "lengte moet minimaal {$min} zijn",
        French -> "la longueur doit être supérieure à {$min}",
      )("min" -> min)

    def stringMaxLength(max: Int) =
      Message.withNamedArgs(
        English -> "length should be lower than {$max}",
        Dutch -> "lengte moet maximaal {$max} zijn",
        French -> "la longueur doit être inférieure à {$max}",
      )("max" -> max)

    def stringShouldEqual(otherName: String) =
      Message.withNamedArgs(
        English -> "should equal {$otherName}",
        Dutch -> "moet overeenkomen met {$otherName}",
        French -> "doit être égale à {$otherName}",
      )("otherName" -> otherName)

    def intMinimum(min: Int) =
      Message.withNamedArgs(
        English -> "should be minimum {$min}",
        Dutch -> "moet minstens {$min} zijn",
        French -> "doit être au minimum de {$min}",
      )("min" -> min)

    def intMaximum(max: Int) =
      Message.withNamedArgs(
        English -> "should be maximum {$max}",
        Dutch -> "moet hoogstens {$max} zijn",
        French -> "doit être au maximum de {$max}",
      )("max" -> max)

    def longMinimum(min: Long) =
      Message.withNamedArgs(
        English -> "should be minimum {$min}",
        Dutch -> "moet minstens {$min} zijn",
        French -> "doit être au minimum de {$min}",
      )("min" -> min)

    def longMaximum(max: Long) =
      Message.withNamedArgs(
        English -> "should be maximum {$max}",
        Dutch -> "moet hoogstens {$max} zijn",
        French -> "doit être au maximum de {$max}",
      )("max" -> max)

    def dateMinimum(min: LocalDate) =
      Message.withNamedArgs(
        English -> "should be after {$year}/{$month}/{$day}",
        Dutch -> "moet later zijn dan {$day}/{$month}/{$year}",
        French -> "devrait être plus tard que {$day}/{$month}/{$year}",
      )(
        "year" -> min.getYear.toString,
        "month" -> "%02d".format(min.getMonthValue),
        "day" -> "%02d".format(min.getDayOfMonth),
      )

    def dateMaximum(max: LocalDate) =
      Message.withNamedArgs(
        English -> "should be before {$year}/{$month}/{$day}",
        Dutch -> "moet vroeger zijn dan {$day}/{$month}/{$year}",
        French -> "devrait être avant {$day}/{$month}/{$year}",
      )(
        "year" -> max.getYear.toString,
        "month" -> "%02d".format(max.getMonthValue),
        "day" -> "%02d".format(max.getDayOfMonth),
      )

    val dateFuture = Message.with0Args(
      English -> "should be in the future",
      Dutch -> "moet in de toekomst liggen",
      French -> "devrait être dans le futur",
    )

    val datePast = Message.with0Args(
      English -> "should be in the past",
      Dutch -> "moet in het verleden liggen",
      French -> "devrait être dans le passé",
    )

    val nonEmpty = Message.with0Args(
      English -> "should not be empty",
      Dutch -> "mag niet leeg zijn",
      French -> "ne doit pas être vide",
    )

    val shouldNotEqual = Message.with0Args(
      English -> "should not be equal",
      Dutch -> "mag niet gelijk zijn",
      French -> "ne doit pas être égal",
    )

    def intBetween(min: Int, max: Int) =
      Message.withNamedArgs(
        English -> "should be between {$min} and {$max}",
        Dutch -> "moet tussen {$min} en {$max} liggen",
        French -> "doit être entre {$min} et {$max}",
      )("min" -> min, "max" -> max)

    val invalidDate = Message.with0Args(
      English -> "is not a valid date",
      Dutch -> "is geen geldige datum",
      French -> "n'est pas une date valide",
    )

    val invalidDateTime = Message.with0Args(
      English -> "is not a valid date and time",
      Dutch -> "is geen geldige datum en tijd",
      French -> "n'est pas une date et heure valide",
    )

    val dateRangeInvalid = Message.with0Args(
      English -> "start date must be before or equal to end date",
      Dutch -> "startdatum moet voor of gelijk aan einddatum zijn",
      French -> "la date de début doit être antérieure ou égale à la date de fin",
    )

    // Validation errors per field as a plain `Map[String, List[Message]]`, so the
    // i18n library carries no dependency on any validation package.
    def formatErrors(errors: Map[String, List[Message]]): Message =
      if errors.isEmpty then
        invalid
      else
        val errorDetails = errors.toList
          .sortBy(_._1) // Sort by field name for consistent output
          .flatMap: (fieldName, messages) =>
            messages.map: msg =>
              s"$fieldName (${msg.translate(Language.English)})"
          .mkString(", ")

        Message.withNamedArgs(
          English -> "Validation failed: {$details}",
          Dutch -> "Validatie mislukt: {$details}",
          French -> "Échec de validation: {$details}",
        )("details" -> errorDetails)

    def missingTranslations(languages: String) =
      Message.withNamedArgs(
        English -> "missing translations for: {$languages}",
        Dutch -> "ontbrekende vertalingen voor: {$languages}",
        French -> "traductions manquantes pour: {$languages}",
      )("languages" -> languages)

  end Validation

  object Authorization:

    val accountNotActive = Message.with0Args(
      English -> "account is not active",
      Dutch -> "account is niet actief",
      French -> "le compte n'est pas actif",
    )

    val accountNotSystem = Message.with0Args(
      English -> "account is not a system account",
      Dutch -> "account is geen systeemaccount",
      French -> "le compte n'est pas un compte système",
    )

    val insufficientRole = Message.with0Args(
      English -> "account does not have one of the required roles",
      Dutch -> "account heeft niet een van de vereiste rollen",
      French -> "le compte n'a pas l'un des rôles requis",
    )

    val insufficientGrant = Message.with0Args(
      English -> "account does not have permission for this scope",
      Dutch -> "account heeft geen toestemming voor dit bereik",
      French -> "le compte n'a pas l'autorisation pour cette portée",
    )

    val personAccountMismatch = Message.with0Args(
      English -> "person does not correspond to the authenticated account",
      Dutch -> "persoon komt niet overeen met het geauthenticeerde account",
      French -> "la personne ne correspond pas au compte authentifié",
    )

    val registerPersonForDifferentAccount = Message.with0Args(
      English -> "trying to register person for a different account than the one authenticated",
      Dutch -> "poging om persoon te registreren voor een ander account dan het geauthenticeerde",
      French -> "tentative d'enregistrer une personne pour un compte différent de celui authentifié",
    )

  end Authorization

end CoreMessages
