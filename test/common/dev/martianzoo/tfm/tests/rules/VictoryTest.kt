package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VictoryTest {

  @Test
  internal fun exactMultiplayerTiesProduceJointVictories() {
    val game = setUpGame()
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    admin.manual("End FROM Phase")

    p1.count("Victory<Player1>") shouldBe 1
    p2.count("Victory<Player2>") shouldBe 1
    admin.count("End") shouldBe 1
    shouldThrow<LimitsException> { admin.manual("-End") }
  }

  @Test
  internal fun mcBreaksAVictoryPointTie() {
    val game = setUpGame()
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p1.sneak("MC")

    admin.manual("End FROM Phase")

    p1.count("Victory<Player1>") shouldBe 1
    p2.count("Victory<Player2>") shouldBe 0
  }
}
