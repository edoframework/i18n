package example.orders

import java.time.LocalDate

import dev.taihei.tsuji.*
import dev.taihei.tsuji.Language.*

// A family outside the library's package, where every user defines theirs: the expansion of
// `message` may only use what is public in the bytecode.
object Shop extends MessageFamily("example.shop"):

  val closed = message("The shop is closed", Dutch -> "De winkel is gesloten")

  val opens = message[(on: LocalDate, by: String)](
    "Opens on {$on :date dateStyle=long}, says {$by}",
    Dutch -> "Opent op {$on :date dateStyle=long}, zegt {$by}",
  )

  lazy val later = message("Defined lazily", Dutch -> "Lui gedefinieerd")
