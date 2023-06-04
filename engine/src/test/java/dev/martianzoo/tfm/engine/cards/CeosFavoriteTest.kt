package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CeosFavoriteTest {
  @Test
  fun ceosFavoriteProject() {
    val game = Engine.newGame(GameSetup(Canon, "CVERB", 2))

    with(game.gameplay(PLAYER1).turnLayer().operationLayer()) {
      initiate("10 ProjectCard, ForcedPrecipitation")

      // We can't CEO's onto an empty card
      assertThrows<DependencyException> {
        initiate("CeosFavoriteProject") { doTask("Floater<ForcedPrecipitation>") }
      }

      taskLayer().changeLayer().sneak("Floater<ForcedPrecipitation>")
      assertCounts(1 to "Floater")

      initiate("CeosFavoriteProject") { doTask("Floater<ForcedPrecipitation>") }
      assertCounts(2 to "Floater")
    }
  }
}
