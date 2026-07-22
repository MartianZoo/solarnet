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

  @Test
  fun `Giant Ice Asteroid can remove plants just granted by Arctic Algae`() {
    val game = newGame(Canon.SIMPLE_GAME)
    val attacker = game.tfm(PLAYER1)
    val algaeOwner = game.tfm(PLAYER2)
    algaeOwner.sneak("ArcticAlgae, 2 Plant")
    attacker.sneak("ProjectCard, 36 Megacredit")
    attacker.phase("Action")

    attacker.playProject("GiantIceAsteroid", 36) {
      doFirstTask("OceanTile<Tharsis_1_2>")
      doFirstTask("OceanTile<Tharsis_1_4>")
      doTask("-6 Plant<Player2>")
    }

    algaeOwner.count("Plant") shouldBe 0
  }
}
