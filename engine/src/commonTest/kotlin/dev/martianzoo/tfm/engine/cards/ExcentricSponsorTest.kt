package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.BeforeTest
import kotlin.test.Test

class ExcentricSponsorTest : CardTest() {
  init {
    newGame("BPVM", 2) // base, prelude, venus, default map, 2p
  }

  @BeforeTest
  fun `common setup steps for all tests`() {
    with(game.tfm(PLAYER1)) {
      phase("Prelude")
      sneak("44, ProjectCard, PreludeCard")
    }
  }

  @Test
  fun `getting the full discount`() {
    with(game.tfm(PLAYER1)) {
      playPrelude("ExcentricSponsor") {
        playProject("NitrogenRichAsteroid", 6) // 31 base cost - 25 discount
      }

      assertProds(1 to "Plant")
      assertCounts(
          38 to "Megacredit", // 47 - 3 - 6
          1 to "ExcentricSponsor",
          1 to "PlayedEvent",
          23 to "TerraformRating",
          0 to "Tag",
      )
    }
  }

  @Test
  fun `getting only a partial discount`() {
    with(game.tfm(PLAYER1)) {
      playPrelude("ExcentricSponsor") {
        playProject("GhgImportFromVenus", 0) // base cost 23 but we get no change
      }

      assertProds(3 to "Heat")
      assertCounts(
          44 to "Megacredit", // 47 - 3
          1 to "ExcentricSponsor",
          1 to "PlayedEvent",
          21 to "TerraformRating",
          0 to "Tag",
      )
    }
  }
}
