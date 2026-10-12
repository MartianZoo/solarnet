package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AdditionalGoalsTest : TfmSandboxTest() {
  @Test
  internal fun `Terraformer26 can coexist with Terraformer35 and uses its own threshold`() {
    newTestGame(addOptions = "TurmoilExpansion, Terraformer26, Terraformer35, Builder")
    kim.setToExMachina(25, "TerraformRating")
    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Terraformer26>>")
        selectTask("-X MC?")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }

    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
      selectTask("TemperatureStep.")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Terraformer26>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
    shouldThrow<RequirementException> {
      stan.inTurn {
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Terraformer35>>")
        selectTask("-X MC?")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }
    stan.setToExMachina(35, "TerraformRating")
    stan.inTurn {
      doTasks("ClaimMilestone<Class<Terraformer35>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
  }

  @Test
  internal fun `Lobbyist counts a chairman and six delegates without double counting leaders`() {
    newTestGame(addOptions = "TurmoilExpansion, Lobbyist, Builder, Engineer")
    kim.exMachina("5 PartyDelegate<Scientists>, Chairman<Kim FROM Neutral>")
    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("ClaimMilestone<Class<Lobbyist>>")
        selectTask("-X MC?")
        doTask("-MC / Owed<Class<MC>>")
        selectOnlyTask()
      }
    }

    kim.inTurn {
      doTask("LobbyAction<Action1>")
      selectOnlyTask()
      doTask("PartyDelegate<Scientists>")
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Lobbyist>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
  }
}
