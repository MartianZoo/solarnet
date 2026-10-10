package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AwardsRulesTest : TfmSandboxTest() {
  @Test
  internal fun `Tied players receive the appropriate first and second place award points`() {
    newTestGame()
    kim.setToExMachina(3, "Heat")
    kim.setToExMachina(3, "Steel")
    stan.setToExMachina(2, "Heat")
    stan.setToExMachina(3, "Steel")
    rob.setToExMachina(2, "Heat")
    rob.setToExMachina(2, "Steel")
    kim.fundAward(cn("Thermalist"), 8)
    stan.fundAward(cn("Miner"), 14)

    victoryPoints() shouldBe listOf(30, 27, 22)
  }

  @Test
  internal fun `A two-player game awards no second-place points`() {
    newTestGame(playerCount = 2)
    kim.setToExMachina(1, "Heat")
    kim.fundAward(cn("Thermalist"), 8)

    victoryPoints() shouldBe listOf(25, 20)
  }
}
