package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

class LocalHeatTrappingTest {
  val game = setUpGame(Canon.SIMPLE_GAME)
  val p1 = game.tfm(PLAYER1)

  @Test
  fun notEnoughHeat() {
    with(p1) {
      godMode().manual("4 Heat, 2 ProjectCard, Pets, 100")
      assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
      assertCounts(3 to "Card", 2 to "CardBack", 1 to "CardFront", 0 to "PlayedEvent")

      phase("Action")

      playProject("LocalHeatTrapping", 1) {
        // The card is played but nothing else
        assertCounts(3 to "Card", 1 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
        abort()
      }
    }
  }

  @Test
  fun getPlants() {
    with(p1) {
      godMode().manual("6 Heat, 2 ProjectCard, Pets")

      godMode().manual("LocalHeatTrapping") {
        // The card is played and the heat is gone
        assertCounts(1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")
        doFirstTask("4 Plant")
      }

      assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(4 to "Plant", 1 to "Heat", 1 to "Animal")
    }
  }

  @Test
  fun getPets() {
    with(p1) {
      godMode().manual("6 Heat, 2 ProjectCard, Pets")

      godMode().manual("LocalHeatTrapping") {
        // The card is played and the heat is gone
        assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

        shouldThrow<AbstractException> { doFirstTask("2 Animal") }

        // card I don't have
        shouldThrow<DependencyException> { doFirstTask("2 Animal<Fish>") }

        // but this should work
        doFirstTask("2 Animal<Pets>")
      }
      assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 1 to "Heat", 3 to "Animal")
    }
  }

  // @Test // TODO - make this work
  fun getNothing() {
    with(p1) {
      godMode().manual("6 Heat, 2 ProjectCard")

      godMode().manual("LocalHeatTrapping") {
        tasks
            .extract { it.whyPending }
            .shouldContainExactlyInAnyOrder(
                "choice required in: `4 Plant<Player1>! OR 2 Animal<Player1>.`"
            )

        p1.prepareTask(tasks.ids().single())
        tasks
            .extract { it.whyPending }
            .shouldContainExactlyInAnyOrder("choice required in: `4 Plant<Player1>! OR Ok`")
      }
    }
  }
}
