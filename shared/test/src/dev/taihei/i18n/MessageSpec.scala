package dev.taihei.i18n

import java.time.LocalDate

import zio.json.*
import zio.test.*

object MessageSpec extends ZIOSpecDefault:

  def spec = suite("MessageSpec")(
    suite("Message creation with positional arguments")(
      test("with0Args creates message without arguments") {
        val msg = Message.with0Args(
          Language.English -> "Hello",
          Language.Dutch -> "Dag",
        )
        assertTrue(
          msg.args.isEmpty,
          msg.namedArgs.isEmpty,
          msg.translationTemplates.size == 2,
        )
      },
      test("with1Arg creates message with one argument") {
        val msg = Message.withNamedArgs(
          Language.English -> "Hello {$name}"
        )("name" -> "World")
        assertTrue(
          msg.namedArgs.size == 1,
          msg.namedArgs("name") == "World",
        )
      },
      test("with2Args creates message with two arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "User {$user} has {$points} points"
        )("user" -> "Alice", "points" -> 100)
        assertTrue(
          msg.namedArgs.size == 2,
          msg.namedArgs("user") == "Alice",
          msg.namedArgs("points") == 100,
        )
      },
      test("with3Args creates message with three arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "{$year}/{$month}/{$day}"
        )("year" -> "2024", "month" -> "01", "day" -> "15")
        assertTrue(
          msg.namedArgs.size == 3,
          msg.namedArgs("year") == "2024",
          msg.namedArgs("month") == "01",
          msg.namedArgs("day") == "15",
        )
      },
      test("with4Args creates message with four arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "{$a} {$b} {$c} {$d}"
        )("a" -> "A", "b" -> "B", "c" -> "C", "d" -> "D")
        assertTrue(
          msg.namedArgs.size == 4,
          msg.namedArgs("a") == "A",
          msg.namedArgs("b") == "B",
          msg.namedArgs("c") == "C",
          msg.namedArgs("d") == "D",
        )
      },
      test("with5Args creates message with five arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "{$a} + {$b} + {$c} + {$d} + {$e}"
        )("a" -> 1, "b" -> 2, "c" -> 3, "d" -> 4, "e" -> 5)
        assertTrue(
          msg.namedArgs.size == 5,
          msg.namedArgs("a") == 1,
          msg.namedArgs("e") == 5,
        )
      },
      test("with6Args creates message with six arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "{$a},{$b},{$c},{$d},{$e},{$f}"
        )("a" -> 10, "b" -> 20, "c" -> 30, "d" -> 40, "e" -> 50, "f" -> 60)
        assertTrue(
          msg.namedArgs.size == 6,
          msg.namedArgs("a") == 10,
          msg.namedArgs("f") == 60,
        )
      },
      test("with7Args creates message with seven arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "{$c0}{$c1}{$c2}{$c3}{$c4}{$c5}{$c6}"
        )(
          "c0" -> "M",
          "c1" -> "o",
          "c2" -> "n",
          "c3" -> "d",
          "c4" -> "a",
          "c5" -> "y",
          "c6" -> "!",
        )
        assertTrue(
          msg.namedArgs.size == 7,
          msg.namedArgs("c0") == "M",
          msg.namedArgs("c6") == "!",
        )
      },
      test("with8Args creates message with eight arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "Sum: {$n1} + {$n2} + {$n3} + {$n4} + {$n5} + {$n6} + {$n7} + {$n8}"
        )(
          "n1" -> 1,
          "n2" -> 2,
          "n3" -> 3,
          "n4" -> 4,
          "n5" -> 5,
          "n6" -> 6,
          "n7" -> 7,
          "n8" -> 8,
        )
        assertTrue(
          msg.namedArgs.size == 8,
          msg.namedArgs("n1") == 1,
          msg.namedArgs("n8") == 8,
        )
      },
      test("with9Args creates message with nine arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "{$n1}{$n2}{$n3}{$n4}{$n5}{$n6}{$n7}{$n8}{$n9}"
        )(
          "n1" -> 1,
          "n2" -> 2,
          "n3" -> 3,
          "n4" -> 4,
          "n5" -> 5,
          "n6" -> 6,
          "n7" -> 7,
          "n8" -> 8,
          "n9" -> 9,
        )
        assertTrue(
          msg.namedArgs.size == 9,
          msg.namedArgs("n1") == 1,
          msg.namedArgs("n9") == 9,
        )
      },
      test("with10Args creates message with ten arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "Ten args: {$n0} through {$n9}"
        )(
          "n0" -> 0,
          "n1" -> 1,
          "n2" -> 2,
          "n3" -> 3,
          "n4" -> 4,
          "n5" -> 5,
          "n6" -> 6,
          "n7" -> 7,
          "n8" -> 8,
          "n9" -> 9,
        )
        assertTrue(
          msg.namedArgs.size == 10,
          msg.namedArgs("n0") == 0,
          msg.namedArgs("n9") == 9,
        )
      },
    ),
    suite("Message creation with named arguments")(
      test("withNamedArgs creates message with named arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "Hello {$name}"
        )("name" -> "World")
        assertTrue(
          msg.args.isEmpty,
          msg.namedArgs.size == 1,
          msg.namedArgs("name") == "World",
        )
      },
      test("withNamedArgs supports multiple named arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "{$firstName} {$lastName}"
        )("firstName" -> "John", "lastName" -> "Doe")
        assertTrue(
          msg.namedArgs.size == 2,
          msg.namedArgs("firstName") == "John",
          msg.namedArgs("lastName") == "Doe",
        )
      },
    ),
    suite("Message argument types")(
      test("supports String arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "Text: {$value}"
        )("value" -> "hello")
        assertTrue(msg.translate(Language.English).contains("hello"))
      },
      test("supports Int arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "Count: {$value}"
        )("value" -> 42)
        assertTrue(msg.translate(Language.English).contains("42"))
      },
      test("supports Long arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "Value: {$value}"
        )("value" -> 9999999999L)
        assertTrue(msg.translate(Language.English).contains("9999999999"))
      },
      test("supports Double arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "Price: {$value}"
        )("value" -> 19.99)
        assertTrue(msg.translate(Language.English).contains("19.99"))
      },
      test("supports Boolean arguments") {
        val msg = Message.withNamedArgs(
          Language.English -> "Active: {$value}"
        )("value" -> true)
        assertTrue(msg.translate(Language.English).contains("true"))
      },
      test("supports LocalDate arguments") {
        val date = LocalDate.of(2024, 1, 15)
        val msg  = Message.withNamedArgs(
          Language.English -> "Date: {$value}"
        )("value" -> date)
        assertTrue(msg.translate(Language.English).contains("2024-01-15"))
      },
    ),
    suite("Message translation")(
      test("translate returns correct language version") {
        val msg = Message.with0Args(
          Language.English -> "Hello",
          Language.Dutch -> "Dag",
          Language.French -> "Bonjour",
        )
        assertTrue(
          msg.translate(Language.English) == "Hello",
          msg.translate(Language.Dutch) == "Dag",
          msg.translate(Language.French) == "Bonjour",
        )
      },
      test("translate falls back to default language if translation missing") {
        val msg = Message.with0Args(
          Language.English -> "Hello"
        )
        assertTrue(
          msg.translate(Language.English) == "Hello",
          msg.translate(Language.Dutch) == "Hello", // falls back to English
          msg.translate(Language.French) == "Hello", // falls back to English
        )
      },
      test("render is an alias for translate") {
        val msg = Message.with0Args(
          Language.English -> "Test"
        )
        assertTrue(
          msg.render(Language.English) == msg.translate(Language.English)
        )
      },
      test("toString returns English translation") {
        val msg = Message.with0Args(
          Language.English -> "Default",
          Language.Dutch -> "Standaard",
        )
        assertTrue(msg.toString == "Default")
      },
    ),
    suite("Message with MessageFormat2 syntax")(
      test("simple variable substitution works") {
        val msg = Message.withNamedArgs(
          Language.English -> "Hello {$name}"
        )("name" -> "World")
        assertTrue(msg.translate(Language.English) == "Hello World")
      },
      test("multiple variable substitution works") {
        val msg = Message.withNamedArgs(
          Language.English -> "{$user} has {$points} points"
        )("user" -> "Alice", "points" -> 100)
        assertTrue(msg.translate(Language.English) == "Alice has 100 points")
      },
    ),
    suite("Message JSON codec")(
      test("Message can be encoded to JSON") {
        val msg  = Message.withNamedArgs(Language.English -> "Hello {$name}")("name" -> "World")
        val json = msg.toJson
        assertTrue(json.nonEmpty)
      },
      test("Message can be decoded from JSON") {
        val msg     = Message.withNamedArgs(Language.English -> "Test {$value}")("value" -> "Value")
        val encoded = msg.toJson
        val decoded = encoded.fromJson[Message]
        assertTrue(decoded.isRight)
      },
      test("Message round-trips through JSON preserving translations") {
        val original = Message.with0Args(
          Language.English -> "Hello",
          Language.Dutch -> "Dag",
        )
        val encoded  = original.toJson
        val decoded  = encoded.fromJson[Message]
        decoded match
          case Right(msg) =>
            assertTrue(
              msg.translate(Language.English) == "Hello",
              msg.translate(Language.Dutch) == "Dag",
            )
          case Left(_)    => assertTrue(false)
      },
    ),
    suite("CoreMessages validation messages")(
      test("stringShouldNotBeBlank is multilingual") {
        val msg = CoreMessages.Validation.stringShouldNotBeBlank
        assertTrue(
          msg.translate(Language.English).contains("should not be blank"),
          msg.translate(Language.Dutch).contains("mag niet leeg zijn"),
          msg.translate(Language.French).contains("ne doit pas être vide"),
        )
      },
      test("stringValidEmail is multilingual") {
        val msg = CoreMessages.Validation.stringValidEmail
        assertTrue(
          msg.translate(Language.English).contains("valid email"),
          msg.translate(Language.Dutch).contains("geldig e-mailadres"),
          msg.translate(Language.French).contains("adresse e-mail valide"),
        )
      },
      test("intMinimum generates correct message with argument") {
        val msg = CoreMessages.Validation.intMinimum(5)
        val en  = msg.translate(Language.English)
        assertTrue(
          en.contains("minimum"),
          en.contains("5"),
        )
      },
      test("intMaximum generates correct message with argument") {
        val msg = CoreMessages.Validation.intMaximum(100)
        val en  = msg.translate(Language.English)
        assertTrue(
          en.contains("maximum"),
          en.contains("100"),
        )
      },
      test("stringLengthBetween generates correct message with two arguments") {
        val msg = CoreMessages.Validation.stringLengthBetween(5, 20)
        val en  = msg.translate(Language.English)
        assertTrue(
          en.contains("between"),
          en.contains("5"),
          en.contains("20"),
        )
      },
    ),
    suite("CoreMessages general messages")(
      test("helloWorld substitutes name") {
        val msg = CoreMessages.General.helloWorld("Alice")
        assertTrue(
          msg.translate(Language.English).contains("Alice"),
          msg.translate(Language.Dutch).contains("Alice"),
          msg.translate(Language.French).contains("Alice"),
        )
      }
    ),
    suite("CoreMessages authentication messages")(
      test("success message is available") {
        val msg = CoreMessages.Authentication.success
        assertTrue(msg.translate(Language.English).contains("successful"))
      },
      test("accountNotFound message is available") {
        val msg = CoreMessages.Authentication.accountNotFound
        assertTrue(msg.translate(Language.English).contains("not found"))
      },
    ),
    suite("CoreMessages exception messages")(
      test("unsupportedCommand is multilingual") {
        val msg = CoreMessages.Exceptions.unsupportedCommand
        assertTrue(
          msg.translate(Language.English).contains("Unsupported"),
          msg.translate(Language.Dutch).contains("niet ondersteund"),
          msg.translate(Language.French).contains("non supportée"),
        )
      },
      test("invalidCommand is multilingual") {
        val msg = CoreMessages.Exceptions.invalidCommand
        assertTrue(
          msg.translate(Language.English).contains("Invalid"),
          msg.translate(Language.Dutch).contains("Ongeldige"),
          msg.translate(Language.French).contains("non valide"),
        )
      },
    ),
    suite("Message properties")(
      test("nonEmpty is true for non-empty messages") {
        val msg = Message.with0Args(Language.English -> "Test")
        assertTrue(msg.nonEmpty)
      },
      test("nonEmpty is false for empty messages") {
        val msg = Message.with0Args(Language.English -> "")
        assertTrue(!msg.nonEmpty)
      },
    ),
    suite("Message.of(throwable) convenience method")(
      test("creates message from throwable") {
        val exception = new RuntimeException("Something went wrong")
        val msg       = Message.of(exception)
        assertTrue(
          msg.translate(Language.English) == "Something went wrong"
        )
      },
      test("creates message from throwable with null message") {
        val exception = new NullPointerException("Null reference")
        val msg       = Message.of(exception)
        assertTrue(
          msg.translate(Language.English).contains("Null reference")
        )
      },
      test("handles IllegalArgumentException") {
        val exception = new IllegalArgumentException("Invalid input provided")
        val msg       = Message.of(exception)
        assertTrue(
          msg.translate(Language.English).contains("Invalid input")
        )
      },
      test("handles custom exceptions") {
        case class CustomException(details: String) extends Exception(s"Custom error: $details")
        val exception = CustomException("test details")
        val msg       = Message.of(exception)
        assertTrue(
          msg.translate(Language.English).contains("Custom error"),
          msg.translate(Language.English).contains("test details"),
        )
      },
      test("Message.of creates English-only translation") {
        val exception = new Exception("Error message")
        val msg       = Message.of(exception)
        assertTrue(
          msg.translationTemplates.size == 1,
          msg.translationTemplates.contains(Language.English),
        )
      },
    ),
  )

end MessageSpec
