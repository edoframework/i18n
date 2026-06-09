package genius.i18n

import zio.json.*

enum Language(val isoCode: String, val isDefault: Boolean) derives JsonCodec:
  case English extends Language("en", true)
  case Dutch extends Language("nl", false)
  case French extends Language("fr", false)

  def label: Message = this match
    case English =>
      Message.with0Args(
        English -> "English",
        Dutch -> "Engels",
        French -> "Anglais",
      )
    case Dutch   =>
      Message.with0Args(
        English -> "Dutch",
        Dutch -> "Nederlands",
        French -> "Néerlandais",
      )
    case French  =>
      Message.with0Args(
        English -> "French",
        Dutch -> "Frans",
        French -> "Français",
      )

end Language

object Language:

  given JsonFieldEncoder[Language] =
    JsonFieldEncoder.string.contramap(_.toString) // or a custom .code/.iso if you have one

  given JsonFieldDecoder[Language] =
    JsonFieldDecoder.string.mapOrFail { s =>
      scala.util.Try(Language.valueOf(s)).toEither.left.map(_.getMessage)
    }

  def defaultLanguage: Language = English

  def empty: Language = English

  def sample: Language =
    val values = Language.values
    values(scala.util.Random.nextInt(values.length))

  def of(isoCode: String): Language =
    Language.values.find(_.isoCode == isoCode).getOrElse(defaultLanguage)

end Language
