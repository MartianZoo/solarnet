package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class LakefrontResortsTest : CardTest() {
  @Test
  fun `opponents' oceans raise production and owner's adjacent tiles pay one`() {
    val game = newGame(GameSetup(Canon, "BMT", 2))
    val owner = game.tfm(PLAYER1)
    val opponent = game.tfm(PLAYER2)

    owner.phase("Action")
    owner.godMode().sneak("LakefrontResorts, 54")
    owner.assertCounts(0 to "Mandate")

    opponent.godMode().manual("OceanTile<Tharsis_1_2>").expect("PROD[1]")

    // Two is the normal ocean-adjacency bonus; the third is Lakefront Resorts' bonus.
    owner.godMode().manual("CityTile<Tharsis_2_2>").expect("3 Megacredit")
  }
}
