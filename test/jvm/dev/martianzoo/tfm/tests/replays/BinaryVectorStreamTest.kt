package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete source-backed solo replay: Binary Vector Stream (g57c6739abab5).
// http://newazure.local:8080/the-end?id=pf66ef32d2f75
// Source archive: _local/replays/Game20261102/.
internal class BinaryVectorStreamTest : CardTrackingFullGameTest() {
  override val globalEventDeck =
      listOf(
          "FreeAcademiaTreaty",
          "LocalTerraformingSupport",
          "ViralModificationsApproved",
          "TerraformingLobbying",
          "IndependenceMovement",
          "WorldGovernmentDirectives",
          "HeatFirstPolicy",
          "ExperimentalLifeforms",
          "ScientificConsensus",
          "ThawMining",
          "SelfSufficiencyProgram",
          "RisingAlloyDemand",
          "MartianProtectionism",
      )
  override val soloTileLocations =
      listOf("Amazonis_02_05", "Amazonis_01_04", "Amazonis_10_11", "Amazonis_10_10")

  override val config =
      GameConfig(
          """
          AmazonisMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, TurmoilExpansion, promos:cards
          Tr63SoloObjective, 1 SelectableCorporationCount
          """,
          "Pink",
      )

  override val cardArrivalOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(cn("Pink") to listOf(InterplanetaryCinematics, LakefrontResorts, PointLuna)),
          cn("PreludeCard") to
              mapOf(
                  cn("Pink") to
                      listOf(SelfSufficientSettlement, HighCircles, SpaceLanes, GreatAquifer)
              ),
      )
  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to mapOf(cn("Pink") to listOf(InterplanetaryCinematics, PointLuna)),
          cn("PreludeCard") to mapOf(cn("Pink") to listOf(SelfSufficientSettlement, SpaceLanes)),
      )

  @Test
  fun replayBinaryVectorStream() {
    startGame()
    p1.doTasks(
        "14 TerraformRating",
        "3 CorporationCard",
        "-2 CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    // Database save 0: neutral greenery/city at source spaces 06/13 and 86/87.
    p1.doTask("-5 ProjectCard<Selecting>")
    // Player-record evidence: these five offered projects are kept and played later.
    p1.draw(
        OptimalAerobraking,
        SponsoredAcademies,
        EnergyTapping,
        AerobrakedAmmoniaAsteroid,
        SubterraneanReservoir,
    )
    p1.doTasks(
        "PlayCard<Class<CorporationCard>, Class<LakefrontResorts>>",
        "LakefrontResorts FROM CorporationCard",
        "54 MC",
        "15 Pay<> FROM MC",
        "5 ProjectCard FROM ProjectCard<Selecting>",
    )
    with(p1) {
      draw(EventAnalysts)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<HighCircles>>",
          "HighCircles FROM PreludeCard",
          "TerraformRating",
      )
      doTask("SearchForCard<Pink, ReferenceFilter<Class<Ruling>>>!")
      doTasks("PartyFollower<MarsFirst>", "PartyLeader<MarsFirst>")
      doTask("PlayCard<Class<PreludeCard>, Class<GreatAquifer>>")
      doTask("GreatAquifer FROM PreludeCard")
      doTask("OceanTile<Amazonis_07_04>")
      doTask("2 Plant")
      doTask("TerraformRating")
      doTask("PROD[MC]")
      doTask("0 MC")
      draw(CorporateStronghold, EnergySaving)
      doTask("OceanTile<Amazonis_08_04>")
      doTask("2 ProjectCard")
      doTasks("MC", "2 MC")
      doTask("TerraformRating")
      doTask("PROD[MC]")
    }
    p1.turn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Reds> FROM LobbyDelegate")
      doTasks(
          *playCardTasks("EnergyTapping"),
          "3 Pay<> FROM MC",
          "EnergyTapping FROM ProjectCard",
          "PROD[-Energy<SoloOpponent>]",
          "PROD[Energy]",
      )
      doTasks(
          *playCardTasks("CorporateStronghold"),
          "11 Pay<> FROM MC",
          "CorporateStronghold FROM ProjectCard",
          "PROD[3 MC]",
          "PROD[-Energy]",
          "CityTile<Amazonis_07_03>",
          "2 Plant",
          "6 MC",
      )
      doTasks(
          *playCardTasks("OptimalAerobraking"),
          "7 Pay<> FROM MC",
          "OptimalAerobraking FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("AerobrakedAmmoniaAsteroid"),
          "26 Pay<> FROM MC",
          "AerobrakedAmmoniaAsteroid FROM ProjectCard",
          "PROD[Plant]",
          "PROD[3 Heat]",
          "3 MC",
          "3 Heat",
      )
      doTask("Ok") // No microbe card receives the optional two microbes.
      // Database save 9, after the last generation 1 action and before passing.
      assertResources(m = 4, s = 0, t = 0, p = 4, e = 0, h = 3)
      assertProduction(m = 5, s = 0, t = 0, p = 1, e = 0, h = 3)
      assertCounts(17 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("22 MC", "Plant", "3 Heat")
    p1.doTasks("OceanTile<Amazonis_10_05>! BY Admin", "PROD[MC]")
    // Database save 10, before generation 2 Research.
    p1.assertResources(m = 28, s = 0, t = 0, p = 5, e = 0, h = 6)
    p1.assertProduction(m = 6, s = 0, t = 0, p = 1, e = 0, h = 3)
    p1.assertCounts(17 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(AerialMappers, TowingAComet)
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    // Database saves 11-12 identify Energy Saving as the sold patent.
    p1.discard(EnergySaving)
    p1.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTasks(
          *playCardTasks("TowingAComet"),
          "23 Pay<> FROM MC",
          "TowingAComet FROM ProjectCard",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
          "3 MC",
          "3 Heat",
          "OceanTile<Amazonis_09_04>",
          "2 Steel",
          "Titanium",
          "6 MC",
          "TerraformRating",
          "PROD[MC]",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTasks(
          "UseAction<PaidLobbyAction, Action1>",
          "5 Pay<> FROM MC",
          "PartyLeader<Greens>",
      )
      // Database save 17, after the last generation 2 action.
      assertResources(m = 4, s = 2, t = 1, p = 7, e = 0, h = 1)
      assertProduction(m = 7, s = 0, t = 0, p = 1, e = 0, h = 3)
      assertCounts(20 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("27 MC", "Plant", "3 Heat")
    p1.doTasks("OceanTile<Amazonis_06_05>! BY Admin", "PROD[MC]", "10 MC")
    // Database save 18, before generation 3 Research.
    p1.assertResources(m = 41, s = 2, t = 1, p = 8, e = 0, h = 4)
    p1.assertProduction(m = 8, s = 0, t = 0, p = 1, e = 0, h = 3)
    p1.assertCounts(20 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(RedShips)
    p1.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_08_03>",
          "Plant",
          "10 MC",
          "OxygenStep",
          "TerraformRating",
      )
      draw(SymbioticFungus)
      doTasks(
          *playCardTasks("SubterraneanReservoir"),
          "11 Pay<> FROM MC",
          "SubterraneanReservoir FROM ProjectCard",
          "OceanTile<Amazonis_05_05>",
          "ProjectCard",
          "3 MC",
          "TerraformRating",
          "PROD[MC]",
      )
      draw(RotatorImpacts, AstraMechanica, Archaebacteria)
      // Database saves 21-22: Sponsored Academies discards Symbiotic Fungus.
      discard(SymbioticFungus)
      doTasks(
          *playCardTasks("SponsoredAcademies"),
          "9 Pay<> FROM MC",
          "SponsoredAcademies FROM ProjectCard",
      )
      doTask("-ProjectCard<Hand>!", SponsoredAcademies)
      doTask("3 ProjectCard")
      doTask("EACH Other@Player(NOT Pink) { ProjectCard<Other@Player, Hand>! }")
      // No other player draws from Sponsored Academies.
      doTasks(
          *playCardTasks("RotatorImpacts"),
          "Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "RotatorImpacts FROM ProjectCard",
      )
      doTasks(*cardActionTasks("RotatorImpacts"), "6 Pay<> FROM MC", "Asteroid<RotatorImpacts>")
      doTasks(
          *playCardTasks("Archaebacteria"),
          "6 Pay<> FROM MC",
          "Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
      )
      doTasks(
          *playCardTasks("AerialMappers"),
          "11 Pay<> FROM MC",
          "AerialMappers FROM ProjectCard",
      )
      doTasks(*cardActionTasks("AerialMappers"), "Floater<AerialMappers>")
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
      // Database save 29, after the last generation 3 action.
      assertResources(m = 5, s = 2, t = 0, p = 1, e = 0, h = 4)
      assertProduction(m = 9, s = 0, t = 0, p = 2, e = 0, h = 3)
      assertCounts(22 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("31 MC", "2 Plant", "3 Heat")
    p1.doTask("OceanTile<Amazonis_03_06>! BY Admin")
    p1.doTask("PROD[MC]")
    p1.selectTask("Plant<Pink>! / Production<Pink, Class<Plant>> MAX 5")
    p1.doTask("2 Plant")
    p1.doTask("2 Plant") // Successful Organisms also grants plants for influence.
    // Database save 30, before generation 4 Research.
    p1.assertResources(m = 37, s = 2, t = 0, p = 7, e = 0, h = 7)
    p1.assertProduction(m = 10, s = 0, t = 0, p = 2, e = 0, h = 3)
    p1.assertCounts(21 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(SupportedResearch, NitriteReducingBacteria, SpinInducingAsteroid)
    p1.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.draw(FrontierTown)
    p1.turn {
      doTasks(
          *cardActionTasks("AerialMappers", "Action2"),
          "-Floater<AerialMappers>",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("RotatorImpacts", "Action2"),
          "-Asteroid<RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*playCardTasks("EventAnalysts"), "5 Pay<> FROM MC", "EventAnalysts FROM ProjectCard")
      draw(StratosphericBirds, HydrogenToVenus)
      doTasks(
          *playCardTasks("SupportedResearch"),
          "3 Pay<> FROM MC",
          "SupportedResearch FROM ProjectCard",
          "2 ProjectCard",
      )
      doTasks(
          *playCardTasks("NitriteReducingBacteria"),
          "11 Pay<> FROM MC",
          "NitriteReducingBacteria FROM ProjectCard",
          "3 Microbe<NitriteReducingBacteria>",
      )
      doTasks(
          *cardActionTasks("NitriteReducingBacteria", "Action2"),
          "-3 Microbe<NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
      // Database save 39, after the last generation 4 action.
      assertResources(m = 9, s = 2, t = 0, p = 7, e = 0, h = 7)
      assertProduction(m = 10, s = 0, t = 0, p = 2, e = 0, h = 3)
      assertCounts(23 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("33 MC", "2 Plant", "3 Heat")
    p1.doTask("TemperatureStep! BY Admin")
    p1.doTask("Ok") // Pandemic has no payable loss at the sourced influence count.
    // Database save 40, before generation 5 Research.
    p1.assertResources(m = 44, s = 2, t = 0, p = 9, e = 0, h = 10)
    p1.assertProduction(m = 10, s = 0, t = 0, p = 2, e = 0, h = 3)
    p1.assertCounts(22 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(IshtarMining, TitaniumMine, CommercialDistrict, EarthCatapult)
    p1.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.turn {
      doTasks(*cardActionTasks("AerialMappers"), "Floater<AerialMappers>")
      doTasks(*cardActionTasks("RotatorImpacts"), "6 Pay<> FROM MC", "Asteroid<RotatorImpacts>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
      doTasks(
          *playCardTasks("FrontierTown"),
          "2 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "FrontierTown FROM ProjectCard",
      )
      doTask("FrontierTownBonus!")
      doTasks(
          "PROD[-Energy]",
          "CityTile<Amazonis_06_04>",
          "2 Plant",
          "6 MC",
          "Steel",
      )
      doTask("-FrontierTownBonus!")
      doTask("RepeatPlacementBonus<Pink, Amazonis_06_04>!")
      doTasks("2 Plant", "2 Plant")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_06_03>",
          "2 Plant",
          "3 MC",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_05>",
          "2 Energy",
          "6 MC",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("NitriteReducingBacteria"), "Microbe<NitriteReducingBacteria>")
      doTasks(
          *playCardTasks("EarthCatapult"),
          "23 Pay<> FROM MC",
          "EarthCatapult FROM ProjectCard",
      )
      doTasks(*playCardTasks("RedShips"), "RedShips FROM ProjectCard")
      doTasks(*cardActionTasks("RedShips"))
      doTask("3 MC") // Red Ships pays for the sourced three adjacent tiles.
      doTasks(
          *playCardTasks("TitaniumMine"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Pay<> FROM MC",
          "TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<MarsFirst> FROM LobbyDelegate")
      // Database save 55, after the last generation 5 action.
      assertResources(m = 2, s = 1, t = 0, p = 1, e = 2, h = 2)
      assertProduction(m = 10, s = 0, t = 1, p = 2, e = 0, h = 4)
      assertCounts(25 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("35 MC", "Titanium", "2 Plant", "4 Heat", "2 Heat FROM Energy")
    p1.doTasks("OceanTile<Amazonis_04_02>! BY Admin", "PROD[MC]", "10 MC")
    // Database save 56, before generation 6 Research.
    p1.assertResources(m = 47, s = 1, t = 1, p = 3, e = 0, h = 8)
    p1.assertProduction(m = 11, s = 0, t = 1, p = 2, e = 0, h = 4)
    p1.assertCounts(25 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(GhgProducingBacteria, MirandaResort)
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.draw(WgProject)
    p1.turn {
      doTasks(
          *cardActionTasks("AerialMappers", "Action2"),
          "-Floater<AerialMappers>",
          "ProjectCard",
      )
      doTasks(*playCardTasks("WgProject"), "7 Pay<> FROM MC", "WgProject FROM ProjectCard")
      doTask("3 PreludeCard<Selecting>")
      draw(WeatherBalloons)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<UnmiContractor>>",
          "UnmiContractor FROM PreludeCard<Selecting>",
          "ProjectCard",
          "3 TerraformRating",
          "-9 MC",
      )
      doTasks(
          *playCardTasks("MirandaResort"),
          "Ok", // Source payment uses no titanium.
          "10 Pay<> FROM MC",
          "MirandaResort FROM ProjectCard",
          "PROD[5 MC]",
      )
      doTasks(*cardActionTasks("RedShips"))
      doTask("3 MC") // Three sourced eligible city or special tiles.
      doTasks(
          *cardActionTasks("RotatorImpacts"),
          "Ok", // Pay no titanium for this Rotator Impacts action.
          "6 Pay<> FROM MC",
          "Asteroid<RotatorImpacts>",
      )
      doTasks(*cardActionTasks("NitriteReducingBacteria"), "Microbe<NitriteReducingBacteria>")
      doTasks(
          *playCardTasks("GhgProducingBacteria"),
          "6 Pay<> FROM MC",
          "GhgProducingBacteria FROM ProjectCard",
      )
      doTasks(*cardActionTasks("GhgProducingBacteria"), "Microbe<GhgProducingBacteria>")
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Scientists> FROM LobbyDelegate")
      // Database save 67, after the last generation 6 action.
      assertResources(m = 6, s = 1, t = 1, p = 3, e = 0, h = 8)
      assertProduction(m = 16, s = 0, t = 1, p = 2, e = 0, h = 4)
      assertCounts(28 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("44 MC", "Titanium", "2 Plant", "4 Heat")
    p1.doTask("VenusStep! BY Admin")
    p1.doTask("Ok") // Riots has no payable loss at the sourced influence count.
    // Database save 68, before generation 7 Research.
    p1.assertResources(m = 57, s = 1, t = 2, p = 5, e = 0, h = 12)
    p1.assertProduction(m = 16, s = 0, t = 1, p = 2, e = 0, h = 4)
    p1.assertCounts(27 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(Psychrophiles, Worms)
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.turn {
      doTasks(
          *cardActionTasks("RotatorImpacts", "Action2"),
          "-Asteroid<RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      draw(WaterImportFromEuropa)
      doTasks(
          *playCardTasks("SpinInducingAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "SpinInducingAsteroid FROM ProjectCard",
          "2 VenusStep",
          "2 TerraformRating",
          "ProjectCard",
          "3 MC",
          "3 Heat",
      )
      doTasks(
          *playCardTasks("HydrogenToVenus"),
          "9 Pay<> FROM MC",
          "HydrogenToVenus FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 MC",
          "3 Heat",
          "Floater<AerialMappers>",
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
          "PROD[Heat]",
      )
      doTasks(
          *playCardTasks("StratosphericBirds"),
          "10 Pay<> FROM MC",
          "StratosphericBirds FROM ProjectCard",
          "-Floater<AerialMappers>",
      )
      doTasks(*cardActionTasks("AerialMappers"), "Floater<AerialMappers>")
      doTasks(*cardActionTasks("StratosphericBirds"), "Animal<StratosphericBirds>")
      doTasks(*cardActionTasks("NitriteReducingBacteria"), "Microbe<NitriteReducingBacteria>")
      doTasks(*playCardTasks("Psychrophiles"), "Psychrophiles FROM ProjectCard")
      doTasks(*cardActionTasks("Psychrophiles"), "Microbe<Psychrophiles>")
      doTasks(
          *playCardTasks("AstraMechanica"),
          "5 Pay<> FROM MC",
          "AstraMechanica FROM ProjectCard",
          "ProjectCard FROM PlayedEvent<Class<TowingAComet>>",
          "ProjectCard FROM PlayedEvent<Class<AerobrakedAmmoniaAsteroid>>",
      )
      doTasks(
          *playCardTasks("AerobrakedAmmoniaAsteroid"),
          "24 Pay<> FROM MC",
          "AerobrakedAmmoniaAsteroid FROM ProjectCard",
          "PROD[Plant]",
          "PROD[3 Heat]",
          "3 MC",
          "3 Heat",
          "2 Microbe<GhgProducingBacteria>",
      )
      doTasks(
          *cardActionTasks("GhgProducingBacteria", "Action2"),
          "-2 Microbe<GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("RedShips"))
      doTask("3 MC") // Three eligible tiles in the source board.
      doTasks(
          *playCardTasks("Worms"),
          "6 Pay<> FROM MC",
          "Worms FROM ProjectCard",
          "PROD[2 Plant]",
      )
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTasks(
          *playCardTasks("IshtarMining"),
          "3 Pay<> FROM MC",
          "IshtarMining FROM ProjectCard",
          "PROD[Titanium]",
      )
      // Database save 88, after the last generation 7 action.
      assertResources(m = 0, s = 1, t = 0, p = 5, e = 0, h = 5)
      assertProduction(m = 16, s = 0, t = 2, p = 5, e = 0, h = 8)
      assertCounts(34 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("50 MC", "2 Titanium", "5 Plant", "8 Heat")
    p1.doTask("TemperatureStep! BY Admin")
    p1.doTask("-2 TerraformRating") // Revolution's sourced loss.
    // Database save 89, before generation 8 Research.
    p1.assertResources(m = 56, s = 1, t = 2, p = 10, e = 0, h = 13)
    p1.assertProduction(m = 16, s = 0, t = 2, p = 5, e = 0, h = 8)
    p1.assertCounts(32 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(Recruitment, SpaceMirrors, RadChemFactory, ImportedNitrogen)
    p1.doTasks("Ok", "12 Pay<> FROM MC", "4 ProjectCard FROM ProjectCard<Selecting>") // Keep four.
    p1.turn {
      doTasks(
          *cardActionTasks("NitriteReducingBacteria", "Action2"),
          "-3 Microbe<NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("RotatorImpacts", "Action2"),
          "-Asteroid<RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      draw(Heather)
      doTasks(
          *cardActionTasks("AerialMappers", "Action2"),
          "-Floater<AerialMappers>",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("TowingAComet"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "TowingAComet FROM ProjectCard",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
          "3 MC",
          "3 Heat",
          "OceanTile<Amazonis_02_06>",
          "2 Plant",
          "3 MC",
          "TerraformRating",
          "PROD[MC]",
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
          "PROD[Plant]",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_04>",
          "Plant",
          "10 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("Psychrophiles"), "Microbe<Psychrophiles>")
      doTasks(
          *playCardTasks("Heather"),
          "2 PayFromCard<Psychrophiles> FROM Microbe<Psychrophiles>",
          "Ok", // Card microbes and Earth Catapult cover the printed cost.
          "Heather FROM ProjectCard",
          "PROD[Plant]",
          "Plant",
      )
      draw(EarthOffice)
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_06_02>",
          "Plant",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
          "ProjectCard",
      )
      doTask("0 MC") // Source log records no ocean bonus at space 44.
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
      doTasks(
          "UseAction<PaidLobbyAction, Action1>",
          "5 Pay<> FROM MC",
          "PartyFollower<MarsFirst>",
      )
      doTasks(*playCardTasks("SpaceMirrors"), "Pay<> FROM MC", "SpaceMirrors FROM ProjectCard")
      doTasks(*cardActionTasks("SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      draw(Flooding)
      doTasks(
          *playCardTasks("WeatherBalloons"),
          "9 Pay<> FROM MC",
          "WeatherBalloons FROM ProjectCard",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("WeatherBalloons"), "Floater<WeatherBalloons>")
      doTasks(
          *playCardTasks("Flooding"),
          "5 Pay<> FROM MC",
          "Flooding FROM ProjectCard",
          "OceanTile<Amazonis_02_07>",
          "2 Titanium",
          "3 MC",
          "TerraformRating",
          "PROD[MC]",
      )
      doTasks(*cardActionTasks("StratosphericBirds"), "Animal<StratosphericBirds>")
      doTasks(*cardActionTasks("GhgProducingBacteria"), "Microbe<GhgProducingBacteria>")
      doTasks(*cardActionTasks("RedShips"))
      doTask("3 MC") // Three eligible tiles in the source board.
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_04_04>",
          "7 MC",
          "OxygenStep",
          "TerraformRating",
      )
      // Database save 112, after the last generation 8 action.
      assertResources(m = 12, s = 1, t = 2, p = 1, e = 0, h = 0)
      assertProduction(m = 18, s = 0, t = 2, p = 7, e = 1, h = 8)
      assertCounts(42 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("60 MC", "2 Titanium", "7 Plant", "Energy", "8 Heat")
    p1.doTask("TemperatureStep! BY Admin")
    p1.doTask("-8 Heat") // Global Dust Storm's sourced loss.
    p1.doTask("Ok") // Four influence cancels the event's MC loss.
    // Database save 113, before generation 9 Research.
    p1.assertResources(m = 76, s = 1, t = 4, p = 8, e = 1, h = 0)
    p1.assertProduction(m = 18, s = 0, t = 2, p = 7, e = 1, h = 8)
    p1.assertCounts(42 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(Supermarkets, LunaMetropolis, InterplanetaryTrade)
    p1.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.draw(VenusShuttles, ConvoyFromEuropa)
    p1.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_03_03>",
          "2 ProjectCard",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("Steel") // Mars First policy after the greenery placement.
      doTask("0 MC") // No adjacent ocean for Lakefront Resorts.
      doTasks(
          *playCardTasks("VenusShuttles"),
          "7 Pay<> FROM MC",
          "VenusShuttles FROM ProjectCard",
          "2 Floater<AerialMappers>",
      )
      draw(SearchForLife)
      doTasks(
          *cardActionTasks("AerialMappers", "Action2"),
          "-Floater<AerialMappers>",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("StratosphericBirds"), "Animal<StratosphericBirds>")
      doTasks(*cardActionTasks("Psychrophiles"), "Microbe<Psychrophiles>")
      doTasks(
          *cardActionTasks("NitriteReducingBacteria", "Action1"),
          "Microbe<NitriteReducingBacteria>",
      )
      doTasks(
          *cardActionTasks("GhgProducingBacteria", "Action2"),
          "-2 Microbe<GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*playCardTasks("EarthOffice"), "EarthOffice FROM ProjectCard")
      doTasks(
          *playCardTasks("LunaMetropolis"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "LunaMetropolis FROM ProjectCard",
          "PROD[7 MC]",
          "CityTile<LunaMetropolis_RemoteArea>",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_04_03>",
          "3 MC",
          "Steel",
      )
      doTasks(
          *playCardTasks("CommercialDistrict"),
          "3 Pay<Class<Steel>> FROM Steel",
          "8 Pay<> FROM MC",
          "CommercialDistrict FROM ProjectCard",
          "PROD[4 MC]",
          "PROD[-Energy]",
          "CommercialDistrict_SpecialTile<Amazonis_01_05>",
          "2 Titanium",
          "3 MC",
          "Steel",
      )
      doTasks(*cardActionTasks("RedShips"))
      doTask("5 MC")
      doTasks(*cardActionTasks("WeatherBalloons", "Action2"), "-Floater<WeatherBalloons>", "5 MC")
      doTasks(
          *cardActionTasks("RotatorImpacts", "Action1"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // Two titanium cover this Rotator Impacts action; pay no MC.
          "Asteroid<RotatorImpacts>",
      )
      doTasks(
          *playCardTasks("InterplanetaryTrade"),
          "25 Pay<> FROM MC",
          "InterplanetaryTrade FROM ProjectCard",
      )
      doTask("EACH @WildTagElection { Tag<@WildTagElection>? }")
      doTask("PROD[11 MC]")
      doTasks(*cardActionTasks("VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Greens>")
      // Database save 133, after the last generation 9 action.
      assertResources(m = 1, s = 1, t = 0, p = 0, e = 1, h = 0)
      assertProduction(m = 41, s = 0, t = 2, p = 7, e = 0, h = 8)
      assertCounts(45 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("86 MC", "2 Titanium", "7 Plant", "8 Heat", "Heat FROM Energy")
    p1.doTask("TemperatureStep! BY Admin")
    p1.doTask("-Plant") // Eco Sabotage.
    // Database save 134, before generation 10 Research.
    p1.assertResources(m = 95, s = 1, t = 2, p = 6, e = 0, h = 9)
    p1.assertProduction(m = 41, s = 0, t = 2, p = 7, e = 0, h = 8)
    p1.assertCounts(44 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(MediaArchives, InventorsGuild, AirScrappingExpedition)
    p1.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.turn {
      doTasks(
          *playCardTasks("InventorsGuild"),
          "7 Pay<> FROM MC",
          "InventorsGuild FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("InventorsGuild"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
      draw(SnowAlgae)
      doTasks(
          *cardActionTasks("AerialMappers", "Action2"),
          "-Floater<AerialMappers>",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("Psychrophiles"), "Microbe<Psychrophiles>")
      doTasks(
          *cardActionTasks("NitriteReducingBacteria", "Action1"),
          "Microbe<NitriteReducingBacteria>",
      )
      doTasks(
          *playCardTasks("ImportedNitrogen"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "12 Pay<> FROM MC",
          "ImportedNitrogen FROM ProjectCard",
          "TerraformRating",
          "4 Plant",
          "3 MC",
          "3 Heat",
          "3 Microbe<GhgProducingBacteria>",
          "2 Animal<StratosphericBirds>",
      )
      doTasks(
          *cardActionTasks("RotatorImpacts", "Action2"),
          "-Asteroid<RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("GhgProducingBacteria", "Action2"),
          "-2 Microbe<GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("AirScrappingExpedition"),
          "11 Pay<> FROM MC",
          "AirScrappingExpedition FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 Floater<AerialMappers>",
      )
      doTasks(*cardActionTasks("StratosphericBirds"), "Animal<StratosphericBirds>")
      doTasks(
          "UseAction<KelvinistsPolicy, Action1>",
          "10 Pay<> FROM MC",
          "PROD[Heat]",
          "PROD[Energy]",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Amazonis_06_06>",
          "2 Plant",
          "6 MC",
          "TerraformRating",
          "PROD[MC]",
      )
      doTasks(
          *cardActionTasks("VenusShuttles"),
          "8 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("RadChemFactory"),
          "Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "RadChemFactory FROM ProjectCard",
          "PROD[-Energy]",
          "2 TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_05_02>",
          "Plant",
          "3 MC",
      )
      doTasks(
          *playCardTasks("MediaArchives"),
          "3 Pay<> FROM MC",
          "MediaArchives FROM ProjectCard",
          "8 MC",
      )
      doTasks(*cardActionTasks("RedShips"))
      doTask("6 MC")
      doTasks(*cardActionTasks("WeatherBalloons", "Action1"), "Floater<WeatherBalloons>")
      doTasks(
          *playCardTasks("Supermarkets"),
          "7 Pay<> FROM MC",
          "Supermarkets FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTasks(
          *playCardTasks("SnowAlgae"),
          "2 PayFromCard<Psychrophiles> FROM Microbe<Psychrophiles>",
          "6 Pay<> FROM MC",
          "SnowAlgae FROM ProjectCard",
          "PROD[Plant]",
          "PROD[Heat]",
      )
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Unity> FROM LobbyDelegate")
      discard(WaterImportFromEuropa, Recruitment, ConvoyFromEuropa, SearchForLife)
      doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
      // Database save 159, after the last generation 10 action.
      assertResources(m = 9, s = 0, t = 0, p = 13, e = 0, h = 4)
      assertProduction(m = 45, s = 0, t = 2, p = 8, e = 0, h = 10)
      assertCounts(55 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("100 MC", "2 Titanium", "8 Plant", "10 Heat")
    p1.doTask("VenusStep! BY Admin")
    p1.doTask("8 MC") // Paradigm Breakdown.
    p1.doTask("Ok") // Paradigm Breakdown cannot discard cards from an empty hand.
    // Database save 160, before generation 11 Research.
    p1.assertResources(m = 125, s = 0, t = 2, p = 21, e = 0, h = 14)
    p1.assertProduction(m = 45, s = 0, t = 2, p = 8, e = 0, h = 10)
    p1.assertCounts(55 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(SolarLogistics, BigAsteroid)
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.turn {
      doTasks(
          *cardActionTasks("InventorsGuild"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
      doTasks(*cardActionTasks("Psychrophiles"), "Microbe<Psychrophiles>")
      draw(PowerSupplyConsortium)
      doTasks(
          *cardActionTasks("AerialMappers", "Action2"),
          "-Floater<AerialMappers>",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_03>",
          "Titanium",
          "7 MC", // Two from the ocean, four from Greens, one from Lakefront Resorts.
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("SolarLogistics"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_06_01>",
          "Plant",
      )
      doTask("0 MC") // No adjacent ocean.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_02>",
          "Plant",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("0 MC") // No adjacent ocean for Lakefront Resorts.
      doTask("TemperatureStep") // Oxygen's threshold raises temperature.
      doTask("TerraformRating")
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_04_05>",
          "Plant",
          "7 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_04_01>",
          "Titanium",
          "7 MC",
          "OxygenStep",
          "TerraformRating",
      )
      draw(SmallAnimals)
      doTasks(
          *playCardTasks("BigAsteroid"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "16 Pay<> FROM MC",
          "BigAsteroid FROM ProjectCard",
          "4 Titanium",
          "2 TemperatureStep",
          "2 TerraformRating",
          "3 MC",
          "3 Heat",
          "ProjectCard",
          "Ok", // Big Asteroid removes no optional plants from another player.
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
      doTasks(*cardActionTasks("StratosphericBirds"), "Animal<StratosphericBirds>")
      doTasks(*cardActionTasks("GhgProducingBacteria", "Action1"), "Microbe<GhgProducingBacteria>")
      doTasks(
          *cardActionTasks("NitriteReducingBacteria", "Action1"),
          "Microbe<NitriteReducingBacteria>",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_03_04>",
      )
      doTask("0 MC") // Lakefront Resorts finds no adjacent ocean.
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_02_02>",
      )
      doTask("PartyFollower<Greens>") // The map space grants a delegate.
      doTask("0 MC") // No adjacent ocean.
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Greens>")
      doTasks(*cardActionTasks("RedShips"))
      doTask("6 MC")
      doTasks(*cardActionTasks("WeatherBalloons", "Action2"), "-Floater<WeatherBalloons>", "9 MC")
      doTasks(*cardActionTasks("VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTasks(
          *cardActionTasks("RotatorImpacts", "Action1"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // Two titanium cover this Rotator Impacts action; pay no MC.
          "Asteroid<RotatorImpacts>",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_02_03>",
          "Steel",
          "4 MC",
      )
      doTask("0 MC") // No adjacent ocean for Lakefront Resorts.
      doTask("OxygenStep") // The plain log omits this oxygen step.
      doTask("TerraformRating")
      doTasks(
          *playCardTasks("SmallAnimals"),
          "4 Pay<> FROM MC",
          "SmallAnimals FROM ProjectCard",
          "-Production<SoloOpponent, Class<Plant>>",
      )
      doTasks(*cardActionTasks("SmallAnimals"), "Animal<SmallAnimals>")
      // Database save 188, after the last generation 11 action.
      assertResources(m = 6, s = 1, t = 2, p = 0, e = 0, h = 1)
      assertProduction(m = 48, s = 0, t = 2, p = 8, e = 0, h = 10)
      assertCounts(66 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("114 MC", "2 Titanium", "8 Plant", "10 Heat")
    p1.doTask("VenusStep! BY Admin")
    p1.doTask("Ok") // Influence cancels Mud Slides' MC loss.
    // Database save 189, before generation 12 Research.
    p1.assertResources(m = 129, s = 1, t = 4, p = 8, e = 0, h = 11)
    p1.assertProduction(m = 48, s = 0, t = 2, p = 8, e = 0, h = 10)
    p1.assertCounts(66 to "TerraformRating")
    p1.doTask("4 ProjectCard<Selecting>")
    p1.draw(OutdoorSports, NoctisFarming, Atmoscoop)
    p1.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard FROM ProjectCard<Selecting>",
    )
    p1.turn {
      doTasks(
          *cardActionTasks("InventorsGuild"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
      draw(Sabotage)
      doTasks(
          *cardActionTasks("AerialMappers", "Action2"),
          "-Floater<AerialMappers>",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("GhgProducingBacteria", "Action2"),
          "-2 Microbe<GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("Psychrophiles"), "Microbe<Psychrophiles>")
      doTasks(
          *cardActionTasks("NitriteReducingBacteria", "Action2"),
          "-3 Microbe<NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("RotatorImpacts", "Action2"),
          "-Asteroid<RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      discard(PowerSupplyConsortium, Sabotage)
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTasks(*cardActionTasks("SmallAnimals"), "Animal<SmallAnimals>")
      doTasks(*cardActionTasks("StratosphericBirds"), "Animal<StratosphericBirds>")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_01>",
          "2 Steel",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("0 MC")
      doTasks(
          *playCardTasks("NoctisFarming"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 PayFromCard<Psychrophiles> FROM Microbe<Psychrophiles>",
          "Ok", // Steel and Psychrophiles microbes cover the cost; pay no MC.
          "NoctisFarming FROM ProjectCard",
          "PROD[MC]",
          "2 Plant",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_03_02>",
          "7 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_04_06>",
          "Plant",
          "3 MC",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_04_07>",
          "2 Plant",
          "7 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_05_07>",
          "2 Plant",
          "4 MC", // The source log records no ocean bonus at space 39.
          "OxygenStep",
          "TerraformRating",
      )
      doTask("0 MC")
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_05_06>",
          "Plant",
          "10 MC",
      )
      doTask("0 OxygenStep") // Oxygen is at its Amazonis maximum.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_03_05>",
          "Plant",
          "7 MC",
      )
      doTask("0 OxygenStep") // Oxygen is at its Amazonis maximum.
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_01_03>",
          "Steel",
      )
      doTask("0 MC")
      doTasks(*cardActionTasks("RedShips"))
      doTask("7 MC")
      doTasks(
          *playCardTasks("Atmoscoop"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "Atmoscoop FROM ProjectCard",
          "2 VenusStep",
          "2 TerraformRating",
          "2 Floater<WeatherBalloons>",
      )
      doTasks(*cardActionTasks("WeatherBalloons", "Action2"), "-Floater<WeatherBalloons>", "11 MC")
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_01_02>",
          "2 Steel",
          "4 MC",
      )
      doTask("0 OxygenStep") // Oxygen is at its Amazonis maximum.
      doTask("0 MC") // No adjacent ocean for Lakefront Resorts.
      doTasks(*cardActionTasks("VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Kelvinists>")
      discard(OutdoorSports)
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      // Database save 217, after the last generation 12 action.
      assertResources(m = 1, s = 4, t = 0, p = 1, e = 0, h = 3)
      assertProduction(m = 51, s = 0, t = 2, p = 8, e = 0, h = 10)
      assertCounts(77 to "TerraformRating")
    }
    p1.doTask("Pass")
    p1.doTasks("128 MC", "2 Titanium", "8 Plant", "10 Heat")
    // Database save 218, after final production.
    p1.assertResources(m = 129, s = 4, t = 2, p = 9, e = 0, h = 13)
    p1.doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Amazonis_02_04>",
        "0 MC",
    )
    p1.doTask("PassBaton")
    // Database save 220 and player-record evidence for the 160-point solo win.
    p1.cardsHand shouldBe emptySet()
    assertCardTrackingComplete()
    p1.assertResources(m = 129, s = 4, t = 2, p = 1, e = 0, h = 13)
    p1.assertProduction(m = 51, s = 0, t = 2, p = 8, e = 0, h = 10)
    val score = Summarizer(game)
    score.net("GreeneryTile", "VictoryPoint<Pink>") shouldBe 20
    score.net("CityTile", "VictoryPoint<Pink>") shouldBe 40
    score.net("Card", "VictoryPoint<Pink>") shouldBe 18
    score.net("Delegate", "VictoryPoint<Pink>") shouldBe 5
    p1.assertCounts(77 to "TerraformRating", 160 to "VictoryPoint", 1 to "Victory")
  }
}
