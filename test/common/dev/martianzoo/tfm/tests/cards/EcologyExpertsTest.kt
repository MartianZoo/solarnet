package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class EcologyExpertsTest : TfmSandboxTest() {
  // BGG Ecology Experts tag timing:
  // https://boardgamegeek.com/thread/2096075/article/30501757#30501757
  @Ignore // Known defect: the newly played listener misses Ecology Experts' plant tag.
  @Test
  internal fun `Its plant tag triggers the Ecological Zone it plays`() {
    playEcologicalZone().expect("3 Animal<$EcologicalZone>")
  }

  @Test
  internal fun `BUG - Its plant tag does not trigger the Ecological Zone it plays`() {
    playEcologicalZone().expect("2 Animal<$EcologicalZone>")
  }

  private fun playEcologicalZone(): TaskResult {
    newTestGame(addOptions = "PreludeExpansion, $EcologyExperts, Unsafe")
    kim.exMachina("GreeneryTile<Tharsis_4_4>")

    return kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$EcologicalZone>>",
      )
      // Execute the requirement waiver before playing the project.
      doTasks(
          "-Required* / Required",
          "12 Pay<> FROM MC",
          "Animal<$EcologicalZone>*",
          "Animal<$EcologicalZone>*",
          "EcologicalZone_SpecialTile<Tharsis_4_5>",
          "2 Plant",
      )
    }
  }

  @Test
  internal fun `Cannot choose another Prelude card`() {
    newTestGame(addOptions = "PreludeExpansion, $EcologyExperts, Unsafe")
    kim.setToExMachina(2, "PreludeCard")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
            "PROD[Plant]",
        )
        doTask("PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>")
      }
    }

    kim.assertCounts(0 to "$EcologyExperts", 0 to "$ExcentricSponsor")
    kim.count("PreludeCard") shouldBe 2
  }

  @Test
  internal fun `Plays Decomposers while ignoring its global requirement`() {
    newTestGame(addOptions = "PreludeExpansion, $EcologyExperts, Unsafe")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$Decomposers>>",
      )
      // Execute the requirement waiver before playing the project.
      doTasks(
          "-Required* / Required",
          "5 Pay<> FROM MC",
          "Microbe<$Decomposers>*",
      )
    }
  }

  @Test
  internal fun freeAlgaeWaitsForTheEcologyExpertsRequirementWaiver() {
    newTestGame(
        addOptions =
            "PreludeExpansion, $EcologyExperts, -$ViralEnhancers, -$Decomposers, -$EcologicalZone",
    )
    kim.exMachina("$EarthCatapult, IndenturedWorkers_NextCardEffect")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$Algae>>",
      )
      kim.count("$Algae") shouldBe 0
      kim.count("Required") shouldBe 5
      doTask("-Required* / Required")
      kim.count("$Algae") shouldBe 1
      doTasks("Plant", "PROD[2 Plant]")
    }

    kim.count("MC") shouldBe 0
    kim.count("Owed") shouldBe 0
  }

  @Test
  internal fun cardResourcesCanBePaidBeforeTheEcologyExpertsRequirementWaiver() {
    newTestGame(
        addOptions =
            "PreludeExpansion, $EcologyExperts, $Psychrophiles, -$ViralEnhancers, -$Decomposers, -$EcologicalZone",
    )
    kim.exMachina("$Psychrophiles, 5 Microbe<$Psychrophiles>")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$Algae>>",
          "5 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
      )
      kim.count("$Algae") shouldBe 0
      doTask("-Required* / Required")
      kim.count("$Algae") shouldBe 0
      doTask("Ok") // acknowledge the zero MC remainder
      kim.count("$Algae") shouldBe 1
      doTasks("Plant", "PROD[2 Plant]")
    }

    kim.count("Microbe<$Psychrophiles>") shouldBe 0
    kim.count("Owed") shouldBe 0
  }

  @Test
  internal fun discountedEcologyExpertsPlayStillWaivesTheRequirement() {
    newTestGame(addOptions = "PreludeExpansion, $EcologyExperts, Unsafe")
    kim.exMachina("$EarthCatapult, $AntiGravityTechnology, $ResearchOutpost")
    val cashBefore = kim.count("MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$Decomposers>>",
          "-Required* / Required",
          "Microbe<$Decomposers>*",
      )
    }

    kim.count("$Decomposers") shouldBe 1
    kim.count("Owed") shouldBe 0
    kim.count("MC") shouldBe cashBefore
  }

  @Test
  internal fun `Splice money from Ecology Experts tags can pay for Decomposers`() {
    newTestGame(addOptions = "PreludeExpansion, $EcologyExperts, Unsafe")
    stan.exMachina("$SpliceTacticalGenomics")
    kim.setToExMachina(4, "MC")
    stan.setToExMachina(0, "MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
      )
      // The active player chooses when to hand this task to its owner.
      giveTask(stan, "2 MC<Stan>")
      doTasks("2 MC", "PlayCard<Class<ProjectCard>, Class<$Decomposers>>")
      // Execute the requirement waiver before playing the project.
      doTasks(
          "-Required* / Required",
          "5 Pay<> FROM MC",
      )
      // Resolve Decomposers' mandatory resource separately from the optional tag bonus.
      selectTask("Microbe<$Decomposers>*")
      doTask("Microbe<$Decomposers>")
      // The active player chooses when to hand this task to its owner.
      giveTask(stan, "2 MC<Stan>")
      doTask("2 MC")
    }

    kim.assertCounts(3 to "MC")
    stan.count("MC") shouldBe 4
  }

  @Test
  internal fun `Can play a card without a bio tag`() {
    newTestGame(addOptions = "PreludeExpansion, $EcologyExperts, Unsafe")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$DustSeals>>",
      )
      // Execute the requirement waiver before playing the project.
      doTasks(
          "-Required* / Required",
          "2 Pay<> FROM MC",
      )
    }
  }

  @Test
  internal fun `Double Down copies Ecology Experts project play and requirement waiver`() {
    newTestGame(addOptions = "PreludeExpansion, $EcologyExperts, Unsafe")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$DustSeals>>",
      )
      // Execute the requirement waiver before playing the project.
      doTasks(
          "-Required* / Required",
          "2 Pay<> FROM MC",
      )
    }
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<$DoubleDown>>")
      doTasks(
          "CopyPrelude<$EcologyExperts>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$Decomposers>>",
      )
      // Execute the requirement waiver before playing the project.
      doTasks(
          "-Required* / Required",
          "5 Pay<> FROM MC",
          "Microbe<$Decomposers>*",
      )
    }
  }

  @Test
  internal fun `Ecology Experts does not waive a political requirement`() {
    newTestGame(addOptions = "PreludeExpansion, TurmoilExpansion, $EcologyExperts, Unsafe")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
            "PROD[Plant]",
        )
        doTask("PlayCard<Class<ProjectCard>, Class<$SupportedResearch>>")
      }
    }
    kim.assertCounts(0 to "$EcologyExperts", 0 to "$SupportedResearch", 10 to "ProjectCard")
  }

  // https://boardgamegeek.com/thread/2096075/article/30501757#30501757
  @Ignore // The newly played listener misses Ecology Experts bio tags.
  @Test
  internal fun `Its bio tags trigger the Viral Enhancers it plays`() {
    playViralEnhancers().expect("3 Plant")
  }

  @Test
  internal fun `BUG - Its bio tags do not trigger the Viral Enhancers it plays`() {
    playViralEnhancers().expect("Plant")
  }

  private fun playViralEnhancers(): TaskResult {
    newTestGame(addOptions = "PreludeExpansion, CorporateEraExpansion, $EcologyExperts, Unsafe")
    return kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$ViralEnhancers>>",
      )
      // Execute the requirement waiver before playing the project.
      doTasks(
          "-Required* / Required",
          "9 Pay<> FROM MC",
          "Plant",
      )
    }
  }
}
