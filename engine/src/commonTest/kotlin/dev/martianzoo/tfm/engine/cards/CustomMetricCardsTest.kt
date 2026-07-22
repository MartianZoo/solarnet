package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CustomMetricCardsTest : CardTest() {
  @Test
  fun credicorRefundsExpensiveCardsAndStandardProjects() {
    val p1 = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)
    p1.phase("Action")
    p1.sneak("100, 2 ProjectCard, Credicor")

    p1.playProject("EarthCatapult", 23).expect("-19")

    p1.stdProject("CitySP") { doTask("CityTile<Tharsis_2_1>") }.expect("-21")
  }

  @Test
  fun advertisingRaisesProductionForCostTwentyButNotNineteen() {
    val p1 = newGame(Canon.fromOptionCodes("BRCMX", 2, testColonyTiles(2))).tfm(PLAYER1)
    p1.count("CardCost<Advertising>") shouldBe 4
    p1.sneak("Advertising")

    p1.manual("LunarExports") { doTask("PROD[5]") }.expect("PROD[5]")
    p1.manual("GanymedeColony").expect("PROD[1]")
  }

  @Test
  fun spinOffDepartmentDrawsOnlyForExpensiveCards() {
    val p1 = newGame(Canon.fromOptionCodes("BRCM", 2, testColonyTiles(2))).tfm(PLAYER1)
    p1.sneak("SpinOffDepartment")

    p1.manual("Mine")
    p1.count("ProjectCard") shouldBe 0

    p1.manual("EarthCatapult").expect("ProjectCard")
  }

  @Test
  fun cuttingEdgeTechnologyDiscountsOnlyCardsWithRequirements() {
    val p1 = newGame(Canon.fromOptionCodes("BRMVX", 2)).tfm(PLAYER1)
    p1.phase("Action")
    p1.sneak(
        "100, 2 ProjectCard, CuttingEdgeTechnology, Steel, Titanium, Plant, Energy, Heat, " +
            "Pets, Decomposers, ForcedPrecipitation, Animal<Pets>, Microbe<Decomposers>, " +
            "Floater<ForcedPrecipitation>"
    )

    p1.playProject("DiversitySupport", 0).expect("TerraformRating")
    p1.playProject("Mine", 4)
  }

  @Test
  fun miningGuildRaisesSteelProductionOnlyForPrintedSteelOrTitaniumBonuses() {
    val p1 = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)
    p1.sneak("MiningGuild")
    val before = p1.count("PROD[Steel]")

    p1.manual("CityTile<Tharsis_1_1>") // LSS
    p1.count("PROD[Steel]") shouldBe before + 1

    p1.manual("CityTile<Tharsis_2_1>") // L
    p1.count("PROD[Steel]") shouldBe before + 1
  }

  @Test
  fun miningAreaProducesTheResourcePrintedOnItsChosenArea() {
    val steelPlayer = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)
    steelPlayer.manual("CityTile<Tharsis_2_1>")

    steelPlayer
        .manual("MiningArea") {
          doTask("MiningAreaTile<Tharsis_1_1>")
        }
        .expect("2 Steel, PROD[Steel]")

    val titaniumPlayer = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)
    titaniumPlayer.manual("CityTile<Tharsis_7_9>")
    titaniumPlayer
        .manual("MiningArea") {
          doTask("MiningAreaTile<Tharsis_8_9>")
        }
        .expect("Titanium, PROD[Titanium]")
  }

  @Test
  fun miningRightsProducesTheResourceOnItsChosenArea() {
    val p1 = newGame(Canon.fromOptionCodes("BM", 2)).tfm(PLAYER1)

    p1.manual("MiningRights") {
          doTask("MiningRightsTile<Tharsis_1_1>")
        }
        .expect("2 Steel, PROD[Steel]")

    p1.count("PROD[Titanium]") shouldBe 0
  }

  @Test
  fun miningAreaRequiresAnAdjacentOwnedTile() {
    val p1 = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)

    shouldThrow<DependencyException> {
      p1.manual("MiningArea") { doTask("MiningAreaTile<Tharsis_1_1>") }
    }
  }

  @Test
  fun miningCardsRejectAreasWithoutMetalBonuses() {
    val areaPlayer = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)
    areaPlayer.manual("CityTile<Tharsis_2_1>")
    shouldThrow<NotNowException> {
      areaPlayer.manual("MiningArea") { doTask("MiningAreaTile<Tharsis_3_2>") }
    }

    val rightsPlayer = newGame(Canon.fromOptionCodes("BM", 2)).tfm(PLAYER1)
    shouldThrow<NotNowException> {
      rightsPlayer.manual("MiningRights") { doTask("MiningRightsTile<Tharsis_2_1>") }
    }
  }

  @Test
  fun miningRightsOnBothMetalsCanChooseAgainWhenCopied() {
    // https://boardgamegeek.com/thread/2663453/rule-opinions-mining-rights-robotic-workforce
    val p1 = newGame(Canon.fromOptionCodes("BRI", 2)).tfm(PLAYER1)

    p1.manual("MiningRights") {
          doTask("MiningRightsTile<TerraCimmeria_6_4>")
          doTask("PROD[Steel]")
        }
        .expect("Titanium, 2 Steel, PROD[Steel]")

    val manual = p1.godMode().also { it.autoExecMode = NONE }
    manual.beginManual("RoboticWorkforce")
    manual.reviseTask(game.tasks.ids().single(), "CopyProductionBox<MiningRights>")
    manual.finish { doTask("PROD[Titanium]") }.expect("PROD[Titanium]")
  }

  @Test
  fun interplanetaryTradeCountsDistinctTagTypes() {
    val p1 = newGame(Canon.fromOptionCodes("BMX", 2)).tfm(PLAYER1)
    // These have to be played: tags cannot be sneaked independently of their cards.
    p1.manual("Ecoline, Thorgate, Phobolog")
    p1.count("DistinctTagType<Player1>") shouldBe 3

    p1.manual("InterplanetaryTrade").expect("PROD[3]")
  }

  @Test
  fun diversitySupportRequiresNineDistinctResourceTypes() {
    val p1 = newGame(Canon.fromOptionCodes("BRMVX", 2)).tfm(PLAYER1)
    p1.phase("Action")
    p1.sneak(
        "100, ProjectCard, Steel, Titanium, Plant, Energy, Heat, Pets, Decomposers, " +
            "Extremophiles, Tardigrades, Animal<Pets>, Microbe<Decomposers>, " +
            "2 Microbe<Extremophiles>, 3 Microbe<Tardigrades>"
    )
    p1.count("DistinctResourceType<Player1>") shouldBe 8
    val ratingBefore = p1.count("TerraformRating")

    shouldThrow<RequirementException> { p1.playProject("DiversitySupport", 1) }
    p1.count("TerraformRating") shouldBe ratingBefore

    p1.sneak("ForcedPrecipitation, Floater<ForcedPrecipitation>")
    p1.count("DistinctResourceType<Player1>") shouldBe 9
    p1.playProject("DiversitySupport", 1).expect("TerraformRating")
  }
}
