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
              // A declared length over the limit is refused before the body is read. The
              // declared length is what travels, which a compressed body exceeds once decoded,
              // so the body is also counted as it is read, and dropped past the limit.
              val length   = response.headers.get("content-length")
              val declared =
                if js.isUndefined(length) || length == null then None
                else String.valueOf(length).trim.toLongOption
              if !response.ok.asInstanceOf[Boolean] then
                cancel(response)
                Future.failed(new IllegalStateException(s"HTTP ${response.status}"))
              else if declared.exists(_ > MaxBytes) then
                cancel(response)
                Future.failed(new IllegalStateException(s"larger than $MaxBytes bytes"))
              else readBounded(response)
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

  // Lets go of a body that will not be read, so that its connection can close.
  private def cancel(response: js.Dynamic): Unit =
    try
      val body = response.body
      if !js.isUndefined(body) && body != null then
        body.cancel()
        ()
    catch case NonFatal(_) => ()

  // The body as text, read chunk by chunk and given up as soon as it passes MaxBytes.
  private def readBounded(
    response: js.Dynamic
  )(
    using ExecutionContext
  ): Future[String] =
    val body = response.body
    if js.isUndefined(body) || body == null then
      response.text().asInstanceOf[js.Promise[String]].toFuture
    else
      val reader                 = body.getReader()
      val decoder                = js.Dynamic.newInstance(js.Dynamic.global.TextDecoder)("utf-8")
      val out                    = new StringBuilder
      var total                  = 0.0
      def next(): Future[String] =
        reader
          .read()
          .asInstanceOf[js.Promise[js.Dynamic]]
          .toFuture
          .flatMap: chunk =>
            if chunk.done.asInstanceOf[Boolean] then
              out ++= decoder.decode().asInstanceOf[String]
              Future.successful(out.result())
            else
              total += chunk.value.length.asInstanceOf[Double]
              if total > MaxBytes then
                reader.cancel()
                Future.failed(new IllegalStateException(s"larger than $MaxBytes bytes"))
              else
                out ++=
                  decoder.decode(chunk.value, js.Dynamic.literal(stream = true)).asInstanceOf[String]
                next()
      next()
    end if

  end readBounded

end CatalogueLoader
