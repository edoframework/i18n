package dev.taihei.tsuji

import java.time.{LocalDate, LocalDateTime, LocalTime}

import scala.NamedTuple.AnyNamedTuple
import scala.annotation.publicInBinary
import scala.quoted.*

// The compile-time half of MessageFamily.message. It reads only the code it is applied to and
// runs the library's own MF2 checks, so it needs no build plugin and incremental compilation stays
// correct.
// Named by the inline MessageFamily.message: public in the bytecode so that no accessor is needed.
@publicInBinary
private[tsuji] object MessageMacros:

  def message[A <: AnyNamedTuple : Type](
    family: Expr[MessageFamily],
    template: Expr[String],
    translations: Expr[Seq[(Language, String)]],
  )(
    using Quotes
  ): Expr[Any] =
    import quotes.reflect.*

    val source = template.value.getOrElse(
      report.errorAndAbort("A message's template must be a literal string.", template)
    )

    // The splice's owner is the expansion; its owner is the val being defined. Only its kind
    // and name are read: asking a val for its flags while its type is being inferred is a
    // cyclic reference, which is also why a lazy val or a var cannot be told from a val here.
    val definedVal  = Symbol.spliceOwner.owner
    val familyClass = family.asTerm.tpe.widen.typeSymbol
    if definedVal.isNoSymbol || !definedVal.isValDef || definedVal.owner != familyClass ||
      !familyClass.flags.is(Flags.Module)
    then
      report.errorAndAbort(
        "A message is defined as a val in the body of its MessageFamily object: not in a def, a nested object or a class.",
        template,
      )
    val name        = definedVal.name
    if !MessageKey.isPart(name) then
      report.errorAndAbort(
        s"'$name' cannot be part of a message key: use an ASCII letter followed by letters, digits and _.",
        template,
      )

    def isNamed(
      t: TypeRepr,
      owner: String,
      n: String,
    ): Boolean =
      t.typeSymbol.name == n && t.typeSymbol.owner.name.stripSuffix("$") == owner

    def elements(t: TypeRepr): List[TypeRepr] = t.dealias match
      case AppliedType(tycon, List(head, tail)) if tycon.typeSymbol.name == "*:" =>
        head :: elements(tail)
      case AppliedType(tycon, args) if tycon.typeSymbol.name.startsWith("Tuple") => args
      case _                                                                     => Nil

    val requested = TypeRepr.of[A]
    val fields    =
      if requested =:= TypeRepr.of[Nothing] || requested =:= TypeRepr.of[AnyNamedTuple] then Nil
      else
        requested.dealias match
          case AppliedType(tycon, List(names, values))
            if isNamed(tycon, "NamedTuple", "NamedTuple") =>
            val ns = elements(names).map:
              case ConstantType(StringConstant(n)) => n
              case other => report.errorAndAbort(s"Not an argument name: ${other.show}", template)
            ns.zip(elements(values))
          case other =>
            report.errorAndAbort(
              s"The arguments of a message are a named tuple, as in message[(count: Int, on: LocalDate)], not ${other.show}.",
              template,
            )

    fields.foreach: (n, _) =>
      if !n.headOption.exists(c => c.isLetter || c == '_') ||
        !n.forall(c => c.isLetterOrDigit || c == '_')
      then
        report.error(s"'$n' is not a valid argument name in a template.", template)

    def staticKind(t: TypeRepr): ArgKind =
      val d = t.dealias
      if d =:= TypeRepr.of[Int] || d =:= TypeRepr.of[Long] || d =:= TypeRepr.of[Double] ||
        d =:= TypeRepr.of[Float]
      then ArgKind.Number
      else if d =:= TypeRepr.of[LocalDate] then ArgKind.Date
      else if d =:= TypeRepr.of[LocalDateTime] then ArgKind.DateTime
      else if d =:= TypeRepr.of[LocalTime] then ArgKind.Time
      else if d =:= TypeRepr.of[String] || d =:= TypeRepr.of[Boolean] || d =:= TypeRepr.of[Language]
      then ArgKind.Text
      else if d <:< TypeRepr.of[Displayable] || d <:< TypeRepr.of[List[Any]] then ArgKind.Text
      else if d <:< TypeRepr.of[Option[Any]] then
        d.baseType(
          TypeRepr.of[Option[Any]].typeSymbol
        ).typeArgs.headOption.map(staticKind).getOrElse(ArgKind.Unknown)
      else ArgKind.Unknown
    end staticKind

    val instances = fields.map: (n, t) =>
      t.asType match
        case '[a] =>
          val instance = Expr.summon[MessageArg[a]].getOrElse(
            report.errorAndAbort(
              s"No MessageArg for the type ${t.show} of argument $n: give it one with MessageArg.as.",
              template,
            )
          )
          '{ (${ Expr(n) }, $instance) }

    val kinds = fields.map((n, t) => n -> staticKind(t)).toMap

    def checkTemplate(
      t: String,
      at: Expr[?],
      what: String,
    ): Unit =
      val (errors, unused) = MessageFormat2.check(t, kinds)
      errors.foreach(e => report.error(s"$what: ${e.message} (at character ${e.position})", at))
      if unused.nonEmpty then
        report.warning(s"$what does not use ${unused.map("$" + _).mkString(", ")}", at)

    checkTemplate(source, template, s"The template of $name")

    translations match
      case Varargs(pairs) =>
        pairs.foreach:
          case '{ ($l: Language) -> ($t: String) } =>
            t.value.foreach(checkTemplate(_, t, s"The ${l.show} translation of $name"))
          case '{ ($l: Language, $t: String) }     =>
            t.value.foreach(checkTemplate(_, t, s"The ${l.show} translation of $name"))
          case _                                   => ()
      case _              => ()

    // An inline vararg arrives as `xs*`, which only a repeated parameter takes: rebuild the Seq.
    val translationSeq: Expr[Seq[(Language, String)]] = translations match
      case Varargs(pairs) => Expr.ofSeq(pairs)
      case other          => '{ $other.toSeq }
    val nameExpr                                      = Expr(name)
    val arguments = '{ Vector[(String, MessageArg[?])](${ Varargs(instances) }*) }
    if fields.isEmpty then
      '{
        $family.define[NamedTuple.Empty](
          $nameExpr,
          $template,
          $translationSeq,
          Vector.empty,
        ).constant
      }
    else
      // The definition's type gains `apply` with the arguments as named, typed parameters, which
      // MessageDefinition, a Selectable, receives in applyDynamic.
      val applyType = MethodType(fields.map(_._1))(_ => fields.map(_._2), _ => TypeRepr.of[Message])
      Refinement(TypeRepr.of[MessageDefinition[A]], "apply", applyType).asType match
        case '[t] =>
          '{ $family.define[A]($nameExpr, $template, $translationSeq, $arguments).asInstanceOf[t] }
    end if
  end message

end MessageMacros
