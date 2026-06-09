package genius.i18n

import zio.test.*

object MessageFormat2Spec extends ZIOSpecDefault:

  def spec = suite("MessageFormat2Spec")(
    suite("Simple message formatting")(
      test("formats plain text without variables") {
        val result = MessageFormat2.format("Hello World")
        assertTrue(result == "Hello World")
      },
      test("formats text with single variable") {
        val result = MessageFormat2.format("Hello {$name}", Map("name" -> "Alice"))
        assertTrue(result == "Hello Alice")
      },
      test("formats text with multiple variables") {
        val result = MessageFormat2.format(
          "{$greeting} {$name}!",
          Map("greeting" -> "Hello", "name" -> "Bob"),
        )
        assertTrue(result == "Hello Bob!")
      },
      test("handles missing variables gracefully") {
        val result = MessageFormat2.format("Hello {$name}")
        assertTrue(result == "Hello $name")
      },
      test("formats text with numeric variables") {
        val result = MessageFormat2.format(
          "You have {$count} messages",
          Map("count" -> 5),
        )
        assertTrue(result == "You have 5 messages")
      },
    ),
    suite("Input declarations")(
      test("processes .input declarations") {
        val template = """.input {$name}
                         |Hello {$name}""".stripMargin
        val result   = MessageFormat2.format(template, Map("name" -> "World"))
        assertTrue(result.trim == "Hello World")
      },
      test("processes .input with function annotation") {
        val template = """.input {$count :number}
                         |Count: {$count}""".stripMargin
        val result   = MessageFormat2.format(template, Map("count" -> 42))
        assertTrue(result.trim.contains("42"))
      },
      test("processes multiple .input declarations") {
        val template = """.input {$firstName}
                         |.input {$lastName}
                         |{$firstName} {$lastName}""".stripMargin
        val result   = MessageFormat2.format(
          template,
          Map("firstName" -> "John", "lastName" -> "Doe"),
        )
        assertTrue(result.trim == "John Doe")
      },
    ),
    suite("Local declarations (.let)")(
      test("processes .let declarations") {
        val template = """.let {$greeting = (Hello)}
                         |{$greeting} World""".stripMargin
        val result   = MessageFormat2.format(template)
        assertTrue(result.trim == "Hello World")
      },
      test(".let can reference input variables") {
        val template = """.input {$name}
                         |.let {$message = (Hello)}
                         |{$message} {$name}""".stripMargin
        val result   = MessageFormat2.format(template, Map("name" -> "Alice"))
        assertTrue(result.trim == "Hello Alice")
      },
    ),
    suite("Function annotations")(
      test(":number function formats numbers") {
        val template = "Price: {$price :number}"
        val result   = MessageFormat2.format(template, Map("price" -> 19.99))
        assertTrue(result.contains("19.99"))
      },
      test(":number with style option") {
        val template = "Price: {$price :number style=currency}"
        val result   = MessageFormat2.format(template, Map("price" -> 19.99))
        assertTrue(result.contains("19.99"))
      },
      test(":plural function returns plural selector") {
        val template = "{$count :plural}"
        val result   = MessageFormat2.format(template, Map("count" -> 1))
        assertTrue(result.contains("1"))
      },
    ),
    suite("Match statements")(
      test("simple match with exact keys") {
        val template = """.match {$count}
                         |when 1 {You have one item}
                         |when 2 {You have two items}
                         |when * {You have many items}""".stripMargin
        val result1  = MessageFormat2.format(template, Map("count" -> 1))
        val result2  = MessageFormat2.format(template, Map("count" -> 2))
        val result3  = MessageFormat2.format(template, Map("count" -> 5))

        assertTrue(
          result1.trim == "You have one item",
          result2.trim == "You have two items",
          result3.trim == "You have many items",
        )
      },
      test("match with plural selector") {
        val template = """.match {$count :plural}
                         |when 1 {one item}
                         |when other {many items}""".stripMargin
        val result1  = MessageFormat2.format(template, Map("count" -> 1))
        val result2  = MessageFormat2.format(template, Map("count" -> 5))

        assertTrue(
          result1.trim == "one item",
          result2.trim == "many items",
        )
      },
      test("match with variables in patterns") {
        val template = """.match {$count}
                         |when 1 {You have {$count} item}
                         |when * {You have {$count} items}""".stripMargin
        val result1  = MessageFormat2.format(template, Map("count" -> 1))
        val result2  = MessageFormat2.format(template, Map("count" -> 5))

        assertTrue(
          result1.trim == "You have 1 item",
          result2.trim == "You have 5 items",
        )
      },
      test("match with =N exact match syntax") {
        val template = """.match {$count :plural}
                         |when =0 {no items}
                         |when =1 {one item}
                         |when other {many items}""".stripMargin
        val result0  = MessageFormat2.format(template, Map("count" -> 0))
        val result1  = MessageFormat2.format(template, Map("count" -> 1))
        val result5  = MessageFormat2.format(template, Map("count" -> 5))

        assertTrue(
          result0.trim == "no items",
          result1.trim == "one item",
          result5.trim == "many items",
        )
      },
      test("match with 'one' and 'other' plural categories") {
        val template = """.match {$count :plural}
                         |when one {singular}
                         |when other {plural}""".stripMargin
        val result1  = MessageFormat2.format(template, Map("count" -> 1))
        val result2  = MessageFormat2.format(template, Map("count" -> 2))

        assertTrue(
          result1.trim == "singular",
          result2.trim == "plural",
        )
      },
      test("match returns [no match] for unmatched cases") {
        val template = """.match {$count}
                         |when 1 {one}
                         |when 2 {two}""".stripMargin
        val result   = MessageFormat2.format(template, Map("count" -> 3))
        assertTrue(result.trim == "[no match]")
      },
    ),
    suite("Multi-selector match statements")(
      test("match with multiple selectors") {
        val template = """.match {$gender} {$count :plural}
                         |when male 1 {He has one item}
                         |when male other {He has many items}
                         |when female 1 {She has one item}
                         |when female other {She has many items}
                         |when * * {They have items}""".stripMargin

        val result1 = MessageFormat2.format(template, Map("gender" -> "male", "count" -> 1))
        val result2 = MessageFormat2.format(template, Map("gender" -> "female", "count" -> 1))
        val result3 = MessageFormat2.format(template, Map("gender" -> "male", "count" -> 5))
        val result4 = MessageFormat2.format(template, Map("gender" -> "other", "count" -> 1))

        assertTrue(
          result1.trim == "He has one item",
          result2.trim == "She has one item",
          result3.trim == "He has many items",
          result4.trim == "They have items",
        )
      }
    ),
    suite("Complex message patterns")(
      test("message with declarations and match statement") {
        val template = """.input {$count :number}
                         |.match {$count :plural}
                         |when 0 {No items}
                         |when 1 {One item}
                         |when other {{$count} items}""".stripMargin

        val result0 = MessageFormat2.format(template, Map("count" -> 0))
        val result1 = MessageFormat2.format(template, Map("count" -> 1))
        val result5 = MessageFormat2.format(template, Map("count" -> 5))

        assertTrue(
          result0.trim == "No items",
          result1.trim == "One item",
          result5.trim.contains("5 items"),
        )
      },
      test("message with let and match") {
        val template = """.let {$max = (10)}
                         |.match {$count}
                         |when 0 {Empty}
                         |when * {Has items}""".stripMargin

        val result = MessageFormat2.format(template, Map("count" -> 5))
        assertTrue(result.trim == "Has items")
      },
    ),
    suite("Locale support")(
      test("format accepts locale parameter") {
        val result = MessageFormat2.format("Hello {$name}", Map("name" -> "World"), "en")
        assertTrue(result == "Hello World")
      },
      test("different locales can be specified") {
        val template = "Price: {$price :number}"
        val resultEn = MessageFormat2.format(template, Map("price" -> 19.99), "en")
        val resultNl = MessageFormat2.format(template, Map("price" -> 19.99), "nl")

        // Both should contain the price, but formatting might differ by locale
        // English uses "." and Dutch uses "," as decimal separator
        assertTrue(
          resultEn.contains("19.99") || resultEn.contains("19,99"),
          resultNl.contains("19.99") || resultNl.contains("19,99"),
        )
      },
      test("French plural rules: 0 and 1 are 'one'") {
        val template = """.match {$count :plural}
                         |when one {un élément}
                         |when other {plusieurs éléments}""".stripMargin
        val result0  = MessageFormat2.format(template, Map("count" -> 0), "fr")
        val result1  = MessageFormat2.format(template, Map("count" -> 1), "fr")
        val result2  = MessageFormat2.format(template, Map("count" -> 2), "fr")

        assertTrue(
          result0.trim == "un élément",        // French: 0 is "one"
          result1.trim == "un élément",        // French: 1 is "one"
          result2.trim == "plusieurs éléments", // French: 2+ is "other"
        )
      },
      test("English/Dutch plural rules: only 1 is 'one'") {
        val template  = """.match {$count :plural}
                          |when one {singular}
                          |when other {plural}""".stripMargin
        val resultEn0 = MessageFormat2.format(template, Map("count" -> 0), "en")
        val resultEn1 = MessageFormat2.format(template, Map("count" -> 1), "en")
        val resultNl0 = MessageFormat2.format(template, Map("count" -> 0), "nl")
        val resultNl1 = MessageFormat2.format(template, Map("count" -> 1), "nl")

        assertTrue(
          resultEn0.trim == "plural",   // English: 0 is "other"
          resultEn1.trim == "singular", // English: 1 is "one"
          resultNl0.trim == "plural",   // Dutch: 0 is "other"
          resultNl1.trim == "singular", // Dutch: 1 is "one"
        )
      },
    ),
    suite("DateTime formatting")(
      test("formats LocalDate with locale") {
        import java.time.LocalDate
        val date     = LocalDate.of(2024, 1, 15)
        val template = "{$date :datetime}"
        val resultEn = MessageFormat2.format(template, Map("date" -> date), "en")
        val resultFr = MessageFormat2.format(template, Map("date" -> date), "fr")

        // Both should contain parts of the date
        assertTrue(
          resultEn.contains("2024") || resultEn.contains("Jan") || resultEn.contains("15"),
          resultFr.contains("2024") || resultFr.contains("janv") || resultFr.contains("15"),
        )
      },
      test("formats LocalDate with different dateStyle options") {
        import java.time.LocalDate
        val date          = LocalDate.of(2024, 1, 15)
        val templateShort = "{$date :datetime dateStyle=short}"
        val templateLong  = "{$date :datetime dateStyle=long}"

        val resultShort = MessageFormat2.format(templateShort, Map("date" -> date), "en")
        val resultLong  = MessageFormat2.format(templateLong, Map("date" -> date), "en")

        // Both should format the date, long should be more verbose
        assertTrue(
          resultShort.nonEmpty,
          resultLong.nonEmpty,
          resultLong.length >= resultShort.length,
        )
      },
      test("formats LocalDateTime with locale") {
        import java.time.LocalDateTime
        val datetime = LocalDateTime.of(2024, 1, 15, 14, 30)
        val template = "{$datetime :datetime}"
        val resultEn = MessageFormat2.format(template, Map("datetime" -> datetime), "en")
        val resultNl = MessageFormat2.format(template, Map("datetime" -> datetime), "nl")

        // Should contain date and time components
        assertTrue(
          resultEn.contains("2024") || resultEn.contains("14") || resultEn.contains("30"),
          resultNl.contains("2024") || resultNl.contains("14") || resultNl.contains("30"),
        )
      },
      test("formats LocalTime with locale") {
        import java.time.LocalTime
        val time     = LocalTime.of(14, 30, 0)
        val template = "{$time :datetime}"
        val resultEn = MessageFormat2.format(template, Map("time" -> time), "en")
        val resultFr = MessageFormat2.format(template, Map("time" -> time), "fr")

        // Should contain time components
        assertTrue(
          resultEn.contains("14") || resultEn.contains("30") || resultEn.contains("2:30"),
          resultFr.contains("14") || resultFr.contains("30"),
        )
      },
    ),
    suite("Edge cases")(
      test("empty template returns empty string") {
        val result = MessageFormat2.format("")
        assertTrue(result == "")
      },
      test("template with only whitespace") {
        val result = MessageFormat2.format("   ")
        assertTrue(result == "   ")
      },
      test("template with braces but no variables") {
        val result = MessageFormat2.format("Test { } text")
        assertTrue(result.contains("Test"))
      },
      test("nested braces in literal") {
        val template = ".let {$msg = (Hello {World})}\n{$msg}"
        val result   = MessageFormat2.format(template)
        // The literal should preserve nested content
        assertTrue(result.contains("Hello"))
      },
      test("variable names with underscores") {
        val result = MessageFormat2.format(
          "Hello {$first_name} {$last_name}",
          Map("first_name" -> "John", "last_name" -> "Doe"),
        )
        assertTrue(result == "Hello John Doe")
      },
    ),
    suite("Parser robustness")(
      test("handles templates with newlines") {
        val template = """Hello
                         |{$name}
                         |Welcome!""".stripMargin
        val result   = MessageFormat2.format(template, Map("name" -> "Alice"))
        assertTrue(
          result.contains("Hello"),
          result.contains("Alice"),
          result.contains("Welcome"),
        )
      },
      test("handles mixed text and variables") {
        val template = "Start {$var1} middle {$var2} end"
        val result   = MessageFormat2.format(
          template,
          Map("var1" -> "A", "var2" -> "B"),
        )
        assertTrue(result == "Start A middle B end")
      },
    ),
  )

end MessageFormat2Spec
