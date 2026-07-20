package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ArcticAlgaeTest : CardTest() {
  @Test
  fun `ocean placed by another player gives plants to Arctic Algae's owner`() {
    val game = newGame(Canon.SIMPLE_GAME)
    val owner = game.tfm(PLAYER1)
    val other = game.tfm(PLAYER2)
    owner.sneak("ArcticAlgae")
    val plantsBefore = owner.count("Plant")

    other.manual("OceanTile<Tharsis_1_2>")

    owner.count("Plant") shouldBe plantsBefore + 2
  }
}
