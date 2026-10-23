package dev.taihei.i18n

import zio.json.*

/**
 * The stable key of a message: its family's prefix and its name, joined by dots
 * (`order.shipped`, `taihei.validation.nonEmpty`). Each part starts with an ASCII letter and holds
 * letters, digits and `_`; a key has at most 255 characters.
 *
 * A key is a contract between the writer and the reader of a message: never reuse one for a
 * different set of arguments.
 */
opaque type MessageKey = String

object MessageKey:

  private val MaxLength = 255

  /** The key for `value`, or why it is not one. */
  def parse(value: String): Either[String, MessageKey] =
    if value.length > MaxLength then Left(s"Not a message key: longer than $MaxLength characters")
    else if isPrefix(value) && value.contains('.') then Right(value)
    else Left(s"Not a message key: '${Printable(value)}'")

  extension (key: MessageKey)
    /** The key as text. */
    def value: String = key

  // One or more parts joined by dots: a family prefix, or a whole key.
  private[i18n] def isPrefix(value: String): Boolean =
    value.length <= MaxLength && value.nonEmpty && value.split("\\.", -1).forall(isPart)

  private[i18n] def isPart(part: String): Boolean =
    part.nonEmpty && isLetter(part.head) &&
    part.forall(c => isLetter(c) || (c >= '0' && c <= '9') || c == '_')

  private def isLetter(c: Char): Boolean = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')

  // For keys the library builds from a checked prefix and a checked name.
  private[i18n] def trusted(value: String): MessageKey = value

  given JsonEncoder[MessageKey] = JsonEncoder.string
  given JsonDecoder[MessageKey] = JsonDecoder.string.mapOrFail(parse)
  given JsonFieldEncoder[MessageKey] = JsonFieldEncoder.string
  given JsonFieldDecoder[MessageKey] = JsonFieldDecoder.string.mapOrFail(parse)

  given Ordering[MessageKey] = Ordering.String

end MessageKey

// Text from input the application did not write, as it may safely appear in an error: anything but
// printable ASCII as \uXXXX, and cut short after 80 characters.
private[i18n] object Printable:

  def apply(s: String): String =
    val cut = if s.length > 80 then s.take(80) + "..." else s
    cut.flatMap(c => if c >= ' ' && c <= '~' then c.toString else "\\u%04x".format(c.toInt))
