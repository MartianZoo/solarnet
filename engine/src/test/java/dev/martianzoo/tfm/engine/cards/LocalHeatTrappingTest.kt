package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LocalHeatTrappingTest {
  @Test
  fun notEnoughHeat() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    with(game.session(PLAYER1)) {
      operation("4 Heat, 2 ProjectCard, Pets")
      assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
      assertCounts(3 to "Card", 2 to "CardBack", 1 to "CardFront", 0 to "PlayedEvent")

      operation("LocalHeatTrapping") {
        // The card is played but nothing else
        assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")

        // And for the expected reasons
        assertThat(tasks.extract { it.whyPending })
            .containsExactly(
                // TODO "When gaining null and removing Heat<Player1>: can do only 4 of 5 required",
                null,
                "choice required in: `4 Plant<Player1>! OR 2 Animal<Player1>.`",
            )

        rollItBack()
      }
    }
  }

  @Test
  fun getPlants() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    with(game.session(PLAYER1)) {
      operation("6 Heat, 2 ProjectCard, Pets")

      operation("LocalHeatTrapping") {
        // The card is played and the heat is gone
        assertCounts(1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

        assertThat(tasks.extract { it.whyPending }.single())
            .isEqualTo("choice required in: `4 Plant<Player1>! OR 2 Animal<Player1>.`")

        task("4 Plant")
      }

      assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(4 to "Plant", 1 to "Heat", 1 to "Animal")
    }
  }
  @Test
  fun getPets() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    with(game.session(PLAYER1)) {
      operation("6 Heat, 2 ProjectCard, Pets")

      operation("LocalHeatTrapping") {
        // The card is played and the heat is gone
        assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

        assertThrows<AbstractException>("1") { task("2 Animal") }
        assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

        // card I don't have
        assertThrows<DependencyException>("2") { task("2 Animal<Fish>") }

        // but this should work
        task("2 Animal<Pets>")
      }
      assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 1 to "Heat", 3 to "Animal")
    }
  }

  // @Test // TODO - overeager DependencyException
  fun getNothing() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    with(game.session(PLAYER1)) {
      operation("6 Heat, 2 ProjectCard")

      operation("LocalHeatTrapping") {
        assertThat(tasks.extract { "${it.whyPending}" })
            .containsExactly("choice required in: `4 Plant<Player1>! OR 2 Animal<Player1>.`")

        writer.prepareTask(tasks.ids().single())
        assertThat(tasks.extract { "${it.whyPending}" })
            .containsExactly("choice required in: `4 Plant<Player1>! OR Ok`")
      }
    }
  }
}
