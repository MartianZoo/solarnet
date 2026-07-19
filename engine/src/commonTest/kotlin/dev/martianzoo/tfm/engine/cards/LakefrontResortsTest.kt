package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LakefrontResortsTest : CardTest() {
  @Test
  fun `opponents' oceans raise production and owner's adjacent tiles pay one`() {
    val game = newGame(Canon.fromOptionCodes("BMT", 2))
    val owner = game.tfm(PLAYER1)
    val opponent = game.tfm(PLAYER2)

    owner.phase("Action")
    owner.godMode().sneak("LakefrontResorts, 54")
    owner.assertCounts(0 to "Mandate")

    opponent.godMode().manual("OceanTile<Tharsis_1_2>").expect("PROD[1 Megacredit<Player1>]")
    owner.assertProds(1 to "Megacredit")
    opponent.assertProds(0 to "Megacredit")

    // Two is the normal ocean-adjacency bonus; the third is Lakefront Resorts' bonus.
    owner.godMode().manual("CityTile<Tharsis_2_2>").expect("3 Megacredit")
  }

  @Test
  fun `opponent receives only the ordinary ocean adjacency bonus`() {
    val game = newGame(Canon.fromOptionCodes("BMT", 2))
    val owner = game.tfm(PLAYER1)
    val opponent = game.tfm(PLAYER2)

    owner.phase("Action")
    owner.godMode().sneak("LakefrontResorts, 54")
    opponent.godMode().manual("OceanTile<Tharsis_1_2>").expect("PROD[1 Megacredit<Player1>]")

    val ownerMoney = owner.count("Megacredit")
    val opponentMoney = opponent.count("Megacredit")
    opponent.godMode().manual("CityTile<Tharsis_2_2>").expect("2 Megacredit<Player2>")
    owner.count("Megacredit") shouldBe ownerMoney
    opponent.count("Megacredit") shouldBe opponentMoney + 2
  }
}
