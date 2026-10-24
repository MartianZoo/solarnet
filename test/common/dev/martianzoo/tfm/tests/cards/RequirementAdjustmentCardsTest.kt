package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class RequirementAdjustmentCardsTest : TfmSandboxTest() {
  @Test
  internal fun `Uses the printed requirement when no adjustment is needed`() {
    newTestGame(kimCorporation = Inventrix)
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("3 ProjectCard")
    }
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    kim.inTurn {
      doTasks(
          *playCardTasks("Algae"),
          "10 Pay<> FROM MC",
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
      doTask("DoRequiredActions")
      doTask("3 ProjectCard")
    }
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")

    kim.inTurn {
      doTasks(
          *playCardTasks("Algae"),
          "10 Pay<> FROM MC",
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
      doTask("DoRequiredActions")
      doTask("3 ProjectCard")
    }
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    kim.inTurn {
      doTasks(
          *playCardTasks("DustSeals"),
          "2 Pay<> FROM MC",
          "DustSeals FROM ProjectCard",
      )
    }
  }

  @Test
  internal fun `Stacks Inventrix Adaptation Technology and Special Design`() {
    newTestGame(kimCorporation = Inventrix)
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("3 ProjectCard")
    }
    kim.exMachina("$AdaptationTechnology")
    kim.setToExMachina(11, "TemperatureStep")
    kim.setToExMachina(30, "MC")
    kim.inTurn {
      doTasks(
          *playCardTasks("SpecialDesign"),
          "4 Pay<> FROM MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
    }

    kim.inTurn {
      doTasks(
          *playCardTasks("Farming"),
          "16 Pay<> FROM MC",
          "Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
      )
    }
  }

  @Test
  internal fun `Special Design relaxes only the next project's requirement`() {
    newTestGame()
    kim.setToExMachina(15, "TemperatureStep")
    kim.inTurn {
      doTasks(
          *playCardTasks("SpecialDesign"),
          "4 Pay<> FROM MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
    }

    // Farming needs 17 temperature steps, so Special Design makes it playable at 15.
    kim.inTurn {
      doTasks(
          *playCardTasks("Farming"),
          "16 Pay<> FROM MC",
          "Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
      )
    }

    // Fish needs 16 steps: it would also be playable if Special Design had survived.
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(*playCardTasks("Fish"))
        doTask("9 Pay<> FROM MC")
        selectTask("MAX 0 Barrier: Fish FROM ProjectCard!")
        doTask("PROD[-Plant]")
      }
    }
  }

  @Test
  internal fun `Morning Star adjusts Venus requirements on cards without Venus tags`() {
    newTestGame(kimCorporation = MorningStarInc)
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("3 SearchForCard<TagFilter<Class<VenusTag>>>")
    }
    kim.setToExMachina(9, "VenusStep")

    kim.inTurn {
      doTasks(*playCardTasks("RotatorImpacts"), "6 Pay<> FROM MC")
      doTasks("RotatorImpacts FROM ProjectCard")
    }
  }

  @Test
  internal fun `Morning Star does not adjust ocean requirements`() {
    newTestGame(kimCorporation = MorningStarInc)
    kim.inTurn {
      doTask("DoRequiredActions")
      doTask("3 SearchForCard<TagFilter<Class<VenusTag>>>")
    }
    kim.setToExMachina(9, "VenusStep")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(*playCardTasks("Algae"))
        doTask("10 Pay<> FROM MC")
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
          *playCardTasks("SpecialDesign"),
          "4 Pay<> FROM MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
    }
    kim.inTurn {
      doTasks(*playCardTasks("Mine"), "4 Pay<> FROM MC")
      doTasks("Mine FROM ProjectCard", "PROD[Steel]")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(*playCardTasks("ArcticAlgae"))
        doTask("12 Pay<> FROM MC")
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
          *playCardTasks("SpecialDesign"),
          "4 Pay<> FROM MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("DustSeals"),
          "2 Pay<> FROM MC",
          "DustSeals FROM ProjectCard",
      )
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(*playCardTasks("ArcticAlgae"))
        doTask("12 Pay<> FROM MC")
        // Resolve the pending play to evaluate the card requirement before transferring it.
        selectTask("ArcticAlgae FROM ProjectCard")
      }
    }
  }

  // Unresolved adjudication (question 11, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/2143393
  @Test
  internal fun `Keeps Special Design when a Prelude intervenes before the next project`() {
    newTestGame(addOptions = "PreludeExpansion, prelude2:cards")
    advanceUntilActionPhase()
    kim.exMachina("$BoardOfDirectors, Director<$BoardOfDirectors>, $AdaptationTechnology")
    kim.setToExMachina(13, "TemperatureStep")
    kim.inTurn {
      doTasks(
          *playCardTasks("SpecialDesign"),
          "4 Pay<> FROM MC",
          "SpecialDesign FROM ProjectCard",
      )
      doTask("SpecialDesign_NextCardEffect")
    }
    kim.inTurn {
      doTasks(*cardActionTasks("BoardOfDirectors"))
      doTask("PreludeCard<Kim, Hand>")
      doTask("-12 MC")
      doTask("-Director<BoardOfDirectors>")
      doTasks("PlayCard<Class<PreludeCard>, Class<Donation>>", "Donation FROM PreludeCard", "21 MC")
    }

    kim.inTurn {
      doTasks(
          *playCardTasks("Farming"),
          "16 Pay<> FROM MC",
          "Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
      )
    }
  }
}
