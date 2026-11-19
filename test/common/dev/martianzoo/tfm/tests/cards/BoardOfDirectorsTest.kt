package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class BoardOfDirectorsTest : TfmSandboxTest() {
  @Test
  internal fun `Remains available after Prelude phase and spends a director to play another Prelude`() {
    newTestGame("PreludeExpansion, $BoardOfDirectors")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "$BoardOfDirectors FROM PreludeCard",
          "4 Director<$BoardOfDirectors>",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTasks(
              *cardActionTasks("$BoardOfDirectors"),
              "PreludeCard<Hand>",
              "-12 MC",
              "-Director<$BoardOfDirectors>",
          )
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$Donation>>",
              "$Donation FROM PreludeCard",
              "21 MC",
          )
        }
        .expect("0 $BoardOfDirectors")
  }

  @Test
  internal fun `Still spends its money and director when the selected Prelude fizzles`() {
    newTestGame("PreludeExpansion, $BoardOfDirectors, $IndustrialComplex")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "$BoardOfDirectors FROM PreludeCard",
          "4 Director<$BoardOfDirectors>",
      )
    }
    advanceUntilActionPhase()
    kim.setToExMachina(24, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard<Hand>",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
        )
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$IndustrialComplex>>",
            "$IndustrialComplex FROM PreludeCard",
            "-18 MC",
        )
      }
    }
    kim.inTurn {
          doTasks(
              *cardActionTasks("$BoardOfDirectors"),
              "PreludeCard<Hand>",
              "-12 MC",
              "-Director<$BoardOfDirectors>",
              "-PreludeCard",
          )
          // Execute the fizzle audit before receiving compensation.
          doTasks(
              "Audit!",
              "15 MC",
          )
        }
        .expect("0 $IndustrialComplex")
  }

  @Test
  internal fun `A corporation acquired through Merger requires its first action`() {
    newTestGame("PreludeExpansion, $BoardOfDirectors")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "$BoardOfDirectors FROM PreludeCard",
          "4 Director<$BoardOfDirectors>",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTasks(
              *cardActionTasks("$BoardOfDirectors"),
              "PreludeCard<Hand>",
              "-12 MC",
              "-Director<$BoardOfDirectors>",
          )
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$Merger>>",
              "$Merger FROM PreludeCard",
              "4 CorporationCard<Selecting>",
          )
          doTasks(
              "PlayCard<Class<CorporationCard>, Class<$Inventrix>>",
              "$Inventrix FROM CorporationCard<Selecting>!",
              "45 MC",
              "-42 MC",
          )
        }
        .expect("$Inventrix")
    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*standardProjectTasks("PowerPlantProject"))
      }
    }
    kim.inTurn {
      doTasks(
          "DoRequiredActions",
          "3 ProjectCard",
      )
    }
    kim.inTurn {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }
  }

  @Test
  internal fun `Sky Docks discounts the project played through Ecology Experts`() {
    newTestGame("PreludeExpansion, $BoardOfDirectors, $EcologyExperts, Unsafe")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "$BoardOfDirectors FROM PreludeCard",
          "4 Director<$BoardOfDirectors>",
      )
    }
    advanceUntilActionPhase()
    kim.exMachina("$SkyDocks")
    kim.setToExMachina(13, "MC")

    kim.inTurn {
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard<Hand>",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "$EcologyExperts FROM PreludeCard",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$DustSeals>>",
      )
      // Execute the requirement waiver before playing the project.
      doTasks(
          "-Required* / Required",
          "1 Pay<> FROM MC",
          "$DustSeals FROM ProjectCard",
      )
    }
  }
}
