package dev.taihei.i18n

import scala.collection.mutable
import scala.util.Try

object MessageFormat2:

  // AST types for MessageFormat 2.0
  sealed trait Expression
  case class Variable(name: String) extends Expression
  case class Literal(value: String) extends Expression

  case class FunctionAnnotation(
    expr: Expression,
    function: String,
    options: Map[String, String] = Map.empty)
    extends Expression

  sealed trait Pattern
  case class Text(value: String) extends Pattern
  case class Placeholder(expr: Expression) extends Pattern
  case class PatternSequence(patterns: List[Pattern]) extends Pattern

  sealed trait Declaration
  case class InputDeclaration(name: String, expr: Expression) extends Declaration
  case class LocalDeclaration(name: String, expr: Expression) extends Declaration

  case class MatchCase(keys: List[String], pattern: Pattern)
  case class MatchStatement(selectors: List[Expression], cases: List[MatchCase])

  case class Message(
    declarations: List[Declaration] = Nil,
    body: Either[Pattern, MatchStatement])

  // Declaration and match keywords
  private val KeywordInput = ".input"
  private val KeywordLet   = ".let"
  private val KeywordMatch = ".match"
  private val KeywordWhen  = "when"

  // Imperative recursive-descent parser for ICU MessageFormat 2.0.
  // Uses var/return intentionally for parser performance and simplicity.
  class Parser(template: String):
    private var pos = 0

    private def hasMore: Boolean = pos < template.length

    private def peek(): Option[Char] =
      if hasMore then Some(template(pos)) else None

    private def advance(): Char =
      val c = template(pos)
      pos += 1
      c

    private def skipWhitespace(): Unit =
      while hasMore && template(pos).isWhitespace do
        pos += 1

    private def isPatternTerminator(c: Char): Boolean =
      c == '{' || c == '\n' || c == '|'

    private def parseIdentifier(): String =
      skipWhitespace()
      val start = pos
      while hasMore && (template(pos).isLetterOrDigit || template(pos) == '_') do
        pos += 1
      template.substring(start, pos)

    private def parseVariable(): Variable =
      advance() // skip $
      Variable(parseIdentifier())

    private def parseLiteral(): Literal =
      advance() // skip (
      val start = pos
      var depth = 1
      while hasMore && depth > 0 do
        template(pos) match
          case '(' => depth += 1
          case ')' => depth -= 1
          case _   =>
        if depth > 0 then pos += 1
      val value = template.substring(start, pos)
      if hasMore then advance() // skip closing )
      Literal(value.trim)

    private def parseFunctionOptions(): Map[String, String] =
      val options = mutable.Map[String, String]()
      skipWhitespace()

      while hasMore && peek().exists(_ != '}') do
        val startPos = pos
        val key      = parseIdentifier()
        skipWhitespace()
        if peek().contains('=') then
          advance() // skip =
          skipWhitespace()
          val value = if peek().contains('"') then
            advance() // skip opening quote
            val start = pos
            while hasMore && template(pos) != '"' do pos += 1
            val v     = template.substring(start, pos)
            if hasMore then advance() // skip closing quote
            v
          else
            parseIdentifier()
          if key.nonEmpty then
            options(key) = value
        skipWhitespace()
        // Prevent infinite loop - if we didn't advance, break
        if pos == startPos then
          if hasMore && template(pos) != '}' then
            pos += 1 // Force advance to avoid infinite loop
      end while

      options.toMap

    end parseFunctionOptions

    private def parseExpression(): Expression =
      skipWhitespace()
      val expr = parseSimpleExpression()
      skipWhitespace()
      if peek().contains(':') then
        advance() // skip :
        val function = parseIdentifier()
        val options  = parseFunctionOptions()
        FunctionAnnotation(expr, function, options)
      else
        expr

    private def parseSimpleExpression(): Expression =
      skipWhitespace()
      peek() match
        case Some('$') => parseVariable()
        case Some('(') => parseLiteral()
        case _         => Variable(parseIdentifier())

    private def parsePlaceholder(): Placeholder =
      advance() // skip {
      val expr = parseExpression()
      skipWhitespace()
      if peek().contains('}') then advance() // skip }
      Placeholder(expr)

    // Parses a pattern (text interspersed with {placeholders}).
    // When terminateAtNewline=true, stops at \n or | (used after declarations).
    // When terminateAtNewline=false, consumes the entire remaining input.
    private def parsePatternUntil(terminateAtNewline: Boolean): Pattern =
      val patterns = mutable.ListBuffer[Pattern]()

      def atTerminator: Boolean =
        if terminateAtNewline then peek().exists(c => c == '\n' || c == '|')
        else false

      while hasMore && !atTerminator do
        peek() match
          case Some('{') =>
            patterns += parsePlaceholder()
          case _         =>
            val start = pos
            while hasMore && template(pos) != '{' &&
              !(terminateAtNewline && isPatternTerminator(template(pos)))
            do
              pos += 1
            if pos > start then
              patterns += Text(template.substring(start, pos))
            else if hasMore then
              pos += 1 // Force advance to avoid infinite loop

      if patterns.isEmpty then Text("")
      else if patterns.size == 1 then patterns.head
      else PatternSequence(patterns.toList)

    end parsePatternUntil

    private def parseDeclaration(): Option[Declaration] =
      skipWhitespace()
      if !hasMore then return None

      if hasMore && template.substring(pos).startsWith(KeywordInput) then
        pos += KeywordInput.length
        skipWhitespace()

        // Parse the declaration which may or may not be in braces
        val hadBrace = peek().contains('{')
        if hadBrace then advance()

        // Parse variable name
        skipWhitespace()
        val name = if peek().contains('$') then
          advance() // skip $
          parseIdentifier()
        else
          parseIdentifier()

        // Parse expression (which may include :function)
        skipWhitespace()
        val expr = if hadBrace && peek().contains(':') then
          // Parse function annotation
          advance() // skip :
          val function = parseIdentifier()
          val options  = parseFunctionOptions()
          skipWhitespace()
          if peek().contains('}') then advance() // skip closing brace
          FunctionAnnotation(Variable(name), function, options)
        else if hadBrace then
          // Just the variable itself
          skipWhitespace()
          if peek().contains('}') then advance()
          Variable(name)
        else
          // No brace, just variable
          Variable(name)

        Some(InputDeclaration(name, expr))
      else if hasMore && template.substring(pos).startsWith(KeywordLet) then
        pos += KeywordLet.length
        skipWhitespace()

        val hadBrace = peek().contains('{')
        if hadBrace then advance()

        val name = if peek().contains('$') then
          advance()
          parseIdentifier()
        else
          parseIdentifier()

        skipWhitespace()
        if peek().contains('=') then advance()
        skipWhitespace()

        val expr = parseExpression()

        if hadBrace then
          skipWhitespace()
          if peek().contains('}') then advance()

        Some(LocalDeclaration(name, expr))
      else
        None

      end if

    end parseDeclaration

    private def parseMatchStatement(): MatchStatement =
      pos += KeywordMatch.length
      skipWhitespace()

      // Parse selectors (should be in braces)
      val selectors = mutable.ListBuffer[Expression]()

      // Look for expressions in braces
      while hasMore && peek().exists(_ != '\n') do
        skipWhitespace()
        if peek().contains('{') then
          advance() // skip {
          val expr = parseExpression()
          skipWhitespace()
          if peek().contains('}') then advance() // skip }
          selectors += expr
        else
          // No more selectors
          if peek().exists(_ != '\n') then
            // Advance to end of line
            while hasMore && template(pos) != '\n' do pos += 1

      if peek().contains('\n') then advance()

      // Parse cases
      val cases    = mutable.ListBuffer[MatchCase]()
      var continue = true
      while hasMore && continue do
        skipWhitespace()
        if hasMore && template.substring(pos).startsWith(KeywordWhen) then
          pos += KeywordWhen.length
          skipWhitespace()

          // Parse keys
          val keys = mutable.ListBuffer[String]()
          while hasMore && peek().exists(c => c != '{' && c != '\n') do
            val startPos = pos
            // Parse match key (can be identifier, number, or special like * or =N)
            val key      = if peek().contains('*') then
              advance()
              "*"
            else if peek().contains('=') then
              advance() // skip =
              val num = parseIdentifier()
              "=" + num
            else
              // Parse regular identifier or number
              val id = parseIdentifier()
              if id.nonEmpty then id
              else
                // Try to parse a number directly
                val numStart = pos
                if peek().exists(c => c == '-' || c == '+') then advance()
                while hasMore && template(pos).isDigit do pos += 1
                if pos > numStart then template.substring(numStart, pos) else ""
            if key.nonEmpty then
              keys += key
            skipWhitespace()
            // Prevent infinite loop
            if pos == startPos then
              if hasMore && !peek().exists(c => c == '{' || c == '\n') then
                pos += 1
              else
                pos = template.length
          end while

          // Parse pattern
          skipWhitespace()
          if peek().contains('{') then advance()
          val patternStart = pos
          var depth        = 1
          while hasMore && depth > 0 do
            template(pos) match
              case '{' => depth += 1
              case '}' => depth -= 1
              case _   =>
            if depth > 0 then pos += 1
          val patternStr   = template.substring(patternStart, pos)
          if hasMore then advance() // skip closing }

          // Parse the pattern inline to avoid recursive parser creation
          val pattern = if patternStr.isEmpty then
            Text("")
          else
            // Simple pattern parsing - just handle text and placeholders
            val parts = mutable.ListBuffer[Pattern]()
            var i     = 0
            while i < patternStr.length do
              if patternStr(i) == '{' && i + 1 < patternStr.length && patternStr(i + 1) == '$'
              then
                // Found a placeholder
                val start = i
                i += 2 // skip {$
                while i < patternStr.length && patternStr(i) != '}' do i += 1
                if i < patternStr.length then
                  val varName = patternStr.substring(start + 2, i)
                  parts += Placeholder(Variable(varName))
                  i += 1 // skip }
              else
                // Regular text
                val textStart = i
                while i < patternStr.length && patternStr(i) != '{' do i += 1
                if i > textStart then
                  parts += Text(patternStr.substring(textStart, i))
                else if i < patternStr.length then
                  // We hit a { that isn't followed by $, treat it as text
                  i += 1 // Skip the {
                  parts += Text("{")
            end while
            if parts.isEmpty then Text("")
            else if parts.size == 1 then parts.head
            else PatternSequence(parts.toList)
          cases += MatchCase(keys.toList, pattern)

          skipWhitespace()
          if peek().contains('\n') then advance()
        else
          // No more when clauses
          continue = false
        end if
      end while

      MatchStatement(selectors.toList, cases.toList)

    end parseMatchStatement

    def parse(): Message =
      val declarations = mutable.ListBuffer[Declaration]()

      // Parse declarations
      var continueDeclarations = true
      while hasMore && continueDeclarations do
        val savedPos = pos
        parseDeclaration() match
          case Some(decl) =>
            declarations += decl
            skipWhitespace()
            if peek().contains('\n') then advance()
          case None       =>
            pos = savedPos
            continueDeclarations = false

      // Parse body (pattern or match statement)
      // Check for .match without skipping whitespace first (to preserve whitespace-only messages)
      val body =
        if hasMore && template.substring(pos).trim.startsWith(KeywordMatch) then
          skipWhitespace()
          Right(parseMatchStatement())
        else if declarations.isEmpty && pos == 0 then
          Left(parsePatternUntil(terminateAtNewline = false))
        else
          Left(parsePatternUntil(terminateAtNewline = true))

      Message(declarations.toList, body)

    end parse

  end Parser

  // Runtime evaluation
  case class Context(
    variables: Map[String, Any] = Map.empty,
    locale: String = "en")

  def format(
    template: String,
    args: Map[String, Any] = Map.empty,
    locale: String = "en",
  ): String =
    val parser  = new Parser(template)
    val message = parser.parse()
    val context = Context(args, locale)
    evaluate(message, context)

  private def evaluate(message: Message, initialContext: Context): String =
    // Process declarations to build context
    val context = message.declarations.foldLeft(initialContext) { (ctx, decl) =>
      decl match
        case InputDeclaration(name, expr) =>
          val value = evaluateExpression(expr, ctx)
          ctx.copy(variables = ctx.variables + (name -> value))
        case LocalDeclaration(name, expr) =>
          val value = evaluateExpression(expr, ctx)
          ctx.copy(variables = ctx.variables + (name -> value))
    }

    // Evaluate body
    message.body match
      case Left(pattern)    => evaluatePattern(pattern, context)
      case Right(matchStmt) => evaluateMatch(matchStmt, context)

  end evaluate

  private def evaluateExpression(expr: Expression, context: Context): Any =
    expr match
      case Variable(name) => context.variables.getOrElse(name, s"$$${name}")
      case Literal(value) => value
      case FunctionAnnotation(innerExpr, function, options) =>
        val value = evaluateExpression(innerExpr, context)
        applyFunction(value, function, options, context)

  private def applyFunction(
    value: Any,
    function: String,
    options: Map[String, String],
    context: Context,
  ): Any =
    function match
      case "number"   => formatNumber(value, options, context.locale)
      case "datetime" => formatDateTime(value, options, context.locale)
      case "plural"   => PluralSelector(value, context.locale)
      case "select"   => value
      case _          => value

  private def formatNumber(
    value: Any,
    options: Map[String, String],
    locale: String,
  ): String =
    Try(value.toString.toDouble) match
      case scala.util.Success(num) =>
        PlatformNumberFormatter.formatNumber(num, options.get("style"), locale)
      case _                       => value.toString

  private def formatDateTime(
    value: Any,
    options: Map[String, String],
    locale: String,
  ): String =
    PlatformDateTimeFormatter.formatDateTime(value, options, locale)

  case class PluralSelector(value: Any, locale: String)

  private def evaluatePattern(pattern: Pattern, context: Context): String =
    pattern match
      case Text(value)               => value
      case Placeholder(expr)         =>
        val value = evaluateExpression(expr, context)
        value match
          case PluralSelector(v, _) => v.toString
          case other                => other.toString
      case PatternSequence(patterns) =>
        patterns.map(p => evaluatePattern(p, context)).mkString

  private def evaluateMatch(matchStmt: MatchStatement, context: Context): String =
    // Evaluate selectors
    val selectorValues = matchStmt.selectors.map(s => evaluateExpression(s, context))

    // Find matching case
    val matchedCase = matchStmt.cases.find { mcase =>
      // Check if all keys match their corresponding selector values
      if mcase.keys.size == selectorValues.size then
        mcase.keys.zip(selectorValues).forall {
          case (key, PluralSelector(value, locale)) =>
            matchesPluralKey(value, key, locale)
          case (key, value)                         =>
            key == value.toString || key == "*"
        }
      else
        false
    }

    matchedCase match
      case Some(mcase) => evaluatePattern(mcase.pattern, context)
      case None        => "[no match]"

  end evaluateMatch

  private def matchesPluralKey(
    value: Any,
    key: String,
    locale: String,
  ): Boolean =
    Try(value.toString.toDouble.toInt) match
      case scala.util.Success(count) =>
        key match
          // Exact numeric matches (work for all locales)
          case "=0"                   => count == 0
          case "=1"                   => count == 1
          case "0"                    => count == 0
          case "1"                    => count == 1
          case "2"                    => count == 2
          case s if s.startsWith("=") => count == s.drop(1).toInt
          case s if s.matches("\\d+") => count == s.toInt

          // Plural category matching (locale-specific)
          case "one" =>
            locale.takeWhile(_ != '-').toLowerCase match
              case "fr" => count == 0 || count == 1 // French (any region): one = 0 or 1
              case _    => count == 1               // English/Dutch: one = 1

          case "other" => true // "other" matches everything
          case "*"     => true // wildcard matches everything
          case _       => false
      case _                         => key == "other" || key == "*"

end MessageFormat2
