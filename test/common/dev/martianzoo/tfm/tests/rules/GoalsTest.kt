package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GoalsTest : TfmSandboxTest() {
  @Test
  internal fun `Briber costs twelve MC in addition to the normal claim cost`() {
    newTestGame(addOptions = "Briber, Builder, Engineer")
    kim.setToExMachina(20, "MC")

    kim.inTurn {
          doTasks("ClaimMilestone<Class<Briber>>", "-8 MC")
          doTask("Briber")
          doTask("-12 MC")
        }
        .expect("-20 MC, Briber")
  }

  @Test
  internal fun `Briber claim is atomic when the player cannot pay the extra cost`() {
    newTestGame(addOptions = "Briber, Builder, Engineer")
    kim.setToExMachina(19, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Briber>>")
        doTask("-8 MC")
        doTask("Briber")
        doTask("-12 MC")
      }
    }

    kim.count("MC") shouldBe 19
    kim.count("Milestone") shouldBe 0
  }

  @Test
  internal fun `Philantropist counts own scoring cards but not Vitor's reference`() {
    newTestGame(addOptions = "Vitor, Philantropist, Builder, Engineer")
    kim.exMachina("$Vitor, $SearchForLife, $Tardigrades, $ColonizerTrainingCamp, $DustSeals")
    stan.exMachina("$Trees")

    // Each concrete card class permits at most one played copy across owners, so counting played
    // card classes counts cards. Vitor's class reference is not a played card, and Stan's Trees is
    // outside Kim's requirement.
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Philantropist>>")
        doTask("-8 MC")
        doTask("Philantropist")
      }
    }

    kim.exMachina("$SpaceElevator")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Philantropist>>", "-8 MC")
      doTask("Philantropist")
    }
  }

  @Test
  internal fun `Merchant checks resources after the normal claim cost`() {
    newTestGame(addOptions = "Merchant, Builder, Engineer")
    kim.exMachina("2 Steel, 2 Titanium, 2 Plant, 2 Energy, 2 Heat")
    kim.setToExMachina(9, "MC")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Merchant>>")
        doTask("-8 MC")
        doTask("Merchant")
      }
    }
    kim.count("MC") shouldBe 9
    kim.count("Merchant") shouldBe 0

    kim.setToExMachina(10, "MC")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Merchant>>", "-8 MC")
      doTask("Merchant")
    }
  }

  private fun aquifer(area: String, bonus: String) {
    kim.inTurn {
      doTasks(
          "UseStandardProject<AquiferProject>",
          "-18 MC",
          "OceanTile<$area>",
          "TerraformRating",
          "OceanCredit",
          bonus,
      )
    }
  }

  @Test
  internal fun `Hydrologist can be claimed after placing four oceans`() {
    newTestGame(addOptions = "Hydrologist, Builder, Engineer")

    kim.setToExMachina(80, "MC")
    aquifer("Tharsis_1_2", "2 Steel")
    aquifer("Tharsis_1_4", "ProjectCard")
    aquifer("Tharsis_2_6", "2 ProjectCard")
    aquifer("Tharsis_4_8", "2 Plant")

    shouldThrow<RequirementException> {
      stan.inTurn {
        doTask("ClaimMilestone<Class<Hydrologist>>")
        doTask("-8 MC")
        doTask("Hydrologist")
      }
    }
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Hydrologist>>", "-8 MC")
      doTask("Hydrologist")
    }
  }

  // Producer wants 16 printed production, and Producer22 wants 22 because QuickStartVariant hands
  // you 6 at setup. Both start one short of their threshold after these grants.
  private fun claimProducerOneProductionShortOfThreshold(milestone: String, modules: String) {
    newTestGame(addOptions = "$milestone, Builder, Engineer$modules")
    kim.exMachina("PROD[5 Steel, 5 Titanium, 5 Plant]")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("ClaimMilestone<Class<$milestone>>", "-8 MC")
        doTask(milestone)
      }
    }

    kim.exMachina("PROD[Energy]")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<$milestone>>", "-8 MC")
      doTask(milestone)
    }
  }

  @Test
  internal fun `Producer requires sixteen printed production`() =
      claimProducerOneProductionShortOfThreshold("Producer", ", -QuickStartVariant")

  @Test
  internal fun `Producer22 requires twenty two printed production`() =
      claimProducerOneProductionShortOfThreshold("Producer22", ", -CorporateEraExpansion")

  @Test
  internal fun `Producer counts Quick Start production toward sixteen`() {
    newTestGame(addOptions = "Producer, Builder, Engineer, -CorporateEraExpansion")
    kim.exMachina("PROD[3 Steel, 3 Titanium, 3 Plant]")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Producer>>")
        doTask("-8 MC")
        doTask("Producer")
      }
    }

    kim.exMachina("PROD[Energy]")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Producer>>", "-8 MC")
      doTask("Producer")
    }
  }

  @Test
  internal fun `Producer22 can be selected without Quick Start`() {
    newTestGame(addOptions = "Producer22, Builder, Engineer, -QuickStartVariant")
    kim.exMachina("PROD[7 Steel, 7 Titanium, 7 Plant]")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("ClaimMilestone<Class<Producer22>>")
        doTask("-8 MC")
        doTask("Producer22")
      }
    }

    kim.exMachina("PROD[Energy]")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Producer22>>", "-8 MC")
      doTask("Producer22")
    }
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `Dry Deserts does not erase Hydrologist credit for a removed ocean`() {
      newTestGame(addOptions = "TurmoilExpansion, Hydrologist, Builder, Engineer", playerCount = 2)
      // Generation 1: Kim earns the credit that Dry Deserts will later test.
      // Revealed events fix delegate placements; none will resolve before the final claim.
      kim.completeOperation {
        doTasks("UseStandardProject<AquiferProject>", "-18 MC")
        doTask("OceanTile<Tharsis_1_2>")
        autoExecNow(EAGER)
      }
      kim.doTask("0 StandardAction") // Decline the second action.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("SolarnetGlobalEvent")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      // Generation 2: Democratic Reform adds an Admin ocean elsewhere, preserving Kim's ocean.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      stan.doTask("OceanTile<Tharsis_1_4> BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("FreeAcademiaTreaty")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      // Generation 3: Minimal Impact Policy's Dry Deserts removes Kim's original ocean.
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.doTask("-OceanTile<Tharsis_1_2>")
      admin.doTask("SelfSufficiencyProgram")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      // Generation 4: reusing the emptied area still earns another credit toward four placements.
      stan.doTask("Pass")
      kim.completeOperation {
        doTasks("UseStandardProject<AquiferProject>", "-18 MC")
        doTask("OceanTile<Tharsis_1_2>")
        autoExecNow(EAGER)
      }
      kim.completeOperation {
        doTasks("UseStandardProject<AquiferProject>", "-18 MC")
        doTask("OceanTile<Tharsis_1_5>")
        autoExecNow(EAGER)
      }
      kim.completeOperation {
        doTasks("UseStandardProject<AquiferProject>", "-18 MC")
        doTask("OceanTile<Tharsis_2_6>")
        autoExecNow(EAGER)
      }
      kim.doTasks("ClaimMilestone<Class<Hydrologist>>", "-8 MC")
    }
  }
}
