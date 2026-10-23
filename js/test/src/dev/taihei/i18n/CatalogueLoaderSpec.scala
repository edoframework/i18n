package dev.taihei.i18n

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
  )

end CatalogueLoaderSpec
