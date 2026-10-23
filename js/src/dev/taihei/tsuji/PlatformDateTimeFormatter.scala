package dev.taihei.tsuji

import java.time.{LocalDate, LocalDateTime, LocalTime}

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("Intl.DateTimeFormat")
private[tsuji] class DateTimeFormat(@unused locale: String, @unused options: js.Dictionary[Any])
  extends js.Object:
  def format(date: js.Date): String = js.native

private[tsuji] object PlatformDateTimeFormatter:

  def formatDateTime(
    value: Any,
    options: Map[String, String],
    locale: String,
  ): String =
    val jsLocale = PlatformLocales.jsLocale(locale)

    value match
      case ld: LocalDate =>
        val jsDate    = toJsDate(ld)
        val dateStyle = styleOf(options, "dateStyle")
        val jsOptions = js.Dictionary[Any](
          "dateStyle" -> dateStyle
        )
        new DateTimeFormat(jsLocale, jsOptions).format(jsDate)

      case ldt: LocalDateTime =>
        val jsDate    = toJsDate(ldt)
        val dateStyle = styleOf(options, "dateStyle")
        val timeStyle = styleOf(options, "timeStyle")
        val jsOptions = js.Dictionary[Any](
          "dateStyle" -> dateStyle,
          "timeStyle" -> timeStyle,
        )
        new DateTimeFormat(jsLocale, jsOptions).format(jsDate)

      case lt: LocalTime =>
        // For LocalTime, use today's date with the given time
        val jsDate    = toJsDate(lt)
        val timeStyle = styleOf(options, "timeStyle")
        val jsOptions = js.Dictionary[Any](
          "timeStyle" -> timeStyle
        )
        new DateTimeFormat(jsLocale, jsOptions).format(jsDate)

      case _ =>
        // Fallback for non-date/time values
        value.toString
    end match

  end formatDateTime

  // Intl throws a RangeError on a style it does not know. Like the JVM side, take the style in
  // any case and fall back to medium for anything else.
  private val styles = Set("short", "medium", "long", "full")

  private def styleOf(options: Map[String, String], key: String): String =
    options.get(key).map(_.toLowerCase).filter(styles).getOrElse("medium")

  private def toJsDate(ld: LocalDate): js.Date =
    new js.Date(ld.getYear, ld.getMonthValue - 1, ld.getDayOfMonth)

  private def toJsDate(ldt: LocalDateTime): js.Date =
    new js.Date(
      ldt.getYear,
      ldt.getMonthValue - 1,
      ldt.getDayOfMonth,
      ldt.getHour,
      ldt.getMinute,
      ldt.getSecond,
    )

  private def toJsDate(lt: LocalTime): js.Date =
    val now = new js.Date()
    new js.Date(
      now.getFullYear().toInt,
      now.getMonth().toInt,
      now.getDate().toInt,
      lt.getHour,
      lt.getMinute,
      lt.getSecond,
    )

end PlatformDateTimeFormatter
