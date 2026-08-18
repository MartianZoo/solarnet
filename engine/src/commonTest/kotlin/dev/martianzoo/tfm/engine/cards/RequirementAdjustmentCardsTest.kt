package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class RequirementAdjustmentCardsTest : CardTest() {
  @Test
  fun `a satisfied printed requirement bypasses adjustment debt`() {
    newGame()
    p1.playCorp(Inventrix, 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates")
    p1.manual(
        "10 Megacredit, ProjectCard, OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, " +
            "OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    p1.playProject(Algae, 10)

    p1.assertCounts(1 to "$Algae")
  }

  @Test
  fun `Inventrix adjusts minimum and maximum global requirements by two`() {
    newGame()
    p1.playCorp(Inventrix, 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates")
    p1.manual(
        "30 Megacredit, 4 ProjectCard, OceanTile<Tharsis_1_2>, " +
            "OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>"
    )

    p1.playProject(Algae, 10)
    p1.manual("OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>")
    p1.playProject(DustSeals, 2)

    p1.assertCounts(1 to "$Algae", 1 to "$DustSeals")
  }

  @Test
  fun `requirement adjustments stack and Special Design expires on the next card`() {
    newGame()
    p1.playCorp(Inventrix, 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates")
    p1.manual("50 Megacredit, 5 ProjectCard, 11 TemperatureStep, $AdaptationTechnology")

    p1.playProject(SpecialDesign, 4)
    p1.playProject(Farming, 16)

    p1.assertCounts(1 to "$Farming")
    shouldThrow<RequirementException> { p1.playProject(Birds, 10) }
  }

  @Test
  fun `Morning Star adjusts Venus requirements regardless of the card's tags`() {
    newGame(VenusNextExpansion)
    p1.playCorp(MorningStarInc, 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates")
    p1.manual("30 Megacredit, 3 ProjectCard, 9 VenusStep")

    p1.playProject(RotatorImpacts, 6)
    shouldThrow<RequirementException> { p1.playProject(Algae, 10) }
  }
}
