package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class HeadStartTest {
  val game = Engine.newGame(GameSetup(Canon, "BMPTX", 2))

  @Test
  fun `decline both actions`() {
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("TerralabsResearch", 10)

      phase("Prelude")
      playPrelude("HeadStart") {
        assertCounts(2 to "Steel", 24 to "M")

        doFirstTask("Ok")
        doFirstTask("Ok")
      }
    }
  }

  @Test
  fun `do both actions`() {
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("TerralabsResearch", 10)

      godMode().sneak("10 Heat")

      phase("Prelude")
      playPrelude("HeadStart") {
        assertCounts(2 to "Steel", 24 to "M")
        doTask("UseAction1<UseStandardProjectSA>")
        doTask("UseAction1<ConvertHeatSA>")
        doTask("UseAction1<AquiferSP>")
        doTask("OceanTile<M55>")
      }
    }
  }
}
