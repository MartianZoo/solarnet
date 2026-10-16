package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class BoardOfDirectorsTest : TfmSandboxTest() {
  @Test
  internal fun `Remains available after Prelude phase and spends a director to play another Prelude`() {
    newTestGame("PreludeExpansion, BoardOfDirectors")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      doTask("4 Director<BoardOfDirectors>")
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("UseCardAction<BoardOfDirectors, Action1>")
          doTask("PreludeCard<Kim, Hand>")
          doTask("-12 MC")
          doTask("-Director<BoardOfDirectors>")
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<Donation>>",
              "Donation FROM PreludeCard",
              "21 MC",
          )
        }
        .expect("0 $BoardOfDirectors")
  }

  @Test
  internal fun `Still spends its money and director when the selected Prelude fizzles`() {
    newTestGame("PreludeExpansion, BoardOfDirectors, IndustrialComplex")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      doTask("4 Director<BoardOfDirectors>")
    }
    advanceUntilActionPhase()
    kim.setToExMachina(24, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("UseCardAction<BoardOfDirectors, Action1>")
        doTask("PreludeCard<Kim, Hand>")
        doTask("-12 MC")
        doTask("-Director<BoardOfDirectors>")
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
            "IndustrialComplex FROM PreludeCard",
        )
        doTask("-18 MC")
      }
    }
    kim.inTurn {
          doTask("UseCardAction<BoardOfDirectors, Action1>")
          doTask("PreludeCard<Kim, Hand>")
          doTask("-12 MC")
          doTask("-Director<BoardOfDirectors>")
          doTask("-PreludeCard")
          // Hand the fizzle audit to Admin before receiving compensation.
          selectTask("Audit!")
          doTask("15 MC")
        }
        .expect("0 $IndustrialComplex")
  }

  @Test
  internal fun `A corporation acquired through Merger requires its first action`() {
    newTestGame("PreludeExpansion, BoardOfDirectors")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      doTask("4 Director<BoardOfDirectors>")
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("UseCardAction<BoardOfDirectors, Action1>")
          doTask("PreludeCard<Kim, Hand>")
          doTask("-12 MC")
          doTask("-Director<BoardOfDirectors>")
          doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
          doTask("4 CorporationCard<Kim, Selecting>")
          doTask("PlayCard<Class<CorporationCard>, Class<Inventrix>>")
          doTask("Inventrix<Kim> FROM CorporationCard<Kim, Selecting>!")
          doTasks("45 MC", "-42 MC")
        }
        .expect("$Inventrix")
    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("UseStandardProject<PowerPlantProject>")
      }
    }
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("3 ProjectCard")
    }
    kim.inTurn {
      doTasks("UseStandardProject<PowerPlantProject>", "11 Pay<> FROM MC", "PROD[Energy]")
    }
  }

  @Test
  internal fun `Sky Docks discounts the project played through Ecology Experts`() {
    newTestGame("PreludeExpansion, BoardOfDirectors, EcologyExperts, Unsafe")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      doTask("4 Director<BoardOfDirectors>")
    }
    advanceUntilActionPhase()
    kim.exMachina("$SkyDocks")
    kim.setToExMachina(13, "MC")

    kim.inTurn {
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      doTask("PreludeCard<Kim, Hand>")
      doTask("-12 MC")
      doTask("-Director<BoardOfDirectors>")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
          "EcologyExperts FROM PreludeCard",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<DustSeals>>",
      )
      // Hand the requirement waiver to Admin before playing the project.
      selectTask("-Required* / Required")
      doTasks("1 Pay<> FROM MC", "DustSeals FROM ProjectCard")
    }
  }
}
