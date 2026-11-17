package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestonesTest : TfmSandboxTest() {
  @Test
  internal fun `A milestone cannot be claimed twice`() {
    newTestGame()
    kim.setToExMachina(35, "TerraformRating")
    stan.setToExMachina(35, "TerraformRating")
    kim.inTurn {
      doTask("UseAction<ClaimMilestoneAction, Action1>")
      shouldThrow<TaskException> { doTask("Terraformer35") }
      doTask("8 Pay<> FROM MC")
      doTask("Terraformer35")
    }

    shouldThrow<LimitsException> {
      stan.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Terraformer35")
      }
    }
  }

  @Test
  internal fun `an unqualified milestone claim cannot complete`() {
    newTestGame()
    kim.setToExMachina(8, "MC")

    kim.inTurn {
      shouldThrow<RequirementException> {
        kim.continueOperation {
          doTasks(
              "UseAction<ClaimMilestoneAction, Action1>",
              "8 Pay<> FROM MC",
              "Terraformer35",
          )
        }
      }
      kim.count("Terraformer35") shouldBe 0
      kim.count("MC") shouldBe 8
      kim.doTask("Pass")
    }
    game.isIdle() shouldBe true
  }
}
