package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

class StandardTechnologyTest : CardTest() {
  @Test
  fun `rebates three megacredits without reducing the standard project payment`() {
    newGame()
    engine.phase("Action")
    p1.manual("8 Megacredit, $StandardTechnology")

    val result = p1.stdProject("PowerPlantSP")
    val megacreditChanges =
        result.changes.mapNotNull { event ->
          when {
            event.change.gaining?.let(game.reader::resolve) == p1.resolve("Megacredit") ->
                event.change.count
            event.change.removing?.let(game.reader::resolve) == p1.resolve("Megacredit") ->
                -event.change.count
            else -> null
          }
        }

    megacreditChanges.shouldContainExactlyInAnyOrder(3, -11)
  }
}
