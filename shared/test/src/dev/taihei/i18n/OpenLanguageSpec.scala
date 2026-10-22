package dev.taihei.i18n

import zio.json.*
import zio.test.*

import dev.taihei.i18n.Language.*

/** Any BCP 47 language, beside the three built-ins (since 2.0.0). */
object OpenLanguageSpec extends ZIOSpecDefault:

  private val German  = Language("de")
  private val Flemish = Language("nl-BE")

  // A match as consumers write it: the built-ins by name, everything else by the wildcard that a
  // class (unlike the enum) requires.
  private def greeting(language: Language): String = language match
    case English => "Hello"
    case Dutch   => "Dag"
    case French  => "Bonjour"
    case _       => "?"

  def spec = suite("OpenLanguageSpec")(
    suite("Language(tag)")(
      test("a language beyond the built-ins carries its tag") {
        assertTrue(
          German.tag == "de",
          German.isoCode == "de",
          !German.isDefault,
          German.toString == "de",
          German.base == German,
          German.locale.getLanguage == "de",
        )
      },
      test("a tag is canonicalized and compared by value") {
        assertTrue(
          Language("NL-be") == Flemish,
          Language("nl_BE").tag == "nl-BE",
          Language("zh-hant-tw").tag == "zh-Hant-TW",
          Language("es-419").tag == "es-419",
          Language("de").hashCode == German.hashCode,
          Map(German -> "x").get(Language("DE")).contains("x"),
        )
      },
      test("the built-ins are found by their tag") {
        assertTrue(
          Language("en") == English,
          Language("EN") eq English,
          Language("nl") == Dutch,
          Language("fr") == French,
          Language.of("de") == German,
          Language.of("nl-BE") == Flemish,
          Language.of("EN") == English,
        )
      },
      test("a region's base is its language") {
        assertTrue(
          Flemish.base == Dutch,
          Language("fr-BE").base == French,
          Flemish.locale.getCountry == "BE",
        )
      },
      test("longer tags are canonicalized subtag by subtag") {
        assertTrue(
          Language("SR-latn-rs").tag == "sr-Latn-RS",
          Language("de-CH-1996").tag == "de-CH-1996",
          Language("en-US-u-CA-gregory").tag == "en-US-u-ca-gregory",
          Language("de-x-Private").tag == "de-x-private",
          Language.parse("toolong-US").isLeft,
        )
      },
      test("a language serializes and still equals itself") {
        assertTrue(German.isInstanceOf[java.io.Serializable])
      },
      test("a JsonCodec can still be summoned") {
        val codec = summon[JsonCodec[Language]]
        assertTrue(codec.decoder.decodeJson(codec.encoder.encodeJson(German).toString) == Right(German))
      },
      test("what is not a language tag is refused, and of() falls back to English") {
        assertTrue(
          Language.parse("invalid").isLeft,
          Language.parse("").isLeft,
          Language.parse("e").isLeft,
          Language.parse("en-").isLeft,
          Language.of("not a tag") == English,
          scala.util.Try(Language("1x")).isFailure,
        )
      },
      test("the built-in names and values are those of the enum") {
        assertTrue(
          Language.values.toList == List(English, Dutch, French),
          Language.valueOf("Dutch") == Dutch,
          scala.util.Try(Language.valueOf("German")).isFailure,
          English.toString == "English",
          English.isDefault,
        )
      },
      test("a match on the built-ins still works, other languages take the wildcard") {
        assertTrue(
          greeting(Language("en")) == "Hello",
          greeting(Dutch) == "Dag",
          greeting(Language("fr")) == "Bonjour",
          greeting(German) == "?",
        )
      },
    ),
    suite("Message fallback")(
      test("a region falls back to its language, then to English") {
        val both    = Message.with0Args(English -> "Bike", Dutch -> "Fiets")
        val english = Message.with0Args(English -> "Bike")
        val exact   = Message.with0Args(English -> "Bike", Dutch -> "Fiets", Flemish -> "Velo")
        assertTrue(
          both.translate(Flemish) == "Fiets",
          english.translate(Flemish) == "Bike",
          exact.translate(Flemish) == "Velo",
          exact.translate(Dutch) == "Fiets",
          both.translate(German) == "Bike",
        )
      },
      test("a message written for another language is used for it") {
        val msg = Message.withNamedArgs(English -> "{$n} items", German -> "{$n} Artikel")(
          "n" -> 3
        )
        assertTrue(msg.translate(German) == "3 Artikel")
      },
      test("French plural rules hold for a French region") {
        val msg = Message.withNamedArgs(
          English -> ".match {$n :plural}\nwhen one {one}\nwhen other {many}",
          French -> ".match {$n :plural}\nwhen one {un}\nwhen other {plusieurs}",
        )("n" -> 0)
        assertTrue(
          msg.translate(French).trim == "un",
          msg.translate(Language("fr-BE")).trim == "un",
          msg.translate(English).trim == "many",
        )
      },
    ),
    suite("label")(
      test("the built-ins keep their hand translations") {
        assertTrue(
          Dutch.label.translate(English) == "Dutch",
          Dutch.label.translate(French) == "Néerlandais",
          English.label.translate(Dutch) == "Engels",
        )
      },
      test("another language is named by the platform, in the built-ins and in itself") {
        assertTrue(
          German.label.translate(English) == "German",
          German.label.translate(Dutch) == "Duits",
          German.label.translate(French) == "allemand",
          German.label.translate(German) == "Deutsch",
        )
      },
    ),
    suite("JSON")(
      test("the built-ins are still written by name") {
        assertTrue(
          English.toJson == "\"English\"",
          Dutch.toJson == "\"Dutch\"",
          French.toJson == "\"French\"",
        )
      },
      test("other languages are written by tag, and both forms round-trip") {
        val all = List(English, Dutch, French, German, Flemish)
        assertTrue(
          German.toJson == "\"de\"",
          Flemish.toJson == "\"nl-BE\"",
          all.forall(l => l.toJson.fromJson[Language] == Right(l)),
        )
      },
      test("decoding reads names (stored data) and tags") {
        assertTrue(
          "\"Dutch\"".fromJson[Language] == Right(Dutch),
          "\"nl\"".fromJson[Language] == Right(Dutch),
          "\"de\"".fromJson[Language] == Right(German),
          "\"Klingon!\"".fromJson[Language].isLeft,
        )
      },
      test("map keys use the same two forms") {
        val map     = Map[Language, String](English -> "a", German -> "b", Flemish -> "c")
        val encoded = map.toJson
        assertTrue(
          encoded == """{"English":"a","de":"b","nl-BE":"c"}""",
          encoded.fromJson[Map[Language, String]] == Right(map),
          """{"Dutch":"x","fr":"y"}""".fromJson[Map[Language, String]] ==
          Right(Map(Dutch -> "x", French -> "y")),
        )
      },
      test("a message with another language round-trips") {
        val msg = Message.with0Args(English -> "Hello", German -> "Hallo", Flemish -> "Hoi")
        assertTrue(msg.toJson.fromJson[Message].map(_.translate(German)) == Right("Hallo"))
      },
      test("a message stored by the enum still reads") {
        val stored = """{"args":[],"namedArgs":{},"translationTemplates":{"English":"Hi","French":"Salut"}}"""
        assertTrue(stored.fromJson[Message].map(_.translate(French)) == Right("Salut"))
      },
    ),
  )

end OpenLanguageSpec
