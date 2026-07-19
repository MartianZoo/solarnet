package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AphroditeTest : CardTest() {
  @Test
  fun `venus raised by another player rewards Aphrodite's owner`() {
    val game = newGame(GameSetup(Canon, "BMV", 2))
    val owner = game.tfm(PLAYER1)
    val other = game.tfm(PLAYER2)
    owner.godMode().sneak("Aphrodite")
    val moneyBefore = owner.count("Megacredit")

    other.godMode().manual("VenusStep")

    owner.count("Megacredit") shouldBe moneyBefore + 2
  }
}
