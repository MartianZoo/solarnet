package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.*
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SoloGenerationCountdownTest : TfmSandboxTest() {

  @Test
  internal fun laterGenerationsRemoveOneGenerationLeft() {
    val game = setUpGame(players = 1)
    val agents = Agents(game) { NONE }
    val admin = agents[ADMIN]

    admin.runOperation("Generation")

    admin.count("SoloGenerationsLeft") shouldBe 12
  }

  @Test
  internal fun enteringTheFinalSoloGenerationRemovesTheLastGameEndBarrier() {
    val game = setUpGame(players = 1)
    val agents = Agents(game) { NONE }
    val admin = agents[ADMIN]
    admin.exMachina("-12 SoloGenerationsLeft")

    admin.runOperation("-SoloGenerationsLeft")

    admin.count("SoloGenerationsLeft") shouldBe 0
    admin.count("GameEndBarrier") shouldBe 0
  }

  @Test
  internal fun `TR63 solo permits buying a rating step with Buffer Gas`() {
    newTestGame(addOptions = "Tr63SoloObjective, -ColoniesExpansion", playerCount = 1)
    kim.setToExMachina(16, "MC")

    kim.inTurn {
      doTasks(*standardProjectTasks("BufferGasProject"), "16 Pay<> FROM MC", "TerraformRating")
    }
  }
}
