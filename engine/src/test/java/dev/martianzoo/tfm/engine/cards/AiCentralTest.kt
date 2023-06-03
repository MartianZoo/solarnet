package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.NewTerraformingMars
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AiCentralTest {
  @Test
  fun aiCentral() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    val eng = NewTerraformingMars(game, ENGINE)
    val p1 = NewTerraformingMars(game, PLAYER1)

    eng.phase("Action")

    with(p1) {
      sneak("5 ProjectCard, 100, Steel")

      playCard("SearchForLife", 3)
      playCard("InventorsGuild", 9)

      assertThrows<RequirementException>("1") { playCard("AiCentral") }
      playCard("DesignedMicroorganisms", 16)

      assertCounts(3 to "ScienceTag")

      // Now I do have the 3 science tags, but not the energy production
      assertThrows<LimitsException>("2") { playCard("AiCentral", 19, steel = 1) }

      // Give energy prod and try again - success
      sneak("PROD[Energy]")
      playCard("AiCentral", 19, steel = 1)
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
      eng.game.operationsLayer().initiate("Generation")

      cardAction1("AiCentral")
      assertCounts(5 to "ProjectCard")
    }
  }
}
