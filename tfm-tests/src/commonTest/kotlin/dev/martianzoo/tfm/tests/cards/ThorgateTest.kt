package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ThorgateTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    p1.playCorp(ThorGate, 10)
    p1.manual("-10")
    engine.phase("Action")
  }

  @Test
  internal fun `Discounts power-production standard projects`() {
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
  internal fun `Cannot buy power production with only seven megacredits`() {
    p1.manual("-Megacredit")
    shouldThrow<LimitsException> { p1.stdProject("PowerPlantSP") }
  }
}
