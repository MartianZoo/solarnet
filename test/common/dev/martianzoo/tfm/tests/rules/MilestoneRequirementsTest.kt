package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class MilestoneRequirementsTest : TfmSandboxTest() {
  @Test
  internal fun `Tycoon requires fifteen active and automated cards`() {
    newTestGame("ElysiumMap")
    kim.exMachina(
        "$ColonizerTrainingCamp, $DeepWellHeating, $CloudSeeding, $MartianRails, " +
            "$WaterImportFromEuropa, $EquatorialMagnetizer, $DomedCrater, $NoctisCity, " +
            "$MethaneFromTitan, $ResearchOutpost, $PhobosSpaceHaven, $BlackPolarDust, " +
            "$ArcticAlgae, $Predators"
    )
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Tycoon15")
      }
    }
    kim.exMachina("$EosChasmaNationalPark")
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Tycoon15")
    }
  }

  @Test
  internal fun `Diversifier requires eight different tag types belonging to its owner`() {
    newTestGame("HellasMap")
    kim.exMachina(
        "$Ecoline, $ThorGate, $PhoboLog, $InventorsGuild, $EarthOffice, $IoMiningIndustries, $Pets"
    )
    stan.exMachina("$Tardigrades")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Diversifier")
      }
    }
    kim.exMachina("$Decomposers")
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Diversifier")
    }
  }

  @Test
  internal fun `Tactician requires five cards with requirements`() {
    newTestGame("HellasMap")
    kim.exMachina("$ArtificialLake, $Birds, $Algae, $AsteroidMiningConsortium")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Tactician5")
      }
    }
    kim.exMachina("$BreathingFilters")
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Tactician5")
    }
  }

  @Test
  internal fun `Polar Explorer requires three owned tiles in the bottom two rows`() {
    newTestGame("HellasMap")
    kim.exMachina("GreeneryTile<Hellas_7_4>, GreeneryTile<Hellas_8_4>, GreeneryTile<Hellas_8_5>")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "PolarExplorer")
      }
    }
    kim.exMachina("GreeneryTile<Hellas_9_5>")
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "PolarExplorer")
    }
  }
}
