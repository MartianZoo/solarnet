package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class HeadStartTest : CardTest() {
  init {
    newGame("BMPTX", 2)
  }

  @Test
  fun `decline both actions`() {
    with(game.tfm(PLAYER1)) {
      phase("Prelude")
      sneak("4, 10 ProjectCard, PreludeCard")
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
      phase("Prelude")
      sneak("4, 10 ProjectCard, PreludeCard, 10 Heat")
      playPrelude("HeadStart") {
        assertCounts(2 to "Steel", 24 to "M")
        doFirstTask("UseAction1<UseStandardProjectSA>")
        doTask("UseAction1<ConvertHeatSA>")
        doTask("UseAction1<AquiferSP>")
        doTask("OceanTile<M55>")
      }
    }
  }
}
