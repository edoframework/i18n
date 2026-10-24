package dev.taihei.tsuji

import scala.scalajs.js
import scala.util.Try

private[tsuji] object PlatformPreferredLanguages:

  /**
   * The runtime's preferred language tags: `navigator.languages`, else `navigator.language`, else
   * the locale `Intl.DateTimeFormat` resolves to. A browser, a worker and Node with or without
   * `navigator` each have some of these; whatever is missing or of the wrong type is skipped, and
   * nothing here throws.
   */
  def tags: Vector[String] = select(languages, language, intlLocale)

  /**
   * The first non-empty source, in order of preference: a pure choice, so it is tested on
   * supplied values rather than on the runtime that runs the tests.
   */
  def select(
    languages: Vector[String],
    language: Option[String],
    intlLocale: Option[String],
  ): Vector[String] =
    if languages.nonEmpty then languages
    else language.orElse(intlLocale).toVector

  // `typeof navigator` is safe where `navigator` is not declared; reading it directly is not.
  private def navigator: Option[js.Dynamic] =
    Try(Option.when(js.typeOf(js.Dynamic.global.navigator) == "object")(js.Dynamic.global.navigator))
      .toOption.flatten.filter(n => n != null)

  private def string(value: js.Any): Option[String] =
    if js.typeOf(value) == "string" then Some(value.asInstanceOf[String]) else None

  private def languages: Vector[String] =
    Try {
      navigator.toVector.flatMap: n =>
        val ls = n.languages
        if js.Array.isArray(ls) then ls.asInstanceOf[js.Array[js.Any]].toVector.flatMap(string)
        else Vector.empty
    }.getOrElse(Vector.empty)

  private def language: Option[String] =
    Try(navigator.flatMap(n => string(n.language))).toOption.flatten

  private def intlLocale: Option[String] =
    Try {
      if js.typeOf(js.Dynamic.global.Intl) == "object" then
        string(
          js.Dynamic.newInstance(js.Dynamic.global.Intl.DateTimeFormat)().resolvedOptions().locale
        )
      else None
    }.toOption.flatten

end PlatformPreferredLanguages
