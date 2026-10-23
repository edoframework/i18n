package dev.taihei.tsuji

import java.time.LocalDate

import dev.taihei.tsuji.Language.*

// Families the tests share.
object Orders extends MessageFamily("order"):

  val cancelled = message("Your order was cancelled", Dutch -> "Uw bestelling werd geannuleerd")

  val shipped = message[(count: Int, on: LocalDate)](
    ".match {$count :plural} when one {One item shipped on {$on :date dateStyle=long}} " +
    "when * {{$count} items shipped on {$on :date dateStyle=long}}",
    Dutch ->
    (".match {$count :plural} when one {Eén artikel verzonden op {$on :date dateStyle=long}} " +
    "when * {{$count} artikels verzonden op {$on :date dateStyle=long}}"),
  )

  val greeting = message[(name: String)]("Hello {$name}")

final case class OrderId(value: String)

object OrderId:
  given MessageArg[OrderId] = MessageArg.as[OrderId, String](_.value, s => Right(OrderId(s)))

object Shapes extends MessageFamily("shape", sourceLanguage = Dutch):

  val circle = message("Cirkel", English -> "Circle")

  val note = message[(id: OrderId, by: Option[String], in: Language)]("Bestelling {$id} door {$by} in {$in}")

  val list = message[(items: List[Int])]("Items: {$items}")

  val nested = message[(field: String, error: Message)]("{$field}: {$error}")

object Clash extends MessageFamily("order"):

  val other = message("Another family with the prefix of Orders")
