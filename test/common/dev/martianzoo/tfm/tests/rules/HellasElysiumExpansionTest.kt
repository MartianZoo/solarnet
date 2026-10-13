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
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Specialist>>")
        selectTask("-X? MC")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }

    kim.inTurn {
      doTasks("UseStandardProject<CityProject>", "-25 MC", "PROD[MC]", "CityTile<Elysium_1_5>")
      selectTask("ProjectCard<Kim, Hand>!")
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Specialist>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
  }
}
