package dev.taihei.tsuji

import java.util.Locale

private[tsuji] object PlatformPreferredLanguages:

  /** The JVM's preferred language: the default locale for display, as its only element. */
  def tags: Vector[String] = Vector(Locale.getDefault(Locale.Category.DISPLAY).toLanguageTag)
