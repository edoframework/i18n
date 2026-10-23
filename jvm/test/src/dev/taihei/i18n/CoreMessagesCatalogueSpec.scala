package dev.taihei.i18n

import java.time.LocalDate

import zio.test.*

import dev.taihei.i18n.CoreMessages.*
import dev.taihei.i18n.Language.*

// The Dutch and French catalogues of CoreMessages are files in the jar, read from the classpath.
object CoreMessagesCatalogueSpec extends ZIOSpecDefault:

  def spec = suite("CoreMessages catalogues on the classpath")(
    test("the shipped Dutch and French files are valid catalogues of their families") {
      assertTrue(
        ClasspathCatalogues.verify(CoreMessages.families, Seq(Dutch, French)).isEmpty,
        ClasspathCatalogues.read(Validation, Dutch).exists(_.isRight),
        ClasspathCatalogues.read(Exceptions, French).exists(_.isRight),
      )
    },
    test("messages show in Dutch and French, and in English where no translation exists") {
      given Catalogues = Catalogues(CoreMessages.families)
      assertTrue(
        Validation.stringMinLength(min = 8).localize(Dutch) == "lengte moet minimaal 8 zijn",
        Validation.stringShouldNotBeBlank.localize(French) == "ne doit pas être vide",
        Exceptions.commandVeto(command = "close", state = "open").localize(Dutch) ==
        "Kan opdracht close niet uitvoeren op status open",
        Validation.dateMinimum(min =
          LocalDate.of(2024, 1, 15)
        ).localize(Language("nl-BE")).startsWith("moet later zijn dan 15"),
        Authentication.locked.localize(Dutch) == "Too many failed attempts; please try again later",
      )
    },
    test("formatErrors shows each field's error in the reader's language") {
      given Catalogues = Catalogues(CoreMessages.families)
      val errors = Validation.formatErrors(Map("email" -> List(Validation.stringValidEmail)))
      assertTrue(
        errors.localize(Dutch) == "Validatie mislukt: email (moet een geldig e-mailadres zijn)",
        errors.localize(French) == "Échec de validation: email (Doit être une adresse e-mail valide)",
      )
    },
  )

end CoreMessagesCatalogueSpec
