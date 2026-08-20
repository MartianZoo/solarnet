package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
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
    p1.stdProject("PowerPlantSP").expect("-8, PROD[Energy]")
  }

  @Test
  fun `with seven megacredits, tries to buy power production as Thorgate`() {
    p1.manual("-Megacredit")
    shouldThrow<LimitsException> { p1.stdProject("PowerPlantSP") }
  }
}
