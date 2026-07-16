package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class CommercialDistrictTest : CardTest() {
  @Test
  fun `scores one point for each adjacent city`() {
    val game = newGame(GameSetup(Canon, "BMR", 2))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    p1.godMode().manual("PROD[Energy], CityTile<Tharsis_3_2>")
    p1.godMode().manual("CommercialDistrict") { doTask("CdTile<Tharsis_3_3>") }
    p2.godMode().manual("CityTile<Tharsis_3_4>")

    game.tfm(ENGINE).phase("End")
    p1.assertCounts(22 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }
}
