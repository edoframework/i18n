package genius.i18n

import java.util.Locale

import zio.json.*

/**
 * A language, identified by its canonical BCP 47 tag (`en`, `nl-BE`, `de`, `zh-Hant-TW`).
 *
 * It used to be a closed enum of English, Dutch and French (APP-2338). It is a class now so that
 * any language can be named, while the three built-ins stay `val`s in the companion: `case
 * English =>` still matches (a stable identifier compares with `==`), but a `match` on a
 * `Language` is no longer checked for exhaustiveness and needs a `case _`.
 *
 * Equality and hash are by tag, so `Language("en") == English` and a `Map[Language, _]` built
 * from either finds the other.
 */
final class Language private (val tag: String):

  /** The BCP 47 tag. The name predates regions: for `nl-BE` it is `"nl-BE"`, not `"nl"`. */
  def isoCode: String = tag

  def isDefault: Boolean = this == Language.defaultLanguage

  /** The language without script, region or variant: `nl-BE` gives `nl`, `nl` gives itself. */
  def base: Language =
    val primary = tag.takeWhile(_ != '-')
    if primary == tag then this else Language.fromCanonical(primary)

  def locale: Locale = Locale.forLanguageTag(tag)

  /**
   * The language's name. The built-ins carry hand translations; any other language is named by
   * the platform (the JDK's locale data, `Intl.DisplayNames` in JavaScript) in English, Dutch,
   * French and in itself, and falls back to its tag where the platform does not know it.
   */
  def label: Message = Language.builtInLabels.getOrElse(
    this, {
      val named = (Language.values.toList :+ this).distinct.map: in =>
        in -> PlatformLanguageNames.displayName(tag, in.tag).getOrElse(tag)
      Message.with0Args(named*)
    },
  )

  override def equals(other: Any): Boolean = other match
    case that: Language => tag == that.tag
    case _              => false

  override def hashCode: Int = tag.hashCode

  /**
   * The built-ins keep their enum names (`"English"`), which is also their JSON form; any other
   * language shows its tag.
   */
  override def toString: String = Language.builtInNames.getOrElse(tag, tag)

end Language

object Language:

  val English: Language = new Language("en")
  val Dutch: Language   = new Language("nl")
  val French: Language  = new Language("fr")

  private val builtIns: Vector[Language] = Vector(English, Dutch, French)

  private val builtInNames: Map[String, String] =
    Map("en" -> "English", "nl" -> "Dutch", "fr" -> "French")

  private val byName: Map[String, Language] =
    builtIns.map(l => builtInNames(l.tag) -> l).toMap

  private lazy val builtInLabels: Map[Language, Message] = Map(
    English -> Message.with0Args(
      English -> "English",
      Dutch -> "Engels",
      French -> "Anglais",
    ),
    Dutch -> Message.with0Args(
      English -> "Dutch",
      Dutch -> "Nederlands",
      French -> "Néerlandais",
    ),
    French -> Message.with0Args(
      English -> "French",
      Dutch -> "Frans",
      French -> "Français",
    ),
  )

  // BCP 47 (RFC 5646) langtag: language[-extlang][-script][-region][-variant]*[-extension]*
  // [-privateuse]. The primary language is held to two or three letters (ISO 639), so that a
  // word such as "invalid" is refused rather than read as a reserved 5-8 letter subtag.
  private val LangTag =
    ("(?i)[a-z]{2,3}(-[a-z]{3}){0,3}(-[a-z]{4})?(-([a-z]{2}|[0-9]{3}))?" +
    "(-([a-z0-9]{5,8}|[0-9][a-z0-9]{3}))*(-[0-9a-wyz](-[a-z0-9]{2,8})+)*(-x(-[a-z0-9]{1,8})+)?")
      .r

  /** The language for a BCP 47 tag, canonicalized (`NL-be` gives `nl-BE`), or why it is not one. */
  def parse(tag: String): Either[String, Language] =
    val trimmed = tag.trim.replace('_', '-')
    if LangTag.matches(trimmed) then Right(fromCanonical(canonical(trimmed)))
    else Left(s"Not a BCP 47 language tag: '$tag'")

  /** The language for a BCP 47 tag; throws `IllegalArgumentException` on anything else. */
  def apply(tag: String): Language =
    parse(tag).fold(e => throw new IllegalArgumentException(e), identity)

  /** The built-ins, in the enum's old order. Any other language is made with `Language(tag)`. */
  def values: Array[Language] = builtIns.toArray

  /** A built-in by its name (`"English"`), as the enum's `valueOf` did. */
  def valueOf(name: String): Language =
    byName.getOrElse(
      name,
      throw new IllegalArgumentException(s"enum genius.i18n.Language has no case with name: $name"),
    )

  def defaultLanguage: Language = English

  def empty: Language = English

  def sample: Language =
    val vs = values
    vs(scala.util.Random.nextInt(vs.length))

  /** The language for a BCP 47 tag; English for anything that is not one. */
  def of(isoCode: String): Language = parse(isoCode).getOrElse(defaultLanguage)

  /** A built-in's name or any tag: the two forms the JSON codecs read. */
  private def decode(s: String): Either[String, Language] =
    byName.get(s).map(Right(_)).getOrElse(parse(s))

  // A built-in is written by its name, as the enum was, so stored data and older readers keep
  // working; any other language by its tag. Both forms are read.
  given JsonEncoder[Language] = JsonEncoder.string.contramap(_.toString)
  given JsonDecoder[Language] = JsonDecoder.string.mapOrFail(decode)

  given JsonFieldEncoder[Language] = JsonFieldEncoder.string.contramap(_.toString)
  given JsonFieldDecoder[Language] = JsonFieldDecoder.string.mapOrFail(decode)

  private def fromCanonical(tag: String): Language =
    builtIns.find(_.tag == tag).getOrElse(new Language(tag))

  // RFC 5646 §2.1.1: language lower case, script title case, region upper case, the rest lower.
  // Everything after a singleton (an extension or private use) stays lower case.
  private def canonical(tag: String): String =
    val parts    = tag.split('-').toVector
    val (_, out) = parts.zipWithIndex.foldLeft((false, Vector.empty[String])):
      case ((afterSingleton, acc), (p, i)) =>
        val singleton = p.length == 1
        val formatted =
          if i == 0 || afterSingleton || singleton then p.toLowerCase
          else if p.length == 4 && p.head.isLetter then s"${p.head.toUpper}${p.tail.toLowerCase}"
          else if p.length == 2 || (p.length == 3 && p.head.isDigit) then p.toUpperCase
          else p.toLowerCase
        (afterSingleton || singleton, acc :+ formatted)
    out.mkString("-")

end Language
