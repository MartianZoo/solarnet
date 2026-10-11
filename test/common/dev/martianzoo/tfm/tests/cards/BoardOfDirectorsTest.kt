package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
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
      selectOnlyTask()
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("UseCardAction<BoardOfDirectors, Action1>")
          selectTask("PreludeCard<Kim, Hand>!")
          selectOnlyTask()
          doTask("-12 MC")
          selectTask("-Director<BoardOfDirectors>!")
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
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.setToExMachina(24, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<BoardOfDirectors, Action1>")
        selectTask("PreludeCard<Kim, Hand>!")
        selectOnlyTask()
        doTask("-12 MC")
        selectTask("-Director<BoardOfDirectors>!")
        selectOnlyTask()
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
            "IndustrialComplex FROM PreludeCard",
        )
        selectTask("-18 MC!")
      }
    }
    kim.inTurn {
          doTask("UseCardAction<BoardOfDirectors, Action1>")
          selectTask("PreludeCard<Kim, Hand>!")
          selectOnlyTask()
          doTask("-12 MC")
          selectTask("-Director<BoardOfDirectors>!")
          doTask("-PreludeCard")
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
      selectOnlyTask()
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("UseCardAction<BoardOfDirectors, Action1>")
          selectTask("PreludeCard<Kim, Hand>!")
          selectOnlyTask()
          doTask("-12 MC")
          selectTask("-Director<BoardOfDirectors>!")
          doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
          selectTask("4 CorporationCard<Kim, Selecting>!")
          selectTask("-3 CorporationCard<Kim, Selecting>!")
          doTask("PlayCard<Class<CorporationCard>, Class<Inventrix>>")
          selectTask("MAX 0 Barrier: Inventrix FROM CorporationCard<Kim, Selecting>!")
          doTasks("45 MC", "-42 MC")
          selectOnlyTask()
        }
        .expect("$Inventrix")
    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<PowerPlantProject>")
      }
    }
    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectOnlyTask()
        }
        .expect("3 ProjectCard")
    kim.inTurn {
      doTasks("UseStandardProject<PowerPlantProject>", "-11 MC", "PROD[Energy]")
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
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.exMachina("$SkyDocks")
    kim.setToExMachina(13, "MC")

    kim.inTurn {
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      selectTask("PreludeCard<Kim, Hand>!")
      selectOnlyTask()
      doTask("-12 MC")
      selectTask("-Director<BoardOfDirectors>!")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<EcologyExperts>>",
          "EcologyExperts FROM PreludeCard",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<DustSeals>>",
      )
      selectTask("-Required. / Required")
      doTasks("-1 MC", "DustSeals FROM ProjectCard")
    }
  }
}
