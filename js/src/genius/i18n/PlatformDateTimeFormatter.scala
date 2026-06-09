package genius.i18n

import java.time.{LocalDate, LocalDateTime, LocalTime}

import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("Intl.DateTimeFormat")
class DateTimeFormat(@unused locale: String, @unused options: js.Dictionary[Any]) extends js.Object:
  def format(date: js.Date): String = js.native

object PlatformDateTimeFormatter:

  def formatDateTime(
    value: Any,
    options: Map[String, String],
    locale: String,
  ): String =
    // Convert locale to browser format
    val jsLocale = locale match
      case "en" => "en-US"
      case "nl" => "nl-NL"
      case "fr" => "fr-FR"
      case _    => "en-US"

    value match
      case ld: LocalDate =>
        val jsDate    = toJsDate(ld)
        val dateStyle = options.get("dateStyle").getOrElse("medium")
        val jsOptions = js.Dictionary[Any](
          "dateStyle" -> dateStyle
        )
        new DateTimeFormat(jsLocale, jsOptions).format(jsDate)

      case ldt: LocalDateTime =>
        val jsDate    = toJsDate(ldt)
        val dateStyle = options.get("dateStyle").getOrElse("medium")
        val timeStyle = options.get("timeStyle").getOrElse("medium")
        val jsOptions = js.Dictionary[Any](
          "dateStyle" -> dateStyle,
          "timeStyle" -> timeStyle,
        )
        new DateTimeFormat(jsLocale, jsOptions).format(jsDate)

      case lt: LocalTime =>
        // For LocalTime, use today's date with the given time
        val jsDate    = toJsDate(lt)
        val timeStyle = options.get("timeStyle").getOrElse("medium")
        val jsOptions = js.Dictionary[Any](
          "timeStyle" -> timeStyle
        )
        new DateTimeFormat(jsLocale, jsOptions).format(jsDate)

      case _ =>
        // Fallback for non-date/time values
        value.toString
    end match

  end formatDateTime

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
