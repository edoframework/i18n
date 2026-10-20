package genius.i18n

object PlatformLocales:

  /**
   * The `Intl` locale a formatter uses for a language tag. A bare `en`, `nl` or `fr` keeps the
   * region it always had (US, NL, FR), so their output did not change when any language became
   * possible (APP-2338); any other valid tag is passed on canonicalized, and anything that is not
   * one falls back to the US format English uses rather than make `Intl` throw a `RangeError`.
   */
  def jsLocale(tag: String): String = tag match
    case "en" => "en-US"
    case "nl" => "nl-NL"
    case "fr" => "fr-FR"
    case t    => Language.parse(t).fold(_ => "en-US", _.tag)

