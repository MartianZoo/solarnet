package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class LocalHeatTrappingTest : CardTest() {
  init {
    newGame(Canon.SIMPLE_GAME)
  }

  val p1 = game.tfm(PLAYER1)

  @Test
  fun notEnoughHeat() {
    with(p1) {
      sneak("4 Heat, 2 ProjectCard, Pets, Animal<Pets>, 100")
      assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
      assertCounts(4 to "Card", 3 to "CardBack", 1 to "CardFront", 0 to "PlayedEvent")

      phase("Action")

      playProject("LocalHeatTrapping", 1) {
        // The card is played but nothing else
        assertCounts(4 to "Card", 2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
        abort()
      }
    }
  }

  @Test
  fun getPlants() {
    with(p1) {
      sneak("6 Heat, 2 ProjectCard, Pets, Animal<Pets>")

      manual("LocalHeatTrapping") {
        // The card is played and the heat is gone
        assertCounts(1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")
        doFirstTask("4 Plant")
      }

      assertCounts(3 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(4 to "Plant", 1 to "Heat", 1 to "Animal")
    }
  }

  @Test
  fun getPets() {
    with(p1) {
      sneak("6 Heat, 2 ProjectCard, Pets, Animal<Pets>")

      manual("LocalHeatTrapping") {
        // The card is played and the heat is gone
        assertCounts(3 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

        shouldThrow<AbstractException> { doFirstTask("2 Animal") }

        // card I don't have
        shouldThrow<DependencyException> { doFirstTask("2 Animal<Fish>") }

        // but this should work
        doFirstTask("2 Animal<Pets>")
      }
      assertCounts(3 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 1 to "Heat", 3 to "Animal")
    }
  }
}
