package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.BeforeTest
import kotlin.test.Test

class ThorgateTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    p1.playCorp(ThorGate, 10)
    p1.manual("-10")
    engine.phase("Action")
  }

  @Test
  fun `with Thorgate, buys power production`() {
    val result = p1.stdProject("PowerPlantSP")
    result.expect("-8, PROD[Energy]")

    result.changes
        .filter { event ->
          event.change.removing?.let(game.reader::resolve) == p1.resolve("Megacredit") ||
              event.change.gaining?.let(game.reader::resolve) == p1.resolve("Megacredit")
        }
        .map { event ->
          if (event.change.removing != null) -event.change.count else event.change.count
        }
        .shouldContainExactly(-8)
  }

  @Test
  fun `with seven megacredits, tries to buy power production as Thorgate`() {
    p1.manual("-Megacredit")
    shouldThrow<LimitsException> { p1.stdProject("PowerPlantSP") }
  }
}
