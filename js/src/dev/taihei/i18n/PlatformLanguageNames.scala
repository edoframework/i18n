package dev.taihei.i18n

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.util.Try

@js.native
@JSGlobal("Intl.DisplayNames")
class DisplayNames(@unused locales: js.Array[String], @unused options: js.Dictionary[Any])
  extends js.Object:
  def of(code: String): js.UndefOr[String] = js.native

object PlatformLanguageNames:

  /**
   * The name of the language `tag` in the language `in`, from `Intl.DisplayNames`, or `None`
   * when the runtime does not know it (it then returns the code itself, or nothing).
   */
  def displayName(tag: String, in: String): Option[String] =
    Try {
      val names = new DisplayNames(js.Array(in), js.Dictionary[Any]("type" -> "language"))
      names.of(tag).toOption
    }.toOption.flatten.filter(n => n.nonEmpty && n != tag)
