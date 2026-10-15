package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class RequirementAdjustmentCardsTest : TfmSandboxTest() {
  @Test
  internal fun `Uses the printed requirement when no adjustment is needed`() {
    newTestGame(kimCorporation = Inventrix)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("3 ProjectCard")
    }
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    kim.inTurn {
      doTasks(
          "PlayProject<Class<Algae>>",
          "-10 MC",
          "Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
      )
    }
  }

  @Test
  internal fun `Inventrix relaxes minimum global requirements by two`() {
    newTestGame(kimCorporation = Inventrix)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("3 ProjectCard")
    }
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<Algae>>",
          "-10 MC",
          "Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
      )
    }
  }

  @Test
  internal fun `Inventrix relaxes maximum global requirements by two`() {
    newTestGame(kimCorporation = Inventrix)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("3 ProjectCard")
    }
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    kim.inTurn {
      doTasks("PlayProject<Class<DustSeals>>", "-2 MC", "DustSeals FROM ProjectCard")
    }
  }

  @Test
  internal fun `Stacks Inventrix Adaptation Technology and Special Design`() {
    newTestGame(kimCorporation = Inventrix)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("3 ProjectCard")
    }
    kim.exMachina("$AdaptationTechnology")
    kim.setToExMachina(11, "TemperatureStep")
    kim.setToExMachina(30, "MC")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpecialDesign>>",
          "-4 MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
      doTask("PlayedEvent<Class<SpecialDesign>> FROM SpecialDesign")
    }

    kim.inTurn {
      doTasks(
          "PlayProject<Class<Farming>>",
          "-16 MC",
          "Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
      )
    }
  }

  @Test
  internal fun `Consumes Special Design when stacked adjustments are used`() {
    newTestGame(kimCorporation = Inventrix)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("3 ProjectCard")
    }
    kim.exMachina("$AdaptationTechnology")
    kim.setToExMachina(11, "TemperatureStep")
    kim.setToExMachina(30, "MC")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpecialDesign>>",
          "-4 MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
      doTask("PlayedEvent<Class<SpecialDesign>> FROM SpecialDesign")
    }
    kim.inTurn {
      doTasks(
          "PlayProject<Class<Farming>>",
          "-16 MC",
          "Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
      )
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<Birds>>")
        doTask("-10 MC")
        // Resolve the pending play to evaluate the card requirement before transferring it.
        selectTask("Birds FROM ProjectCard")
      }
    }
  }

  @Test
  internal fun `Morning Star adjusts Venus requirements on cards without Venus tags`() {
    newTestGame(kimCorporation = MorningStarInc)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("3 SearchForCard<TagFilter<Class<VenusTag>>>")
    }
    kim.setToExMachina(9, "VenusStep")

    kim.inTurn {
      doTasks("PlayProject<Class<RotatorImpacts>>", "-6 MC")
      doTasks("RotatorImpacts FROM ProjectCard")
    }
  }

  @Test
  internal fun `Morning Star does not adjust ocean requirements`() {
    newTestGame(kimCorporation = MorningStarInc)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      doTask("3 SearchForCard<TagFilter<Class<VenusTag>>>")
    }
    kim.setToExMachina(9, "VenusStep")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<Algae>>")
        doTask("-10 MC")
        // Resolve the pending play to evaluate the card requirement before transferring it.
        selectTask("Algae FROM ProjectCard")
      }
    }
  }

  @Test
  internal fun `Consumes Special Design when the next project has no requirement`() {
    newTestGame()
    kim.setToExMachina(11, "TemperatureStep")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpecialDesign>>",
          "-4 MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
      doTask("PlayedEvent<Class<SpecialDesign>> FROM SpecialDesign")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<Mine>>", "-4 MC")
      doTasks("Mine FROM ProjectCard", "PROD[Steel]")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<ArcticAlgae>>")
        doTask("-12 MC")
        // Resolve the pending play to evaluate the card requirement before transferring it.
        selectTask("ArcticAlgae FROM ProjectCard")
      }
    }
  }

  @Test
  internal fun `Consumes Special Design when the next requirement is already satisfied`() {
    newTestGame()
    kim.setToExMachina(11, "TemperatureStep")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpecialDesign>>",
          "-4 MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
      doTask("PlayedEvent<Class<SpecialDesign>> FROM SpecialDesign")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<DustSeals>>", "-2 MC", "DustSeals FROM ProjectCard")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PlayProject<Class<ArcticAlgae>>")
        doTask("-12 MC")
        // Resolve the pending play to evaluate the card requirement before transferring it.
        selectTask("ArcticAlgae FROM ProjectCard")
      }
    }
  }

  @Test
  internal fun `Keeps Special Design when a Prelude intervenes before the next project`() {
    newTestGame(addOptions = "PreludeExpansion, prelude2:cards")
    advanceUntilActionPhase()
    kim.exMachina("$BoardOfDirectors, Director<$BoardOfDirectors>, $AdaptationTechnology")
    kim.setToExMachina(13, "TemperatureStep")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpecialDesign>>",
          "-4 MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
      doTask("PlayedEvent<Class<SpecialDesign>> FROM SpecialDesign")
    }
    kim.inTurn {
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      doTask("PreludeCard<Kim, Hand>")
      doTask("-12 MC")
      doTask("-Director<BoardOfDirectors>")
      doTasks("PlayCard<Class<PreludeCard>, Class<Donation>>", "Donation FROM PreludeCard", "21 MC")
    }

    kim.inTurn {
      doTasks(
          "PlayProject<Class<Farming>>",
          "-16 MC",
          "Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
      )
    }
  }
}
