package com.edoframework.i18n

import java.util.Locale

object PlatformLanguageNames:

  /**
   * The name of the language `tag` in the language `in`, from the JDK's locale data, or `None`
   * when the JDK does not know it (it then returns the code itself).
   */
  def displayName(tag: String, in: String): Option[String] =
    val locale = Locale.forLanguageTag(tag)
    val name   = locale.getDisplayName(Locale.forLanguageTag(in))
    Option(name).filter(n => n.nonEmpty && n != tag && n != locale.getLanguage)
