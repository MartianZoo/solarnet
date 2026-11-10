package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VictoryTest : TfmSandboxTest() {

  @Test
  internal fun exactMultiplayerTiesProduceJointVictories() {
    game = setUpGame()
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]

    admin.runOperation("End FROM Phase")

    p1.count("Victory<Player1>") shouldBe 1
    p2.count("Victory<Player2>") shouldBe 1
    admin.count("End") shouldBe 1
    admin.count("GameScope") shouldBe 0
    val finalChanges = game.events.changesSinceSetup().takeLast(3)
    finalChanges.first().change.removing?.type?.className shouldBe cn("GameScope")
    finalChanges.drop(1).map { it.change.gaining?.type?.className } shouldBe
        listOf(cn("Victory"), cn("Victory"))
  }

  @Test
  internal fun mcBreaksAVictoryPointTie() {
    game = setUpGame()
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    p1.exMachina("MC")

    admin.runOperation("End FROM Phase")

    p1.count("Victory<Player1>") shouldBe 1
    p2.count("Victory<Player2>") shouldBe 0
  }
}
