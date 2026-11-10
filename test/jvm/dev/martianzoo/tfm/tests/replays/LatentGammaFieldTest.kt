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

// Complete source-log replay with documented fake/map corrections: Latent Gamma Field
// (g890264103e4b).
// https://terraforming-mars.herokuapp.com/the-end?id=p9a4445506f19
// Source archive: _local/replays/Game20261031/p9a4445506f19/.
internal class LatentGammaFieldTest : CardTrackingFullGameTest() {
  override val firstGlobalEvents =
      listOf(
          "MiningRestrictions",
          "ExperimentalLifeforms",
          "MoholeLakeGlobalEvent",
          "ThawMining",
          "RedResistance",
          "HeatFirstPolicy",
          "FreeAcademiaTreaty",
          "TerraformingLobbying",
          "PateraBoring",
          "ScientificConsensus",
          "CorporateAlliance",
          "CharismaticWgPresident",
          "BioengineeringBoom",
      )

  private val FakeNewHolland = cn("FakeNewHolland")
  override val config =
      GameConfig(
          """
 VastitasMap
 VenusNextExpansion, PreludeExpansion, Prelude2Expansion, TurmoilExpansion, promos:cards
 Tr63SoloObjective, 1 SelectableCorporationCount, FakeNewHolland
 """,
          "Pink",
      )
  // Unsupported component: user-authorized stand-in. The ocean stays on the board;
  // this card does not place a city. Missing consequences are corrected where they occur.
  override val catalog by lazy {
    object : TfmCatalog(canonicalCatalog(config)) {
      override val explicitClassDeclarations =
          super.explicitClassDeclarations +
              parseClasses(
                  """
    CLASS FakeNewHolland : AutomatedCard {
     cost = 20
     HAS MAX 1 FakeNewHolland
     HAS =1 CityTag<This>
     HAS =1 BuildingTag<This>
     This: PROD[3 MC]
     // Replay-only city scoring at the sourced location; no city tile is created.
     End: VictoryPoint / Neighbor<GreeneryTile<Anyone>, Vastitas_5_3>
    }
   """
              )
    }
  }
  override val cardArrivalOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(cn("Pink") to listOf(PharmacyUnion, LakefrontResorts, UtopiaInvest)),
          cn("PreludeCard") to
              mapOf(
                  cn("Pink") to
                      listOf(NewPartner, IndustrialComplex, PolarIndustries, MiningOperations)
              ),
      )
  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to mapOf(cn("Pink") to listOf(PharmacyUnion, UtopiaInvest)),
          cn("PreludeCard") to mapOf(cn("Pink") to listOf(NewPartner, MiningOperations)),
      )
  override val soloTileLocations =
      listOf("Vastitas_2_6", "Vastitas_3_7", "Vastitas_8_4", "Vastitas_7_4")

  @Test
  fun latentGammaField() {
    startGame()
    p1.doTasks(
        "14 TerraformRating",
        "3 CorporationCard",
        "-2 CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    p1.doTask("-5 ProjectCard<Selecting>")
    // Test inference: kept projects identified by subsequent plays without matching draws.
    p1.draw(MarsNomads, BuildingIndustries, GiantIceAsteroid, NitrophilicMoss, OptimalAerobraking)
    p1.doTasks(
        "PlayCard<Class<CorporationCard>, Class<LakefrontResorts>>",
        "LakefrontResorts FROM CorporationCard",
        "54 MC",
        "15 Pay<> FROM MC",
        "5 ProjectCard FROM ProjectCard<Selecting>",
    )
    with(p1) {
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
          "PROD[MC]",
          "PROD[Heat]",
          "PROD[Energy]",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<PolarIndustries>>",
          "PolarIndustries FROM PreludeCard",
          "PROD[2 Heat]",
          "OceanTile<Vastitas_5_6>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "0 MC",
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
    // Player-record evidence: occupied Mars spaces; area 31 retains its ocean under FakeNewHolland.
    admin.assertCounts(
        1 to "GreeneryTile<Vastitas_1_1, Pink>",
        1 to "CityTile<Vastitas_2_1, Pink>",
        1 to "GreeneryTile<Vastitas_2_2, Pink>",
        1 to "CityTile<Vastitas_2_3, Pink>",
        1 to "GreeneryTile<Vastitas_2_4, Pink>",
        1 to "GreeneryTile<Vastitas_2_5, Pink>",
        1 to "CityTile<Vastitas_2_6, SoloOpponent>",
        1 to "GreeneryTile<Vastitas_3_1, Pink>",
        1 to "MoholeArea_SpecialTile<Vastitas_3_3, Pink>",
        1 to "GreeneryTile<Vastitas_3_4, Pink>",
        1 to "CityTile<Vastitas_3_5, Pink>",
        1 to "GreeneryTile<Vastitas_3_6, Pink>",
        1 to "GreeneryTile<Vastitas_3_7, SoloOpponent>",
        1 to "CityTile<Vastitas_4_1, Pink>",
        1 to "GreeneryTile<Vastitas_4_2, Pink>",
        1 to "OceanTile<Vastitas_4_3>",
        1 to "CityTile<Vastitas_4_4, Pink>",
        1 to "GreeneryTile<Vastitas_4_5, Pink>",
        1 to "GreeneryTile<Vastitas_4_6, Pink>",
        1 to "CityTile<Vastitas_4_7, Pink>",
        1 to "GreeneryTile<Vastitas_4_8, Pink>",
        1 to "GreeneryTile<Vastitas_5_2, Pink>",
        1 to "OceanTile<Vastitas_5_3>",
        1 to "GreeneryTile<Vastitas_5_4, Pink>",
        1 to "GreeneryTile<Vastitas_5_5, Pink>",
        1 to "OceanTile<Vastitas_5_6>",
        1 to "OceanTile<Vastitas_5_7>",
        1 to "OceanTile<Vastitas_5_8>",
        1 to "GreeneryTile<Vastitas_5_9, Pink>",
        1 to "GreeneryTile<Vastitas_6_2, Pink>",
        1 to "GreeneryTile<Vastitas_6_3, Pink>",
        1 to "NomadsMarker<Vastitas_6_4, Pink>",
        1 to "OceanTile<Vastitas_6_5>",
        1 to "OceanTile<Vastitas_6_6>",
        1 to "OceanTile<Vastitas_6_7>",
        1 to "CityTile<Vastitas_6_8, Pink>",
        1 to "MiningRights_SpecialTile<Vastitas_6_9, Pink>",
        1 to "CityTile<Vastitas_7_3, Pink>",
        1 to "GreeneryTile<Vastitas_7_4, SoloOpponent>",
        1 to "OceanTile<Vastitas_7_6>",
        1 to "CapitalTile<Vastitas_7_7, Pink>",
        1 to "GreeneryTile<Vastitas_7_8, Pink>",
        1 to "GreeneryTile<Vastitas_7_9, Pink>",
        1 to "CityTile<Vastitas_8_4, SoloOpponent>",
        1 to "CityTile<Vastitas_8_6, Pink>",
        1 to "GreeneryTile<Vastitas_8_7, Pink>",
        1 to "GreeneryTile<Vastitas_8_8, Pink>",
        1 to "CityTile<Vastitas_8_9, Pink>",
        1 to "GreeneryTile<Vastitas_9_7, Pink>",
        1 to "CityTile<Vastitas_9_8, Pink>",
        1 to "GreeneryTile<Vastitas_9_9, Pink>",
        0 to "CityTile<Vastitas_5_3, Pink>",
        1 to "CityTile<StanfordTorus_RemoteArea, Pink>",
        1 to "CityTile<GanymedeColony_RemoteArea, Pink>",
        1 to "Chairman<Neutral>",
        1 to "Ruling<Kelvinists>",
        1 to "Dominant<Unity>",
        1 to "PartyLeader<Unity, Neutral>",
        2 to "PartyFollower<Unity, Neutral>",
        1 to "PartyLeader<Reds, Neutral>",
        1 to "PartyFollower<Reds, Neutral>",
        1 to "PartyFollower<Greens, Neutral>",
    )
    with(p1) {
      cardsHand shouldBe emptySet()
      assertCardTrackingComplete()
      // Player-record evidence: after final production and both final greenery placements.
      assertResources(m = 114, s = 3, t = 14, p = 0, e = 5, h = 83)
      assertProduction(m = 45, s = 3, t = 4, p = 13, e = 5, h = 21)
      // The source has 15 cities. FakeNewHolland deliberately contributes no CityTile.
      assertCounts(14 to "CityTile", 23 to "GreeneryTile", 1 to "FakeNewHolland")
      assertCounts(5 to "Animal<SmallAnimals>", 5 to "CardResource")
      val score = Summarizer(game)
      // The fake's four points stand for the missing city's four neighboring greeneries.
      score.net("FakeNewHolland", "VictoryPoint<Pink>") shouldBe 4
      score.net("CityTile", "VictoryPoint<Pink>") shouldBe 41
      score.net("Card", "VictoryPoint<Pink>") shouldBe 28
      score.net("GreeneryTile", "VictoryPoint<Pink>") shouldBe 23
      score.net("Delegate", "VictoryPoint<Pink>") shouldBe 4
      score.net("TerraformRating", "VictoryPoint<Pink>") shouldBe 63
      assertCounts(63 to "TerraformRating", 159 to "VictoryPoint", 1 to "Victory")
      assertCounts(
          1 to "PartyLeader<MarsFirst>",
          1 to "PartyLeader<Scientists>",
          1 to "PartyLeader<Greens>",
          1 to "PartyLeader<Kelvinists>",
          4 to "Delegate",
          0 to "LobbyDelegate",
      )
    }
  }

  private fun Agent.generation1() {
    // Test inference: Nomads starts at area 32 and follows 33,32,33,32,33,32,33,32,40,39,40.
    // The source omits destinations; this legal route matches every recorded bonus and final
    // marker.
    doTasks(
        *playCardTasks("MarsNomads"),
        "13 Pay<> FROM MC",
        "MarsNomads FROM ProjectCard",
        "NomadsMarker<Vastitas_5_4>",
    )
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_5_5> FROM NomadsMarker<Vastitas_5_4>",
        "TemperatureStep",
        "-1 MC",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("BuildingIndustries"),
        "6 Pay<> FROM MC",
        "BuildingIndustries FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[2 Steel]",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Unity> FROM LobbyDelegate")
    doTasks(
        "Pass",
        "18 MC",
        "Titanium",
        "3 Steel",
        "Plant",
        "3 Heat",
    )
    doTasks("OceanTile<Vastitas_6_5>! BY Admin", "PROD[MC]")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 2, temp = -28, oxygen = 0, oceans = 2, venus = 0)
  }

  private fun Agent.generation2() {
    draw(RedAppeasement, CloudSeeding)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_5_4> FROM NomadsMarker<Vastitas_5_5>",
        "2 Plant",
        "3 MC",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
    doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Greens>")
    doTasks(
        *playCardTasks("RedAppeasement"),
        "RedAppeasement FROM ProjectCard",
        "PROD[2 MC]",
        "Pass",
        "21 MC",
        "Titanium",
        "3 Steel",
        "Plant",
        "3 Heat",
    )
    // Red Appeasement ends participation in this generation; the source then logs the pass.
    doTasks("OceanTile<Vastitas_4_3>! BY Admin", "PROD[MC]", "0 Titanium", "2 Titanium")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 3, temp = -28, oxygen = 0, oceans = 3, venus = 0)
  }

  private fun Agent.generation3() {
    draw(MicroMills, MiningExpedition, MoholeArea)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_5_5> FROM NomadsMarker<Vastitas_5_4>",
        "TemperatureStep",
        "2 MC",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("MiningExpedition"),
        "12 Pay<> FROM MC",
        "MiningExpedition FROM ProjectCard",
        "OxygenStep",
        "2 Steel",
        "TerraformRating",
        "-2 Plant<SoloOpponent>",
    )
    doTasks(
        *playCardTasks("MoholeArea"),
        "8 Pay<Class<Steel>> FROM Steel",
        "4 Pay<> FROM MC",
        "MoholeArea FROM ProjectCard",
        "PROD[4 Heat]",
        "MoholeArea_SpecialTile<Vastitas_3_3>",
        "2 Plant",
        "3 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_3_4>",
        "OxygenStep",
        "4 MC",
        "2 Plant",
        "TerraformRating",
    )
    doTask("MC! / Neighbor<OceanTile, Vastitas_3_4>")
    doTasks(
        *playCardTasks("NitrophilicMoss"),
        "8 Pay<> FROM MC",
        "NitrophilicMoss FROM ProjectCard",
        "PROD[2 Plant]",
        "-2 Plant",
    )
    doTasks(
        *playCardTasks("MicroMills"),
        "3 Pay<> FROM MC",
        "MicroMills FROM ProjectCard",
        "PROD[Heat]",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
    doTasks(
        "Pass",
        "25 MC",
        "Titanium",
        "3 Steel",
        "3 Plant",
        "8 Heat",
    )
    doTasks("OceanTile<Vastitas_5_3>! BY Admin", "PROD[MC]", "-0 Plant")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 4, temp = -26, oxygen = 2, oceans = 4, venus = 0)
  }

  private fun Agent.generation4() {
    draw(PublicBaths, GiantSpaceMirror)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_5_4> FROM NomadsMarker<Vastitas_5_5>",
        "2 Plant",
        "9 MC",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
        "PROD[Heat]",
    )
    doTasks(
        *playCardTasks("OptimalAerobraking"),
        "Pay<Class<Titanium>> FROM Titanium",
        "3 Pay<> FROM MC",
        "OptimalAerobraking FROM ProjectCard",
    )
    doTasks(
        *playCardTasks("GiantIceAsteroid"),
        "4 Pay<Class<Titanium>> FROM Titanium",
        "20 Pay<> FROM MC",
        "GiantIceAsteroid FROM ProjectCard",
        "2 TemperatureStep",
        "3 MC",
        "3 Heat",
        "2 TerraformRating",
        "PROD[Heat]",
        "-6 Plant<SoloOpponent>",
        "OceanTile<Vastitas_6_6>",
        "TerraformRating",
        "PROD[MC]",
        "2 Heat",
        "Plant",
        "6 MC",
        "OceanTile<Vastitas_7_6>",
        "TerraformRating",
        "PROD[MC]",
        "2 Heat",
        "6 MC",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
    doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Reds>")
    doTasks(
        *playCardTasks("CloudSeeding"),
        "11 Pay<> FROM MC",
        "CloudSeeding FROM ProjectCard",
        "PROD[-MC]",
        "PROD[2 Plant]",
        "-Production<SoloOpponent, Class<Heat>>",
    )
    doTasks(
        "Pass",
        "33 MC",
        "Titanium",
        "3 Steel",
        "5 Plant",
        "10 Heat",
    )
    draw(SpecialDesign, Algae, NuclearPower)
    doTasks(
        "OceanTile<Vastitas_5_8>! BY Admin",
        "PROD[MC]",
        "-2* TemperatureStep BY Admin",
        "3 ProjectCard",
    )
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 5, temp = -22, oxygen = 2, oceans = 7, venus = 0)
  }

  private fun Agent.generation5() {
    draw(LocalShading, Capital, LargeConvoy)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_5_5> FROM NomadsMarker<Vastitas_5_4>",
        "TemperatureStep",
        "5 MC",
        "TerraformRating",
        "PROD[Heat]",
        "-3 MC",
    )
    doTasks(*playCardTasks("LocalShading"), "4 Pay<> FROM MC", "LocalShading FROM ProjectCard")
    doTasks(*cardActionTasks("LocalShading"), "Floater<LocalShading>")
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
    doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Greens>")
    doTasks(
        *playCardTasks("Algae"),
        "10 Pay<> FROM MC",
        "Algae FROM ProjectCard",
        "Plant",
        "PROD[2 Plant]",
    )
    doTasks(
        "Pass",
        "35 MC",
        "Titanium",
        "3 Steel",
        "7 Plant",
        "11 Heat",
    )
    doTasks("VenusStep! BY Admin", "-0* MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 6, temp = -20, oxygen = 2, oceans = 7, venus = 2)
  }

  private fun Agent.generation6() {
    draw(SupportedResearch)
    doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    draw(SpaceHotels, SpaceMirrors, ExtremeColdFungus)
    doTasks(
        *playCardTasks("LargeConvoy"),
        "2 Pay<Class<Titanium>> FROM Titanium",
        "30 Pay<> FROM MC",
        "LargeConvoy FROM ProjectCard",
        "2 ProjectCard",
        "3 MC",
        "3 Heat",
        "5 Plant",
        "OceanTile<Vastitas_6_7>",
        "TerraformRating",
        "PROD[MC]",
        "ProjectCard",
        "6 MC",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
    doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Scientists>")
    draw(MarsUniversity, CometForVenus)
    doTasks(
        *playCardTasks("SupportedResearch"),
        "3 Pay<> FROM MC",
        "SupportedResearch FROM ProjectCard",
        "2 ProjectCard",
    )
    draw(ElectroCatapult)
    discard(SpaceMirrors)
    doTasks(
        *playCardTasks("MarsUniversity"),
        "4 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "MarsUniversity FROM ProjectCard",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_5_4> FROM NomadsMarker<Vastitas_5_5>",
        "2 Plant",
        "9 MC",
    )
    doTasks(
        *playCardTasks("GiantSpaceMirror"),
        "17 Pay<> FROM MC",
        "GiantSpaceMirror FROM ProjectCard",
        "PROD[3 Energy]",
    )
    doTasks(
        *playCardTasks("ElectroCatapult"),
        "5 Pay<Class<Steel>> FROM Steel",
        "7 Pay<> FROM MC",
        "ElectroCatapult FROM ProjectCard",
        "PROD[-Energy]",
    )
    // Test inference: plant cost; all seven actions are constrained by subsequent steel
    // payments.
    doTasks(*cardActionTasks("ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
    doTasks(*cardActionTasks("LocalShading", "Action2"), "-Floater<LocalShading>", "PROD[MC]")
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
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("ExtremeColdFungus"),
        "13 Pay<> FROM MC",
        "ExtremeColdFungus FROM ProjectCard",
    )
    doTasks(*cardActionTasks("ExtremeColdFungus"), "Plant")
    doTasks(
        "Pass",
        "40 MC",
        "Titanium",
        "3 Steel",
        "7 Plant",
        "11 Heat",
        "2 Energy",
    )
    doTasks("VenusStep! BY Admin", "4 MC")
    p1.doTask("2! MC / EarthTag")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 7, temp = -14, oxygen = 2, oceans = 8, venus = 4)
  }

  private fun Agent.generation7() {
    draw(MiningRights, AsteroidRights, LavaTubeSettlement)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(*cardActionTasks("ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_5_5> FROM NomadsMarker<Vastitas_5_4>",
        "TemperatureStep",
        "5 MC",
        "TerraformRating",
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
    doTasks(
        *playCardTasks("MiningRights"),
        "3 Pay<Class<Steel>> FROM Steel",
        "3 Pay<> FROM MC",
        "MiningRights FROM ProjectCard",
        "MiningRights_SpecialTile<Vastitas_6_9>",
        "PROD[Titanium]",
        "2 Titanium",
        "3 MC",
    )
    draw(Research, Psychrophiles)
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_5_9>",
        "OxygenStep",
        "4 MC",
        "2 ProjectCard",
        "3 MC",
        "TerraformRating",
    )
    draw(Casinos, TitaniumMine, EnvoysFromVenus, MartianRails)
    discard(Psychrophiles, EnvoysFromVenus)
    doTasks(
        *playCardTasks("Research"),
        "11 Pay<> FROM MC",
        "Research FROM ProjectCard",
        "2 ProjectCard",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTask("-ProjectCard")
    doTask("ProjectCard")
    doTasks(
        *playCardTasks("LavaTubeSettlement"),
        "15 Pay<> FROM MC",
        "LavaTubeSettlement FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[2 MC]",
        "NormalCityTile<Vastitas_7_3>",
        "Titanium",
        "0 MC",
    )
    draw(GhgFactories, StanfordTorus)
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_6_2>",
        "OxygenStep",
        "4 MC",
        "2 ProjectCard",
        "TerraformRating",
    )
    doTask("MC! / Neighbor<OceanTile, Vastitas_6_2>")
    doTasks(
        *playCardTasks("AsteroidRights"),
        "3 Pay<Class<Titanium>> FROM Titanium",
        "Pay<> FROM MC",
        "AsteroidRights FROM ProjectCard",
        "2 Asteroid<AsteroidRights>",
    )
    doTasks(
        *cardActionTasks("AsteroidRights", "Action2"),
        "-Asteroid<AsteroidRights>",
        "2 Titanium",
    )
    doTasks(*cardActionTasks("LocalShading"), "Floater<LocalShading>")
    doTasks(*cardActionTasks("ExtremeColdFungus"), "Plant")
    doTasks(
        *playCardTasks("Casinos"),
        "5 Pay<> FROM MC",
        "Casinos FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[4 MC]",
    )
    doTasks(
        *playCardTasks("NuclearPower"),
        "10 Pay<> FROM MC",
        "NuclearPower FROM ProjectCard",
        "PROD[-2 MC]",
        "PROD[3 Energy]",
    )
    doTasks(
        *playCardTasks("Capital"),
        "26 Pay<> FROM MC",
        "Capital FROM ProjectCard",
        "PROD[-2 Energy]",
        "PROD[5 MC]",
        "CapitalTile<Vastitas_7_7, Capital>",
        "2 Plant",
        "9 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_8_7>",
        "OxygenStep",
        "4 MC",
        "Plant",
        "Steel",
        "3 MC",
        "TerraformRating",
    )
    draw(ImportOfAdvancedGhg)
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_9_7>",
        "OxygenStep",
        "4 MC",
        "ProjectCard",
        "TerraformRating",
    )
    doTask("MC! / Neighbor<OceanTile, Vastitas_9_7>")
    doTasks(
        *playCardTasks("MartianRails"),
        "Pay<Class<Steel>> FROM Steel",
        "11 Pay<> FROM MC",
        "MartianRails FROM ProjectCard",
    )
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "4 MC")
    doTasks(
        *playCardTasks("TitaniumMine"),
        "7 Pay<> FROM MC",
        "TitaniumMine FROM ProjectCard",
        "PROD[Titanium]",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<MarsFirst> FROM LobbyDelegate")
    doTasks(
        "Pass",
        "56 MC",
        "3 Titanium",
        "3 Steel",
        "7 Plant",
        "11 Heat",
        "Heat FROM Energy",
        "Energy",
    )
    doTasks("VenusStep! BY Admin", "-12 Heat", "-4 MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 8, temp = -8, oxygen = 6, oceans = 8, venus = 6)
  }

  private fun Agent.generation8() {
    draw(Potatoes, AsteroidMiningConsortium, FakeNewHolland)
    doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    draw(SpecialPermit, DomedCrater, CarbonNanosystems)
    doTasks("UseAction<ScientistsPolicy, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_5_4> FROM NomadsMarker<Vastitas_5_5>",
        "2 Plant",
        "9 MC",
    )
    draw(AdvancedAlloys)
    discard(SpaceHotels)
    // Source explicitly preserves steel for later payments.

    doTasks(
        *playCardTasks("CarbonNanosystems"),
        "Ok", // no steel paid
        "14 Pay<> FROM MC",
        "CarbonNanosystems FROM ProjectCard",
        "Graphene<CarbonNanosystems>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(SmallAnimals)
    discard(Potatoes)
    doTasks(
        *playCardTasks("AdvancedAlloys"),
        "9 Pay<> FROM MC",
        "AdvancedAlloys FROM ProjectCard",
        "Graphene<CarbonNanosystems>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(BigAsteroid)
    doTasks(
        *playCardTasks("CometForVenus"),
        "2 Pay<Class<Titanium>> FROM Titanium",
        "Ok", // no graphene paid
        "3 Pay<> FROM MC",
        "CometForVenus FROM ProjectCard",
        "VenusStep",
        "3 MC",
        "3 Heat",
        "TerraformRating",
        "ProjectCard",
        "Ok", // decline Comet for Venus's MC attack
    )
    doTasks(
        *playCardTasks("ImportOfAdvancedGhg"),
        "2 Pay<Class<Titanium>> FROM Titanium",
        "Ok", // no graphene paid
        "Pay<> FROM MC",
        "ImportOfAdvancedGhg FROM ProjectCard",
        "PROD[2 Heat]",
        "3 MC",
        "3 Heat",
    )
    doTasks(
        *playCardTasks("GhgFactories"),
        "3 Pay<Class<Steel>> FROM Steel",
        "2 Pay<> FROM MC",
        "GhgFactories FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[4 Heat]",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Unity> FROM LobbyDelegate")
    doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Unity>")
    doTasks(*cardActionTasks("ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
    doTasks(
        *cardActionTasks("AsteroidRights", "Action2"),
        "-Asteroid<AsteroidRights>",
        "2 Titanium",
    )
    doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "4 MC")
    doTasks(*cardActionTasks("LocalShading", "Action2"), "-Floater<LocalShading>", "PROD[MC]")
    doTasks(*cardActionTasks("ExtremeColdFungus"), "Plant")
    doTasks(
        *playCardTasks("SmallAnimals"),
        "6 Pay<> FROM MC",
        "SmallAnimals FROM ProjectCard",
        "-Production<SoloOpponent, Class<Plant>>",
    )
    doTasks(*cardActionTasks("SmallAnimals"), "Animal<SmallAnimals>")
    doTasks(
        *playCardTasks("AsteroidMiningConsortium"),
        "13 Pay<> FROM MC",
        "AsteroidMiningConsortium FROM ProjectCard",
        "PROD[Titanium]",
        "-Production<SoloOpponent, Class<Titanium>>",
    )
    doTasks(
        "Pass",
        "58 MC",
        "4 Titanium",
        "3 Steel",
        "7 Plant",
        "17 Heat",
        "Energy",
    )
    doTasks("VenusStep! BY Admin", "10 MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 9, temp = -8, oxygen = 6, oceans = 8, venus = 10)
  }

  private fun Agent.generation9() {
    draw(ProtectedHabitats, PowerSupplyConsortium, Greenhouses, Shuttles)
    doTasks(
        "Ok", // keep all four research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *playCardTasks("Shuttles"),
        "2 Pay<Class<Titanium>> FROM Titanium",
        "Ok", // no graphene paid
        "0 Pay<> FROM MC",
        "Shuttles FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[2 MC]",
    )
    doTasks(
        *playCardTasks("BigAsteroid"),
        "5 Pay<Class<Titanium>> FROM Titanium",
        "Ok", // no graphene paid
        "0 Pay<> FROM MC",
        "BigAsteroid FROM ProjectCard",
        "2 TemperatureStep",
    )
    doTasks(
        "4 Titanium",
        "3 MC",
        "3 Heat",
        "TerraformRating",
        "TerraformRating",
    )
    doTasks(
        "-4 Plant<SoloOpponent>",
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
        "OceanTile<Vastitas_5_7>",
        "TerraformRating",
        "PROD[MC]",
        "2 Plant",
        "9 MC",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_6_4> FROM NomadsMarker<Vastitas_5_4>",
        "Plant",
        "6 MC",
    )
    doTasks(
        *playCardTasks("PowerSupplyConsortium"),
        "5 Pay<> FROM MC",
        "PowerSupplyConsortium FROM ProjectCard",
        "-Production<SoloOpponent, Class<Energy>>",
        "PROD[Energy]",
    )
    doTasks(*cardActionTasks("ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
    doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Greens>")
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Vastitas_3_5>",
        "Plant",
        "0 MC",
    )
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Vastitas_9_8>",
        "Titanium",
        "0 MC",
    )
    // Source map variant: area 62 gives steel; our Vastitas map gives titanium.
    exMachina("Steel, -Titanium")
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Vastitas_4_4>",
        "Plant",
        "3 MC",
    )
    doTasks(
        *playCardTasks("StanfordTorus"),
        "2 Pay<Class<Titanium>> FROM Titanium",
        "Ok", // no graphene paid
        "0 Pay<> FROM MC",
        "StanfordTorus FROM ProjectCard",
        "NormalCityTile<StanfordTorus_RemoteArea>",
    )
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "7 MC")
    doTasks(*cardActionTasks("LocalShading"), "Floater<LocalShading>")
    doTasks(*cardActionTasks("AsteroidRights"), "Pay<> FROM MC", "Asteroid<AsteroidRights>")
    doTasks(*cardActionTasks("SmallAnimals"), "Animal<SmallAnimals>")
    doTasks(*cardActionTasks("ExtremeColdFungus"), "Plant")
    // The source has seven Mars cities before New Holland, satisfying its printed four-city
    // requirement.
    admin.assertCounts(7 to "CityTile<MarsArea, Anyone>")
    doTasks(
        *playCardTasks("FakeNewHolland"),
        "2 PayFromCard<CarbonNanosystems> FROM Graphene<CarbonNanosystems>",
        "4 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "FakeNewHolland FROM ProjectCard",
        "PROD[3 MC]",
    )
    // Unsupported component: source area 31 remains an ocean; collect its recorded adjacency
    // reward.
    exMachina("3 MC")
    doTasks(
        "Pass",
        "73 MC",
        "4 Titanium",
        "3 Steel",
        "7 Plant",
        "17 Heat",
        "Energy",
    )
    doTasks("VenusStep! BY Admin", "16 MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 10, temp = 2, oxygen = 6, oceans = 9, venus = 12)
  }

  private fun Agent.generation10() {
    draw(FusionPower)
    doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_6_3> FROM NomadsMarker<Vastitas_6_4>",
        "3 MC",
    )
    draw(IceCapMelting)
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_4_6>",
        "OxygenStep",
        "4 MC",
        "ProjectCard",
        "6 MC",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_8_8>",
        "OxygenStep",
        "4 MC",
        "Steel",
        "TerraformRating",
        "TemperatureStep",
        "TerraformRating",
    )
    doTask("MC! / Neighbor<OceanTile, Vastitas_8_8>")
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_5_5>",
        "OxygenStep",
        "4 MC",
        "TemperatureStep",
        "5 MC",
        "2 TerraformRating",
    )
    draw(AcquiredCompany)
    discard(IceCapMelting)
    doTasks(
        *playCardTasks("SpecialDesign"),
        "4 Pay<> FROM MC",
        "SpecialDesign FROM ProjectCard",
        "SpecialDesign_NextCardEffect",
        "Graphene<CarbonNanosystems>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(
        *playCardTasks("DomedCrater"),
        "PayFromCard<CarbonNanosystems> FROM Graphene<CarbonNanosystems>",
        "4 Pay<Class<Steel>> FROM Steel",
        "8 Pay<> FROM MC",
        "DomedCrater FROM ProjectCard",
        "3 Plant",
        "PROD[-Energy]",
        "PROD[3 MC]",
        "NormalCityTile<Vastitas_8_9>",
        "Plant",
        "0 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_5_4>",
        "OxygenStep",
        "4 MC",
        "2 Plant",
        "9 MC",
        "TerraformRating",
    )
    doTasks(
        *playCardTasks("SpecialPermit"),
        "5 Pay<> FROM MC",
        "SpecialPermit FROM ProjectCard",
        "4 Plant FROM Plant<SoloOpponent>",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_7_8>",
        "OxygenStep",
        "4 MC",
        "Plant",
        "3 MC",
        "TerraformRating",
    )
    doTasks(
        "UseAction<ConvertHeatAction, Action1>",
        "8 Pay<Class<Heat>> FROM Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
    doTasks(
        *cardActionTasks("AsteroidRights", "Action2"),
        "-Asteroid<AsteroidRights>",
        "PROD[MC]",
    )
    doTasks(*cardActionTasks("LocalShading", "Action2"), "-Floater<LocalShading>", "PROD[MC]")
    draw(QuantumExtractor)
    discard(ProtectedHabitats)
    doTasks(
        *playCardTasks("FusionPower"),
        "14 Pay<> FROM MC",
        "FusionPower FROM ProjectCard",
        "PROD[3 Energy]",
        "Graphene<CarbonNanosystems>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(SolarLogistics)
    discard(AcquiredCompany)
    doTasks(
        *playCardTasks("QuantumExtractor"),
        "13 Pay<> FROM MC",
        "QuantumExtractor FROM ProjectCard",
        "PROD[4 Energy]",
        "Graphene<CarbonNanosystems>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    doTasks(
        *playCardTasks("SolarLogistics"),
        "4 Pay<Class<Titanium>> FROM Titanium",
        "Ok", // no graphene paid
        "0 Pay<> FROM MC",
        "SolarLogistics FROM ProjectCard",
        "2 Titanium",
    )
    doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
    doTasks(
        "UseAction<PaidLobbyAction, Action1>",
        "5 Pay<> FROM MC",
        "PartyFollower<Scientists>",
    )
    doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Scientists>")
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "8 MC")
    // Unsupported component: include New Holland in the source city count.
    exMachina("MC")
    doTasks(*cardActionTasks("SmallAnimals"), "Animal<SmallAnimals>")
    doTasks(*cardActionTasks("ExtremeColdFungus"), "Plant")
    doTasks(
        "Pass",
        "86 MC",
        "4 Titanium",
        "3 Steel",
        "7 Plant",
        "17 Heat",
        "7 Energy",
    )
    doTasks("VenusStep! BY Admin", "PROD[3 Heat]")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 11, temp = 8, oxygen = 11, oceans = 9, venus = 14)
  }

  private fun Agent.generation11() {
    draw(EquatorialMagnetizer, GanymedeColony)
    doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    draw(KelpFarming, FueledGenerators, ResearchCoordination)
    doTasks("UseAction<ScientistsPolicy, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
        "TerraformRating",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Vastitas_4_1>",
        "2 Steel",
        "0 MC",
    )
    draw(AiCentral)
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_3_1>",
        "OxygenStep",
        "ProjectCard",
        "0 MC",
        "TerraformRating",
    )
    draw(CorroderSuits)
    discard(FueledGenerators)
    doTasks(
        *playCardTasks("AiCentral"),
        "5 Pay<Class<Steel>> FROM Steel",
        "6 Pay<> FROM MC",
        "AiCentral FROM ProjectCard",
        "PROD[-Energy]",
        "Graphene<CarbonNanosystems>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(Soletta, HiredRaiders)
    doTasks(*cardActionTasks("AiCentral"), "2 ProjectCard")
    // Test inference: two stolen steel are required by the subsequent payments and final stock.
    doTasks(
        *playCardTasks("HiredRaiders"),
        "Pay<> FROM MC",
        "HiredRaiders FROM ProjectCard",
        "2 Steel FROM Steel<SoloOpponent>",
    )
    doTasks(*cardActionTasks("ExtremeColdFungus"), "Plant")
    doTasks(*cardActionTasks("SmallAnimals"), "Animal<SmallAnimals>")
    doTasks(
        *cardActionTasks("MarsNomads"),
        "NomadsMarker<Vastitas_6_4> FROM NomadsMarker<Vastitas_6_3>",
        "Plant",
        "6 MC",
    )
    doTasks(*cardActionTasks("ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
    doTasks(*cardActionTasks("LocalShading"), "Floater<LocalShading>")
    doTasks(
        *playCardTasks("GanymedeColony"),
        "4 Pay<Class<Titanium>> FROM Titanium",
        "Ok", // no graphene paid
        "0 Pay<> FROM MC",
        "GanymedeColony FROM ProjectCard",
        "NormalCityTile<GanymedeColony_RemoteArea>",
    )
    doTasks(
        *playCardTasks("KelpFarming"),
        "17 Pay<> FROM MC",
        "KelpFarming FROM ProjectCard",
        "PROD[2 MC]",
        "PROD[3 Plant]",
        "2 Plant",
    )
    // Test inference: the source gives the sale count, not card identities.
    discard(CorroderSuits, ResearchCoordination, Soletta)
    doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
    doTasks(
        *playCardTasks("EquatorialMagnetizer"),
        "2 Pay<Class<Steel>> FROM Steel",
        "5 Pay<> FROM MC",
        "EquatorialMagnetizer FROM ProjectCard",
    )
    doTasks(*cardActionTasks("EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
    doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
    doTasks(
        *playCardTasks("PublicBaths"),
        "6 Pay<> FROM MC",
        "PublicBaths FROM ProjectCard",
        "6 MC",
    )
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Vastitas_4_7>",
        "Plant",
        "6 MC",
    )
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Vastitas_4_8>",
        "OxygenStep",
        "3 MC",
        "TerraformRating",
        "PartyLeader<Scientists>",
    )
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Vastitas_8_6>",
        "Plant",
        "3 MC",
    )
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Vastitas_2_1>",
        "2 Plant",
        "0 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_4_5>",
        "OxygenStep",
        "3 MC",
        "TerraformRating",
    )
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "12 MC")
    // Unsupported component: include New Holland in the source city count.
    exMachina("MC")
    doTasks(
        *playCardTasks("Greenhouses"),
        "6 Pay<> FROM MC",
        "Greenhouses FROM ProjectCard",
        "14 Plant",
    )
    // Unsupported component: New Holland is also a city for Greenhouses.
    exMachina("Plant")
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_4_2>",
        "0 OxygenStep",
        "Titanium",
        "6 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_5_2>",
        "0 OxygenStep",
        "3 MC",
    )
    doTasks(
        "Pass",
        "98 MC",
        "4 Titanium",
        "3 Steel",
        "10 Plant",
        "20 Heat",
        "5 Energy",
        "6 Heat FROM Energy",
    )
    // No cards remain for Paradigm Breakdown to discard.
    doTasks("VenusStep! BY Admin", "-0 ProjectCard", "2 MC")
    p1.doTask("4 ProjectCard<Selecting>")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 12, temp = 8, oxygen = 14, oceans = 9, venus = 18)
  }

  private fun Agent.generation12() {
    draw(Trees)
    doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    draw(LunarBeam, InventionContest)
    doTasks(*cardActionTasks("AiCentral"), "2 ProjectCard")
    draw(TransNeptuneProbe, GhgShipment)
    discard(LunarBeam)
    doTasks(
        *playCardTasks("InventionContest"),
        "2 Pay<> FROM MC",
        "InventionContest FROM ProjectCard",
        "3 ProjectCard<Selecting>",
        "ProjectCard FROM ProjectCard<Selecting>",
        "Graphene<CarbonNanosystems>",
        "-ProjectCard",
    )
    doTask("ProjectCard")
    draw(FieldCappedCity)
    doTasks(
        *playCardTasks("GhgShipment"),
        "GhgShipment FROM ProjectCard",
        "PROD[Heat]",
        "Heat",
        "3 MC",
        "3 Heat",
        "ProjectCard",
    )
    doTasks(
        *playCardTasks("TransNeptuneProbe"),
        "Pay<Class<Titanium>> FROM Titanium",
        "Ok", // no graphene paid
        "0 Pay<> FROM MC",
        "TransNeptuneProbe FROM ProjectCard",
        "Graphene<CarbonNanosystems>",
        "Ok", // decline the Mars University exchange
    )
    doTasks(
        *playCardTasks("FieldCappedCity"),
        "5 PayFromCard<CarbonNanosystems> FROM Graphene<CarbonNanosystems>",
        "3 Pay<Class<Steel>> FROM Steel",
        "0 Pay<> FROM MC",
        "FieldCappedCity FROM ProjectCard",
        "PROD[2 MC]",
        "PROD[Energy]",
        "3 Plant",
        "NormalCityTile<Vastitas_6_8>",
        "Plant",
        "9 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_6_3>",
        "0 OxygenStep",
        "3 MC",
    )
    doTasks(*cardActionTasks("LocalShading", "Action2"), "-Floater<LocalShading>", "PROD[MC]")
    doTasks(*cardActionTasks("EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
    doTasks(*cardActionTasks("ExtremeColdFungus"), "Plant")
    doTasks(*cardActionTasks("ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
    doTasks(*cardActionTasks("SmallAnimals"), "Animal<SmallAnimals>")
    doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Kelvinists> FROM LobbyDelegate")
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Vastitas_2_2>",
        "0 OxygenStep",
        "2 Plant",
        "0 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_1_1>",
        "0 OxygenStep",
        "Plant",
        "0 MC",
    )
    doTasks(
        *playCardTasks("Trees"),
        "13 Pay<> FROM MC",
        "Trees FROM ProjectCard",
        "PROD[3 Plant]",
        "Plant",
    )
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Vastitas_2_5>",
        "0 OxygenStep",
        "Plant",
        "0 MC",
    )
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Vastitas_3_6>",
        "0 OxygenStep",
        "0 MC",
    )
    doTasks(
        *standardProjectTasks("CityProject"),
        "25 Pay<> FROM MC",
        "PROD[MC]",
        "NormalCityTile<Vastitas_2_3>",
        "0 MC",
    )
    doTasks(*cardActionTasks("MartianRails"), "Pay<Class<Energy>> FROM Energy", "14 MC")
    // Unsupported component: include New Holland in the source city count.
    exMachina("MC")
    doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Vastitas_9_9>",
        "0 OxygenStep",
        "Steel",
        "0 MC",
    )
    // Source map variant: area 63 gives titanium; our Vastitas map gives steel.
    exMachina("Titanium, -Steel")
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
        "TerraformRating",
    )
    doTasks(
        *standardProjectTasks("AirScrappingProject"),
        "15 Pay<> FROM MC",
        "VenusStep",
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
        "108 MC",
        "4 Titanium",
        "3 Steel",
        "13 Plant",
        "21 Heat",
        "5 Energy",
        "4 Heat FROM Energy",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_2_4>",
        "0 MC",
    )
    doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Vastitas_7_9>",
        "0 MC",
    )
    doTask("PassBaton")
    // Player-record evidence: globalsPerGeneration.
    assertSidebar(gen = 12, temp = 8, oxygen = 14, oceans = 9, venus = 30)
  }
}
