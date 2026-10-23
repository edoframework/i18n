package dev.taihei.tsuji.legacy

import java.time.{LocalDate, LocalDateTime, LocalTime}

import zio.json.*

import dev.taihei.tsuji.*
import dev.taihei.tsuji.Language.*

/** The argument values a 4.x message takes. */
@deprecated("Use the typed arguments of a MessageFamily; legacy is removed in 6.0.0", "5.0.0")
type MessageArg = String | Int | Long | Double | Float | Boolean | LocalDate | LocalDateTime |
  LocalTime

/**
 * The message of taihei-i18n 4.x, unchanged: arguments and every translation in one value, with
 * the 4.x JSON codec. It stays in 5.x so that moving to 5.0.0 is a change of imports
 * (`import dev.taihei.tsuji.legacy.Message`); define new messages in a `MessageFamily`. Removed in
 * 6.0.0.
 */
@deprecated(
  "Use a MessageFamily: typed messages with per-language catalogues; legacy is removed in 6.0.0",
  "5.0.0",
)
case class Message(
  args: List[MessageArg],
  namedArgs: Map[String, MessageArg] = Map.empty,
  translationTemplates: Map[Language, String])
  extends Displayable:

  /** The 4.x translation; a legacy message carries its own templates and needs no catalogue. */
  def localize(
    language: Language
  )(
    using catalogues: Catalogues
  ): String = translate(language)

  private def formatArgs: Map[String, Any] =
    val positionalArgs = args.zipWithIndex.map:
      case (value, idx) => idx.toString -> value
    val mf2Args        = namedArgs ++ positionalArgs
    mf2Args.map:
      case (k, v) => k -> (v: Any)

  private def message(template: String, language: Language): String =
    MessageFormat2.format(template, formatArgs, language.isoCode)

  // Exact, then the base language (a Belgian Dutch reader gets the Dutch text when no nl-BE one
  // was written), then English.
  private def template(language: Language): String = translationTemplates
    .get(language)
    .orElse(translationTemplates.get(language.base))
    .orElse(translationTemplates.get(Language.defaultLanguage))
    .getOrElse("!!unknown message!!")

  def translate(language: Language): String =
    message(template(language), language)

  // As translate, within the budget of the 5.x message this one is an argument of.
  private[tsuji] def translateWithin(language: Language, budget: Budget): String =
    val t = template(language)
    MessageFormat2.tryFormat(
      MessageFormat2.parse(t).message,
      formatArgs,
      language.isoCode,
      budget,
    ).getOrElse(t)

  def render(language: Language): String = translate(language)

  override lazy val toString: String = translate(English)

  lazy val nonEmpty: Boolean = toString.nonEmpty

end Message

@deprecated(
  "Use a MessageFamily: typed messages with per-language catalogues; legacy is removed in 6.0.0",
  "5.0.0",
)
object Message:
  // Make the codec available
  given JsonCodec[Message] = DeriveJsonCodec.gen[Message]

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
