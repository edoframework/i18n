package dev.taihei.i18n

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.format.{DateTimeFormatter, FormatStyle}

object PlatformDateTimeFormatter:

  def formatDateTime(
    value: Any,
    options: Map[String, String],
    locale: String,
  ): String =
    val javaLocale = PlatformLocales.javaLocale(locale)

    value match
      case ld: LocalDate =>
        val style = options.get("dateStyle").flatMap(parseFormatStyle).getOrElse(FormatStyle.MEDIUM)
        val formatter = DateTimeFormatter.ofLocalizedDate(style).withLocale(javaLocale)
        formatter.format(ld)

      case ldt: LocalDateTime =>
        val dateStyle =
          options.get("dateStyle").flatMap(parseFormatStyle).getOrElse(FormatStyle.MEDIUM)
        val timeStyle =
          options.get("timeStyle").flatMap(parseFormatStyle).getOrElse(FormatStyle.MEDIUM)
        val formatter =
          DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle).withLocale(javaLocale)
        formatter.format(ldt)

      case lt: LocalTime =>
        val style = options.get("timeStyle").flatMap(parseFormatStyle).getOrElse(FormatStyle.MEDIUM)
        val formatter = DateTimeFormatter.ofLocalizedTime(style).withLocale(javaLocale)
        formatter.format(lt)

      case _ =>
        // Fallback for non-date/time values
        value.toString
    end match

  end formatDateTime

  private def parseFormatStyle(style: String): Option[FormatStyle] =
    style.toLowerCase match
      case "short"  => Some(FormatStyle.SHORT)
      case "medium" => Some(FormatStyle.MEDIUM)
      case "long"   => Some(FormatStyle.LONG)
      case "full"   => Some(FormatStyle.FULL)
      case _        => None

end PlatformDateTimeFormatter
