package dev.taihei.tsuji

import scala.concurrent.ExecutionContext
import scala.scalajs.js.URIUtils

import zio.test.*

import zio.ZIO

// fetch and Future-based loading exist in Scala.js only; Node's fetch reads data: URLs.
object CatalogueLoaderSpec extends ZIOSpecDefault:

  private val German = Language("de")

  private def dataUrl(json: String) = "data:application/json," + URIUtils.encodeURIComponent(json)

  private def load(
    c: Catalogues,
    l: Language,
    url: String,
  ) =
    ZIO.fromFuture(ec =>
      CatalogueLoader.load(c, l, url)(
        using ec: ExecutionContext
      )
    )

  def spec = suite("CatalogueLoader")(
    test("a fetched catalogue is added and its listeners hear of it") {
      given c: Catalogues = Catalogues(Seq(Orders))
      var heard  = List.empty[Language]
      c.onAdded(l => heard = l :: heard)
      val before = Orders.cancelled.localize(German)
      for problems <-
          load(c, German, dataUrl("""{"format":1,"language":"de","messages":{"order.cancelled":"Storniert"}}"""))
      yield assertTrue(
        problems.isEmpty,
        before == "Your order was cancelled",
        Orders.cancelled.localize(German) == "Storniert",
        heard == List(German),
      )
    },
    test("CoreMessages show in English until their exported Dutch file is loaded") {
      given c: Catalogues = Catalogues(CoreMessages.families)
      val min    = CoreMessages.Validation.stringMinLength(min = 8)
      val before = min.localize(Language.Dutch)
      val file   =
        """{"format":1,"language":"nl","messages":{"taihei.validation.stringMinLength":"lengte moet minimaal {$min} zijn"}}"""
      for problems <- load(c, Language.Dutch, dataUrl(file))
      yield assertTrue(
        problems.isEmpty,
        before == "length should be greater than 8",
        min.localize(Language.Dutch) == "lengte moet minimaal 8 zijn",
      )
    },
    test("a body larger than the limit is dropped while it is read, also without a declared length") {
      import scala.scalajs.js
      val c        = Catalogues(Seq(Orders))
      val original = js.Dynamic.global.fetch
      val big      = "x" * (Catalogue.MaxLength * 4 + 10)
      js.Dynamic.global.updateDynamic("fetch")((_: String) =>
        js.Promise.resolve[js.Dynamic](js.Dynamic.newInstance(js.Dynamic.global.Response)(big))
      )
      load(c, German, "https://example.invalid/de.json")
        .map(problems => assertTrue(problems.exists(_.message.contains("larger than"))))
        .ensuring(zio.ZIO.succeed(js.Dynamic.global.updateDynamic("fetch")(original)))
    },
    test("a file in another language, a file that is no catalogue, and a failed fetch are problems") {
      val c = Catalogues(Seq(Orders))
      for
        other   <- load(c, German, dataUrl("""{"format":1,"language":"fr","messages":{}}"""))
        garbage <- load(c, German, dataUrl("[[[["))
        failed  <- load(c, German, "nosuchscheme://nowhere")
      yield assertTrue(
        other.exists(_.message.contains("catalogue of fr")),
        garbage.exists(_.message.contains("not JSON")),
        failed.exists(_.message.contains("cannot fetch")),
        c.languages.isEmpty,
      )
    },
  ) @@ TestAspect.sequential // one test replaces the global fetch for a while

end CatalogueLoaderSpec
