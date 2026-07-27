package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class EcolineTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    p1.manual("Ecoline")
    engine.phase("Action")
  }

  @Test
  fun `with seven plants, converts plants as Ecoline`() {
    p1.manual("4 Plant")
    p1.assertCounts(7 to "Plant")
    p1.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<M42>") }.expect("GreeneryTile")
    p1.assertCounts(1 to "Plant")
  }

  @Test
  fun `with six plants, tries to convert plants as Ecoline`() {
    p1.manual("3 Plant")
    shouldThrow<LimitsException> { p1.stdAction("ConvertPlantsSA") }
  }
}
