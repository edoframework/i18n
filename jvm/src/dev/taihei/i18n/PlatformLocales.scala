package dev.taihei.i18n

import java.util.Locale

object PlatformLocales:

  /**
   * The JDK locale a formatter uses for a language tag. A bare `en`, `nl` or `fr` keeps the
   * region it always had (US, NL, FR), so their output did not change when any language became
   * possible (APP-2338); any other tag is taken as written, and one that names no language falls
   * back to the US format that English uses.
   */
  def javaLocale(tag: String): Locale = tag match
    case "en" => Locale.US
    case "nl" => Locale.of("nl", "NL")
    case "fr" => Locale.FRANCE
    case _    =>
      val locale = Locale.forLanguageTag(tag)
      if locale.getLanguage.isEmpty then Locale.US else locale

end PlatformLocales
