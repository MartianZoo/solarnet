package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.Agent
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete accepted-log replay: Kinetic Carbon State (g70576ecb7c18), played 2026-10-30.
// https://terraforming-mars.herokuapp.com/the-end?id=p46f249e3c92d
// Raw log, player record, and per-generation JSON: _local/replays/Game20261030/.
// Payments below are recorded quantities, not reconstructed allocations.
internal class KineticCarbonStateTest : CardTrackingFullGameTest() {
  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, ColoniesExpansion, promos:cards
          Tr63SoloObjective, 1 SelectableCorporationCount
          Enceladus, Titan, Triton, Miranda
          """,
          "Blue",
      )

  override val cardArrivalOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(cn("Blue") to listOf(CheungShingMars, Inventrix, SaturnSystems)),
          cn("PreludeCard") to
              mapOf(
                  cn("Blue") to listOf(FloatingTradeHub, AppliedScience, VenusContract, SpaceLanes)
              ),
      )
  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to mapOf(cn("Blue") to listOf(Inventrix, SaturnSystems)),
          cn("PreludeCard") to mapOf(cn("Blue") to listOf(FloatingTradeHub, SpaceLanes)),
      )
  override val soloTileLocations = listOf("Hellas_3_4", "Hellas_2_4", "Hellas_7_6", "Hellas_7_7")

  @Test
  fun kineticCarbonState() {
    startGame()
    p1.doTasks(
        "14 TerraformRating",
        "3 CorporationCard",
        "-2 CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    // Player-record evidence: neutral cities at 17 and 49, greenery at 11 and 50.
    p1.doTask("-SelectedMiranda")
    p1.doTask("-5 ProjectCard<Selecting>")
    // Test inference: these offered cards are played later without intervening matching draws.
    p1.draw(EarthOffice, FloatingRefinery, AdaptedLichen, OptimalAerobraking, Lichen)
    p1.doTasks(
        "PlayCard<Class<CorporationCard>, Class<CheungShingMars>>",
        "CheungShingMars FROM CorporationCard",
        "44 MC",
        "PROD[3 MC]",
        "15 Pay<> FROM MC",
        "5 ProjectCard FROM ProjectCard<Selecting>",
    )
    with(p1) {
      draw(CorroderSuits)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<VenusContract>>",
          "VenusContract FROM PreludeCard",
          "SearchForCard<TagFilter<Class<VenusTag>>>",
          "TerraformRating",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<AppliedScience>>",
          "AppliedScience FROM PreludeCard",
          "6 Science<AppliedScience>",
      )
    }
    p1.generation1()
    p1.generation2()
    p1.generation3()
    p1.generation4()
    p1.generation5()
    p1.generation6()
    p1.generation7()
    p1.generation8()
    p1.generation9()
    p1.generation10()
    p1.generation11()
    p1.generation12()
    with(p1) {
      cardsHand shouldBe emptySet()
      assertCardTrackingComplete()
      // Player-record evidence: final production completed, final greenery skipped after solo loss.
      assertResources(m = 82, s = 0, t = 3, p = 17, e = 0, h = 13)
      assertProduction(m = 22, s = 0, t = 3, p = 10, e = 0, h = 7)
      assertCounts(60 to "TerraformRating", 3 to "Animal<Pets>", 2 to "Floater<FloatingRefinery>")
      assertSidebar(gen = 12, temp = 2, oxygen = 14, oceans = 9, venus = 22)
      // The source displays 90 VP (60 TR + 10 greenery + 12 cities + 8 cards).
      // Solarnet skips scoring on a solo loss. Do not inject End or final greenery.
      assertCounts(45 to "CardFront OR PlayedEvent", 5 to "CardResource", 1 to "WildTag")
      assertTags(
          but = 7,
          spt = 6,
          sct = 5,
          pot = 1,
          eat = 7,
          vet = 6,
          plt = 7,
          mit = 1,
          ant = 1,
          cit = 3,
      )
      assertCounts(10 to "GreeneryTile", 3 to "CityTile", 1 to "LavaFlows_SpecialTile")
      // Player-record evidence: every occupied Mars space, including neutral tiles.
      assertCounts(
          1 to "GreeneryTile<Hellas_1_2>",
          1 to "GreeneryTile<Hellas_2_3>",
          1 to "GreeneryTile<Hellas_3_2>",
          1 to "GreeneryTile<Hellas_3_3>",
          1 to "GreeneryTile<Hellas_4_2>",
          1 to "GreeneryTile<Hellas_4_4>",
          1 to "GreeneryTile<Hellas_5_2>",
          1 to "GreeneryTile<Hellas_5_4>",
          1 to "GreeneryTile<Hellas_5_5>",
          1 to "GreeneryTile<Hellas_6_6>",
          1 to "CityTile<Hellas_1_3>",
          1 to "CityTile<Hellas_4_3>",
          1 to "CapitalTile<Hellas_2_2>",
          1 to "LavaFlows_SpecialTile<Hellas_9_7>",
      )
      admin.assertCounts(
          1 to "CityTile<SoloOpponent, Hellas_3_4>",
          1 to "CityTile<SoloOpponent, Hellas_7_6>",
          1 to "GreeneryTile<SoloOpponent, Hellas_2_4>",
          1 to "GreeneryTile<SoloOpponent, Hellas_7_7>",
          1 to "OceanTile<Hellas_1_1>",
          1 to "OceanTile<Hellas_2_1>",
          1 to "OceanTile<Hellas_3_1>",
          1 to "OceanTile<Hellas_4_1>",
          1 to "OceanTile<Hellas_4_6>",
          1 to "OceanTile<Hellas_5_6>",
          1 to "OceanTile<Hellas_5_7>",
          1 to "OceanTile<Hellas_5_8>",
          1 to "OceanTile<Hellas_6_7>",
      )
      has("Victory") shouldBe false
      has("GameScope") shouldBe false
    }
  }

  private fun Agent.generation1() {
    doTasks(*playCardTasks("EarthOffice"), "Pay<> FROM MC", "EarthOffice FROM ProjectCard")
    doTasks(
        *playCardTasks("CorroderSuits"),
        "8 Pay<> FROM MC",
        "CorroderSuits FROM ProjectCard",
        "PROD[2 MC]",
        "Science<AppliedScience>",
    )
    doTasks(
        *playCardTasks("FloatingRefinery"),
        "7 Pay<> FROM MC",
        "FloatingRefinery FROM ProjectCard",
        "4 Floater<FloatingRefinery>",
    )
    doTasks(
        *cardActionTasks("FloatingRefinery", "Action2"),
        "-2 Floater<FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    doTasks(*cardActionTasks("AppliedScience"), "-Science<AppliedScience>", "Titanium")
    doTasks(
        *playCardTasks("AdaptedLichen"),
        "9 Pay<> FROM MC",
        "AdaptedLichen FROM ProjectCard",
        "PROD[Plant]",
    )
    doTasks(
        *playCardTasks("OptimalAerobraking"),
        "2 Pay<Class<Titanium>> FROM Titanium",
        "Pay<> FROM MC",
        "OptimalAerobraking FROM ProjectCard",
    )
    doTasks("Pass", "18 MC", "Plant")
    doTasks("TemperatureStep! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[0], after World Government.
    assertSidebar(gen = 2, temp = -28, oxygen = 0, oceans = 0, venus = 0)
  }

  private fun Agent.generation2() {
    draw(FloaterTechnology, PioneerSettlement)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *cardActionTasks("FloatingRefinery", "Action2"),
        "-2 Floater<FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    doTasks(
        *playCardTasks("FloaterTechnology"),
        "7 Pay<> FROM MC",
        "FloaterTechnology FROM ProjectCard",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<FloatingRefinery>")
    doTasks(*cardActionTasks("AppliedScience"), "-Science<AppliedScience>", "Titanium")
    doTasks("Pass", "18 MC", "Plant")
    doTasks("TemperatureStep! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[1], after World Government.
    assertSidebar(gen = 3, temp = -26, oxygen = 0, oceans = 0, venus = 0)
  }

  private fun Agent.generation3() {
    draw(MartianSurvey, LunarMining, BusinessNetwork)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *playCardTasks("BusinessNetwork"),
        "Pay<> FROM MC",
        "BusinessNetwork FROM ProjectCard",
        "PROD[-MC]",
    )
    draw(Capital)
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "Ok", // keep the Business Network card
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<FloatingRefinery>")
    doTasks(
        *cardActionTasks("FloatingRefinery", "Action2"),
        "-2 Floater<FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    doTasks(*cardActionTasks("AppliedScience"), "-Science<AppliedScience>", "Titanium")
    draw(Flooding, SkyDocks)
    doTasks(
        *playCardTasks("MartianSurvey"),
        "9 Pay<> FROM MC",
        "MartianSurvey FROM ProjectCard",
        "2 ProjectCard",
    )
    doTasks(
        *playCardTasks("SkyDocks"),
        "4 Pay<Class<Titanium>> FROM Titanium",
        "3 Pay<> FROM MC",
        "SkyDocks FROM ProjectCard",
        "TradeFleet",
    )
    doTasks(
        *playCardTasks("LunarMining"),
        "7 Pay<> FROM MC",
        "LunarMining FROM ProjectCard",
        "PROD[2 Titanium]",
    )
    doTasks("Pass", "17 MC", "Plant", "2 Titanium")
    doTasks("OceanTile<Hellas_6_7>! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[2], after World Government.
    assertSidebar(gen = 4, temp = -26, oxygen = 0, oceans = 1, venus = 0)
  }

  private fun Agent.generation4() {
    draw(BribedCommittee, ArcticAlgae)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    draw(SulphurEatingBacteria)
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "Ok", // keep the Business Network card
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<FloatingRefinery>")
    draw(Steelworks)
    doTasks(
        *playCardTasks("Flooding"),
        "6 Pay<> FROM MC",
        "Flooding FROM ProjectCard",
        "OceanTile<Hellas_5_6>",
        "TerraformRating",
        "ProjectCard",
        "2 MC",
    )
    doTasks(
        *cardActionTasks("AppliedScience"),
        "-Science<AppliedScience>",
        "Floater<FloatingRefinery>",
    )
    doTasks(
        *cardActionTasks("FloatingRefinery", "Action2"),
        "-2 Floater<FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    doTasks(
        *playCardTasks("PioneerSettlement"),
        "3 Pay<Class<Titanium>> FROM Titanium",
        "3 Pay<> FROM MC",
        "PioneerSettlement FROM ProjectCard",
        "PROD[-2 MC]",
        "Colony<Triton>",
        "3 Titanium",
    )
    doTasks(
        *playCardTasks("BribedCommittee"),
        "3 Pay<> FROM MC",
        "BribedCommittee FROM ProjectCard",
        "2 TerraformRating",
    )
    doTasks("Pass", "18 MC", "Plant", "2 Titanium")
    doTasks("VenusStep! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[3], after World Government.
    assertSidebar(gen = 5, temp = -26, oxygen = 0, oceans = 2, venus = 2)
  }

  private fun Agent.generation5() {
    draw(SubCrustMeasurements, GhgImportFromVenus)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
    )
    doTasks(*cardActionTasks("FloatingRefinery"), "Floater<FloatingRefinery>")
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<FloatingRefinery>")
    doTasks(*cardActionTasks("AppliedScience"), "-Science<AppliedScience>", "Titanium")
    doTasks(
        *playCardTasks("GhgImportFromVenus"),
        "6 Pay<Class<Titanium>> FROM Titanium",
        "4 Pay<> FROM MC",
        "GhgImportFromVenus FROM ProjectCard",
        "VenusStep",
        "PROD[3 Heat]",
        "3 MC",
        "3 Heat",
        "TerraformRating",
        "3 MC",
    )
    doTasks(
        *playCardTasks("SubCrustMeasurements"),
        "14 Pay<> FROM MC",
        "SubCrustMeasurements FROM ProjectCard",
    )
    draw(WaterSplittingPlant)
    doTasks(*cardActionTasks("SubCrustMeasurements"), "ProjectCard")
    doTasks(
        "Pass",
        "19 MC",
        "Plant",
        "2 Titanium",
        "3 Heat",
    )
    doTasks("VenusStep! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[4], after World Government.
    assertSidebar(gen = 6, temp = -26, oxygen = 0, oceans = 2, venus = 6)
  }

  private fun Agent.generation6() {
    draw(AirRaid, TransNeptuneProbe)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<FloatingRefinery>")
    draw(LavaFlows)
    doTasks(*cardActionTasks("SubCrustMeasurements"), "ProjectCard")
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
    )
    doTasks(
        *playCardTasks("AirRaid"),
        "AirRaid FROM ProjectCard",
        "-Floater<FloatingRefinery>",
        "5 MC FROM MC<SoloOpponent>",
    )
    doTasks(
        *cardActionTasks("FloatingRefinery", "Action2"),
        "-2 Floater<FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    doTasks(
        "UseAction<TradeAction, Action3>",
        "3 Pay<Class<Titanium>> FROM Titanium",
        "Trade<Triton>",
        "5 Titanium",
        "Titanium",
    )
    doTasks(
        "UseAction<TradeAction, Action3>",
        "3 Pay<Class<Titanium>> FROM Titanium",
        "Trade<Titan>",
        "4 Floater<FloatingRefinery>",
    )
    doTasks(
        *playCardTasks("SulphurEatingBacteria"),
        "5 Pay<> FROM MC",
        "SulphurEatingBacteria FROM ProjectCard",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria"), "Microbe<SulphurEatingBacteria>")
    doTasks(
        *cardActionTasks("AppliedScience"),
        "-Science<AppliedScience>",
        "Microbe<SulphurEatingBacteria>",
    )
    doTasks(
        *playCardTasks("ArcticAlgae"),
        "11 Pay<> FROM MC",
        "ArcticAlgae FROM ProjectCard",
        "Plant",
    )
    doTasks(
        *playCardTasks("TransNeptuneProbe"),
        "Pay<Class<Titanium>> FROM Titanium",
        "2 Pay<> FROM MC",
        "TransNeptuneProbe FROM ProjectCard",
    )
    doTasks(
        "Pass",
        "19 MC",
        "Plant",
        "2 Titanium",
        "3 Heat",
    )
    doTasks("OceanTile<Hellas_5_8>! BY Admin", "2 Plant", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[5], after World Government.
    assertSidebar(gen = 7, temp = -26, oxygen = 0, oceans = 3, venus = 6)
  }

  private fun Agent.generation7() {
    draw(Grass, RobotPollinators, FloaterPrototypes)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
        "PROD[Heat]",
    )
    doTasks(
        *cardActionTasks("FloatingRefinery", "Action2"),
        "-2 Floater<FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    draw(MiningColony)
    doTasks(*cardActionTasks("SubCrustMeasurements"), "ProjectCard")
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
    )
    doTasks(
        *playCardTasks("MiningColony"),
        "5 Pay<Class<Titanium>> FROM Titanium",
        "4 Pay<> FROM MC",
        "MiningColony FROM ProjectCard",
        "PROD[Titanium]",
        "Colony<Enceladus>",
        "3 Microbe<SulphurEatingBacteria>",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<FloatingRefinery>")
    doTasks(
        *cardActionTasks("AppliedScience"),
        "-Science<AppliedScience>",
        "Microbe<SulphurEatingBacteria>",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria", "Action2"))
    doTasks(
        "-6 Microbe<SulphurEatingBacteria>",
        "18 MC",
    )
    doTasks(
        *playCardTasks("Lichen"),
        "6 Pay<> FROM MC",
        "Lichen FROM ProjectCard",
        "PROD[Plant]",
    )
    // Test inference: the log gives the sale count, but omits the card identities.
    discard(Steelworks)
    doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    doTasks(
        *playCardTasks("LavaFlows"),
        "17 Pay<> FROM MC",
        "LavaFlows FROM ProjectCard",
        "2 TemperatureStep",
        "2 TerraformRating",
        "PROD[Heat]",
        "LavaFlows_SpecialTile<Hellas_9_7>",
        "-6 MC",
        "OceanTile<Hellas_5_7>",
        "TerraformRating",
        "2 Plant",
        "3 Heat",
        "6 MC",
    )
    doTasks(
        "Pass",
        "23 MC",
        "2 Plant",
        "3 Titanium",
        "5 Heat",
    )
    doTasks("OxygenStep! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[6], after World Government.
    assertSidebar(gen = 8, temp = -20, oxygen = 1, oceans = 4, venus = 6)
  }

  private fun Agent.generation8() {
    draw(ImportOfAdvancedGhg, ForcedPrecipitation)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *cardActionTasks("FloatingRefinery", "Action2"),
        "-2 Floater<FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    doTasks(
        *playCardTasks("ForcedPrecipitation"),
        "7 Pay<> FROM MC",
        "ForcedPrecipitation FROM ProjectCard",
    )
    doTasks(
        *playCardTasks("FloaterPrototypes"),
        "Pay<> FROM MC",
        "FloaterPrototypes FROM ProjectCard",
        "2 Floater<ForcedPrecipitation>",
    )
    draw(VenusSoils)
    doTasks(
        *cardActionTasks("ForcedPrecipitation", "Action2"),
        "-2 Floater<ForcedPrecipitation>",
        "VenusStep",
        "TerraformRating",
        "ProjectCard",
        "3 MC",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    draw(OreProcessor)
    doTasks(*cardActionTasks("SubCrustMeasurements"), "ProjectCard")
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<ForcedPrecipitation>")
    draw(Heather)
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "Ok", // keep the Business Network card
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *playCardTasks("ImportOfAdvancedGhg"),
        "Pay<Class<Titanium>> FROM Titanium",
        "2 Pay<> FROM MC",
        "ImportOfAdvancedGhg FROM ProjectCard",
        "PROD[2 Heat]",
        "3 MC",
        "3 Heat",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria"), "Microbe<SulphurEatingBacteria>")
    doTasks(
        *standardProjectTasks("AquiferProject"),
        "18 Pay<> FROM MC",
        "OceanTile<Hellas_1_1>",
        "TerraformRating",
        "4 Plant",
    )
    doTasks(
        "Pass",
        "26 MC",
        "2 Plant",
        "3 Titanium",
        "7 Heat",
    )
    doTasks("OceanTile<Hellas_3_1>! BY Admin", "2 Plant", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[7], after World Government.
    assertSidebar(gen = 9, temp = -18, oxygen = 1, oceans = 6, venus = 8)
  }

  private fun Agent.generation9() {
    draw(ResearchColony, TerraformingContract)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    draw(AerialMappers)
    doTasks(*cardActionTasks("SubCrustMeasurements"), "ProjectCard")
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<ForcedPrecipitation>")
    doTasks(
        *cardActionTasks("ForcedPrecipitation", "Action2"),
        "-2 Floater<ForcedPrecipitation>",
        "VenusStep",
        "TerraformRating",
        "3 MC",
    )
    doTasks(*cardActionTasks("FloatingRefinery"), "Floater<FloatingRefinery>")
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
    )
    draw(ResearchOutpost, DustSeals)
    doTasks(
        *playCardTasks("ResearchColony"),
        "6 Pay<Class<Titanium>> FROM Titanium",
        "Pay<> FROM MC",
        "ResearchColony FROM ProjectCard",
        "2 ProjectCard",
        "Colony<Titan>",
        "3 Floater<ForcedPrecipitation>",
    )
    doTasks(
        *playCardTasks("ResearchOutpost"),
        "15 Pay<> FROM MC",
        "ResearchOutpost FROM ProjectCard",
        "NormalCityTile<Hellas_4_3>",
        "Steel",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_4_4>",
        "OxygenStep",
        "2 Steel",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_5_4>",
        "OxygenStep",
        "2 Steel",
        "TerraformRating",
    )
    // Test inference: the log gives the sale count, but omits the card identities.
    discard(WaterSplittingPlant)
    doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    doTasks(*cardActionTasks("SulphurEatingBacteria"), "Microbe<SulphurEatingBacteria>")
    doTasks(
        *playCardTasks("TerraformingContract"),
        "3 Pay<> FROM MC",
        "TerraformingContract FROM ProjectCard",
        "PROD[4 MC]",
    )
    doTasks(
        "Pass",
        "34 MC",
        "2 Plant",
        "3 Titanium",
        "7 Heat",
    )
    doTasks("OxygenStep! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[8], after World Government.
    assertSidebar(gen = 10, temp = -16, oxygen = 4, oceans = 6, venus = 10)
  }

  private fun Agent.generation10() {
    draw(VenusOrbitalSurvey, Gyropolis, Pets)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    draw(InvestmentLoan)
    doTasks(*cardActionTasks("SubCrustMeasurements"), "ProjectCard")
    doTasks(
        *cardActionTasks("FloatingRefinery", "Action2"),
        "-2 Floater<FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    doTasks(
        *cardActionTasks("ForcedPrecipitation", "Action2"),
        "-2 Floater<ForcedPrecipitation>",
        "VenusStep",
        "TerraformRating",
        "3 MC",
    )
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
    )
    doTasks(
        *playCardTasks("InvestmentLoan"),
        "InvestmentLoan FROM ProjectCard",
        "PROD[-MC]",
        "10 MC",
    )
    doTasks(*playCardTasks("Pets"), "5 Pay<> FROM MC", "Pets FROM ProjectCard", "Animal<Pets>")
    doTasks(
        *playCardTasks("VenusSoils"),
        "18 Pay<> FROM MC",
        "VenusSoils FROM ProjectCard",
        "VenusStep",
        "PROD[Plant]",
        "TerraformRating",
        "3 MC",
        "2 Microbe<SulphurEatingBacteria>",
    )
    doTasks(
        "UseAction<TradeAction, Action3>",
        "3 Pay<Class<Titanium>> FROM Titanium",
        "Trade<Enceladus>",
        "Microbe<SulphurEatingBacteria>",
        "4 Microbe<SulphurEatingBacteria>",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria", "Action2"))
    doTasks(
        "-9 Microbe<SulphurEatingBacteria>",
        "27 MC",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<ForcedPrecipitation>")
    doTasks(
        *playCardTasks("Grass"),
        "9 Pay<> FROM MC",
        "Grass FROM ProjectCard",
        "PROD[Plant]",
        "3 Plant",
    )
    doTasks(
        *playCardTasks("Heather"),
        "4 Pay<> FROM MC",
        "Heather FROM ProjectCard",
        "PROD[Plant]",
        "Plant",
    )
    doTasks(
        *playCardTasks("RobotPollinators"),
        "7 Pay<> FROM MC",
        "RobotPollinators FROM ProjectCard",
        "PROD[Plant]",
        "7 Plant",
    )
    doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    doTasks(
        *playCardTasks("Gyropolis"),
        "5 Pay<Class<Steel>> FROM Steel",
        "6 Pay<> FROM MC",
        "Gyropolis FROM ProjectCard",
        "PROD[-2 Energy]",
        "PROD[14 MC]",
        "NormalCityTile<Hellas_1_3>",
        "Animal<Pets>",
        "2 Plant",
    )

    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_1_2>",
        "OxygenStep",
        "2 Plant",
        "2 MC",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_3_3>",
        "OxygenStep",
        "Steel",
        "TerraformRating",
    )
    doTasks(
        "Pass",
        "52 MC",
        "6 Plant",
        "3 Titanium",
        "7 Heat",
    )
    doTasks("OxygenStep! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[9], after World Government.
    assertSidebar(gen = 11, temp = -14, oxygen = 7, oceans = 6, venus = 14)
  }

  private fun Agent.generation11() {
    draw(Greenhouses, Comet, MiningExpedition)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    draw(NitrogenRichAsteroid)
    doTasks(*cardActionTasks("SubCrustMeasurements"), "ProjectCard")
    draw(SoilStudies)
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "Ok", // keep the Business Network card
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        "UseAction<TradeAction, Action3>",
        "3 Pay<Class<Titanium>> FROM Titanium",
        "Trade<Triton>",
        "5 Titanium",
        "Titanium",
    )
    doTasks(
        *playCardTasks("Comet"),
        "6 Pay<Class<Titanium>> FROM Titanium",
        "Pay<> FROM MC",
        "Comet FROM ProjectCard",
        "TemperatureStep",
        "3 MC",
        "3 Heat",
        "TerraformRating",
        "-3 Plant<SoloOpponent>",
        "OceanTile<Hellas_2_1>",
        "TerraformRating",
        "4 Plant",
        "4 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_3_2>",
        "OxygenStep",
        "Plant",
        "4 MC",
        "TerraformRating",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_2_3>",
        "OxygenStep",
        "Plant",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria"), "Microbe<SulphurEatingBacteria>")
    doTasks(
        *cardActionTasks("ForcedPrecipitation", "Action2"),
        "-2 Floater<ForcedPrecipitation>",
        "VenusStep",
        "2 TerraformRating",
        "3 MC",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<ForcedPrecipitation>")
    doTasks(*cardActionTasks("FloatingRefinery"), "Floater<FloatingRefinery>")
    doTasks(
        *playCardTasks("Greenhouses"),
        "Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "Greenhouses FROM ProjectCard",
        "4 Plant",
    )
    doTasks(
        *playCardTasks("SoilStudies"),
        "11 Pay<> FROM MC",
        "SoilStudies FROM ProjectCard",
        "18 Plant",
    )
    doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    doTasks(
        *playCardTasks("Capital"),
        "22 Pay<> FROM MC",
        "Capital FROM ProjectCard",
        "PROD[-2 Energy]",
        "PROD[5 MC]",
        "CapitalTile<Hellas_2_2, Capital>",
        "Animal<Pets>",
        "2 Plant",
        "4 MC",
    )
    doTasks(
        "Pass",
        "65 MC",
        "6 Plant",
        "3 Titanium",
        "7 Heat",
    )
    doTasks("VenusStep! BY Admin", "4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[10], after World Government.
    assertSidebar(gen = 12, temp = -8, oxygen = 9, oceans = 7, venus = 18)
  }

  private fun Agent.generation12() {
    doTask("-4 ProjectCard<Selecting>")
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    draw(BioPrintingFacility)
    doTasks(*cardActionTasks("SubCrustMeasurements"), "ProjectCard")
    draw(Teslaract)
    doTasks(
        *cardActionTasks("BusinessNetwork"),
        "ProjectCard<Selecting>",
        "Ok", // keep the Business Network card
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    // Test inference: the log gives the sale count, but omits the card identities.
    discard(OreProcessor, AerialMappers, DustSeals, VenusOrbitalSurvey)
    doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
    doTasks(
        "UseAction<TradeAction, Action3>",
        "3 Pay<Class<Titanium>> FROM Titanium",
        "Trade<Enceladus>",
        "Microbe<SulphurEatingBacteria>",
        "3 Microbe<SulphurEatingBacteria>",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<ForcedPrecipitation>")
    doTasks(
        *cardActionTasks("ForcedPrecipitation", "Action2"),
        "-2 Floater<ForcedPrecipitation>",
        "VenusStep",
        "TerraformRating",
        "3 MC",
    )
    doTasks(
        *playCardTasks("MiningExpedition"),
        "10 Pay<> FROM MC",
        "MiningExpedition FROM ProjectCard",
        "OxygenStep",
        "2 Steel",
        "TerraformRating",
        "-2 Plant<SoloOpponent>",
    )
    doTasks(
        *standardProjectTasks("AquiferProject"),
        "18 Pay<> FROM MC",
        "OceanTile<Hellas_4_1>",
        "TerraformRating",
        "2 Plant",
        "Plant",
        "2 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_4_2>",
        "OxygenStep",
        "Plant",
        "4 MC",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_5_5>",
        "OxygenStep",
        "2 MC",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_6_6>",
        "OxygenStep",
        "4 MC",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_5_2>",
        "OxygenStep",
        "2 MC",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria", "Action2"))
    doTasks(
        "-5 Microbe<SulphurEatingBacteria>",
        "15 MC",
    )
    doTasks(
        *playCardTasks("Teslaract"),
        "2 Pay<Class<Steel>> FROM Steel",
        "6 Pay<> FROM MC",
        "Teslaract FROM ProjectCard",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("NitrogenRichAsteroid"),
        "Pay<Class<Titanium>> FROM Titanium",
        "26 Pay<> FROM MC",
        "NitrogenRichAsteroid FROM ProjectCard",
        "2 TerraformRating",
        "TemperatureStep",
        "3 MC",
        "3 Heat",
        "TerraformRating",
        "PROD[4 Plant]",
    )
    doTasks(
        *standardProjectTasks("AsteroidProject"),
        "14 Pay<> FROM MC",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("AsteroidProject"),
        "14 Pay<> FROM MC",
        "TemperatureStep",
        "TerraformRating",
        "OceanTile<Hellas_4_6>",
        "TerraformRating",
        "2 Plant",
        "Plant",
        "4 MC",
    )
    doTasks(*cardActionTasks("FloatingRefinery"), "Floater<FloatingRefinery>")
    // Test inference: the log gives the sale count, but omits the card identities.
    discard(BioPrintingFacility)
    doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
        "TerraformRating",
        "3 MC",
    )
    doTasks(
        *standardProjectTasks("AsteroidProject"),
        "14 Pay<> FROM MC",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        "Pass",
        "82 MC",
        "10 Plant",
        "3 Titanium",
        "7 Heat",
    )
  }
}
