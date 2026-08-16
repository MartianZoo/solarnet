package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CustomMetricCardsTest : CardTest() {
  @Test
  fun `with Credicor, buys an expensive card and standard project`() {
    newGame()
    engine.phase("Action")
    p1.manual("40, 2 ProjectCard, CrediCor")
    p1.playProject("EarthCatapult", 23).expect("-19")
    p1.stdProject("CitySP") { doTask("CityTile<Tharsis_2_1>") }.expect("-21")
  }

  @Test
  fun `with Advertising, adds cards costing twenty and nineteen`() {
    newGame(
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("Advertising")
    p1.manual("LunarExports") { doTask("PROD[5]") }.expect("PROD[5]")
    p1.manual("GanymedeColony").expect("PROD[1]")
  }

  @Test
  fun `with Spin-Off Department, adds cards costing twenty and less`() {
    newGame(
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("SpinOffDepartment")
    p1.manual("Mine")
    p1.count("ProjectCard") shouldBe 0
    p1.manual("EarthCatapult").expect("ProjectCard")
  }

  @Test
  fun `with Cutting Edge Technology, plays cards with and without requirements`() {
    newGame(VenusNextExpansion, PromoCardPack)
    engine.phase("Action")
    p1.manual(
        "4, 2 ProjectCard, CuttingEdgeTechnology, Steel, Titanium, Plant, Energy, Heat, " +
            "Pets, Decomposers, ForcedPrecipitation, Animal<Pets>, Microbe<Decomposers>, " +
            "Floater<ForcedPrecipitation>"
    )

    p1.playProject("DiversitySupport", 0).expect("TerraformRating")
    p1.playProject("Mine", 4)
  }

  @Test
  fun `with Mining Guild, places tiles on steel and card bonuses`() {
    newGame()
    p1.manual("MiningGuild")
    p1.count("PROD[Steel]") shouldBe 1

    p1.manual("CityTile<Tharsis_1_1>").expect("PROD[Steel]") // LSS
    p1.count("PROD[Steel]") shouldBe 2

    p1.manual("CityTile<Tharsis_8_9>").expect("PROD[Steel]") // Titanium
    p1.count("PROD[Steel]") shouldBe 3

    p1.manual("CityTile<Tharsis_2_1>") // L
    p1.count("PROD[Steel]") shouldBe 3
  }

  @Test
  fun `with a steel area adjacent, plays Mining Area`() {
    newGame()
    p1.manual("CityTile<Tharsis_2_1>")
    p1.manual("MiningArea") {
          doTask("Tile064<Tharsis_1_1>")
        }
        .expect("2 Steel, PROD[Steel]")
  }

  @Test
  fun `with a titanium area adjacent, plays Mining Area`() {
    newGame()
    p1.manual("CityTile<Tharsis_7_9>")
    p1.manual("MiningArea") {
          doTask("Tile064<Tharsis_8_9>")
        }
        .expect("Titanium, PROD[Titanium]")
  }

  @Test
  fun `with a steel area selected, plays Mining Rights`() {
    newGame()
    p1.manual("MiningRights") {
          doTask("Tile067<Tharsis_1_1>")
        }
        .expect("2 Steel, PROD[Steel]")
    p1.count("PROD[Titanium]") shouldBe 0
  }

  @Test
  fun `after Mining Rights selects steel, copies its production box`() {
    // https://boardgamegeek.com/thread/2663453/rule-opinions-mining-rights-robotic-workforce
    newGame(TerraCimmeriaMapOption)

    p1.manual("MiningRights") {
          doTask("Tile067<TerraCimmeria_6_4>")
          doTask("PROD[Steel]")
        }
        .expect("Titanium, 2 Steel, PROD[Steel]")

    val manual = p1.godMode().also { it.autoExecMode = NONE }
    manual.beginManual("RoboticWorkforce")
    manual.reviseTask(
        "CopyProductionBox<CardFront(HAS BuildingTag)>",
        "CopyProductionBox<MiningRights>",
    )
    manual.finish { doTask("PROD[Titanium]") }.expect("PROD[Titanium]")
  }

  @Test
  fun `with three existing tag types, Interplanetary Trade adds four production`() {
    newGame(PromoCardPack)
    // These have to be played: tags depend on their cards.
    p1.manual("Ecoline, Mine, SearchForLife, 8 Plant, 6 Steel, 4 Heat, 3 ProjectCard")
    p1.manual("InterplanetaryTrade").expect("PROD[4]")
  }

  @Test
  fun `Interplanetary Trade does not count a tag from a played event`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("100, 2 ProjectCard, Ecoline, Mine, SearchForLife")
    p1.playProject("ImportedHydrogen", 16) {
      doTask("3 Plant")
      doTask("OceanTile<Tharsis_1_2>")
    }

    p1.playProject("InterplanetaryTrade", 27).expect("PROD[4]")
  }

  @Test
  fun `with nine resource types, plays Diversity Support`() {
    seedDiversitySupportResources()
    p1.manual("ForcedPrecipitation, Floater<ForcedPrecipitation>")
    p1.playProject("DiversitySupport", 1).expect("TerraformRating")
  }

  @Test
  fun `without an adjacent tile, tries to play Mining Area`() {
    newGame()
    shouldThrow<DependencyException> {
      p1.manual("MiningArea") { doTask("Tile064<Tharsis_1_1>") }
    }
  }

  @Test
  fun `with a card-bonus area selected, tries to play Mining Area`() {
    newGame()
    p1.manual("CityTile<Tharsis_2_1>")
    shouldThrow<NotNowException> {
      p1.manual("MiningArea") { doTask("Tile064<Tharsis_3_2>") }
    }
  }

  @Test
  fun `with a card-bonus area selected, tries to play Mining Rights`() {
    newGame()
    shouldThrow<NotNowException> {
      p1.manual("MiningRights") { doTask("Tile067<Tharsis_2_1>") }
    }
  }

  @Test
  fun `with eight resource types, tries to play Diversity Support`() {
    seedDiversitySupportResources()
    p1.count("TerraformRating") shouldBe 20
    shouldThrow<RequirementException> { p1.playProject("DiversitySupport", 1) }
    p1.count("TerraformRating") shouldBe 20
  }

  private fun seedDiversitySupportResources() {
    newGame(VenusNextExpansion, PromoCardPack)
    engine.phase("Action")
    requireP2()
        .manual(
            "10 Megacredit, 9 ProjectCard, 8 Steel, 7 Titanium, 6 Plant, 5 Energy, 4 Heat, " +
                "EarthCatapult, Mine, InventorsGuild"
        )
    p1.manual(
        "6 Megacredit, 5 ProjectCard, 4 Steel, 3 Titanium, 2 Plant, 2 Energy, 2 Heat, " +
            "Pets, Decomposers, Animal<Pets>, Microbe<Decomposers>"
    )
  }
}
