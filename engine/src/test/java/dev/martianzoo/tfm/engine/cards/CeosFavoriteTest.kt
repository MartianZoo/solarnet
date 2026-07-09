package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test
import io.kotest.assertions.throwables.shouldThrow

class CeosFavoriteTest {
  @Test
  fun ceosFavoriteProject() {
    val game = Engine.newGame(GameSetup(Canon, "VERB", 2))

    with(game.tfm(PLAYER1)) {
      godMode().manual("10 ProjectCard, ForcedPrecipitation")

      // We can't CEO's onto an empty card
      shouldThrow<DependencyException> {
        godMode().manual("CeosFavoriteProject") { doTask("Floater<ForcedPrecipitation>") }
      }

      godMode().sneak("Floater<ForcedPrecipitation>")
      assertCounts(1 to "Floater")

      godMode().manual("CeosFavoriteProject") { doTask("Floater<ForcedPrecipitation>") }
      assertCounts(2 to "Floater")
    }
  }
}
