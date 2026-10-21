package com.edoframework.i18n

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("Intl.NumberFormat")
class NumberFormat(@unused locale: String, @unused options: js.Dictionary[Any]) extends js.Object:
  def format(value: Double): String = js.native

object PlatformNumberFormatter:

  def formatNumber(
    value: Double,
    style: Option[String],
    locale: String,
  ): String =
    val jsLocale = PlatformLocales.jsLocale(locale)

    style match
      case Some("percent") =>
        val options = js.Dictionary[Any](
          "style" -> "percent",
          "minimumFractionDigits" -> 0,
          "maximumFractionDigits" -> 0,
        )
        new NumberFormat(jsLocale, options).format(value)

      case Some("currency") =>
        val options = js.Dictionary[Any](
          "style" -> "currency",
          "currency" -> "USD", // Always USD for now
          "minimumFractionDigits" -> 2,
          "maximumFractionDigits" -> 2,
        )
        new NumberFormat(jsLocale, options).format(value)

      case _ =>
        if value == value.toInt then
          value.toInt.toString
        else
          val options = js.Dictionary[Any](
            "minimumFractionDigits" -> 2,
            "maximumFractionDigits" -> 2,
          )
          new NumberFormat(jsLocale, options).format(value)

    end match

  end formatNumber

end PlatformNumberFormatter
