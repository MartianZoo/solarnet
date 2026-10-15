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
        doTask("ClaimMilestone<Class<Terraformer26>>")
        doTask("8 Pay<> FROM MC")
        doTask("Terraformer26")
      }
    }

    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "14 Pay<> FROM MC")
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Terraformer26>>", "8 Pay<> FROM MC")
      doTask("Terraformer26")
    }
    shouldThrow<RequirementException> {
      stan.inTurn {
        doTask("ClaimMilestone<Class<Terraformer35>>")
        doTask("8 Pay<> FROM MC")
        doTask("Terraformer35")
      }
    }
    stan.setToExMachina(35, "TerraformRating")
    stan.inTurn {
      doTasks("ClaimMilestone<Class<Terraformer35>>", "8 Pay<> FROM MC")
      doTask("Terraformer35")
    }
  }

  @Test
  internal fun `Lobbyist counts a chairman and six delegates without double counting leaders`() {
    newTestGame(addOptions = "TurmoilExpansion, Lobbyist, Builder, Engineer")
    kim.exMachina("5 PartyDelegate<Scientists>, Chairman<Kim FROM Neutral>")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Lobbyist>>")
        doTask("8 Pay<> FROM MC")
        doTask("Lobbyist")
      }
    }

    kim.inTurn {
      doTask("LobbyAction<Action1>")
      doTask("PartyDelegate<Scientists>")
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Lobbyist>>", "8 Pay<> FROM MC")
      doTask("Lobbyist")
    }
  }
}
