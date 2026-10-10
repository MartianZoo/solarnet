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
    kim.stdAction("RequiredActionsSignal")
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    kim.playProject(Algae, 10).expect("$Algae")
  }

  @Test
  internal fun `Inventrix relaxes minimum global requirements by two`() {
    newTestGame(kimCorporation = Inventrix)
    kim.stdAction("RequiredActionsSignal")
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")

    kim.playProject(Algae, 10).expect("$Algae")
  }

  @Test
  internal fun `Inventrix relaxes maximum global requirements by two`() {
    newTestGame(kimCorporation = Inventrix)
    kim.stdAction("RequiredActionsSignal")
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    kim.playProject(DustSeals, 2).expect("$DustSeals")
  }

  @Test
  internal fun `Stacks Inventrix Adaptation Technology and Special Design`() {
    newTestGame(kimCorporation = Inventrix)
    kim.stdAction("RequiredActionsSignal")
    kim.exMachina("$AdaptationTechnology")
    kim.setToExMachina(11, "TemperatureStep")
    kim.setToExMachina(30, "MC")
    kim.playProject(SpecialDesign, 4)

    kim.playProject(Farming, 16).expect("$Farming")
  }

  @Test
  internal fun `Consumes Special Design when stacked adjustments are used`() {
    newTestGame(kimCorporation = Inventrix)
    kim.stdAction("RequiredActionsSignal")
    kim.exMachina("$AdaptationTechnology")
    kim.setToExMachina(11, "TemperatureStep")
    kim.setToExMachina(30, "MC")
    kim.playProject(SpecialDesign, 4)
    kim.playProject(Farming, 16)

    shouldThrow<RequirementException> { kim.playProject(Birds, 10) }
  }

  @Test
  internal fun `Morning Star adjusts Venus requirements on cards without Venus tags`() {
    newTestGame(kimCorporation = MorningStarInc)
    kim.stdAction("RequiredActionsSignal")
    kim.setToExMachina(9, "VenusStep")

    kim.playProject(RotatorImpacts, 6).expect("$RotatorImpacts")
  }

  @Test
  internal fun `Morning Star does not adjust ocean requirements`() {
    newTestGame(kimCorporation = MorningStarInc)
    kim.stdAction("RequiredActionsSignal")
    kim.setToExMachina(9, "VenusStep")

    shouldThrow<RequirementException> { kim.playProject(Algae, 10) }
  }

  @Test
  internal fun `Consumes Special Design when the next project has no requirement`() {
    newTestGame()
    kim.setToExMachina(11, "TemperatureStep")
    kim.playProject(SpecialDesign, 4)
    kim.playProject(Mine, 4)

    shouldThrow<RequirementException> { kim.playProject(ArcticAlgae, 12) }
  }

  @Test
  internal fun `Consumes Special Design when the next requirement is already satisfied`() {
    newTestGame()
    kim.setToExMachina(11, "TemperatureStep")
    kim.playProject(SpecialDesign, 4)
    kim.playProject(DustSeals, 2)

    shouldThrow<RequirementException> { kim.playProject(ArcticAlgae, 12) }
  }

  @Test
  internal fun `Keeps Special Design when a Prelude intervenes before the next project`() {
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack")
    startActionPhase()
    kim.exMachina("$BoardOfDirectors, Director<$BoardOfDirectors>, $AdaptationTechnology")
    kim.setToExMachina(13, "TemperatureStep")
    kim.playProject(SpecialDesign, 4)
    kim.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      kim.playPrelude(Donation)
    }

    kim.playProject(Farming, 16).expect("2 Plant, PROD[2 Plant]")
  }
}
