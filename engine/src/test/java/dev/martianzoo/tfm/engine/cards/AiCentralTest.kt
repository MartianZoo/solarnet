package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AiCentralTest {
  @Test
  fun aiCentral() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))

    with(game.tfm(PLAYER1)) {
      godMode().sneak("5 ProjectCard, 100, Steel")

      phase("Action")
      playProject("SearchForLife", 3)
      playProject("InventorsGuild", 9)

      assertThrows<RequirementException>("1") { playProject("AiCentral", 21) }
      playProject("DesignedMicroorganisms", 16)

      assertCounts(3 to "ScienceTag")

      // Now I do have the 3 science tags, but not the energy production
      assertThrows<LimitsException>("2") { playProject("AiCentral", 19, steel = 1) }

      // Give energy prod and try again - success
      godMode().sneak("PROD[Energy]")
      playProject("AiCentral", 19, steel = 1)
      assertCounts(0 to "PROD[Energy]")

      // Use the action
      assertCounts(1 to "ProjectCard")
      cardAction1("AiCentral")
      assertCounts(3 to "ProjectCard")
      assertCounts(1 to "ActionUsedMarker<AiCentral>")

      assertThrows<LimitsException>("3") { cardAction1("AiCentral") }
      assertCounts(3 to "ProjectCard")
      assertCounts(1 to "ActionUsedMarker<AiCentral>")

      // Next gen we can again
      asPlayer(ENGINE).godMode().manual("Generation")

      cardAction1("AiCentral")
      assertCounts(5 to "ProjectCard")
    }
  }
}
