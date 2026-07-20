package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
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
    p1.godMode().sneak("100, 2 ProjectCard, Credicor")

    p1.playProject("EarthCatapult", 23).expect("-19")

    p1.stdProject("CitySP") { doTask("CityTile<Tharsis_2_1>") }.expect("-21")
  }

  @Test
  fun advertisingRaisesProductionForCostTwentyButNotNineteen() {
    val p1 = newGame(Canon.fromOptionCodes("BRCMX", 2, testColonyTiles(2))).tfm(PLAYER1)
    p1.count("CardCost<Advertising>") shouldBe 4
    p1.godMode().sneak("Advertising")

    p1.godMode().manual("LunarExports") { doTask("PROD[5]") }.expect("PROD[5]")
    p1.godMode().manual("GanymedeColony").expect("PROD[1]")
  }

  @Test
  fun spinOffDepartmentDrawsOnlyForExpensiveCards() {
    val p1 = newGame(Canon.fromOptionCodes("BRCM", 2, testColonyTiles(2))).tfm(PLAYER1)
    p1.godMode().sneak("SpinOffDepartment")

    p1.godMode().manual("Mine")
    p1.count("ProjectCard") shouldBe 0

    p1.godMode().manual("EarthCatapult").expect("ProjectCard")
  }

  @Test
  fun cuttingEdgeTechnologyDiscountsOnlyCardsWithRequirements() {
    val p1 = newGame(Canon.fromOptionCodes("BRMVX", 2)).tfm(PLAYER1)
    p1.phase("Action")
    p1.godMode().sneak("100, 2 ProjectCard, CuttingEdgeTechnology")
    p1.godMode().sneak("1, Steel, Titanium, Plant, Energy, Heat")
    p1.godMode().sneak("Pets, Decomposers, ForcedPrecipitation")
    p1.godMode().sneak("Animal<Pets>, Microbe<Decomposers>, Floater<ForcedPrecipitation>")

    p1.playProject("DiversitySupport", 0).expect("TerraformRating")
    p1.playProject("Mine", 4)
  }

  @Test
  fun miningGuildRaisesSteelProductionOnlyForPrintedSteelOrTitaniumBonuses() {
    val p1 = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)
    p1.godMode().sneak("MiningGuild")
    val before = p1.count("PROD[Steel]")

    p1.godMode().manual("CityTile<Tharsis_1_1>") // LSS
    p1.count("PROD[Steel]") shouldBe before + 1

    p1.godMode().manual("CityTile<Tharsis_2_1>") // L
    p1.count("PROD[Steel]") shouldBe before + 1
  }

  @Test
  fun miningAreaProducesTheResourcePrintedOnItsChosenArea() {
    val steelPlayer = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)

    steelPlayer
        .godMode()
        .manual("MiningArea") {
          doTask("MiningAreaTile<Tharsis_1_1>")
        }
        .expect("2 Steel, PROD[Steel]")

    val titaniumPlayer = newGame(Canon.fromOptionCodes("BRM", 2)).tfm(PLAYER1)
    titaniumPlayer
        .godMode()
        .manual("MiningArea") {
          doTask("MiningAreaTile<Tharsis_8_9>")
        }
        .expect("Titanium, PROD[Titanium]")
  }

  @Test
  fun miningRightsProducesTheResourceOnItsChosenArea() {
    val p1 = newGame(Canon.fromOptionCodes("BM", 2)).tfm(PLAYER1)
    p1.godMode().manual("CityTile<Tharsis_2_1>")

    p1.godMode()
        .manual("MiningRights") {
          doTask("MiningRightsTile<Tharsis_1_1>")
        }
        .expect("2 Steel, PROD[Steel]")

    p1.count("PROD[Steel]") shouldBe 1
    p1.count("PROD[Titanium]") shouldBe 0
  }

  @Test
  fun interplanetaryTradeCountsDistinctTagTypes() {
    val p1 = newGame(Canon.fromOptionCodes("BMX", 2)).tfm(PLAYER1)
    p1.godMode().manual("Ecoline, Thorgate, Phobolog")
    p1.count("DistinctTagType<Player1>") shouldBe 3

    p1.godMode().manual("InterplanetaryTrade").expect("PROD[3]")
  }

  @Test
  fun diversitySupportRequiresNineDistinctResourceTypes() {
    val p1 = newGame(Canon.fromOptionCodes("BRMVX", 2)).tfm(PLAYER1)
    p1.phase("Action")
    p1.godMode().sneak("100, ProjectCard")
    p1.godMode().sneak("1, Steel, Titanium, Plant, Energy, Heat")
    p1.godMode().sneak("Pets, Decomposers, Extremophiles, Tardigrades")
    p1.godMode()
        .sneak(
            "Animal<Pets>, Microbe<Decomposers>, 2 Microbe<Extremophiles>, 3 Microbe<Tardigrades>"
        )
    p1.count("DistinctResourceType<Player1>") shouldBe 8
    val ratingBefore = p1.count("TerraformRating")

    shouldThrow<RequirementException> { p1.playProject("DiversitySupport", 1) }
    p1.count("TerraformRating") shouldBe ratingBefore

    p1.godMode().sneak("ForcedPrecipitation, Floater<ForcedPrecipitation>")
    p1.count("DistinctResourceType<Player1>") shouldBe 9
    p1.playProject("DiversitySupport", 1).expect("TerraformRating")
  }
}
