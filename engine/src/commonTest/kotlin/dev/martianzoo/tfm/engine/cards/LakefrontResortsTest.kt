package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class LakefrontResortsTest : CardTest() {
  @Test
  fun `opponents' oceans raise production and owner's adjacent tiles pay one`() {
    val game = newGame(Canon.fromOptionCodes("BMT", 2))
    val owner = game.tfm(PLAYER1)
    val opponent = game.tfm(PLAYER2)

    owner.phase("Action")
    owner.sneak("LakefrontResorts, 54")
    owner.assertCounts(0 to "Mandate")

    opponent.manual("OceanTile<Tharsis_1_2>").expect("PROD[1 Megacredit<Player1>]")

    // Two is the normal ocean-adjacency bonus; the third is Lakefront Resorts' bonus.
    owner.manual("CityTile<Tharsis_2_2>").expect("3 Megacredit")
  }

  @Test
  fun `opponent receives only the ordinary ocean adjacency bonus`() {
    val game = newGame(Canon.fromOptionCodes("BMT", 2))
    val owner = game.tfm(PLAYER1)
    val opponent = game.tfm(PLAYER2)

    owner.phase("Action")
    owner.sneak("LakefrontResorts, 54")
    opponent.manual("OceanTile<Tharsis_1_2>").expect("PROD[1 Megacredit<Player1>]")

    opponent.manual("CityTile<Tharsis_2_2>").expect("2 Megacredit<Player2>")
  }
}
