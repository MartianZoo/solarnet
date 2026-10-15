package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class HellasElysiumExpansionTest : TfmSandboxTest() {
  @Test
  internal fun `Specialist uses printed MC production`() {
    newTestGame(addOptions = "ElysiumMap")
    kim.setToExMachina(9, "PROD[MC]")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Specialist>>")
        doTask("8 Pay<> FROM MC")
        doTask("Specialist")
      }
    }

    kim.inTurn {
      doTasks(
          "UseStandardProject<CityProject>",
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_1_5>",
      )
      doTask("ProjectCard<Kim, Hand>")
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Specialist>>", "8 Pay<> FROM MC")
      doTask("Specialist")
    }
  }
}
