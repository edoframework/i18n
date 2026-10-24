package dev.taihei.tsuji

import java.util.Locale

import zio.test.*

// java.util.Locale is the JVM side only; Scala.js maps tags to Intl locales instead.
object PlatformLocalesSpec extends ZIOSpecDefault:

  def spec = suite("PlatformLocales on the JVM")(
    test("a bare nl is Dutch as written in the Netherlands, built from its tag") {
      val nl = PlatformLocales.javaLocale("nl")
      assertTrue(
        nl.getLanguage == "nl",
        nl.getCountry == "NL",
        nl.toLanguageTag == "nl-NL",
        nl == Locale.forLanguageTag("nl-NL"),
      )
    },
    test("a tag with a region goes through forLanguageTag as written") {
      val be = PlatformLocales.javaLocale("nl-BE")
      assertTrue(be.getLanguage == "nl", be.getCountry == "BE", be == Locale.forLanguageTag("nl-BE"))
    },
    test("the bare tags keep the regions they always had") {
      assertTrue(
        PlatformLocales.javaLocale("en") == Locale.US,
        PlatformLocales.javaLocale("fr") == Locale.FRANCE,
      )
    },
  )

end PlatformLocalesSpec
