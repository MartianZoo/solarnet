package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class HellasElysiumExpansionTest : TfmSandboxTest() {
  @Test
  internal fun `Specialist uses printed MC production`() {
    newTestGame(addOptions = "ElysiumMap")
    kim.setToExMachina(9, "PROD[MC]")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Specialist")
      }
    }

    kim.inTurn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_1_5>",
      )
      doTask("ProjectCard<Kim, Hand>")
    }
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC")
      doTask("Specialist")
    }
  }
}
