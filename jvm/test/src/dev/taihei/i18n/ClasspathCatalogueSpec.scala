package dev.taihei.i18n

import java.io.InputStream

import zio.test.*

import dev.taihei.i18n.Language.*

// The classpath exists on the JVM only.
object ClasspathCatalogueSpec extends ZIOSpecDefault:

  private val German  = Language("de")
  private val Spanish = Language("es")

  def spec = suite("catalogues on the classpath")(
    test("a family's language is read the first time it is needed") {
      given c: Catalogues = Catalogues()
      val before    = c.languages
      val cancelled = Orders.cancelled.localize(German)
      assertTrue(
        before.isEmpty,
        cancelled == "Ihre Bestellung wurde storniert",
        Orders.greeting(name = "Ann").localize(Language("de-AT")) == "Hallo Ann",
        c.languages.contains(German),
        c.problems.exists(p => p.key.map(_.value).contains("shape.circle") && p.message.contains("not a key of order")),
      )
    },
    test("a file in the wrong language, with a bad template, or not in UTF-8 falls back with a problem") {
      given c: Catalogues = Catalogues()
      assertTrue(
        Orders.cancelled.localize(French) == "Your order was cancelled",
        Orders.cancelled.localize(Spanish) == "Your order was cancelled",
        Shapes.circle.localize(German) == "Cirkel",
        c.problems.exists(_.message.contains("says it is de")),
        c.problems.exists(p => p.language.contains(Spanish) && p.message.contains("not closed")),
        c.problems.exists(_.message.contains("not UTF-8")),
      )
    },
    test("ClasspathCatalogues reads and verifies the files") {
      val problems = ClasspathCatalogues.verify(Seq(Orders), Seq(German, French, Spanish), required = Set(German))
      assertTrue(
        ClasspathCatalogues.read(Orders, German).exists(_.exists(_.messages.size == 2)),
        ClasspathCatalogues.read(Orders, Language("ja")).isEmpty,
        problems.exists(_.message.contains("says it is de")),
        problems.exists(p => p.language.contains(Spanish) && p.message.contains("not closed")),
        problems.exists(p => p.key.contains(Orders.shipped.key) && p.message.contains("missing in a required language")),
        problems.exists(_.message.contains("not a key of order")),
      )
    },
    test("languages from a request header stop being looked up after 4096") {
      given c: Catalogues = Catalogues()
      (1 to 4200).foreach(i => Orders.cancelled.localize(Language(s"en-x-n$i")))
      assertTrue(
        Orders.cancelled.localize(German) == "Your order was cancelled",
        c.problems.exists(_.message.contains("more than 4096 languages")),
      )
    },
    test("a file larger than the limit is refused after reading the limit, not all of it") {
      var served  = 0L
      val endless = new InputStream:
        def read(): Int =
          served += 1; 'a'
        override def read(
          b: Array[Byte],
          off: Int,
          len: Int,
        ): Int          =
          java.util.Arrays.fill(b, off, off + len, 'a'.toByte)
          served += len
          len
      val result  = PlatformCatalogueResources.readBounded(endless, "endless.json")
      assertTrue(result.left.exists(_.contains("larger than")), served == Catalogue.MaxLength.toLong * 4 + 1)
    },
  )

end ClasspathCatalogueSpec
