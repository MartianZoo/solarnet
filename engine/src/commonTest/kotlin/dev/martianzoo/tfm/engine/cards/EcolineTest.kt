package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class EcolineTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    p1.manual("$Ecoline")
    engine.phase("Action")
  }

  @Test
  fun `Can convert seven plants into greenery`() {
    p1.manual("4 Plant")
    p1.assertCounts(7 to "Plant")
    p1.convertPlants {
          doTask("GreeneryTile<Tharsis_4_2>")
        }
        .expect("-6 Plant, GreeneryTile")
    p1.assertCounts(1 to "Plant")
  }

  @Test
  fun `Cannot convert only six plants into greenery`() {
    p1.manual("3 Plant")
    shouldThrow<LimitsException> {
      p1.convertPlants()
    }
  }
}
