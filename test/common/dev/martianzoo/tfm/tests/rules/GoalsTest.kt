package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GoalsTest : TfmSandboxTest() {
  @Test
  internal fun `Briber costs twelve MC in addition to the normal claim cost`() {
    newTestGame(addOptions = "Briber, Builder, Engineer")
    kim.setToExMachina(20, "MC")

    kim.inTurn {
          doTasks(
              "UseAction<ClaimMilestoneAction, Action1>",
              "8 Pay<> FROM MC",
              "Briber",
              "-12 MC",
          )
        }
        .expect("-20 MC")
  }

  @Test
  internal fun `Briber claim is atomic when the player cannot pay the extra cost`() {
    newTestGame(addOptions = "Briber, Builder, Engineer")
    kim.setToExMachina(19, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Briber",
            "-12 MC",
        )
      }
    }

    kim.count("MC") shouldBe 19
    kim.count("Milestone") shouldBe 0
  }

  @Test
  internal fun `Philantropist counts own scoring cards but not Vitor's reference`() {
    newTestGame(addOptions = "$Vitor, Philantropist, Builder, Engineer")
    kim.exMachina("$Vitor, $SearchForLife, $Tardigrades, $ColonizerTrainingCamp, $DustSeals")
    stan.exMachina("$Trees")

    // Each concrete card class permits at most one played copy across owners, so counting played
    // card classes counts cards. Vitor's class reference is not a played card, and Stan's Trees is
    // outside Kim's requirement.
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Philantropist",
        )
      }
    }

    kim.exMachina("$SpaceElevator")
    kim.inTurn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Philantropist",
      )
    }
  }

  @Test
  internal fun `Merchant checks resources after the normal claim cost`() {
    newTestGame(addOptions = "Merchant, Builder, Engineer")
    kim.exMachina("2 Steel, 2 Titanium, 2 Plant, 2 Energy, 2 Heat")
    kim.setToExMachina(9, "MC")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Merchant",
        )
      }
    }
    kim.count("MC") shouldBe 9
    kim.count("Merchant") shouldBe 0

    kim.setToExMachina(10, "MC")
    kim.inTurn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Merchant",
      )
    }
  }

  private fun aquifer(area: String, bonus: String) {
    kim.inTurn {
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<$area>",
          "TerraformRating",
          "HydrologistCredit",
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
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Hydrologist",
        )
      }
    }
    kim.inTurn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Hydrologist",
      )
    }
  }

  // Producer wants 16 printed production, and Producer22 wants 22 because QuickStartVariant hands
  // you 6 at setup. Both start one short of their threshold after these grants.
  private fun claimProducerOneProductionShortOfThreshold(milestone: String, modules: String) {
    newTestGame(addOptions = "$milestone, Builder, Engineer$modules")
    kim.exMachina("PROD[5 Steel, 5 Titanium, 5 Plant]")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            milestone,
        )
      }
    }

    kim.exMachina("PROD[Energy]")
    kim.inTurn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          milestone,
      )
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
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Producer",
        )
      }
    }

    kim.exMachina("PROD[Energy]")
    kim.inTurn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Producer",
      )
    }
  }

  @Test
  internal fun `Producer22 can be selected without Quick Start`() {
    newTestGame(addOptions = "Producer22, Builder, Engineer, -QuickStartVariant")
    kim.exMachina("PROD[7 Steel, 7 Titanium, 7 Plant]")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Producer22",
        )
      }
    }

    kim.exMachina("PROD[Energy]")
    kim.inTurn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Producer22",
      )
    }
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    // Unresolved adjudication (question 27, 2026-10-14); this records current behavior.
    // https://boardgamegeek.com/thread/3478406/article/47218204#47218204
    @Test
    internal fun `Dry Deserts does not erase Hydrologist credit for a removed ocean`() {
      newTestGame(
          "-QuickStartVariant, -ColoniesExpansion, TurmoilExpansion, Hydrologist, Builder, Engineer",
          playerCount = 2,
          firstGlobalEvents =
              listOf("$MinimalImpactPolicy", "$DemocraticReform", "$ScientificProgress"),
      )
      // Funding the later projects is incidental; earn every milestone credit through real
      // placement.
      kim.exMachina("100 MC")
      kim.turn {
        doTasks(
            *standardProjectTasks("AquiferProject"),
            "18 Pay<> FROM MC",
            "OceanTile<Tharsis_1_2>",
            "TerraformRating",
            "HydrologistCredit",
            "2 Steel",
        )
      }
      stan.doTask("Pass")
      kim.doTask("Pass")
      kim.doTask("21 MC")
      stan.doTask("20 MC")
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

      // Dry Deserts removes the credited ocean in the following Solar phase.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.doTask("20 MC") }
      stan.doTask("VenusStep! BY Admin")
      stan.doTask("-OceanTile<Tharsis_1_2>").expect("0 HydrologistCredit<Kim>")
      players.forEach {
        it.doTask("EACH")
      } // Neither player has influence to exchange for resources.
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

      // Reusing the emptied area earns another credit; all four count toward the completed claim.
      kim.turn {
        doTasks(
            *standardProjectTasks("AquiferProject"),
            "18 Pay<> FROM MC",
            "OceanTile<Tharsis_1_2>",
            "TerraformRating",
            "HydrologistCredit",
            "3 Steel", // Two from placement and one from Mars First's policy.
        )
        doTasks(
            *standardProjectTasks("AquiferProject"),
            "18 Pay<> FROM MC",
            "OceanTile<Tharsis_1_5>",
            "TerraformRating",
            "HydrologistCredit",
            "Steel", // Mars First's policy.
        )
      }
      stan.doTask("Pass")
      kim.turn {
        doTasks(
            *standardProjectTasks("AquiferProject"),
            "18 Pay<> FROM MC",
            "OceanTile<Tharsis_2_6>",
            "TerraformRating",
            "HydrologistCredit",
            "2 ProjectCard",
            "2 MC",
            "Steel", // Mars First's policy.
        )
        doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Hydrologist")
      }
      kim.count("Hydrologist") shouldBe 1
      kim.count("HydrologistCredit") shouldBe 4
    }
  }
}
