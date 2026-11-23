package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.fake.FakeCanon
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

// Complete database replay: Static Pressure Stream (g1212dda978b8)
// Source: _local/replays/Game20260912/Heroku-g1212dda978b8/
// http://newazure.local:8080/the-end?id=pe1c5cff0c611
internal class StaticPressureStreamTest : CardTrackingFullGameTest() {
  override val config =
      GameConfig(
          """
          TharsisMap
          VenusNextExpansion, PreludeExpansion, prelude2:cards, promos:cards
          FakeFloydContinuum, ResearchCoordination, ResearchNetwork

          Fundraiser, Philantropist, Planetologist, Sponsor, Briber, Terran
          Scientist, Landlord, Industrialist, Benefactor, Forecaster, Excentric
          """,
          "Nor",
          "Vin",
      )
  override val catalog = TfmCatalog(Canon, FakeCanon)
  override val cardArrivalOrder =
      mapOf(
          // Source save 0: Nor also saw Mons Insurance and Point Luna; Vin also saw CrediCor
          // and Sagitta Frontier Services.
          cn("CorporationCard") to
              mapOf(
                  cn("Nor") to listOf(Ecoline, MiningGuild),
                  cn("Vin") to listOf(CheungShingMars, Viron),
              ),
          cn("PreludeCard") to
              mapOf(
                  cn("Nor") to listOf(ProjectEden, EcologyExperts, AlbedoPlants, AppliedScience),
                  cn("Vin") to listOf(GalileanMining, Supplier, ResearchNetwork, FloatingTradeHub),
              ),
      )
  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(cn("Nor") to listOf(MiningGuild), cn("Vin") to listOf(Viron)),
          cn("PreludeCard") to
              mapOf(
                  cn("Nor") to listOf(EcologyExperts, AppliedScience),
                  cn("Vin") to listOf(GalileanMining, FloatingTradeHub),
              ),
      )

  // Source save 0 and the final Research drafts in saves 17, 46, 74, 116, 159, 200, 245,
  // 292, 356, and 416 identify every modeled ProjectCard Selecting offer in this replay.
  override val selectingArrivalOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("Nor") to
                      listOf(
                          // Setup
                          Insulation,
                          DesignedMicroorganisms,
                          ResearchOutpost,
                          RedShips,
                          UndergroundCity,
                          ExtractorBalloons,
                          Supermarkets,
                          FoodFactory,
                          AquiferPumping,
                          PowerSupplyConsortium,
                          // Generation 2
                          EquatorialMagnetizer,
                          AerialMappers,
                          AerosportTournament,
                          StaticHarvesting,
                          // Generation 3
                          Research,
                          LocalShading,
                          PublicPlans,
                          Satellites,
                          // Generation 4
                          StratosphericExpedition,
                          Comet,
                          ResearchCoordination,
                          AdaptationTechnology,
                          // Generation 5
                          RobotPollinators,
                          InterplanetaryTrade,
                          Plantation,
                          Shuttles,
                          // Generation 6
                          IshtarMining,
                          Greenhouses,
                          LavaFlows,
                          Zeppelins,
                          // Generation 7
                          ProjectInspection,
                          VestaShipyard,
                          VenusSoils,
                          HermeticOrderOfMars,
                          // Generation 8
                          Trees,
                          MoholeArea,
                          RoverConstruction,
                          IceCapMelting,
                          // Generation 9
                          IndenturedWorkers,
                          GiantSolarShade,
                          SelfReplicatingRobots,
                          DomedCrater,
                          // Generation 10
                          FakeFloydContinuum,
                          RadSuits,
                          DeepWellHeating,
                          LandClaim,
                          // Generation 11
                          OlympusConference,
                          GhgProducingBacteria,
                          FloatingHabs,
                          ColonizerTrainingCamp,
                      ),
                  cn("Vin") to
                      listOf(
                          // Setup
                          Decomposers,
                          CarbonateProcessing,
                          StJosephOfCupertinoMission,
                          OptimalAerobraking,
                          CloudTourism,
                          FuelFactory,
                          BreathingFilters,
                          IoSulphurResearch,
                          OrbitalCleanup,
                          Tardigrades,
                          // Generation 2
                          VenusShuttles,
                          LightningHarvest,
                          Omnicourt,
                          OpenCity,
                          // Generation 3
                          RoboticWorkforce,
                          SoilEnrichment,
                          SmallAnimals,
                          Meltworks,
                          // Generation 4
                          NuclearZone,
                          MarsUniversity,
                          SpinInducingAsteroid,
                          Bushes,
                          // Generation 5
                          Cartel,
                          MethaneFromTitan,
                          CloudSeeding,
                          LuxuryFoods,
                          // Generation 6
                          InterstellarColonyShip,
                          SolarLogistics,
                          Sabotage,
                          NaturalPreserve,
                          // Generation 7
                          Thermophiles,
                          GeneRepair,
                          MagneticFieldGeneratorsPromo,
                          OreProcessor,
                          // Generation 8
                          VenusianAnimals,
                          TundraFarming,
                          PowerGrid,
                          Hospitals,
                          // Generation 9
                          AtalantaPlanitiaLab,
                          MiningArea,
                          CrashSiteCleanup,
                          ArtificialLake,
                          // Generation 10
                          InventorsGuild,
                          SpaceStation,
                          CallistoPenalMines,
                          ProtectedGrowth,
                          // Generation 11
                          IshtarExpedition,
                          AdvancedAlloys,
                          BlackPolarDust,
                          Advertising,
                      ),
              ),
      )
  override val selectingDepartureOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("Nor") to
                      listOf(
                          // Setup
                          Insulation,
                          RedShips,
                          FoodFactory,
                          PowerSupplyConsortium,
                          // Generation 2
                          EquatorialMagnetizer,
                          AerosportTournament,
                          // Generation 3
                          PublicPlans,
                          Satellites,
                          // Generation 4
                          StratosphericExpedition,
                          Comet,
                          AdaptationTechnology,
                          // Generation 5
                          Shuttles,
                          // Generation 6
                          LavaFlows,
                          Zeppelins,
                          // Generation 7
                          ProjectInspection,
                          VestaShipyard,
                          VenusSoils,
                          HermeticOrderOfMars,
                          // Generation 8
                          MoholeArea,
                          IceCapMelting,
                          // Generation 9
                          SelfReplicatingRobots,
                          DomedCrater,
                          // Generation 11
                          FloatingHabs,
                          ColonizerTrainingCamp,
                      ),
                  cn("Vin") to
                      listOf(
                          // Setup
                          Decomposers,
                          StJosephOfCupertinoMission,
                          BreathingFilters,
                          IoSulphurResearch,
                          OrbitalCleanup,
                          Tardigrades,
                          // Generation 2
                          LightningHarvest,
                          OpenCity,
                          // Generation 3
                          SoilEnrichment,
                          Meltworks,
                          // Generation 5
                          LuxuryFoods,
                          // Generation 6
                          NaturalPreserve,
                          // Generation 7
                          MagneticFieldGeneratorsPromo,
                          OreProcessor,
                          // Generation 8
                          PowerGrid,
                          Hospitals,
                          // Generation 9
                          MiningArea,
                          CrashSiteCleanup,
                          ArtificialLake,
                          // Generation 10
                          SpaceStation,
                          CallistoPenalMines,
                          ProtectedGrowth,
                          // Generation 11
                          BlackPolarDust,
                          Advertising,
                      ),
              ),
      )

  private val nor
    get() = p1

  private val vin
    get() = p2

  @Test
  internal fun staticPressureStream() {
    startGame()
    nor.doTask("20 TerraformRating")
    vin.doTask("20 TerraformRating")
    nor.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    vin.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    nor.doTask("-4 ProjectCard<Selecting>")
    vin.doTask("-6 ProjectCard<Selecting>")
    generation1()
    generation2()
    generation3()
    generation4()
    generation5()
    generation6()
    generation7()
    generation8()
    generation9()
    generation10()
    generation11()
    endgame()

    // _local/replays/Game20260912/Heroku-g1212dda978b8/player-pe1c5cff0c611.json; final Mars map.
    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -              LSS   WSS   [G2]   WC    W
        |
        | 2 -            L     VS   [C2]  [G2]  [G1]  [O]
        |
        | 3 -        [G1]   L    [G1]  [G1]  [C2]  [G1]  [G1]
        |
        | 4 -     [G1]  [C1]  [G1]  [C1]  [G1]  [G1]  [C1]  [O]
        |
        | 5 -  [C2]  [G1]  [C2]  [O]   [O]   [O]   [S2]  [G1]  [S1]
        |
        | 6 -     [G2]  [C1]  [G1]  [C1]  [G1]  [O]   [O]   [O]
        |
        | 7 -         L     L    [G1]  [G1]   L    [S2]  [G2]
        |
        | 8 -           LSS    L     LC    LC    L     LT
        |
        | 9 -               LS   LSS    L     L    [O]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }

  private fun generation1() {
    with(nor) {
      draw(
          DesignedMicroorganisms,
          ResearchOutpost,
          AquiferPumping,
          UndergroundCity,
          ExtractorBalloons,
          Supermarkets,
      )

      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Ecoline>>",
          "36 MC",
          "3 Plant",
          "PROD[2 Plant]",
          "18 Pay<> FROM MC",
          "6 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(vin) {
      draw(FuelFactory, CarbonateProcessing, OptimalAerobraking, CloudTourism)
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$CheungShingMars>>",
          "44 MC",
          "PROD[3 MC]",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(nor) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AlbedoPlants>>",
          "PROD[Plant]",
          "Plant",
          "3 Heat",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ProjectEden>>",
          "3 Heat",
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
      )

      // Project Eden opens all three placement choices together.
      draw(SfMemorial, EcologicalZone)
      doTasks("CityTile<Tharsis_4_7>", "2 ProjectCard", "Plant")

      // Save 6 identifies Project Eden's three discarded cards.
      discard(ExtractorBalloons, UndergroundCity, SfMemorial)
      doTasks(
          "GreeneryTile<Tharsis_3_7>",
          "OxygenStep",
          "Steel",
          "2 MC",
          "TerraformRating",
          "-3 ProjectCard",
      )
    }

    with(vin) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
          "PROD[2 Energy]",
          "4 Steel",
      )
      draw(ProtectedHabitats, SisterPlanetSupport, PhysicsComplex)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ResearchNetwork>>",
          "PROD[MC]",
          "3 ProjectCard",
      )
    }

    nor.doTask("Pass")

    vin.turn {
      doTasks(
          *playCardTasks("$FuelFactory"),
          "2 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // no MC remainder
          "PROD[-Energy]",
          "PROD[Titanium]",
          "PROD[MC]",
      )
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "2 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // no MC remainder
          "PROD[-Energy]",
          "PROD[3 Heat]",
      )
    }
    vin.doTask("Pass")

    nor.doTasks("22 MC", "3 Plant")
    vin.doTasks("25 MC", "Titanium", "3 Heat")
    nor.doTask("VenusStep! BY Admin")
  }

  private fun generation2() {
    vin.draw(Omnicourt, VenusShuttles)
    nor.draw(StaticHarvesting, AerialMappers)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    vin.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    nor.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 18: immediately after both Research purchases in generation 2.
    nor.assertResources(m = 36, s = 1, t = 0, p = 8, e = 0, h = 6)
    nor.assertProduction(m = 0, s = 0, t = 0, p = 3, e = 0, h = 0)
    nor.assertDashMiddle(played = 3, actions = 0, tr = 22, hand = 7)
    nor.assertTags(plt = 3, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0)
    vin.assertResources(m = 51, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertProduction(m = 5, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 5, actions = 0, tr = 20, hand = 7)
    vin.assertTags(but = 3, pot = 1)
    vin.assertDashRight(events = 0, tagless = 1)
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 1, venus = 2)
    // Research Network's wild tag supplies Cloud Tourism's missing Earth tag.
    vin.turn {
      doTasks(
          *playCardTasks("$CloudTourism"),
          "11 Pay<> FROM MC",
          "EarthTag<WildTagElection<WildTag<$ResearchNetwork>>>",
          "PROD[MC]",
      )
    }

    nor.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_6>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$AquiferPumping"),
          "1 Pay<Class<Steel>> FROM Steel",
          "16 Pay<> FROM MC",
      )
    }

    // Research Network supplies Sister Planet Support's missing Earth tag.
    vin.turn {
      doTasks(
          *playCardTasks("$SisterPlanetSupport"),
          "7 Pay<> FROM MC",
          "PROD[3 MC]",
      )
    }

    nor.turn {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_6_9>",
          "TerraformRating",
          "Plant",
      )
    }

    vin.turn {
      doTasks(
          *playCardTasks("$Omnicourt"),
          "9 Pay<> FROM MC",
          "2 TerraformRating",
      )
    }

    nor.doTask("Pass")

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTasks(
          *playCardTasks("$VenusShuttles"),
          "9 Pay<> FROM MC",
          "2 Floater<$CloudTourism>",
      )
      doTasks(*cardActionTasks("$VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
    }
    vin.doTask("Pass")

    nor.doTasks("24 MC", "3 Plant")
    vin.doTasks("32 MC", "Titanium", "3 Heat")
    vin.doTask("OxygenStep! BY Admin")
  }

  private fun generation3() {
    nor.draw(LocalShading, Research)
    vin.draw(RoboticWorkforce, SmallAnimals)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    nor.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    vin.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 47: immediately after both Research purchases in generation 3.
    nor.assertResources(m = 32, s = 0, t = 0, p = 5, e = 0, h = 6)
    nor.assertProduction(m = 0, s = 0, t = 0, p = 3, e = 0, h = 0)
    nor.assertDashMiddle(played = 4, actions = 1, tr = 24, hand = 8)
    nor.assertTags(but = 1, plt = 3, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0)
    vin.assertResources(m = 33, s = 0, t = 2, p = 0, e = 0, h = 6)
    vin.assertProduction(m = 9, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 9, actions = 2, tr = 23, hand = 5)
    vin.assertTags(but = 4, pot = 1, eat = 1, jot = 1, vet = 3)
    vin.assertDashRight(events = 0, tagless = 1)
    assertSidebar(gen = 3, temp = -30, oxygen = 3, oceans = 2, venus = 4)
    nor.turn {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_6_8>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$StaticHarvesting"),
          "5 Pay<> FROM MC",
          "PROD[Energy]",
          "MC",
      )
    }

    vin.turn {
      doTasks(
          *playCardTasks("$RoboticWorkforce"),
          "9 Pay<> FROM MC",
          "EACH",
          "0 BuildingTag<WildTagElection<WildTag<$ResearchNetwork>>>",
          "CopyProductionBox<$CheungShingMars>",
          "PROD[3 MC]",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Fundraiser")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$AerialMappers"),
          "11 Pay<> FROM MC",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(*playCardTasks("$LocalShading"), "4 Pay<> FROM MC")
    }

    vin.doTask("Pass")

    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$LocalShading>")
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
    }
    nor.doTask("Pass")

    nor.doTasks("26 MC", "3 Plant", "Energy")
    vin.doTasks("35 MC", "Titanium", "3 Heat")
    nor.doTask("TemperatureStep! BY Admin")
  }

  private fun generation4() {
    nor.draw(ResearchCoordination)
    vin.draw(NuclearZone, MarsUniversity, SpinInducingAsteroid, Bushes)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    nor.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
    vin.doTasks(
        "Ok", // discard no research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 75: immediately after both Research purchases in generation 4.
    nor.assertResources(m = 30, s = 0, t = 0, p = 9, e = 1, h = 6)
    nor.assertProduction(m = 1, s = 0, t = 0, p = 3, e = 1, h = 0)
    nor.assertDashMiddle(played = 7, actions = 3, tr = 25, hand = 6)
    nor.assertTags(but = 1, pot = 1, vet = 2, plt = 3, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0)
    vin.assertResources(m = 39, s = 0, t = 3, p = 0, e = 0, h = 9)
    vin.assertProduction(m = 12, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 10, actions = 2, tr = 23, hand = 8)
    vin.assertTags(but = 4, sct = 1, pot = 1, eat = 1, jot = 1, vet = 3)
    vin.assertDashRight(events = 0, tagless = 1)
    assertSidebar(gen = 4, temp = -28, oxygen = 3, oceans = 3, venus = 4)
    vin.turn {
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
      )
      doTasks(
          *playCardTasks("$SpinInducingAsteroid"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "13 Pay<> FROM MC",
      )
      draw(EnergyTapping)
      doTasks(
          "VenusStep",
          "VenusStep",
          "3 MC",
          "3 Heat",
          "2 TerraformRating",
          "ProjectCard",
      )
    }

    nor.turn {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_4_8>",
          "TerraformRating",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_5_8>",
          "OxygenStep",
          "2 Plant",
          "6 MC",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(
          *playCardTasks("$NuclearZone"),
          "10 Pay<> FROM MC",
          "TemperatureStep",
          "TemperatureStep",
          "2 TerraformRating",
          "PROD[Heat]",
          "NuclearZone_SpecialTile<Tharsis_5_7>",
          "2 Plant",
          "2 MC",
      )

      // Research Network supplies Planetologist's second Jovian tag.
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Planetologist")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$DesignedMicroorganisms"),
          "16 Pay<> FROM MC",
          "PROD[2 Plant]",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "12 Pay<> FROM MC",
          "Animal<$EcologicalZone>",
          "Animal<$EcologicalZone>",
          "3 Heat",
          "EcologicalZone_SpecialTile<Tharsis_5_9>",
          "2 Plant",
          "4 MC",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_2_5>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "3 Pay<> FROM MC",
          "-Production<Nor, Class<Energy>>",
          "PROD[Energy]",
      )
    }

    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$LocalShading>")
    }

    vin.doTask("Pass")

    nor.turn {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
    }
    nor.doTask("Pass")

    nor.doTasks("30 MC", "5 Plant", "Heat FROM Energy")
    vin.doTasks("40 MC", "Titanium", "4 Heat", "Energy")
    vin.doTask("OceanTile<Tharsis_5_5>! BY Admin")
  }

  private fun generation5() {
    vin.draw(Cartel, MethaneFromTitan, CloudSeeding)
    nor.draw(RobotPollinators, Plantation, InterplanetaryTrade)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    vin.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    nor.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 117: immediately after both Research purchases in generation 5.
    nor.assertResources(m = 27, s = 0, t = 0, p = 6, e = 0, h = 10)
    nor.assertProduction(m = 2, s = 0, t = 0, p = 5, e = 0, h = 0)
    nor.assertDashMiddle(played = 9, actions = 3, tr = 28, hand = 7)
    nor.assertTags(but = 1, sct = 1, pot = 1, vet = 2, plt = 4, mit = 1, ant = 1, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0)
    vin.assertResources(m = 32, s = 0, t = 1, p = 2, e = 1, h = 16)
    vin.assertProduction(m = 12, s = 0, t = 1, p = 0, e = 1, h = 4)
    vin.assertDashMiddle(played = 14, actions = 2, tr = 28, hand = 8)
    vin.assertTags(but = 4, spt = 1, sct = 1, pot = 2, eat = 2, jot = 1, vet = 3)
    vin.assertDashRight(events = 1, tagless = 1)
    assertSidebar(gen = 5, temp = -24, oxygen = 5, oceans = 5, venus = 10)
    nor.turn {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_5_6>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_6>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
    }

    vin.turn {
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
    }

    nor.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$LocalShading>")
    }

    vin.turn {
      doTasks(
          *playCardTasks("$Cartel"),
          "8 Pay<> FROM MC",
          "PROD[4 MC]",
      )
    }

    nor.turn {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
    }

    vin.turn {
      doTasks(*cardActionTasks("$VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
    }

    nor.turn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Briber", "-12 MC")
    }

    vin.turn {
      doTasks(
          *playCardTasks("$SmallAnimals"),
          "6 Pay<> FROM MC",
          "-Production<Nor, Class<Plant>>",
      )
    }

    nor.doTask("Pass")

    vin.turn {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTasks(
          *playCardTasks("$ProtectedHabitats"),
          "5 Pay<> FROM MC",
      )
    }
    vin.doTask("Pass")

    vin.doTasks("47 MC", "Titanium", "5 Heat", "Energy", "Heat FROM Energy")
    nor.doTasks("34 MC", "4 Plant")
    nor.doTask("TemperatureStep! BY Admin")
  }

  private fun generation6() {
    vin.draw(SolarLogistics, InterstellarColonyShip, Sabotage)
    nor.draw(IshtarMining, Greenhouses)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    vin.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    nor.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 160: immediately after both Research purchases in generation 6.
    nor.assertResources(m = 31, s = 0, t = 0, p = 6, e = 0, h = 2)
    nor.assertProduction(m = 3, s = 0, t = 0, p = 4, e = 0, h = 0)
    nor.assertDashMiddle(played = 9, actions = 3, tr = 31, hand = 9)
    nor.assertTags(but = 1, sct = 1, pot = 1, vet = 2, plt = 4, mit = 1, ant = 1, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0)
    vin.assertResources(m = 43, s = 0, t = 2, p = 2, e = 1, h = 6)
    vin.assertProduction(m = 16, s = 0, t = 1, p = 0, e = 1, h = 5)
    vin.assertDashMiddle(played = 17, actions = 3, tr = 31, hand = 8)
    vin.assertTags(but = 4, spt = 1, sct = 1, pot = 2, eat = 3, jot = 1, vet = 3, ant = 1)
    vin.assertDashRight(events = 1, tagless = 2)
    assertSidebar(gen = 6, temp = -16, oxygen = 6, oceans = 6, venus = 12)
    vin.turn {
      doTasks(
          *playCardTasks("$Sabotage"),
          "1 Pay<> FROM MC",
          "-7 MC<Nor>",
      )
    }

    nor.turn {
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "4 Pay<> FROM MC",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "2 TerraformRating",
      )
    }

    nor.turn {
      doTasks(
          *playCardTasks("$Plantation"),
          "15 Pay<> FROM MC",
          "Animal<$EcologicalZone>",
          "3 Heat",
          "GreeneryTile<Tharsis_4_5>",
          "OxygenStep",
          "2 Plant",
          "4 MC",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_4>",
          "OxygenStep",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
    }

    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
    }

    vin.turn {
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "14 Pay<> FROM MC",
          "2 Titanium",
      )
    }

    nor.turn {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_6_7>",
          "TerraformRating",
          "Plant",
          "4 MC",
      )
      doTasks(
          *playCardTasks("$IshtarMining"),
          "5 Pay<> FROM MC",
          "PROD[Titanium]",
      )
    }

    vin.doTask("Pass")

    nor.doTask("Pass")

    vin.doTasks("50 MC", "Titanium", "5 Heat", "Energy", "Heat FROM Energy")
    nor.doTasks("38 MC", "4 Plant", "Titanium")
    vin.doTask("OceanTile<Tharsis_9_9>! BY Admin")
  }

  private fun generation7() {
    vin.draw(GeneRepair, Thermophiles)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    nor.doTask("-4 ProjectCard<Selecting>")
    vin.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // _local/replays/Game20260912/Heroku-g1212dda978b8/game-g1212dda978b8.sqlite; source save 201.
    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -              LSS   WSS    L     WC    W
        |
        | 2 -            L     VS    L     L    [G1]  [O]
        |
        | 3 -         VC    L     L    [G1]   L    [G1]  [G1]
        |
        | 4 -     VPT    LP    LP    LP   [G1]  [G1]  [C1]  [O]
        |
        | 5 -  VPP   LPP   NPP   WPP   [O]   [O]   [S2]  [G1]  [S1]
        |
        | 6 -      LP   LPP    LP    LP    LP   [O]   [O]   [O]
        |
        | 7 -         L     L     L     L     L     LP    L
        |
        | 8 -           LSS    L     LC    LC    L     LT
        |
        | 9 -               LS   LSS    L     L    [O]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )

    // Database save 201: immediately after both Research purchases in generation 7.
    nor.assertResources(m = 38, s = 0, t = 1, p = 6, e = 0, h = 5)
    nor.assertProduction(m = 3, s = 0, t = 1, p = 4, e = 0, h = 0)
    nor.assertDashMiddle(played = 12, actions = 3, tr = 35, hand = 6)
    nor.assertTags(but = 1, sct = 1, pot = 1, vet = 3, plt = 5, mit = 1, ant = 1, cit = 1)
    nor.assertDashRight(events = 0, tagless = 1)
    vin.assertResources(m = 49, s = 0, t = 3, p = 2, e = 1, h = 12)
    vin.assertProduction(m = 16, s = 0, t = 1, p = 0, e = 1, h = 5)
    vin.assertDashMiddle(played = 19, actions = 3, tr = 34, hand = 8)
    vin.assertTags(but = 4, spt = 2, sct = 1, pot = 2, eat = 4, jot = 1, vet = 3, ant = 1)
    vin.assertDashRight(events = 2, tagless = 2)
    assertSidebar(gen = 7, temp = -14, oxygen = 8, oceans = 8, venus = 16)
    nor.turn {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_5_4>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Landlord")
    }

    vin.turn {
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "19 Pay<> FROM MC",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
    }

    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers", "Action2"), "-Floater<$AerialMappers>")
      draw(TitaniumMine)
      doTasks("ProjectCard")
    }

    vin.turn {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
    }

    // Research Coordination supplies Interplanetary Trade's tenth distinct tag.
    nor.turn {
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "24 Pay<> FROM MC",
          "EACH",
          "JovianTag<WildTagElection<WildTag<$ResearchCoordination>>>",
          "PROD[10 MC]",
      )
    }

    vin.turn {
      doTasks(*playCardTasks("$Thermophiles"), "9 Pay<> FROM MC")
    }

    nor.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_3>",
          "OxygenStep",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
    }

    vin.turn {
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
    }

    nor.doTask("Pass")

    vin.turn {
      doTasks(*cardActionTasks("$VenusShuttles"), "7 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*playCardTasks("$MarsUniversity"), "6 Pay<> FROM MC")
      discard(PhysicsComplex)
      doTasks("-ProjectCard")
      draw(VenusGovernor)
      doTask("ProjectCard")
      doTasks(
          *playCardTasks("$VenusGovernor"),
          "4 Pay<> FROM MC",
          "PROD[2 MC]",
      )
    }
    vin.doTask("Pass")

    nor.doTasks("51 MC", "4 Plant", "Titanium")
    vin.doTasks("54 MC", "Titanium", "7 Heat", "Energy", "Heat FROM Energy", "2 Plant")
    nor.doTask("TemperatureStep! BY Admin")
  }

  private fun generation8() {
    vin.draw(TundraFarming, VenusianAnimals)
    nor.draw(RoverConstruction, Trees)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    vin.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    nor.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 246: immediately after both Research purchases in generation 8.
    nor.assertResources(m = 45, s = 0, t = 1, p = 5, e = 0, h = 5)
    nor.assertProduction(m = 14, s = 0, t = 1, p = 4, e = 0, h = 0)
    nor.assertDashMiddle(played = 13, actions = 3, tr = 37, hand = 8)
    nor.assertTags(
        but = 1,
        spt = 1,
        sct = 1,
        pot = 1,
        vet = 3,
        plt = 5,
        mit = 1,
        ant = 1,
        cit = 1,
    )
    nor.assertDashRight(events = 0, tagless = 1)
    vin.assertResources(m = 52, s = 0, t = 1, p = 4, e = 1, h = 12)
    vin.assertProduction(m = 18, s = 0, t = 1, p = 2, e = 1, h = 7)
    vin.assertDashMiddle(played = 23, actions = 4, tr = 36, hand = 6)
    vin.assertTags(
        but = 5,
        spt = 3,
        sct = 2,
        pot = 2,
        eat = 4,
        jot = 2,
        vet = 6,
        mit = 1,
        ant = 1,
    )
    vin.assertDashRight(events = 2, tagless = 2)
    assertSidebar(gen = 8, temp = -10, oxygen = 9, oceans = 9, venus = 18)
    vin.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_3_5>",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Excentric")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$RoverConstruction"),
          "8 Pay<> FROM MC",
      )
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "18 Pay<> FROM MC",
          "CityTile<Tharsis_4_2>",
          "2 MC",
          "Plant",
      )
    }

    vin.turn {
      doTasks(
          *playCardTasks("$Bushes"),
          "10 Pay<> FROM MC",
          "PROD[2 Plant]",
          "2 Plant",
      )
    }

    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
    }

    vin.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    nor.turn {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
    }

    vin.turn {
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
    }

    nor.turn {
      doTasks(*playCardTasks("$Research"), "10 Pay<> FROM MC")
      draw(StanfordTorus, PowerPlant)
      doTasks("2 ProjectCard")
    }

    vin.turn {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$Supermarkets"),
          "8 Pay<> FROM MC",
          "PROD[2 MC]",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$PowerPlant"),
          "3 Pay<> FROM MC",
          "PROD[Energy]",
      )
    }

    vin.doTask("Pass")

    nor.doTask("Pass")

    nor.doTasks("53 MC", "4 Plant", "Titanium", "Energy")
    vin.doTasks("56 MC", "Titanium", "7 Heat", "Energy", "Heat FROM Energy", "4 Plant")
    vin.doTask("OxygenStep! BY Admin")
  }

  private fun generation9() {
    vin.draw(AtalantaPlanitiaLab)
    nor.draw(IndenturedWorkers, GiantSolarShade)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    vin.doTask("-3 ProjectCard<Selecting>")
    vin.doTask("3 Pay<> FROM MC")
    vin.doTask("ProjectCard<Hand FROM Selecting>")
    nor.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 293: immediately after both Research purchases in generation 9.
    nor.assertResources(m = 47, s = 0, t = 2, p = 10, e = 1, h = 5)
    nor.assertProduction(m = 16, s = 0, t = 1, p = 4, e = 1, h = 0)
    nor.assertDashMiddle(played = 18, actions = 3, tr = 37, hand = 7)
    nor.assertTags(
        but = 4,
        spt = 1,
        sct = 4,
        pot = 2,
        vet = 3,
        plt = 5,
        mit = 1,
        ant = 1,
        cit = 2,
    )
    nor.assertDashRight(events = 0, tagless = 2)
    vin.assertResources(m = 56, s = 0, t = 2, p = 10, e = 1, h = 12)
    vin.assertProduction(m = 19, s = 0, t = 1, p = 4, e = 1, h = 7)
    vin.assertDashMiddle(played = 24, actions = 4, tr = 37, hand = 6)
    vin.assertTags(
        but = 5,
        spt = 3,
        sct = 2,
        pot = 2,
        eat = 4,
        jot = 2,
        vet = 6,
        plt = 1,
        mit = 1,
        ant = 1,
    )
    vin.assertDashRight(events = 2, tagless = 2)
    assertSidebar(gen = 9, temp = -8, oxygen = 10, oceans = 9, venus = 18)
    nor.turn {
      draw(DuskLaserMining)

      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_4_4>",
          "2 MC",
          "Plant",
          "4 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_1>",
          "OxygenStep",
          "ProjectCard",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Forecaster")
    }

    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers", "Action2"), "-Floater<$AerialMappers>")
      draw(HeatTrappers)
      doTasks("ProjectCard")
    }

    vin.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    nor.turn {
      doTasks(
          *playCardTasks("$DuskLaserMining"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "-Production<Nor, Class<Energy>>",
          "PROD[Titanium]",
          "4 Titanium",
      )
    }

    vin.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_2_3>",
      )
      giveTask(nor, "2 MC")
    }

    nor.turn {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
    }

    vin.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_2_4>",
          "OxygenStep",
          "TerraformRating",
      )
    }

    nor.turn {
      doTasks(
          *playCardTasks("$Greenhouses"),
          "5 Pay<> FROM MC",
          "5 Plant",
          "Animal<$EcologicalZone>",
          "3 Heat",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_3>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$Trees"),
          "12 Pay<> FROM MC",
          "PROD[3 Plant]",
          "Plant",
          "Animal<$EcologicalZone>",
          "3 Heat",
      )
    }

    vin.turn {
      doTasks(
          *cardActionTasks("$Thermophiles", "Action2"),
          "-2 Microbe<$Thermophiles>",
          "VenusStep",
          "TerraformRating",
      )
    }

    // Research Coordination supplies the eighth Plant tag counted by Robot Pollinators.
    nor.turn {
      doTasks(
          *playCardTasks("$RobotPollinators"),
          "8 Pay<> FROM MC",
          "PROD[Plant]",
          "8 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_5_2>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$VenusShuttles"), "5 Pay<> FROM MC", "VenusStep", "TerraformRating")
    }

    nor.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_1>",
          "OxygenStep",
          "Plant",
          "Titanium",
      )
    }

    vin.doTask("Pass")

    nor.turn {
      doTasks(
          *playCardTasks("$IndenturedWorkers"),
          "IndenturedWorkers_NextCardEffect",
      )
      doTasks(
          *playCardTasks("$GiantSolarShade"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "VenusStep",
          "VenusStep",
          "VenusStep",
          "3 TerraformRating",
      )
    }
    nor.doTask("Pass")

    vin.doTasks("61 MC", "Titanium", "7 Heat", "Energy", "Heat FROM Energy", "4 Plant")
    nor.doTasks("62 MC", "8 Plant", "2 Titanium", "Heat FROM Energy")
    nor.doTask("VenusStep! BY Admin")
  }

  private fun generation10() {
    vin.draw(InventorsGuild)
    // Source Floyd Continuum is represented by FakeFloydContinuum here.
    nor.draw(LandClaim, FakeFloydContinuum, RadSuits, DeepWellHeating)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    vin.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
    nor.doTasks(
        "Ok", // discard no research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 357: immediately after both Research purchases in generation 10.
    nor.assertResources(m = 53, s = 0, t = 2, p = 9, e = 0, h = 4)
    nor.assertProduction(m = 18, s = 0, t = 2, p = 8, e = 0, h = 0)
    nor.assertDashMiddle(played = 24, actions = 3, tr = 44, hand = 7)
    nor.assertTags(
        but = 5,
        spt = 3,
        sct = 4,
        pot = 2,
        vet = 4,
        plt = 7,
        mit = 1,
        ant = 1,
        cit = 2,
    )
    nor.assertDashRight(events = 1, tagless = 3)
    vin.assertResources(m = 64, s = 0, t = 3, p = 6, e = 1, h = 12)
    vin.assertProduction(m = 20, s = 0, t = 1, p = 4, e = 1, h = 7)
    vin.assertDashMiddle(played = 24, actions = 4, tr = 41, hand = 7)
    vin.assertTags(
        but = 5,
        spt = 3,
        sct = 2,
        pot = 2,
        eat = 4,
        jot = 2,
        vet = 6,
        plt = 1,
        mit = 1,
        ant = 1,
    )
    vin.assertDashRight(events = 2, tagless = 2)
    assertSidebar(gen = 10, temp = -4, oxygen = 14, oceans = 9, venus = 30)
    vin.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_5_1>",
      )
      giveTask(nor, "2 MC")

      doTask("2 Plant")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_2>",
          "0 OxygenStep",
          "Plant",
      )
    }

    nor.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_6_3>",
          "2 MC",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_4>",
          "0 OxygenStep",
          "Plant",
          "2 MC",
      )
    }

    vin.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    nor.turn {
      doTasks(*playCardTasks("$LandClaim"), "Community<Tharsis_2_2>")
    }

    vin.turn {
      doTasks(*playCardTasks("$VenusianAnimals"), "15 Pay<> FROM MC")
      discard(CloudSeeding)
      draw(Gyropolis)
      doTasks(
          "Animal<$VenusianAnimals>",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    nor.turn {
      doTasks(
          *playCardTasks("$FakeFloydContinuum"),
          "3 Pay<> FROM MC",
      )
    }

    // Research Network supplies Atalanta Planitia Lab's third Science tag.
    vin.turn {
      doTasks(*playCardTasks("$AtalantaPlanitiaLab"), "10 Pay<> FROM MC")
      draw(WaterSplittingPlant, Algae)
      discard(WaterSplittingPlant)
      doTasks(
          "Animal<$VenusianAnimals>",
          "2 ProjectCard",
          "-ProjectCard",
      )
      draw(NoctisCity)
      doTasks("ProjectCard")
    }

    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
    }

    vin.turn {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
    }

    nor.turn {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$DeepWellHeating"),
          "12 Pay<> FROM MC",
          "PROD[Energy]",
          "TemperatureStep",
          "TerraformRating",
          "Ok", // oceans are already complete
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
    }

    nor.turn {
      doTasks(*cardActionTasks("$FakeFloydContinuum"), "9 MC")
    }

    vin.turn {
      doTasks(
          *playCardTasks("$Algae"),
          "10 Pay<> FROM MC",
          "Plant",
          "PROD[2 Plant]",
      )
    }

    nor.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_6_5>",
          "2 MC",
          "Plant",
          "4 MC",
      )
    }

    vin.doTask("Pass")

    nor.turn {
      doTasks(
          *playCardTasks("$RadSuits"),
          "5 Pay<> FROM MC",
          "PROD[MC]",
      )
    }
    nor.doTask("Pass")

    nor.doTasks("66 MC", "8 Plant", "2 Titanium", "Energy")
    vin.doTasks("63 MC", "Titanium", "7 Heat", "Energy", "Heat FROM Energy", "6 Plant")
    vin.doTask("TemperatureStep! BY Admin")
  }

  private fun generation11() {
    nor.draw(GhgProducingBacteria, OlympusConference)
    vin.draw(IshtarExpedition, AdvancedAlloys)
    nor.doTask("4 ProjectCard<Selecting>")
    vin.doTask("4 ProjectCard<Selecting>")
    nor.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    vin.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 417: immediately after both Research purchases in generation 11.
    nor.assertResources(m = 64, s = 0, t = 4, p = 14, e = 1, h = 4)
    nor.assertProduction(m = 21, s = 0, t = 2, p = 8, e = 1, h = 0)
    nor.assertDashMiddle(played = 28, actions = 4, tr = 45, hand = 5)
    nor.assertTags(
        but = 6,
        spt = 3,
        sct = 5,
        pot = 3,
        vet = 4,
        plt = 7,
        mit = 1,
        ant = 1,
        cit = 2,
    )
    nor.assertDashRight(events = 2, tagless = 4)
    vin.assertResources(m = 61, s = 0, t = 4, p = 8, e = 1, h = 12)
    vin.assertProduction(m = 21, s = 0, t = 1, p = 6, e = 1, h = 7)
    vin.assertDashMiddle(played = 27, actions = 4, tr = 42, hand = 8)
    vin.assertTags(
        but = 5,
        spt = 3,
        sct = 4,
        pot = 2,
        eat = 4,
        jot = 2,
        vet = 8,
        plt = 2,
        mit = 1,
        ant = 2,
    )
    vin.assertDashRight(events = 2, tagless = 2)
    assertSidebar(gen = 11, temp = 2, oxygen = 14, oceans = 9, venus = 30)
    nor.turn {
      doTasks(*cardActionTasks("$AerialMappers", "Action2"), "-Floater<$AerialMappers>")
      draw(WeatherBalloons)
      doTask("ProjectCard")
      doTasks(
          *playCardTasks("$OlympusConference"),
          "9 Pay<> FROM MC",
          "Science<$OlympusConference>",
      )
    }

    vin.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*playCardTasks("$IshtarExpedition"), "6 Pay<> FROM MC")
      draw(FreyjaBiodomes, GhgImportFromVenus)
      doTasks(
          "3 Titanium",
          "2 SearchForCard<Vin, TagFilter<Class<VenusTag>>>",
      )
    }

    nor.turn {
      doTasks(*playCardTasks("$GhgProducingBacteria"), "7 Pay<> FROM MC")
      draw(MoholeLake)
      doTasks("ProjectCard FROM Science")
      doTasks(
          *playCardTasks("$MoholeLake"),
          "30 Pay<> FROM MC",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>",
          "TemperatureStep",
          "3 Plant",
          "TerraformRating",
      )
    }

    vin.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_1_3>",
          "0 OxygenStep",
      )
    }

    nor.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_6>",
          "0 OxygenStep",
          "Plant",
          "6 MC",
      )
    }

    vin.turn {
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
    }

    nor.turn {
      doTasks(*playCardTasks("$WeatherBalloons"), "10 Pay<> FROM MC")
      draw(GiantIceAsteroid)
      doTasks("Science<$OlympusConference>", "ProjectCard")
    }

    vin.turn {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }

    nor.turn {
      doTasks(*cardActionTasks("$FakeFloydContinuum"), "12 MC")
    }

    vin.turn {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
    }

    nor.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_5>",
          "0 OxygenStep",
      )
    }

    vin.turn {
      discard(GhgImportFromVenus)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    nor.turn {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
    }

    vin.turn {
      doTasks(
          *playCardTasks("$FreyjaBiodomes"),
          "14 Pay<> FROM MC",
          "-Production<Vin, Class<Energy>>",
          "PROD[2 MC]",
          "2* Animal<$VenusianAnimals>",
      )
    }

    nor.turn {
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
    }

    // Research Network supplies Interstellar Colony Ship's fifth Science tag.
    vin.turn {
      doTasks(
          *playCardTasks("$InterstellarColonyShip"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
      )
      draw(RestrictedArea)
      doTasks("3 MC", "3 Heat", "ProjectCard")
    }

    nor.turn {
      doTasks(*cardActionTasks("$WeatherBalloons"), "Floater<$WeatherBalloons>")
    }

    vin.turn {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }

    nor.turn {
      doTasks(
          *playCardTasks("$StanfordTorus"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC", // no MC remainder
          "NormalCityTile<StanfordTorus_RemoteArea>",
          "2 MC",
      )
    }

    vin.turn {
      doTasks(
          *playCardTasks("$NoctisCity"),
          "16 Pay<> FROM MC",
          "-Production<Vin, Class<Energy>>",
          "PROD[3 MC]",
          "NormalCityTile<Tharsis_5_3>",
      )
      giveTask(nor, "2 MC")

      doTasks("2 Plant", "2 MC")
    }

    nor.turn {
      discard(GiantIceAsteroid)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    vin.turn {
      discard(Gyropolis)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    nor.turn {
      discard(HeatTrappers)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    vin.turn {
      discard(AdvancedAlloys)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    nor.turn {
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$EcologicalZone>")
    }

    vin.turn {
      doTasks(*playCardTasks("$RestrictedArea"), "11 Pay<> FROM MC")
      discard(TundraFarming)
      draw(CorporateStronghold)
      doTasks(
          "Animal<$VenusianAnimals>",
          "RestrictedArea_SpecialTile<Tharsis_7_8>",
          "Plant",
          "4 MC",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    nor.turn {
      discard(TitaniumMine)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    vin.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC")
      draw(MagneticFieldDome)
      doTasks("ProjectCard")
    }

    nor.doTask("Pass")

    vin.turn {
      doTasks(*playCardTasks("$GeneRepair"), "12 Pay<> FROM MC")
      discard(MagneticFieldDome)
      draw(Teslaract)
      doTasks(
          "PROD[2 MC]",
          "Animal<$VenusianAnimals>",
          "-ProjectCard",
          "ProjectCard",
      )
      discard(InventorsGuild, CorporateStronghold, Teslaract)
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
    }
    vin.doTask("Pass")

    nor.doTasks("69 MC", "8 Plant", "2 Titanium", "Energy", "Heat FROM Energy")
    vin.doTasks("71 MC", "Titanium", "7 Heat", "Heat FROM Energy", "6 Plant")
  }

  private fun endgame() {
    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)
    with(nor) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_6>",
      )
      doTask("Ok") // decline final greenery
    }

    with(vin) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_9>",
          "4 MC",
      )
      doTask("Ok") // decline final greenery
    }

    assertCardTrackingComplete()
    nor.cardsHand shouldBe emptySet()
    vin.cardsHand shouldBe emptySet()
    nor.doTask("104 VictoryPoint")
    vin.doTask("107 VictoryPoint")

    nor.assertResources(m = 88, s = 0, t = 2, p = 5, e = 1, h = 5)
    nor.assertProduction(m = 22, s = 0, t = 2, p = 8, e = 1, h = 0)
    vin.assertResources(m = 78, s = 0, t = 1, p = 1, e = 0, h = 15)
    vin.assertProduction(m = 28, s = 0, t = 1, p = 6, e = 0, h = 7)
    nor.assertCardResources(
        6 to EcologicalZone,
        0 to AerialMappers,
        0 to LocalShading,
        1 to OlympusConference,
        1 to GhgProducingBacteria,
        1 to WeatherBalloons,
    )
    vin.assertCardResources(
        12 to CloudTourism,
        7 to SmallAnimals,
        2 to Thermophiles,
        6 to VenusianAnimals,
    )

    nor.assertCounts(47 to "TerraformRating")
    vin.assertCounts(43 to "TerraformRating")
    nor.assertCounts(104 to "VictoryPoint")
    vin.assertCounts(107 to "VictoryPoint")
    nor.assertCounts(0 to "Victory")
    vin.assertCounts(1 to "Victory")
  }
}
