package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon.Option.CorporateEraExpansion
import dev.martianzoo.tfm.canon.Canon.Option.PreludeExpansion
import dev.martianzoo.tfm.canon.Canon.Option.VenusNextExpansion
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class RequirementAdjustmentCardsTest : CardTest() {
  @Test
  fun `a satisfied printed requirement bypasses adjustment debt`() {
    newGame()
    p1.playCorp("Inventrix", 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates")
    p1.manual(
        "10 Megacredit, ProjectCard, OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, " +
            "OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>"
    )

    p1.playProject("Algae", 10)

    p1.assertCounts(1 to "Algae", 0 to "Required")
  }

  @Test
  fun `Inventrix adjusts minimum and maximum global requirements by two`() {
    newGame()
    p1.playCorp("Inventrix", 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates")
    p1.manual(
        "30 Megacredit, 4 ProjectCard, OceanTile<Tharsis_1_2>, " +
            "OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>"
    )

    p1.playProject("Algae", 10)
    p1.manual("OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>")
    p1.playProject("DustSeals", 2)

    p1.assertCounts(1 to "Algae", 1 to "DustSeals", 0 to "Required")
  }

  @Test
  fun `requirement adjustments stack and Special Design expires on the next card`() {
    newGame()
    p1.playCorp("Inventrix", 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates")
    p1.manual("50 Megacredit, 5 ProjectCard, 11 TemperatureStep, AdaptationTechnology")

    p1.playProject("SpecialDesign", 4)
    p1.playProject("Farming", 16)

    p1.assertCounts(1 to "Farming", 0 to "SpecialDesignEffect", 0 to "Required")
    shouldThrow<RequirementException> { p1.playProject("Birds", 10) }
  }

  @Test
  fun `Morning Star adjusts Venus requirements regardless of the card's tags`() {
    newGame(VenusNextExpansion)
    p1.playCorp("MorningStarInc", 0)
    engine.phase("Action")
    p1.stdAction("HandleMandates")
    p1.manual("30 Megacredit, 3 ProjectCard, 9 VenusStep")

    p1.playProject("RotatorImpacts", 6)
    shouldThrow<RequirementException> { p1.playProject("Algae", 10) }
  }

  @Test
  fun `Ecology Experts plays Decomposers while ignoring its global requirement`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("10 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude("EcologyExperts") {
      doTask("PlayCard<Class<ProjectCard>, Class<Decomposers>>")
      p1.pay(megacredits = 5)
    }

    p1.assertCounts(
        1 to "EcologyExperts",
        1 to "PlantTag<EcologyExperts>",
        1 to "MicrobeTag<EcologyExperts>",
        1 to "Decomposers",
        3 to "Microbe<Decomposers>",
        0 to "CardP10Effect",
        0 to "Required",
    )
  }

  @Test
  fun `Ecology Experts can play a card without a bio tag`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("2 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude("EcologyExperts") { p1.playProject("DustSeals", 2) }

    p1.assertCounts(1 to "EcologyExperts", 1 to "DustSeals", 0 to "Required")
  }

  @Test
  fun `Viral Enhancers played by Ecology Experts sees all three relevant microbe and plant tags`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    engine.phase("Prelude")
    p1.manual("9 Megacredit, ProjectCard, PreludeCard")

    p1.playPrelude("EcologyExperts") {
      p1.playProject("ViralEnhancers", 9)
    }

    p1.assertCounts(1 to "ViralEnhancers", 3 to "Plant")
  }

  @Test
  fun `Ecological Zone played by Ecology Experts sees its plant tag`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("12 Megacredit, ProjectCard, PreludeCard, GreeneryTile<Tharsis_4_4>")

    p1.playPrelude("EcologyExperts") {
      p1.playProject("EcologicalZone", 12) { doTask("EzTile<Tharsis_4_5>") }
    }

    p1.assertCounts(1 to "EcologicalZone", 3 to "Animal<EcologicalZone>")
  }
}
