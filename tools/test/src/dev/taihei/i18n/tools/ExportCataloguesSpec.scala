package dev.taihei.i18n.tools

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import zio.json.*
import zio.json.ast.Json
import zio.test.*

import dev.taihei.i18n.*
import dev.taihei.i18n.Language.*

object Shop extends MessageFamily("shop"):
  val open  = message("Open", Dutch -> "Open", Language("de") -> "Geöffnet")
  val items = message[(n: Int)]("{$n} items", Dutch -> "{$n} artikels")

object Broken extends MessageFamily("broken"):
  private val dutch = "{$nope}"
  val a             = message("A", Dutch -> dutch)

object ExportCataloguesSpec extends ZIOSpecDefault:

  private def run(args: String*): (Int, String, String) =
    val out    = new ByteArrayOutputStream()
    val err    = new ByteArrayOutputStream()
    val status =
      ExportCatalogues.run(args.toList, new PrintStream(out, true, "UTF-8"), new PrintStream(err, true, "UTF-8"))
    (status, out.toString("UTF-8"), err.toString("UTF-8"))

  private def messages(dir: Path, tag: String): Map[String, String] =
    Files
      .readString(dir.resolve(s"$tag.json"), StandardCharsets.UTF_8)
      .fromJson[Json]
      .toOption
      .flatMap(_.asObject)
      .flatMap(_.get("messages"))
      .flatMap(_.as[Map[String, String]].toOption)
      .getOrElse(Map.empty)

  def spec = suite("ExportCatalogues")(
    test("merged per-language files of the named families and those of a library") {
      val dir                = Files.createTempDirectory("taihei-export")
      val (status, out, err) =
        run("--out", dir.toString, "--family", "dev.taihei.i18n.tools.Shop", "--scan", "dev.taihei.i18n")
      val en                 = messages(dir, "en")
      val nl                 = messages(dir, "nl")
      val de                 = messages(dir, "de")
      assertTrue(
        en.get("shop.items").contains("{$n} items"),
        en.get("taihei.validation.stringMinLength").contains("length should be greater than {$min}"),
        nl.get("shop.items").contains("{$n} artikels"),
        nl.get("taihei.validation.stringMinLength").contains("lengte moet minimaal {$min} zijn"),
        de == Map("shop.open" -> "Geöffnet"),
        Catalogue.parse(Files.readString(dir.resolve("fr.json"))).isRight,
        out.contains("messages in nl"),
        // Broken is found by the scan of dev.taihei.i18n: its bad translation is a problem, and left out.
        status == 1,
        err.contains("broken.a"),
        !nl.contains("broken.a"),
      )
    },
    test("a family named twice and nested objects are found") {
      val dir            = Files.createTempDirectory("taihei-export")
      val (status, _, _) = run(
        "--out",
        dir.toString,
        "--family",
        "dev.taihei.i18n.CoreMessages.Validation",
        "--family",
        "dev.taihei.i18n.tools.Shop",
        "--family",
        "dev.taihei.i18n.tools.Shop",
      )
      assertTrue(status == 0, messages(dir, "fr").contains("taihei.validation.nonEmpty"))
    },
    test("usage errors") {
      assertTrue(
        run()._1 == 2,
        run("--out", "x")._1 == 2,
        run("--bogus")._1 == 2,
        run("--out", Files.createTempDirectory("x").toString, "--family", "no.such.Family")._1 == 2,
      )
    },
  )

end ExportCataloguesSpec
