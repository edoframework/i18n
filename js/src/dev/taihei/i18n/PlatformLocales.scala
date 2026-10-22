package dev.taihei.i18n

import scala.scalajs.js
import scala.util.Try

object PlatformLocales:

  /**
   * The `Intl` locale a formatter uses for a language tag. A bare `en`, `nl` or `fr` keeps the
   * region it always had (US, NL, FR), so their output did not change when any language became
   * possible (2.0.0); any other valid tag that `Intl` accepts is passed on canonicalized, and
   * anything else falls back to the US format English uses rather than make `Intl` throw.
   */
  def jsLocale(tag: String): String = tag match
    case "en" => "en-US"
    case "nl" => "nl-NL"
    case "fr" => "fr-FR"
    case t    =>
      // A tag valid under RFC 5646 can still be refused by Intl (an extlang such as zh-cmn-Hans
      // throws a RangeError in Node), so Intl itself has the last word.
      Language.parse(t).toOption.map(_.tag).filter(accepted).getOrElse("en-US")

  // format() uses the instance, so the linker cannot drop the constructor call as unused.
  private def accepted(tag: String): Boolean =
    Try(new NumberFormat(tag, js.Dictionary[Any]()).format(0)).isSuccess

end PlatformLocales
