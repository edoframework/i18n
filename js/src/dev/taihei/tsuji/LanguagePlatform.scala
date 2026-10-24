package dev.taihei.tsuji

/**
 * What the companion `Language` has on Scala.js only: nothing. On the JVM this trait holds
 * `fromLocale`; JavaScript has no locale type Tsuji takes as an argument, so an `Intl.Locale` is
 * read by its tag, `Language.parse(intlLocale.toString)`.
 */
private[tsuji] trait LanguagePlatform
