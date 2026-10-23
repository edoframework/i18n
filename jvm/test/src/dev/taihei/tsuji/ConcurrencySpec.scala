package dev.taihei.tsuji

import java.time.LocalDate
import java.util.concurrent.{Callable, Executors, TimeUnit}

import scala.jdk.CollectionConverters.*

import zio.test.*

import dev.taihei.tsuji.Language.*

// Threads exist on the JVM only.
object ConcurrencySpec extends ZIOSpecDefault:

  def spec = suite("Catalogues from several threads")(
    test("showing messages while catalogues are added and read from the classpath") {
      given c: Catalogues = Catalogues()
      val pool    = Executors.newFixedThreadPool(16)
      val date    = LocalDate.of(2026, 10, 23)
      val tasks   = (1 to 400).map: i =>
        (
          () =>
            if i % 50 == 0 then
              c.add(Catalogue(Language("es"), Map(Orders.greeting.key -> s"Hola {$$name}")))
              "added"
            else
              val l = List(Language("de"), Dutch, English, Language("es"))(i % 4)
              Orders.greeting(name = "Ann").localize(l) + "|" + Orders.shipped(i, date).localize(l)
        ): Callable[String]
      val results =
        try pool.invokeAll(tasks.asJava, 60, TimeUnit.SECONDS).asScala.map(_.get).toList
        finally pool.shutdown()
      assertTrue(
        results.size == 400,
        results.filter(_.startsWith("Hallo Ann")).nonEmpty,
        results.forall(r => r == "added" || r.contains("Ann|")),
        c.languages.contains(Language("de")),
        c.problems.count(_.message.contains("not a key of order")) == 1,
      )
    }
  )

end ConcurrencySpec
