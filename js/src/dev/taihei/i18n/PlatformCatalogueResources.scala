package dev.taihei.i18n

import scala.annotation.unused

// In Scala.js there is no classpath: catalogues reach a registry through CatalogueLoader.
private[i18n] object PlatformCatalogueResources:

  /** Whether there is a classpath to read catalogues from. */
  val available: Boolean = false

  def read(@unused family: MessageFamily, @unused tag: String): Option[Either[String, String]] =
    None
