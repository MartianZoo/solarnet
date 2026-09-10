package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class EcolineTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    p1.runOperation("$Ecoline")
    admin.phase("Action")
  }

  @Test
  internal fun `Can convert seven plants into greenery`() {
    p1.runOperation("4 Plant")
    p1.assertCounts(7 to "Plant")
    p1.convertPlants { placeTile(4, 2) }.expect("-6 Plant, GreeneryTile")
    p1.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Cannot convert only six plants into greenery`() {
    p1.runOperation("3 Plant")
    shouldThrow<LimitsException> { p1.convertPlants() }
  }
}
