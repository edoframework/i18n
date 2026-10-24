package dev.taihei.tsuji

import java.util.Locale

/**
 * What the companion `Language` has on the JVM only: the factory from a JDK locale. A companion
 * cannot be split across source sets, so `object Language` extends this trait, which Scala.js
 * defines empty.
 */
private[tsuji] trait LanguagePlatform:

  /**
   * The language of a JDK locale: `Language.parse(locale.toLanguageTag)`, except that a locale
   * without a language (`Locale.ROOT`, any tag `und` or `und-…`) is a `Left`. Extensions stay,
   * as in `parse`: `th_TH_TH` gives `th-TH-u-nu-thai`. The JDK maps its legacy forms first
   * (`iw` gives `he`).
   *
   * {{{
   * Language.fromLocale(Locale.forLanguageTag("nl-BE")) // Right(nl-BE)
   * Language.fromLocale(Locale.ROOT)                    // Left(...)
   * }}}
   *
   * The other way round is `Locale.forLanguageTag(language.tag)`.
   */
  def fromLocale(locale: Locale): Either[String, Language] =
    Language.fromPlatformTag(locale.toLanguageTag)

end LanguagePlatform
