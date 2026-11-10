package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.Agent
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete source-log replay: Remote Vacuum Flow (g40fde6455218).
// http://newazure.local:8080/the-end?id=p5b5fbeff757c
// Source archive: _local/replays/Game20261031/.
internal class RemoteVacuumFlowTest : CardTrackingFullGameTest() {
  override val firstGlobalEvents =
      listOf(
          "CharismaticWgPresident",
          "MoholeLakeGlobalEvent",
          "ImmigrationToVenus",
          "ExperimentalLifeforms",
          "PateraBoring",
          "ExploreFirstDirective",
          "WorldGovernmentDirectives",
          "HeatFirstPolicy",
          "RedResistance",
          "ViralModificationsApproved",
          "ThawMining",
          "IncreasedInterplanetaryTrade",
          "SolarnetGlobalEvent",
      )

  override val soloTileLocations =
      listOf("Cimmeria_3_3", "Cimmeria_3_4", "Cimmeria_6_4", "Cimmeria_6_3")
  private val ShipmentToEarth = cn("ShipmentToEarth")
  override val config =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, ColoniesExpansion, TurmoilExpansion, promos:cards
          Tr63SoloObjective, 2 SelectableCorporationCount, 2 SelectablePreludeCount
          Ceres, Pluto, Titan, Enceladus
          """,
          "Blue",
      )
  // Unsupported component: New Holland is drawn and discarded in generation 4, never played.
  // This identity-only declaration tracks that hand movement without inventing card effects.
  override val catalog by lazy {
    object : TfmCatalog(canonicalCatalog(config)) {
      override val explicitClassDeclarations =
          super.explicitClassDeclarations +
              parseClasses(
                  """
                  CLASS NewHolland : CardFront<Class<ProjectCard>> { cost = 20 }
                  """
              )
    }
  }
  override val cardArrivalOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(cn("Blue") to listOf(Philares, Inventrix, PharmacyUnion, AstroDrill)),
          cn("PreludeCard") to
              mapOf(
                  cn("Blue") to
                      listOf(
                          Loan,
                          IndustrialComplex,
                          BoardOfDirectors,
                          MetalRichAsteroid,
                          ExcentricSponsor,
                          SuitableInfrastructure,
                      )
              ),
      )
  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to mapOf(cn("Blue") to listOf(Philares, Inventrix, PharmacyUnion)),
          cn("PreludeCard") to
              mapOf(
                  cn("Blue") to
                      listOf(
                          BoardOfDirectors,
                          MetalRichAsteroid,
                          ExcentricSponsor,
                          SuitableInfrastructure,
                      )
              ),
      )

  @Test
  fun remoteVacuumFlow() {
    startGame()
    p1.doTasks(
        "14 TerraformRating",
        "4 CorporationCard",
        "-3 CorporationCard",
        "10 ProjectCard<Selecting>",
        "6 PreludeCard",
        "-4 PreludeCard",
    )
    p1.doTask("-SelectedEnceladus")
    p1.doTask("-3 ProjectCard<Selecting>")
    // Test inference: initial kept cards identified by subsequent plays without matching draws.
    p1.draw(
        MartianMediaCenter,
        PeroxidePower,
        ShipmentToEarth,
        TechnologyDemonstration,
        DeimosDownPromo,
        CometAiming,
        IndenturedWorkers,
    )
    p1.doTasks(
        "PlayCard<Class<CorporationCard>, Class<AstroDrill>>",
        "AstroDrill FROM CorporationCard",
        "35 MC",
        "3 Asteroid<AstroDrill>",
        "21 Pay<> FROM MC",
        "7 ProjectCard FROM ProjectCard<Selecting>",
    )
    with(p1) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<Loan>>",
          "Loan FROM PreludeCard",
          "PROD[-2 MC]",
          "30 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
          "IndustrialComplex FROM PreludeCard",
          "-18 MC",
      )
      // Expand the production box into its separate resource increases.
      doTask(tasks.extract { it }.single().id)
      doTasks(
          "PROD[Titanium]",
          "PROD[Steel]",
          "PROD[Plant]",
          "PROD[5 MC]",
          "PROD[Heat]",
          "PROD[Energy]",
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

    // Player-record evidence: all occupied Mars spaces.
    admin.assertCounts(
        1 to "OceanTile<Cimmeria_1_5>",
        1 to "OceanTile<Cimmeria_2_1>",
        1 to "GreeneryTile<Cimmeria_2_6, Blue>",
        1 to "CityTile<Cimmeria_3_3, SoloOpponent>",
        1 to "GreeneryTile<Cimmeria_3_4, SoloOpponent>",
        1 to "GreeneryTile<Cimmeria_3_6, Blue>",
        1 to "GreeneryTile<Cimmeria_4_5, Blue>",
        1 to "CapitalTile<Cimmeria_4_6, Blue>",
        1 to "GreeneryTile<Cimmeria_4_7, Blue>",
        1 to "GreeneryTile<Cimmeria_5_4, Blue>",
        1 to "CityTile<Cimmeria_5_5, Blue>",
        1 to "GreeneryTile<Cimmeria_5_6, Blue>",
        1 to "GreeneryTile<Cimmeria_5_7, Blue>",
        1 to "MiningArea_SpecialTile<Cimmeria_5_8, Blue>",
        1 to "DeimosDownPromo_SpecialTile<Cimmeria_6_2, Blue>",
        1 to "GreeneryTile<Cimmeria_6_3, SoloOpponent>",
        1 to "CityTile<Cimmeria_6_4, SoloOpponent>",
        1 to "GreeneryTile<Cimmeria_6_5, Blue>",
        1 to "GreeneryTile<Cimmeria_6_6, Blue>",
        1 to "CityTile<Cimmeria_6_7, Blue>",
        1 to "GreeneryTile<Cimmeria_7_5, Blue>",
        1 to "CityTile<Cimmeria_7_6, Blue>",
        1 to "GreeneryTile<Cimmeria_7_7, Blue>",
        1 to "GreeneryTile<Cimmeria_7_8, Blue>",
        1 to "OceanTile<Cimmeria_7_9>",
        1 to "OceanTile<Cimmeria_8_4>",
        1 to "MagneticFieldGeneratorsPromo_SpecialTile<Cimmeria_8_5, Blue>",
        1 to "GreeneryTile<Cimmeria_8_6, Blue>",
        1 to "GreeneryTile<Cimmeria_8_7, Blue>",
        1 to "CityTile<Cimmeria_8_8, Blue>",
        1 to "OceanTile<Cimmeria_8_9>",
        1 to "OceanTile<Cimmeria_9_5>",
        1 to "OceanTile<Cimmeria_9_6>",
        1 to "OceanTile<Cimmeria_9_7>",
        1 to "GreeneryTile<Cimmeria_9_8, Blue>",
        1 to "OceanTile<Cimmeria_9_9>",
    )

    with(p1) {
      cardsHand shouldBe emptySet()
      assertCardTrackingComplete()
      // Player-record evidence: after final production and both final greenery placements.
      assertResources(m = 102, s = 5, t = 6, p = 2, e = 2, h = 7)
      assertProduction(m = 30, s = 3, t = 2, p = 11, e = 2, h = 7)
      assertCounts(71 to "TerraformRating", 151 to "VictoryPoint", 1 to "Victory")
      assertCounts(
          1 to "Science<OlympusConference>",
          2 to "Microbe<Extremophiles>",
          1 to "Science<SearchForLife>",
          1 to "Floater<DeuteriumExport>",
          5 to "CardResource",
      )
      assertCounts(15 to "GreeneryTile", 5 to "CityTile", 0 to "Milestone", 0 to "Award")
      val score = Summarizer(game)
      score.net("TerraformRating", "VictoryPoint<Blue>") shouldBe 71
      score.net("GreeneryTile", "VictoryPoint<Blue>") shouldBe 15
      score.net("CityTile", "VictoryPoint<Blue>") shouldBe 25
      score.net("Card", "VictoryPoint<Blue>") shouldBe 35
      score.net("Delegate", "VictoryPoint<Blue>") shouldBe 5
      score.net("Milestone", "VictoryPoint<Blue>") shouldBe 0
      score.net("Award", "VictoryPoint<Blue>") shouldBe 0
      assertCounts(
          1 to "Chairman",
          1 to "PartyLeader<MarsFirst>",
          1 to "PartyLeader<Scientists>",
          1 to "PartyLeader<Unity>",
          1 to "PartyLeader<Kelvinists>",
          1 to "PartyFollower<Unity>",
      )
    }
  }

  private fun Agent.generation1() {
    doTasks(*cardActionTasks("AstroDrill", "Action2"), "-Asteroid<AstroDrill>", "3 Titanium")
    draw(PhobosSpaceHaven, OlympusConference)
    doTasks(
        *playCardTasks("TechnologyDemonstration"),
        "Pay<Class<Titanium>> FROM Titanium",
        "2 Pay<> FROM MC",
        "TechnologyDemonstration FROM ProjectCard",
        "2 ProjectCard",
    )
    doTasks(
        *playCardTasks("IndenturedWorkers"),
        "IndenturedWorkers FROM ProjectCard",
        "IndenturedWorkers_NextCardEffect",
    )
    // Source payment preserves a titanium for the immediately following action.

    doTasks(
        *playCardTasks("CometAiming"),
        "Pay<Class<Titanium>> FROM Titanium",
        "6 Pay<> FROM MC",
        "CometAiming FROM ProjectCard",
    )
    doTasks(
        *cardActionTasks("CometAiming"),
        "Pay<Class<Titanium>> FROM Titanium",
        "Asteroid<CometAiming>",
    )
    doTasks(
        *playCardTasks("PeroxidePower"),
        "7 Pay<> FROM MC",
        "PeroxidePower FROM ProjectCard",
        "PROD[-MC]",
        "PROD[2 Energy]",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<MarsFirst> FROM LobbyDelegate")
    doTasks(
        "Pass",
        "14 MC",
        "Titanium",
        "Steel",
        "Plant",
        "Heat",
        "3 Energy",
    )
    doTask("VenusStep! BY Admin")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[0].
    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 2)
  }

  private fun Agent.generation2() {
    draw(PioneerSettlement, MartianSurvey)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(*cardActionTasks("AstroDrill", "Action2"), "-Asteroid<AstroDrill>", "3 Titanium")
    doTasks(
        *cardActionTasks("CometAiming", "Action2"),
        "-Asteroid<CometAiming>",
        "OceanTile<Cimmeria_2_1>",
        "TerraformRating",
        "2 Titanium",
    )
    draw(MarsUniversity, Penguins)
    doTasks(
        *playCardTasks("DeimosDownPromo"),
        "6 Pay<Class<Titanium>> FROM Titanium",
        "7 Pay<> FROM MC",
        "DeimosDownPromo FROM ProjectCard",
        "3 TemperatureStep",
        "4 Steel",
        "3 TerraformRating",
        "PROD[Heat]",
        "-6 Plant<SoloOpponent>",
        "DeimosDownPromo_SpecialTile<Cimmeria_6_2>",
        "2 ProjectCard",
    )
    draw(WarpDrive)
    discard(PhobosSpaceHaven)
    doTasks(
        *playCardTasks("MarsUniversity"),
        "4 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "MarsUniversity FROM ProjectCard",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    // Test inference: the source gives the sale count, not card identities.
    discard(Penguins)
    doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
    doTasks(
        *playCardTasks("MartianMediaCenter"),
        "Pay<Class<Steel>> FROM Steel",
        "5 Pay<> FROM MC",
        "MartianMediaCenter FROM ProjectCard",
        "PROD[2 MC]",
    )
    doTasks(*cardActionTasks("MartianMediaCenter"), "3 Pay<> FROM MC", "PartyLeader<Reds>")
    doTasks(
        "Pass",
        "19 MC",
        "Titanium",
        "Steel",
        "Plant",
        "2 Heat",
        "3 Energy",
        "3 Heat FROM Energy",
    )
    doTasks("VenusStep! BY Admin", "4 MC")
    p1.doTask("2! MC / Influence")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[1].
    assertSidebar(gen = 3, temp = -24, oxygen = 0, oceans = 1, venus = 4)
  }

  private fun Agent.generation3() {
    draw(InventionContest, Grass, Potatoes, DevelopmentCenter)
    doTasks(
        "Ok", // keep all four research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
    doTasks(*cardActionTasks("AstroDrill"), "Asteroid<CometAiming>")
    doTasks(
        *cardActionTasks("CometAiming", "Action2"),
        "-Asteroid<CometAiming>",
        "OceanTile<Cimmeria_7_9>",
        "TerraformRating",
        "2 Plant",
    )
    doTasks(
        *playCardTasks("Potatoes"),
        "2 Pay<> FROM MC",
        "Potatoes FROM ProjectCard",
        "-2 Plant",
        "PROD[2 MC]",
    )
    doTasks(
        "UseAction<KelvinistsPolicy, Action1>",
        "10 Pay<> FROM MC",
        "PROD[Heat]",
        "PROD[Energy]",
    )
    doTasks(
        *cardActionTasks("MartianMediaCenter"),
        "3 Pay<> FROM MC",
        "PartyLeader<Scientists>",
    )
    doTasks(
        "Pass",
        "21 MC",
        "Titanium",
        "Steel",
        "Plant",
        "3 Heat",
        "4 Energy",
        "3 Heat FROM Energy",
    )
    draw(GiantIceAsteroid, IshtarExpedition)
    doTasks("VenusStep! BY Admin", "-2* TemperatureStep BY Admin", "2 ProjectCard")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[2].
    assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 2, venus = 6)
  }

  private fun Agent.generation4() {
    draw(TradeEnvoys, ExtractorBalloons, Supercapacitors)
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
    doTasks(*cardActionTasks("AstroDrill", "Action2"), "-Asteroid<AstroDrill>", "3 Titanium")
    doTasks(
        *cardActionTasks("CometAiming"),
        "Pay<Class<Titanium>> FROM Titanium",
        "Asteroid<CometAiming>",
    )
    doTasks(
        *playCardTasks("PioneerSettlement"),
        "4 Pay<Class<Titanium>> FROM Titanium",
        "Pay<> FROM MC",
        "PioneerSettlement FROM ProjectCard",
        "PROD[-2 MC]",
        "Colony<Ceres>",
        "PROD[Steel]",
    )
    doTasks(*playCardTasks("TradeEnvoys"), "6 Pay<> FROM MC", "TradeEnvoys FROM ProjectCard")
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Ceres>",
        "ColonyProduction<Ceres>",
        "8 Steel",
        "2 Steel",
    )
    draw(SpaceHotels)
    discard(Supercapacitors)
    doTasks(
        *playCardTasks("OlympusConference"),
        "5 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "OlympusConference FROM ProjectCard",
        "Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(NewHolland, MagneticShield, TopsoilContract, CityParks)
    discard(NewHolland)
    doTasks(
        *playCardTasks("MartianSurvey"),
        "9 Pay<> FROM MC",
        "MartianSurvey FROM ProjectCard",
        "2 ProjectCard",
        "ProjectCard FROM Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(SpinOffDepartment)
    discard(CityParks)
    doTasks(
        *playCardTasks("DevelopmentCenter"),
        "5 Pay<Class<Steel>> FROM Steel",
        "Pay<> FROM MC",
        "DevelopmentCenter FROM ProjectCard",
        "Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
    draw(MiningRights)
    doTasks(
        *cardActionTasks("DevelopmentCenter"),
        "Pay<Class<Energy>> FROM Energy",
        "ProjectCard",
    )
    // Test inference: the source gives the sale count, not card identities.
    discard(MiningRights)
    doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
    doTasks(*cardActionTasks("MartianMediaCenter"), "3 Pay<> FROM MC", "PartyLeader<MarsFirst>")
    doTasks(
        "Pass",
        "20 MC",
        "Titanium",
        "2 Steel",
        "Plant",
        "3 Heat",
        "4 Energy",
    )
    draw(EnergySaving, CaretakerContract, SolarProbe)
    doTasks("OxygenStep! BY Admin", "-10 MC", "3 ProjectCard")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[3].
    assertSidebar(gen = 5, temp = -26, oxygen = 1, oceans = 2, venus = 6)
  }

  private fun Agent.generation5() {
    draw(SkyDocks, AerialMappers)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    draw(SearchForLife)
    doTasks(
        *cardActionTasks("DevelopmentCenter"),
        "Pay<Class<Energy>> FROM Energy",
        "ProjectCard",
    )
    doTasks(
        *cardActionTasks("CometAiming", "Action2"),
        "-Asteroid<CometAiming>",
        "OceanTile<Cimmeria_8_9>",
        "TerraformRating",
        "2 Plant",
        "2 MC",
    )
    doTasks(*cardActionTasks("AstroDrill"), "Asteroid<CometAiming>")
    draw(GiantSolarShade, CeosFavoriteProject, VenusWaystation)
    discard(CaretakerContract)
    doTasks(
        *playCardTasks("InventionContest"),
        "2 Pay<> FROM MC",
        "InventionContest FROM ProjectCard",
        "3 ProjectCard<Selecting>",
        "ProjectCard FROM ProjectCard<Selecting>",
        "ProjectCard FROM Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(
        *cardActionTasks("MartianMediaCenter"),
        "3 Pay<> FROM MC",
        "PartyFollower<MarsFirst>",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
    draw(RedShips, ImmigrantCity, VestaShipyard, RobotPollinators)
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Pluto>",
        "ColonyProduction<Pluto>",
        "4 ProjectCard",
    )
    doTasks(
        "Pass",
        "21 MC",
        "Titanium",
        "2 Steel",
        "Plant",
        "3 Heat",
        "4 Energy",
    )
    doTasks("OxygenStep! BY Admin", "-Plant")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[4].
    assertSidebar(gen = 6, temp = -26, oxygen = 2, oceans = 3, venus = 6)
  }

  private fun Agent.generation6() {
    draw()
    doTask("-4 ProjectCard<Selecting>")
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
        "PROD[Heat]",
        "-3 MC",
    )
    draw(Extremophiles)
    doTasks(
        *cardActionTasks("DevelopmentCenter"),
        "Pay<Class<Energy>> FROM Energy",
        "ProjectCard",
    )
    doTasks(
        *cardActionTasks("CometAiming", "Action2"),
        "-Asteroid<CometAiming>",
        "OceanTile<Cimmeria_9_9>",
        "TerraformRating",
        "Plant",
        "-1 MC",
    )
    doTasks(*cardActionTasks("AstroDrill"), "Asteroid<CometAiming>")
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Ceres>",
        "ColonyProduction<Ceres>",
        "6 Steel",
        "2 Steel",
    )
    doTasks(
        *playCardTasks("SpinOffDepartment"),
        "5 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "SpinOffDepartment FROM ProjectCard",
        "PROD[2 MC]",
    )
    draw(Dirigibles)
    doTasks(
        *playCardTasks("ExtractorBalloons"),
        "21 Pay<> FROM MC",
        "ExtractorBalloons FROM ProjectCard",
        "3 Floater<ExtractorBalloons>",
        "ProjectCard",
    )
    doTasks(*cardActionTasks("ExtractorBalloons"), "Floater<ExtractorBalloons>")
    doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Scientists> FROM LobbyDelegate")
    doTasks(
        "Pass",
        "26 MC",
        "Titanium",
        "2 Steel",
        "Plant",
        "4 Heat",
        "4 Energy",
    )
    doTasks("OxygenStep! BY Admin", "PROD[3 Heat]")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[5].
    assertSidebar(gen = 7, temp = -20, oxygen = 3, oceans = 4, venus = 6)
  }

  private fun Agent.generation7() {
    draw(HiTechLab)
    doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    draw(Archaebacteria)
    doTasks(
        *cardActionTasks("DevelopmentCenter"),
        "Pay<Class<Energy>> FROM Energy",
        "ProjectCard",
    )
    draw(AcquiredCompany)
    doTasks(
        *cardActionTasks("ExtractorBalloons", "Action2"),
        "-2 Floater<ExtractorBalloons>",
        "VenusStep",
        "TerraformRating",
        "ProjectCard",
    )
    doTasks(
        *cardActionTasks("CometAiming", "Action2"),
        "-Asteroid<CometAiming>",
        "OceanTile<Cimmeria_9_5>",
        "TerraformRating",
        "Steel",
        "2 Plant",
    )
    doTasks(
        *playCardTasks("ImmigrantCity"),
        "6 Pay<Class<Steel>> FROM Steel",
        "Pay<> FROM MC",
        "ImmigrantCity FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[-2 MC]",
        "NormalCityTile<Cimmeria_6_7>",
        "Steel",
        "PROD[MC]",
        "2 Steel",
    )
    draw(GhgShipment)
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_5_6>",
        "OxygenStep",
        "Steel",
        "ProjectCard",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("VenusWaystation"),
        "3 Pay<Class<Titanium>> FROM Titanium",
        "0 Pay<> FROM MC",
        "VenusWaystation FROM ProjectCard",
    )
    doTasks(*playCardTasks("Dirigibles"), "9 Pay<> FROM MC", "Dirigibles FROM ProjectCard")
    doTasks(*cardActionTasks("Dirigibles"), "Floater<Dirigibles>")
    doTasks(
        *playCardTasks("Extremophiles"),
        "Ok", // no Dirigibles floaters paid
        "Pay<> FROM MC",
        "Extremophiles FROM ProjectCard",
    )
    doTasks(
        *playCardTasks("TopsoilContract"),
        "8 Pay<> FROM MC",
        "TopsoilContract FROM ProjectCard",
        "3 Plant",
    )
    doTasks(*cardActionTasks("Extremophiles"), "Microbe<Extremophiles>", "MC")
    draw(SulphurEatingBacteria)
    discard(AcquiredCompany)
    doTasks(
        *playCardTasks("HiTechLab"),
        "8 Pay<Class<Steel>> FROM Steel",
        "Pay<> FROM MC",
        "HiTechLab FROM ProjectCard",
        "Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(*cardActionTasks("AstroDrill"), "Asteroid<CometAiming>")
    doTasks(
        *playCardTasks("Archaebacteria"),
        "6 Pay<> FROM MC",
        "Archaebacteria FROM ProjectCard",
        "PROD[Plant]",
    )
    doTasks(
        *playCardTasks("SulphurEatingBacteria"),
        "PayFromCard<Dirigibles> FROM Floater<Dirigibles>",
        "Pay<> FROM MC",
        "SulphurEatingBacteria FROM ProjectCard",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria"), "Microbe<SulphurEatingBacteria>", "MC")
    doTasks(*cardActionTasks("MartianMediaCenter"), "3 Pay<> FROM MC", "PartyLeader<MarsFirst>")
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
    doTasks(
        "Pass",
        "28 MC",
        "Titanium",
        "2 Steel",
        "2 Plant",
        "7 Heat",
        "3 Energy",
        "3 Heat FROM Energy",
    )
    doTasks("VenusStep! BY Admin", "19 MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[6].
    assertSidebar(gen = 8, temp = -20, oxygen = 4, oceans = 5, venus = 10)
  }

  private fun Agent.generation8() {
    draw(SterlingVents, Hospitals, HermeticOrderOfMars)
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
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("Dirigibles"), "Floater<Dirigibles>")
    doTasks(*cardActionTasks("ExtractorBalloons"), "Floater<ExtractorBalloons>")
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Titan>",
        "ColonyProduction<Titan>",
        "3 Floater<Dirigibles>",
    )
    doTasks(
        *playCardTasks("AerialMappers"),
        "Ok", // no Dirigibles floaters paid
        "9 Pay<> FROM MC",
        "AerialMappers FROM ProjectCard",
    )
    doTasks(*cardActionTasks("AerialMappers"), "Floater<Dirigibles>")
    doTasks(
        *playCardTasks("GhgShipment"),
        "Pay<Class<Titanium>> FROM Titanium",
        "0 Pay<> FROM MC",
        "GhgShipment FROM ProjectCard",
        "PROD[Heat]",
        "8 Heat",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    draw(FrontierTown, ColonizerTrainingCamp)
    discard(VestaShipyard)
    doTasks(
        *playCardTasks("SearchForLife"),
        "3 Pay<> FROM MC",
        "SearchForLife FROM ProjectCard",
        "ProjectCard FROM Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(
        *playCardTasks("Hospitals"),
        "4 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "Hospitals FROM ProjectCard",
        "PROD[-Energy]",
    )
    doTasks(
        *playCardTasks("FrontierTown"),
        "11 Pay<> FROM MC",
        "FrontierTown FROM ProjectCard",
        "FrontierTownBonus",
        "PROD[-Energy]",
        "NormalCityTile<Cimmeria_7_6>",
        "PROD[MC]",
        "Disease<Hospitals>",
        "-FrontierTownBonus",
        "Plant",
        "2 Steel",
        "RepeatPlacementBonus<Cimmeria_7_6>",
        "Plant",
        "2 Steel",
        "Plant",
        "2 Steel",
    )
    doTasks(
        *cardActionTasks("CometAiming", "Action2"),
        "-Asteroid<CometAiming>",
        "OceanTile<Cimmeria_9_6>",
        "TerraformRating",
        "Plant",
        "2 MC",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
    doTasks(*cardActionTasks("MartianMediaCenter"), "3 Pay<> FROM MC", "PartyFollower<Greens>")
    doTasks(*cardActionTasks("SulphurEatingBacteria"), "Microbe<SulphurEatingBacteria>", "MC")
    doTasks(*cardActionTasks("Extremophiles"), "Microbe<SulphurEatingBacteria>", "MC")
    doTasks(
        *playCardTasks("ColonizerTrainingCamp"),
        "4 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "ColonizerTrainingCamp FROM ProjectCard",
    )
    doTasks(
        *playCardTasks("HermeticOrderOfMars"),
        "10 Pay<> FROM MC",
        "HermeticOrderOfMars FROM ProjectCard",
        "PROD[2 MC]",
        "15 MC",
    )
    draw(AqueductSystems)
    doTasks(
        *playCardTasks("GiantSolarShade"),
        "5 PayFromCard<Dirigibles> FROM Floater<Dirigibles>",
        "10 Pay<> FROM MC",
        "GiantSolarShade FROM ProjectCard",
        "3 VenusStep",
        "ProjectCard",
        "4 TerraformRating",
    )

    doTasks(
        *playCardTasks("EnergySaving"),
        "15 Pay<> FROM MC",
        "EnergySaving FROM ProjectCard",
        "PROD[4 Energy]",
    )
    doTasks(*cardActionTasks("AstroDrill"), "Asteroid<AstroDrill>")
    doTasks(
        *playCardTasks("SterlingVents"),
        "2 Pay<Class<Steel>> FROM Steel",
        "Pay<> FROM MC",
        "SterlingVents FROM ProjectCard",
        "PROD[-2 Heat]",
        "PROD[2 Energy]",
    )
    draw(DeuteriumExport, FreyjaBiodomes)
    doTasks(
        *playCardTasks("IshtarExpedition"),
        "4 Pay<> FROM MC",
        "IshtarExpedition FROM ProjectCard",
        "3 Titanium",
        "2 SearchForCard<TagFilter<Class<VenusTag>>>",
    )
    doTasks(
        *playCardTasks("DeuteriumExport"),
        "3 Pay<Class<Titanium>> FROM Titanium",
        "0 Pay<> FROM MC",
        "DeuteriumExport FROM ProjectCard",
    )
    doTasks(*cardActionTasks("DeuteriumExport"), "Floater<DeuteriumExport>")
    doTasks(
        "Pass",
        "38 MC",
        "Titanium",
        "2 Steel",
        "2 Plant",
        "6 Heat",
        "7 Energy",
    )
    doTask("OxygenStep! BY Admin")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[7].
    assertSidebar(gen = 9, temp = -14, oxygen = 5, oceans = 6, venus = 16)
  }

  private fun Agent.generation9() {
    draw(Algae, CorporateStronghold)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_7_7>",
        "OxygenStep",
        "4 MC",
        "2 Plant",
        "TerraformRating",
    )
    doTasks(
        *cardActionTasks("DeuteriumExport", "Action2"),
        "-Floater<DeuteriumExport>",
        "PROD[Energy]",
    )
    doTasks(
        *cardActionTasks("ExtractorBalloons", "Action2"),
        "-2 Floater<ExtractorBalloons>",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Ceres>",
        "ColonyProduction<Ceres>",
        "8 Steel",
        "2 Steel",
    )
    draw(IceCapMelting)
    doTasks(
        *cardActionTasks("DevelopmentCenter"),
        "Pay<Class<Energy>> FROM Energy",
        "ProjectCard",
    )
    doTasks(*cardActionTasks("AstroDrill", "Action2"), "-Asteroid<AstroDrill>", "3 Titanium")
    doTasks(
        *cardActionTasks("CometAiming"),
        "Pay<Class<Titanium>> FROM Titanium",
        "Asteroid<CometAiming>",
    )
    doTasks(*cardActionTasks("Extremophiles"), "Microbe<SulphurEatingBacteria>", "MC")
    draw(NitrogenFromTitan)
    discard(RedShips)
    doTasks(
        *playCardTasks("WarpDrive"),
        "14 Pay<> FROM MC",
        "WarpDrive FROM ProjectCard",
        "Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(*cardActionTasks("Dirigibles"), "Floater<AerialMappers>")
    draw(Moss)
    doTasks(
        *cardActionTasks("AerialMappers", "Action2"),
        "-Floater<AerialMappers>",
        "ProjectCard",
    )
    doTasks(
        *playCardTasks("SkyDocks"),
        "3 Pay<Class<Titanium>> FROM Titanium",
        "5 Pay<> FROM MC",
        "SkyDocks FROM ProjectCard",
        "TradeFleet",
    )
    doTasks(
        *playCardTasks("Algae"),
        "9 Pay<> FROM MC",
        "Algae FROM ProjectCard",
        "Plant",
        "PROD[2 Plant]",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_6_6>",
        "OxygenStep",
        "4 MC",
        "Titanium",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("Grass"),
        "10 Pay<> FROM MC",
        "Grass FROM ProjectCard",
        "PROD[Plant]",
        "3 Plant",
    )
    doTasks(
        *playCardTasks("CorporateStronghold"),
        "5 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "CorporateStronghold FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[3 MC]",
        "NormalCityTile<Cimmeria_8_8>",
        "PROD[MC]",
        "Disease<Hospitals>",
        "2 Plant",
        "4 MC",
    )
    draw(FueledGenerators, UrbanizedArea, MartianRails)
    doTasks(
        *playCardTasks("AqueductSystems"),
        "4 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "AqueductSystems FROM ProjectCard",
        "3 SearchForCard<TagFilter<Class<BuildingTag>>>",
    )
    doTasks(
        *playCardTasks("FueledGenerators"),
        "FueledGenerators FROM ProjectCard",
        "PROD[-MC]",
        "PROD[Energy]",
    )
    doTasks(
        *playCardTasks("FreyjaBiodomes"),
        "11 Pay<> FROM MC",
        "FreyjaBiodomes FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[2 MC]",
        "2 Microbe<SulphurEatingBacteria>",
        "2 MC",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria", "Action2"))
    p1.selectOnlyTask(x = 6)
    doTasks("-6 Microbe<SulphurEatingBacteria>", "18 MC")
    doTasks(*cardActionTasks("Hospitals"), "-Disease<Hospitals>", "5 MC")
    doTasks(
        *playCardTasks("Moss"),
        "3 Pay<> FROM MC",
        "Moss FROM ProjectCard",
        "PROD[Plant]",
        "-Plant",
    )
    doTasks(
        *playCardTasks("RobotPollinators"),
        "8 Pay<> FROM MC",
        "RobotPollinators FROM ProjectCard",
        "PROD[Plant]",
        "5 Plant",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_7_8>",
        "OxygenStep",
        "4 MC",
        "Plant",
        "4 MC",
        "TerraformRating",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("SpaceHotels"),
        "Pay<Class<Titanium>> FROM Titanium",
        "4 Pay<> FROM MC",
        "SpaceHotels FROM ProjectCard",
        "PROD[4 MC]",
    )
    doTasks(
        *playCardTasks("MartianRails"),
        "3 Pay<Class<Steel>> FROM Steel",
        "6 Pay<> FROM MC",
        "MartianRails FROM ProjectCard",
    )
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "5 MC")
    doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Kelvinists> FROM LobbyDelegate")
    doTasks(
        *cardActionTasks("MartianMediaCenter"),
        "3 Pay<> FROM MC",
        "PartyFollower<Scientists>",
    )
    draw(PowerPlant, FloaterTechnology, MarketManipulation, DuskLaserMining)
    discard(UrbanizedArea)
    doTasks(
        *playCardTasks("SolarProbe"),
        "4 Pay<> FROM MC",
        "SolarProbe FROM ProjectCard",
        "2 ProjectCard",
        "ProjectCard FROM Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(
        *playCardTasks("DuskLaserMining"),
        "3 Pay<> FROM MC",
        "DuskLaserMining FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[Titanium]",
        "4 Titanium",
    )
    doTasks(
        *playCardTasks("PowerPlant"),
        "3 Pay<> FROM MC",
        "PowerPlant FROM ProjectCard",
        "PROD[Energy]",
    )
    doTasks(
        *cardActionTasks("SearchForLife"),
        "Pay<> FROM MC",
        "ProjectCard<Revealed>",
        "ClaimCardReward<TagFilter<Class<MicrobeTag>>, SearchForLife>",
        "-ProjectCard<Revealed>",
    )
    doTasks(
        "Pass",
        "51 MC",
        "2 Titanium",
        "2 Steel",
        "7 Plant",
        "6 Heat",
        "7 Energy",
        "2 Heat FROM Energy",
    )
    doTasks("TemperatureStep! BY Admin", "-14 Heat", "-6 MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[8].
    assertSidebar(gen = 10, temp = -10, oxygen = 8, oceans = 6, venus = 18)
  }

  private fun Agent.generation10() {
    draw(MercurianAlloys, BribedCommittee, TectonicStressPower)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Ceres>",
        "ColonyProduction<Ceres>",
        "4 Steel",
        "2 Steel",
    )
    doTasks(*cardActionTasks("DeuteriumExport"), "Floater<DeuteriumExport>")
    draw(AntiGravityTechnology)
    doTasks(
        *cardActionTasks("DevelopmentCenter"),
        "Pay<Class<Energy>> FROM Energy",
        "ProjectCard",
    )
    doTasks(*cardActionTasks("Dirigibles"), "Floater<ExtractorBalloons>")
    doTasks(
        *cardActionTasks("ExtractorBalloons", "Action2"),
        "-2 Floater<ExtractorBalloons>",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("TectonicStressPower"),
        "8 Pay<Class<Steel>> FROM Steel",
        "Pay<> FROM MC",
        "TectonicStressPower FROM ProjectCard",
        "PROD[3 Energy]",
    )
    doTasks(*playCardTasks("MercurianAlloys"), "MercurianAlloys FROM ProjectCard")
    doTasks(
        *playCardTasks("MarketManipulation"),
        "MarketManipulation FROM ProjectCard",
        "ColonyProduction<Titan> FROM ColonyProduction<Pluto>",
    )
    doTasks(
        *cardActionTasks("CometAiming", "Action2"),
        "-Asteroid<CometAiming>",
        "OceanTile<Cimmeria_9_7>",
        "TerraformRating",
        "Steel",
        "Plant",
        "2 MC",
    )
    draw(RegolithEaters)
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_8_7>",
        "OxygenStep",
        "Steel",
        "ProjectCard",
        "2 MC",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "5 MC")
    doTasks(*cardActionTasks("SulphurEatingBacteria"), "Microbe<SulphurEatingBacteria>", "MC")
    doTasks(*cardActionTasks("Extremophiles"), "Microbe<SulphurEatingBacteria>", "MC")
    doTasks(
        *playCardTasks("BribedCommittee"),
        "6 Pay<> FROM MC",
        "BribedCommittee FROM ProjectCard",
        "2 TerraformRating",
    )
    draw(MagneticFieldGeneratorsPromo)
    doTasks(
        *playCardTasks("MagneticShield"),
        "4 Pay<Class<Titanium>> FROM Titanium",
        "3 Pay<> FROM MC",
        "MagneticShield FROM ProjectCard",
        "4 TerraformRating",
        "ProjectCard",
    )
    draw(LocalHeatTrapping)
    discard(GiantIceAsteroid)
    doTasks(
        *playCardTasks("FloaterTechnology"),
        "6 Pay<> FROM MC",
        "FloaterTechnology FROM ProjectCard",
        "Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(InterplanetaryColonyShip, ArtificialLake)
    discard(InterplanetaryColonyShip)
    doTasks(
        *playCardTasks("AntiGravityTechnology"),
        "13 Pay<> FROM MC",
        "AntiGravityTechnology FROM ProjectCard",
        "ProjectCard FROM Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(*cardActionTasks("Hospitals"), "-Disease<Hospitals>", "5 MC")
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<AerialMappers>")
    draw(DiversitySupport)
    doTasks(
        *cardActionTasks("AerialMappers", "Action2"),
        "-Floater<AerialMappers>",
        "ProjectCard",
    )
    draw(TropicalResort)
    doTasks(
        *playCardTasks("NitrogenFromTitan"),
        "2 Pay<Class<Titanium>> FROM Titanium",
        "10 Pay<> FROM MC",
        "NitrogenFromTitan FROM ProjectCard",
        // No eligible Jovian card can hold these floaters.
        "2* Floater<CardFront(HAS JovianTag)>",
        "2 TerraformRating",
        "ProjectCard",
    )
    doTasks(*cardActionTasks("AstroDrill"), "Asteroid<AstroDrill>")
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Unity> FROM LobbyDelegate")
    doTasks(*cardActionTasks("MartianMediaCenter"), "3 Pay<> FROM MC", "PartyLeader<MarsFirst>")
    doTasks(
        *standardProjectTasks("AsteroidProject"),
        "14 Pay<> FROM MC",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        "Pass",
        "63 MC",
        "2 Titanium",
        "2 Steel",
        "7 Plant",
        "6 Heat",
        "10 Energy",
        "2 Heat FROM Energy",
    )
    doTasks("TemperatureStep! BY Admin", "12 MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[9].
    assertSidebar(gen = 11, temp = -6, oxygen = 9, oceans = 7, venus = 20)
  }

  private fun Agent.generation11() {
    draw(EnergyMarket, Shuttles)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<AerialMappers>")
    doTasks(
        *cardActionTasks("DeuteriumExport", "Action2"),
        "-Floater<DeuteriumExport>",
        "PROD[Energy]",
    )
    draw(EcologicalZone)
    doTasks(
        *cardActionTasks("AerialMappers", "Action2"),
        "-Floater<AerialMappers>",
        "ProjectCard",
    )
    doTasks(
        *playCardTasks("Shuttles"),
        "Ok", // no titanium paid
        "3 Pay<> FROM MC",
        "Shuttles FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[2 MC]",
    )
    doTasks(
        *playCardTasks("ShipmentToEarth"),
        "Pay<Class<Titanium>> FROM Titanium",
        "3 Pay<> FROM MC",
        "ShipmentToEarth FROM ProjectCard",
        "-3 Plant",
        "-3 Steel",
        "3 TerraformRating",
        "8 MC",
    )
    doTasks(
        *playCardTasks("DiversitySupport"),
        "DiversitySupport FROM ProjectCard",
        "TerraformRating",
    )
    doTasks(*playCardTasks("EnergyMarket"), "EnergyMarket FROM ProjectCard")
    draw(ImportedHydrogen)
    doTasks(
        *cardActionTasks("DevelopmentCenter"),
        "Pay<Class<Energy>> FROM Energy",
        "ProjectCard",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("Dirigibles"), "Floater<ExtractorBalloons>")
    doTasks(*cardActionTasks("ExtractorBalloons"), "Floater<ExtractorBalloons>")
    doTasks(*cardActionTasks("EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
    draw(TradingColony, SpinInducingAsteroid, LunarMining, Soletta)
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Pluto>",
        "ColonyProduction<Pluto>",
        "4 ProjectCard",
    )
    doTasks(*cardActionTasks("Extremophiles"), "Microbe<SulphurEatingBacteria>", "MC")
    doTasks(*cardActionTasks("SulphurEatingBacteria"), "Microbe<SulphurEatingBacteria>", "MC")
    doTasks(*cardActionTasks("AstroDrill", "Action2"), "-Asteroid<AstroDrill>", "3 Titanium")
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
        "OceanTile<Cimmeria_1_5>",
        "TerraformRating",
        "2 Plant",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_8_6>",
        "OxygenStep",
        "Titanium",
        "4 MC",
        "TerraformRating",
    )
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Ceres>",
        "ColonyProduction<Ceres>",
        "4 Steel",
        "2 Steel",
    )
    doTasks(
        *playCardTasks("ImportedHydrogen"),
        "Pay<Class<Titanium>> FROM Titanium",
        "2 Pay<> FROM MC",
        "ImportedHydrogen FROM ProjectCard",
        "OceanTile<Cimmeria_8_4>",
        "TerraformRating",
        "2 MC",
        "3 Microbe<SulphurEatingBacteria>",
        "3 MC",
    )
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Cimmeria_5_5>",
        "PROD[MC]",
        "Disease<Hospitals>",
        "Steel",
    )
    doTasks(*cardActionTasks("Hospitals"), "-Disease<Hospitals>", "6 MC")
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "6 MC")
    doTasks(
        *cardActionTasks("CometAiming"),
        "Pay<Class<Titanium>> FROM Titanium",
        "Asteroid<AstroDrill>",
    )
    draw(QuantumExtractor)
    doTasks(
        *playCardTasks("MagneticFieldGeneratorsPromo"),
        "8 Pay<Class<Steel>> FROM Steel",
        "3 Pay<> FROM MC",
        "MagneticFieldGeneratorsPromo FROM ProjectCard",
        "PROD[-4 Energy]",
        "PROD[2 Plant]",
        "3 TerraformRating",
        "ProjectCard",
        "MagneticFieldGeneratorsPromo_SpecialTile<Cimmeria_8_5>",
        "Plant",
        "6 MC",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Unity> FROM LobbyDelegate")
    doTasks(*cardActionTasks("MartianMediaCenter"), "3 Pay<> FROM MC", "PartyLeader<Unity>")
    // Test inference: the source gives the sale count, not card identities.
    discard(
        ArtificialLake,
        EcologicalZone,
        IceCapMelting,
        LocalHeatTrapping,
        LunarMining,
        QuantumExtractor,
        RegolithEaters,
        Soletta,
        SpinInducingAsteroid,
        TradingColony,
    )
    doTasks(*standardProjectTasks("SellPatentsProject"), "10 MC FROM ProjectCard")
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Cimmeria_4_5>",
        "OxygenStep",
        "2 Steel",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Cimmeria_9_8>",
        "OxygenStep",
        "Plant",
        "4 MC",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(
        "Pass",
        "83 MC",
        "2 Titanium",
        "2 Steel",
        "9 Plant",
        "6 Heat",
        "5 Energy",
        "2 Heat FROM Energy",
    )
    doTasks("VenusStep! BY Admin", "-9 MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration[10].
    assertSidebar(gen = 12, temp = 0, oxygen = 12, oceans = 9, venus = 26)
  }

  private fun Agent.generation12() {
    draw(TransNeptuneProbe, ProtectedValley)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    draw(MicroMills)
    doTasks(
        *cardActionTasks("DevelopmentCenter"),
        "Pay<Class<Energy>> FROM Energy",
        "ProjectCard",
    )
    draw(LunarExports, EosChasmaNationalPark, FusionPower)
    doTasks("UseAction<ScientistsPolicy, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
    doTasks(*cardActionTasks("DeuteriumExport"), "Floater<DeuteriumExport>")
    doTasks(
        *cardActionTasks("ExtractorBalloons", "Action2"),
        "-2 Floater<ExtractorBalloons>",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("AstroDrill", "Action2"), "-Asteroid<AstroDrill>", "3 Titanium")
    doTasks(*cardActionTasks("Extremophiles"), "Microbe<Extremophiles>", "MC")
    draw(GhgImportFromVenus)
    discard(FusionPower)
    doTasks(
        *playCardTasks("TransNeptuneProbe"),
        "TransNeptuneProbe FROM ProjectCard",
        "Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_6_5>",
        "OxygenStep",
        "TerraformRating",
    )
    doTasks(
        "UseAction<TradeAction, Action2>",
        "3 Pay<Class<Energy>> FROM Energy",
        "Trade<Ceres>",
        "ColonyProduction<Ceres>",
        "4 Steel",
        "2 Steel",
    )
    doTasks(
        *playCardTasks("TropicalResort"),
        "5 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "TropicalResort FROM ProjectCard",
        "PROD[-2 Heat]",
        "PROD[3 MC]",
    )
    doTasks(*cardActionTasks("EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
    draw(CloudTourism)
    doTasks(
        *playCardTasks("GhgImportFromVenus"),
        "3 Pay<Class<Titanium>> FROM Titanium",
        "0 Pay<> FROM MC",
        "GhgImportFromVenus FROM ProjectCard",
        "VenusStep",
        "PROD[3 Heat]",
        "ProjectCard",
        "TerraformRating",
    )
    draw(Capital, SponsoredAcademies)
    doTasks(
        "UseAction<TradeAction, Action3>",
        "3 Pay<Class<Titanium>> FROM Titanium",
        "Trade<Pluto>",
        "ColonyProduction<Pluto>",
        "2 ProjectCard",
    )
    doTasks(*cardActionTasks("FloaterTechnology"), "Floater<AerialMappers>")
    draw(Plantation)
    doTasks(
        *cardActionTasks("AerialMappers", "Action2"),
        "-Floater<AerialMappers>",
        "ProjectCard",
    )
    draw(Farming, Sabotage, NitrophilicMoss, SponsoredMohole, TitanAirScrapping)
    // Test inference: Sponsored Academies also requires a discard absent from the source log.
    discard(MicroMills, ProtectedValley)
    doTasks(
        *playCardTasks("SponsoredAcademies"),
        "6 Pay<> FROM MC",
        "SponsoredAcademies FROM ProjectCard",
    )
    // Resolve the mandatory discard before the optional Mars University exchange.
    doTask("-ProjectCard<Hand>!", SponsoredAcademies)
    doTasks("ProjectCard FROM Science<OlympusConference>", "3 ProjectCard", "-ProjectCard")
    doTask("ProjectCard")
    // Resolve the draw for other players; there are none in this solo game.
    doTask(tasks.extract { it }.single().id)
    draw(Mangrove)
    doTasks(
        *playCardTasks("Capital"),
        "5 Pay<Class<Steel>> FROM Steel",
        "13 Pay<> FROM MC",
        "Capital FROM ProjectCard",
        "PROD[-2 Energy]",
        "PROD[5 MC]",
        "ProjectCard",
        "CapitalTile<Cimmeria_4_6, Capital>",
        "PROD[MC]",
        "Disease<Hospitals>",
    )
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "7 MC")
    doTasks(*cardActionTasks("Hospitals"), "-Disease<Hospitals>", "7 MC")
    doTasks(
        *standardProjectTasks("AsteroidProject"),
        "14 Pay<> FROM MC",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("Farming"),
        "13 Pay<> FROM MC",
        "Farming FROM ProjectCard",
        "PROD[2 MC]",
        "PROD[2 Plant]",
        "2 Plant",
    )
    doTasks(
        *playCardTasks("Plantation"),
        "12 Pay<> FROM MC",
        "Plantation FROM ProjectCard",
        "GreeneryTile<Cimmeria_5_7>",
        "OxygenStep",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("SulphurEatingBacteria", "Action2"))
    p1.selectOnlyTask(x = 7)
    doTasks("-7 Microbe<SulphurEatingBacteria>", "21 MC")
    doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Scientists> FROM LobbyDelegate")
    doTasks(
            *playCardTasks("Mangrove"),
            "9 Pay<> FROM MC",
            "Mangrove FROM ProjectCard",
            "GreeneryTile<Cimmeria_2_6>",
            "0 OxygenStep",
            "Plant",
            "2 MC",
        )
        .expect("0 TerraformRating")
    draw(AtalantaPlanitiaLab)
    doTasks(
        *playCardTasks("TitanAirScrapping"),
        "18 Pay<> FROM MC",
        "TitanAirScrapping FROM ProjectCard",
        "ProjectCard",
    )
    doTasks(*cardActionTasks("Dirigibles"), "Floater<TitanAirScrapping>")
    doTasks(
        *playCardTasks("CeosFavoriteProject"),
        "CeosFavoriteProject FROM ProjectCard",
        "Floater<TitanAirScrapping>",
    )
    doTasks(
        *cardActionTasks("TitanAirScrapping", "Action2"),
        "-2 Floater<TitanAirScrapping>",
        "TerraformRating",
    )
    // Test inference: the source gives the sale count, not card identities.
    discard(CloudTourism, EosChasmaNationalPark, LunarExports, NitrophilicMoss)
    doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
    draw(Satellites, MiningArea, Ants)
    discard(SponsoredMohole)
    doTasks(
        *playCardTasks("AtalantaPlanitiaLab"),
        "5 Pay<> FROM MC",
        "AtalantaPlanitiaLab FROM ProjectCard",
        "2 ProjectCard",
        "Science<OlympusConference>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(Decomposers)
    doTasks(
        *playCardTasks("MiningArea"),
        "Pay<> FROM MC",
        "MiningArea FROM ProjectCard",
        "MiningArea_SpecialTile<Cimmeria_5_8>",
        "PROD[Steel]",
        "Steel",
        "ProjectCard",
    )
    // Test inference: the source gives the sale count, not card identities.
    discard(Ants, Decomposers, Sabotage, Satellites)
    doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Cimmeria_3_6>",
        "0 OxygenStep",
        "Plant",
    )
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Cimmeria_7_5>",
        "0 OxygenStep",
    )
    doTasks(
        "Pass",
        "101 MC",
        "2 Titanium",
        "3 Steel",
        "11 Plant",
        "7 Heat",
        "2 Energy",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_4_7>",
        "2 Titanium",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Cimmeria_5_4>",
        "Steel",
    )
    doTask("PassBaton")
    // Player-record evidence: globalsPerGeneration[11].
    assertSidebar(gen = 12, temp = 4, oxygen = 14, oceans = 9, venus = 30)
  }
}
