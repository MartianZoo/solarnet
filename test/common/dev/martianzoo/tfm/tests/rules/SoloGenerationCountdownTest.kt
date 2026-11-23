package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.engine.*
import dev.martianzoo.testsupport.count
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
    val admin = agents.world.reader

    agents.runSystem("Generation")

    admin.count("SoloGenerationsLeft") shouldBe 12
  }

  @Test
  internal fun enteringTheFinalSoloGenerationRemovesTheLastGameEndBarrier() {
    val game = setUpGame(players = 1)
    val agents = Agents(game) { NONE }
    val admin = agents.world.reader
    agents.correct("-12 SoloGenerationsLeft")

    agents.runSystem("-SoloGenerationsLeft")

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
