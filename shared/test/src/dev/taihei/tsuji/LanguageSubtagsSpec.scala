package dev.taihei.tsuji

import zio.test.*

/** `script`, `region` and `preferred`, the platform-free side of 5.1.0's platform locales. */
object LanguageSubtagsSpec extends ZIOSpecDefault:

  // tag -> (script, region)
  private val subtags: Vector[(String, Option[String], Option[String])] = Vector(
    ("en", None, None),
    ("nl-BE", None, Some("BE")),
    ("zh-Hant-TW", Some("Hant"), Some("TW")),
    ("es-419", None, Some("419")),
    ("de-1996", None, None),
    ("zh-yue-HK", None, Some("HK")),
    ("sr-Latn", Some("Latn"), None),
    ("en-x-us", None, None),
    ("de-DE-u-co-phonebk", None, Some("DE")),
  )

  def spec = suite("LanguageSubtagsSpec")(
    suite("script and region")(
      test("are read from the subtags before the variants and the extensions") {
        val results = subtags.map(
          (
            tag,
            script,
            region,
          ) =>
            (tag, Language(tag).script == script && Language(tag).region == region)
        )
        assertTrue(results.size == 9, results.forall(_._2))
      },
      test("are read from the canonical tag") {
        val l = Language("SR-latn-rs")
        assertTrue(l.script.contains("Latn"), l.region.contains("RS"))
      },
    ),
    suite("the platform's tags")(
      test("a tag without a language is refused, a language is not") {
        assertTrue(
          Language.fromPlatformTag("und").isLeft,
          Language.fromPlatformTag("und-US").isLeft,
          Language.fromPlatformTag("UND-u-nu-thai").isLeft,
          Language.fromPlatformTag("nl-BE") == Right(Language("nl-BE")),
          Language.fromPlatformTag("en").exists(_ eq Language.English),
        )
      },
      test("keep their order, lose what is refused and keep the first of a repeat") {
        val tags = Vector("nl-BE", "und", "", "not a tag", "en-US", "NL-be", "fr", "en-US", "x" * 300)
        assertTrue(
          Language.fromPlatformTags(tags) == Vector(Language("nl-BE"), Language("en-US"), Language.French),
          Language.fromPlatformTags(Vector.empty).isEmpty,
          Language.fromPlatformTags(Vector("und")).isEmpty,
        )
      },
      // It may be empty here (a JVM whose default is Locale.ROOT); the platform specs show that it
      // reads something: FromLocaleSpec with a set default, PlatformPreferredLanguagesSpec on Node.
      test("preferred holds no repeat and no und, and does not throw") {
        val preferred = Language.preferred
        assertTrue(
          preferred.distinct == preferred,
          !preferred.exists(l => l.tag == "und" || l.tag.startsWith("und-")),
        )
      },
    ),
  )

end LanguageSubtagsSpec
