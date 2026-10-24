package dev.taihei.tsuji

import java.util.Locale

private[tsuji] object PlatformLocales:

  /**
   * The JDK locale a formatter uses for a language tag. A bare `en`, `nl` or `fr` keeps the
   * region it always had (US, NL, FR), so their output did not change when any language became
   * possible (2.0.0); any other tag is taken as written, and one that names no language falls
   * back to the US format that English uses.
   *
   * Java 17 is the JVM floor: `Locale.of` exists only from Java 19, and the `Locale` constructors
   * are deprecated, so a locale that has no constant is built from its tag.
   */
  def javaLocale(tag: String): Locale = tag match
    case "en" => Locale.US
    case "nl" => Locale.forLanguageTag("nl-NL")
    case "fr" => Locale.FRANCE
    case _    =>
      val locale = Locale.forLanguageTag(tag)
      if locale.getLanguage.isEmpty then Locale.US else locale

end PlatformLocales
