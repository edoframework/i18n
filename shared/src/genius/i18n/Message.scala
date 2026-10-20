package genius.i18n

import java.time.{LocalDate, LocalDateTime, LocalTime}

import zio.json.*

import genius.i18n.Language.*

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
  // Make the codec available
  given JsonCodec[Message] = DeriveJsonCodec.gen[Message]

  given JsonEncoder[MessageArg] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[MessageArg] = JsonDecoder[String].map(identity)

  def of(throwable: Throwable): Message =
    Message(Nil, Map.empty, Map(English -> throwable.getMessage))

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
