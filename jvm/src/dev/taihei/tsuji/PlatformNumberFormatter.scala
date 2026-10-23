package dev.taihei.tsuji

import java.text.NumberFormat
import java.util.Locale

private[tsuji] object PlatformNumberFormatter:

  def formatNumber(
    value: Double,
    style: Option[String],
    locale: String,
  ): String =
    val javaLocale = PlatformLocales.javaLocale(locale)

    style match
      case Some("percent") =>
        val formatter = NumberFormat.getPercentInstance(javaLocale)
        formatter.setMinimumFractionDigits(0)
        formatter.setMaximumFractionDigits(0)
        formatter.format(value)

      case Some("currency") =>
        // Use the appropriate currency for the locale
        NumberFormat.getCurrencyInstance(javaLocale)
        // For now, always use USD regardless of locale
        val usCurrencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        usCurrencyFormatter.setMinimumFractionDigits(2)
        usCurrencyFormatter.setMaximumFractionDigits(2)
        usCurrencyFormatter.format(value)

      case _ =>
        if value == value.toInt then
          value.toInt.toString
        else
          val formatter = NumberFormat.getInstance(javaLocale)
          formatter.setMinimumFractionDigits(2)
          formatter.setMaximumFractionDigits(2)
          formatter.format(value)

    end match

  end formatNumber

end PlatformNumberFormatter
