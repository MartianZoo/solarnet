package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class AiCentralTest : CardTest() {
  @Test
  fun aiCentral() {
    val game = newGame("BRM", 2)

    with(game.tfm(PLAYER1)) {
      sneak("5 ProjectCard, 100, Steel")

      phase("Action")
      playProject("SearchForLife", 3)
      playProject("InventorsGuild", 9)

      shouldThrow<RequirementException> { playProject("AiCentral", 21) }
      playProject("DesignedMicroorganisms", 16)

      assertCounts(3 to "ScienceTag")

      // Now I do have the 3 science tags, but not the energy production
      shouldThrow<LimitsException> { playProject("AiCentral", 19, steel = 1) }

      // Give energy prod and try again - success
      sneak("PROD[Energy]")
      playProject("AiCentral", 19, steel = 1).expect("PROD[-Energy]")

      // Use the action
      cardAction1("AiCentral").expect("2 ProjectCard, ActionUsedMarker<AiCentral>")

      shouldThrow<LimitsException> { cardAction1("AiCentral") }

      // Next gen we can again
      asActor(ENGINE).manual("Generation")

      cardAction1("AiCentral").expect("2 ProjectCard, ActionUsedMarker<AiCentral>")
    }
  }
}
