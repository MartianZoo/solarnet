package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RequirementAdjustmentCardsTest : CardTest() {
  @Test
  internal fun `A satisfied printed requirement bypasses adjustment debt`() {
    newGame()
    playCorporationWithoutStartingProjects(p1, Inventrix)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction")
    p1.runOperation(
        "10 MC, ProjectCard, OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, " +
            "OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    p1.playProject(Algae, 10)

    p1.assertCounts(1 to "$Algae")
  }

  @Test
  internal fun `Inventrix adjusts minimum and maximum global requirements by two`() {
    newGame()
    playCorporationWithoutStartingProjects(p1, Inventrix)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction")
    p1.runOperation(
        "30 MC, 4 ProjectCard, OceanTile<Tharsis_1_2>, " +
            "OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>"
    )

    p1.playProject(Algae, 10)
    p1.runOperation("OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>")
    p1.playProject(DustSeals, 2)

    p1.assertCounts(1 to "$Algae", 1 to "$DustSeals")
  }

  @Test
  internal fun `Requirement adjustments stack and Special Design expires on the next project card`() {
    newGame()
    playCorporationWithoutStartingProjects(p1, Inventrix)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction")
    p1.runOperation("50 MC, 5 ProjectCard, 11 TemperatureStep, $AdaptationTechnology")

    p1.playProject(SpecialDesign, 4)
    p1.playProject(Farming, 16)

    p1.assertCounts(1 to "$Farming")
    shouldThrow<RequirementException> { p1.playProject(Birds, 10) }
  }

  @Test
  internal fun `Morning Star adjusts Venus requirements regardless of the card's tags`() {
    newGame(VenusNextExpansion)
    playCorporationWithoutStartingProjects(p1, MorningStarInc)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction")
    p1.runOperation("30 MC, 3 ProjectCard, 9 VenusStep")

    p1.playProject(RotatorImpacts, 6)
    shouldThrow<RequirementException> { p1.playProject(Algae, 10) }
  }

  @Test
  internal fun `Special Design expires when the next project card has no requirement`() {
    expireSpecialDesignWith(Mine, 4)
  }

  @Test
  internal fun `Special Design expires when the next requirement is already satisfied`() {
    expireSpecialDesignWith(DustSeals, 2)
  }

  @Test
  internal fun `Intervening Prelude leaves Special Design for the next project card`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)
    admin.phase("Action")
    p1.runOperation("50 MC, 2 ProjectCard, 13 TemperatureStep, $AdaptationTechnology")

    p1.playProject(SpecialDesign, 4)
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(Donation)
    }
    p1.playProject(Farming, 16).expect("2 Plant, PROD[2 Plant]")
  }

  private fun expireSpecialDesignWith(card: ClassName, cost: Int) {
    newGame(startingProjects = listOf(5))
    p1.playCorp(CrediCor)
    admin.phase("Action")
    p1.runOperation("11 TemperatureStep")
    p1.playProject(SpecialDesign, 4)
    p1.playProject(card, cost)

    shouldThrow<RequirementException> { p1.playProject(ArcticAlgae, 12) }
    p1.count("$ArcticAlgae") shouldBe 0
    p1.count("ProjectCard") shouldBe 3
  }
}
