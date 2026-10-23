package dev.taihei.i18n

import java.time.{LocalDate, LocalDateTime, LocalTime}

import zio.json.*

import dev.taihei.i18n.Language.*

type MessageArg = String | Int | Long | Double | Float | Boolean | LocalDate | LocalDateTime |
  LocalTime

case class Message(
  args: List[MessageArg],
  namedArgs: Map[String, MessageArg] = Map.empty,
  translationTemplates: Map[Language, String]):

  private def message(template: String, language: Language): String =
    val positionalArgs = args.zipWithIndex.map:
      case (value, idx) => idx.toString -> value
    val mf2Args        = namedArgs ++ positionalArgs
    val formattedArgs  = mf2Args.map:
      case (k, v) => k -> (v: Any)
    MessageFormat2.format(template, formattedArgs, language.isoCode)

  // Exact, then the base language (a Belgian Dutch reader gets the Dutch text when no nl-BE one
  // was written), then English.
  private def template(language: Language): String = translationTemplates
    .get(language)
    .orElse(translationTemplates.get(language.base))
    .orElse(translationTemplates.get(Language.defaultLanguage))
    .getOrElse("!!unknown message!!")

  def translate(language: Language): String =
    message(template(language), language)

  def render(language: Language): String = translate(language)

  override lazy val toString: String = translate(English)

  lazy val nonEmpty: Boolean = toString.nonEmpty

end Message

object Message:

  /**
   * Writes and reads a message as `{"args": [...], "namedArgs": {...}, "translationTemplates":
   * {...}}`. A message read from JSON holds at most 1000 templates and 1000 arguments (positional
   * and named together), and no template, argument or argument name longer than 65536
   * characters; a larger one is a decoding error.
   */
  given JsonCodec[Message] =
    val derived = DeriveJsonCodec.gen[Message]
    JsonCodec(derived.encoder, derived.decoder.mapOrFail(withinBounds))

  // JSON is where a message comes from outside the program. These bounds keep what reading it
  // and formatting it cost in proportion to what a real message needs.
  private val MaxTemplates    = 1000
  private val MaxArgs         = 1000
  private val MaxStringLength = 65536

  private def tooLong(s: String): Boolean = s.length > MaxStringLength

  private def argTooLong(arg: MessageArg): Boolean = arg match
    case s: String => tooLong(s)
    case _         => false

  private def withinBounds(m: Message): Either[String, Message] =
    if m.translationTemplates.size > MaxTemplates then
      Left(s"a message holds more than $MaxTemplates templates")
    else if m.translationTemplates.values.exists(tooLong) then
      Left(s"a template is longer than $MaxStringLength characters")
    else if m.args.size + m.namedArgs.size > MaxArgs then
      Left(s"a message holds more than $MaxArgs arguments")
    else if m.args.exists(argTooLong) || m.namedArgs.values.exists(argTooLong) then
      Left(s"an argument is longer than $MaxStringLength characters")
    else if m.namedArgs.keys.exists(tooLong) then
      Left(s"an argument name is longer than $MaxStringLength characters")
    else Right(m)

  given JsonEncoder[MessageArg] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[MessageArg] = JsonDecoder[String].map(identity)

  /**
   * The throwable's message, in English. A throwable without one (`new RuntimeException()`) gives
   * its `toString`, the class name, rather than a message that cannot be translated.
   */
  def of(throwable: Throwable): Message =
    Message(Nil, Map.empty, Map(English -> Option(throwable.getMessage).getOrElse(throwable.toString)))

  def of(value: String): Message = Message(Nil, Map.empty, Map(English -> value))

  def withNamedArgs(
    translations: (Language, String)*
  )(
    arguments: (String, MessageArg)*
  ): Message = Message(Nil, arguments.toMap, translations.toMap)

  def with0Args(translations: (Language, String)*): Message =
    Message(Nil, Map.empty, translations.toMap)

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with1Arg[A <: MessageArg](translations: (Language, String)*): A => Message = arg1 =>
    Message(List(arg1), Map.empty, translations.toMap)

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with2Args[A <: MessageArg, B <: MessageArg](
    translations: (Language, String)*
  ): (A, B) => Message =
    (arg1, arg2) => Message(List(arg1, arg2), Map.empty, translations.toMap)

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with3Args[A <: MessageArg, B <: MessageArg, C <: MessageArg](
    translations: (Language, String)*
  ): (A, B, C) => Message =
    (
      arg1,
      arg2,
      arg3,
    ) => Message(List(arg1, arg2, arg3), Map.empty, translations.toMap)

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with4Args[A <: MessageArg, B <: MessageArg, C <: MessageArg, D <: MessageArg](
    translations: (Language, String)*
  ): (A, B, C, D) => Message =
    (
      arg1,
      arg2,
      arg3,
      arg4,
    ) => Message(List(arg1, arg2, arg3, arg4), Map.empty, translations.toMap)

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with5Args[
    A <: MessageArg,
    B <: MessageArg,
    C <: MessageArg,
    D <: MessageArg,
    E <: MessageArg,
  ](
    translations: (Language, String)*
  ): (A, B, C, D, E) => Message =
    (
      arg1,
      arg2,
      arg3,
      arg4,
      arg5,
    ) =>
      Message(List(arg1, arg2, arg3, arg4, arg5), Map.empty, translations.toMap)

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with6Args[
    A <: MessageArg,
    B <: MessageArg,
    C <: MessageArg,
    D <: MessageArg,
    E <: MessageArg,
    F <: MessageArg,
  ](
    translations: (Language, String)*
  ): (A, B, C, D, E, F) => Message =
    (
      arg1,
      arg2,
      arg3,
      arg4,
      arg5,
      arg6,
    ) =>
      Message(List(arg1, arg2, arg3, arg4, arg5, arg6), Map.empty, translations.toMap)

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with7Args[
    A <: MessageArg,
    B <: MessageArg,
    C <: MessageArg,
    D <: MessageArg,
    E <: MessageArg,
    F <: MessageArg,
    G <: MessageArg,
  ](
    translations: (Language, String)*
  ): (A, B, C, D, E, F, G) => Message =
    (
      arg1,
      arg2,
      arg3,
      arg4,
      arg5,
      arg6,
      arg7,
    ) =>
      Message(List(arg1, arg2, arg3, arg4, arg5, arg6, arg7), Map.empty, translations.toMap)

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with8Args[
    A <: MessageArg,
    B <: MessageArg,
    C <: MessageArg,
    D <: MessageArg,
    E <: MessageArg,
    F <: MessageArg,
    G <: MessageArg,
    H <: MessageArg,
  ](
    translations: (Language, String)*
  ): (A, B, C, D, E, F, G, H) => Message =
    (
      arg1,
      arg2,
      arg3,
      arg4,
      arg5,
      arg6,
      arg7,
      arg8,
    ) =>
      Message(
        List(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8),
        Map.empty,
        translations.toMap,
      )

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with9Args[
    A <: MessageArg,
    B <: MessageArg,
    C <: MessageArg,
    D <: MessageArg,
    E <: MessageArg,
    F <: MessageArg,
    G <: MessageArg,
    H <: MessageArg,
    I <: MessageArg,
  ](
    translations: (Language, String)*
  ): (A, B, C, D, E, F, G, H, I) => Message =
    (
      arg1,
      arg2,
      arg3,
      arg4,
      arg5,
      arg6,
      arg7,
      arg8,
      arg9,
    ) =>
      Message(
        List(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9),
        Map.empty,
        translations.toMap,
      )

  @deprecated("Use withNamedArgs with named placeholders instead of positional {$0}", "0.42.0")
  def with10Args[
    A <: MessageArg,
    B <: MessageArg,
    C <: MessageArg,
    D <: MessageArg,
    E <: MessageArg,
    F <: MessageArg,
    G <: MessageArg,
    H <: MessageArg,
    I <: MessageArg,
    J <: MessageArg,
  ](
    translations: (Language, String)*
  ): (A, B, C, D, E, F, G, H, I, J) => Message =
    (
      arg1,
      arg2,
      arg3,
      arg4,
      arg5,
      arg6,
      arg7,
      arg8,
      arg9,
      arg10,
    ) =>
      Message(
        List(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10),
        Map.empty,
        translations.toMap,
      )

end Message
