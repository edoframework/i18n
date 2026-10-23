package dev.taihei.i18n

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.util.control.NonFatal

/**
 * Fetches a language's catalogue file in a browser (or anywhere `fetch` exists) and adds it to a
 * registry, whose [[Catalogues.onAdded]] listeners then hear of it. No dependency on Laminar or
 * on an effect library: it uses the global `fetch` and a `Future`.
 *
 * {{{
 * given Catalogues = Catalogues(Seq(Orders))
 * CatalogueLoader.load(summon[Catalogues], Language("de"), "/i18n/de.json")
 * }}}
 *
 * Until the file arrives, messages show through the fallback chain. On a language switch, load
 * the next language; this library does not prefetch.
 */
object CatalogueLoader:

  // UTF-8 takes at most four bytes per character of a catalogue.
  private val MaxBytes = Catalogue.MaxLength.toLong * 4

  /**
   * Fetches the file at `url`, checks that it is a catalogue of `language`, and adds it. The
   * `Future` never fails: what went wrong, from the network to a template, is in the list.
   */
  def load(
    catalogues: Catalogues,
    language: Language,
    url: String,
  )(
    using ExecutionContext
  ): Future[List[CatalogueProblem]] =
    def problem(message: String) = List(CatalogueProblem(message, Some(language)))
    val fetched                  =
      try
        val fetch = js.Dynamic.global.fetch
        if js.isUndefined(fetch) then
          Future.failed(new IllegalStateException("there is no fetch here"))
        else
          fetch(url)
            .asInstanceOf[js.Promise[js.Dynamic]]
            .toFuture
            .flatMap: response =>
              // A declared length over the limit is refused before the body is read; a body
              // without one is still held to Catalogue.parse's limit after it is read.
              val length   = response.headers.get("content-length")
              val declared =
                if js.isUndefined(length) || length == null then None
                else String.valueOf(length).trim.toLongOption
              if !response.ok.asInstanceOf[Boolean] then
                Future.failed(new IllegalStateException(s"HTTP ${response.status}"))
              else if declared.exists(_ > MaxBytes) then
                Future.failed(new IllegalStateException(s"larger than $MaxBytes bytes"))
              else response.text().asInstanceOf[js.Promise[String]].toFuture
        end if
      catch case NonFatal(e) => Future.failed(e)
    fetched
      .map: text =>
        Catalogue.parse(text) match
          case Left(p) => List(p.copy(language = Some(language)))
          case Right(catalogue) if catalogue.language != language =>
            problem(s"the file at ${Printable(url)} is a catalogue of ${catalogue.language.tag}")
          case Right(catalogue)                                   => catalogues.add(catalogue)
      .recover:
        case NonFatal(e) =>
          problem(s"cannot fetch ${Printable(url)}: ${Printable(String.valueOf(e.getMessage))}")
  end load

end CatalogueLoader
