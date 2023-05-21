package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.cardAction1
import dev.martianzoo.tfm.engine.TerraformingMars.playCard
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AiCentralTest {
  @Test
  fun aiCentral() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))

    with(game.session(PLAYER1)) {
      phase("Action")
      writer.unsafe().sneak("5 ProjectCard, 100, Steel")

      playCard("SearchForLife", 3)
      playCard("InventorsGuild", 9)

      assertThrows<RequirementException>("1") { playCard("AiCentral") }
      playCard("DesignedMicroorganisms", 16)

      // Now I do have the 3 science tags, but not the energy production
      assertThrows<LimitsException>("2") { playCard("AiCentral", 19, steel = 1) }

      // Give energy prod and try again - success
      writer.unsafe().sneak("PROD[Energy]")
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
      operation("Generation")
      cardAction1("AiCentral")
      assertCounts(5 to "ProjectCard")
    }
  }
}
