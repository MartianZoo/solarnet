package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.fake.FakeCanon
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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
          "$Ecoline FROM CorporationCard",
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
          "$CheungShingMars FROM CorporationCard",
          "44 MC",
          "PROD[3 MC]",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(nor) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AlbedoPlants>>",
          "$AlbedoPlants FROM PreludeCard",
          "PROD[Plant]",
          "Plant",
          "3 Heat",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ProjectEden>>",
          "$ProjectEden FROM PreludeCard",
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
          "$Supplier FROM PreludeCard",
          "PROD[2 Energy]",
          "4 Steel",
      )
      draw(ProtectedHabitats, SisterPlanetSupport, PhysicsComplex)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ResearchNetwork>>",
          "$ResearchNetwork FROM PreludeCard",
          "PROD[MC]",
          "3 ProjectCard",
      )
    }

    with(nor) {
      doTask("Pass")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$FuelFactory"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$FuelFactory FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "PROD[MC]",
      )
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$CarbonateProcessing FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 Heat]",
      )
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 51, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertProduction(m = 5, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 5, actions = 0, tr = 20, hand = 7)
    vin.assertTags(but = 3, pot = 1)
    vin.assertDashRight(events = 0, tagless = 1, cities = 0)
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 1, venus = 2)
    // Research Network's wild tag supplies Cloud Tourism's missing Earth tag.
    with(vin) {
      doTasks(
              *playCardTasks("$CloudTourism"),
              "11 Pay<> FROM MC",
              "$CloudTourism FROM ProjectCard",
              "EarthTag<WildTagElection<WildTag<$ResearchNetwork>>>",
              "PROD[MC]",
          )
          .expect("PROD[MC]")
      doTask("PassBaton")
    }

    with(nor) {
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
          "$AquiferPumping FROM ProjectCard",
      )
    }

    // Research Network supplies Sister Planet Support's missing Earth tag.
    with(vin) {
      doTasks(
              *playCardTasks("$SisterPlanetSupport"),
              "7 Pay<> FROM MC",
              "$SisterPlanetSupport FROM ProjectCard",
              "PROD[3 MC]",
          )
          .expect("PROD[3 MC]")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_6_9>",
          "TerraformRating",
          "Plant",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$Omnicourt"),
          "9 Pay<> FROM MC",
          "$Omnicourt FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTask("Pass")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTasks(
          *playCardTasks("$VenusShuttles"),
          "9 Pay<> FROM MC",
          "$VenusShuttles FROM ProjectCard",
          "2 Floater<$CloudTourism>",
      )
      doTasks(*cardActionTasks("$VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 33, s = 0, t = 2, p = 0, e = 0, h = 6)
    vin.assertProduction(m = 9, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 9, actions = 2, tr = 23, hand = 5)
    vin.assertTags(but = 4, pot = 1, eat = 1, jot = 1, vet = 3)
    vin.assertDashRight(events = 0, tagless = 1, cities = 0)
    assertSidebar(gen = 3, temp = -30, oxygen = 3, oceans = 2, venus = 4)
    with(nor) {
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
          "$StaticHarvesting FROM ProjectCard",
          "PROD[Energy]",
          "MC",
      )
    }

    with(vin) {
      doTasks(
          *playCardTasks("$RoboticWorkforce"),
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
          "EACH @WildTagElection(HAS MAX 0 Tag) { Tag<@WildTagElection>? }",
          "0 BuildingTag<WildTagElection<WildTag<$ResearchNetwork>>>",
          "CopyProductionBox<$CheungShingMars>",
          "PROD[3 MC]",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Fundraiser")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$AerialMappers"),
          "11 Pay<> FROM MC",
          "$AerialMappers FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*playCardTasks("$LocalShading"), "4 Pay<> FROM MC", "$LocalShading FROM ProjectCard")
      doTask("PassBaton")
    }

    with(vin) {
      doTask("Pass")
    }

    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$LocalShading>")
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 39, s = 0, t = 3, p = 0, e = 0, h = 9)
    vin.assertProduction(m = 12, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 10, actions = 2, tr = 23, hand = 8)
    vin.assertTags(but = 4, sct = 1, pot = 1, eat = 1, jot = 1, vet = 3)
    vin.assertDashRight(events = 0, tagless = 1, cities = 0)
    assertSidebar(gen = 4, temp = -28, oxygen = 3, oceans = 3, venus = 4)
    with(vin) {
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$SpinInducingAsteroid"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "13 Pay<> FROM MC",
      )
      draw(EnergyTapping)
      doTasks(
          "$SpinInducingAsteroid FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "TerraformRating",
          "ProjectCard",
      )
    }

    with(nor) {
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
          "2 MC",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
    }

    with(vin) {
      doTasks(
          *playCardTasks("$NuclearZone"),
          "10 Pay<> FROM MC",
          "$NuclearZone FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TerraformRating",
          "TerraformRating",
          "PROD[Heat]",
          "NuclearZone_SpecialTile<Tharsis_5_7>",
          "2 Plant",
          "2 MC",
      )

      // Research Network supplies Planetologist's second Jovian tag.
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Planetologist")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$DesignedMicroorganisms"),
          "16 Pay<> FROM MC",
          "$DesignedMicroorganisms FROM ProjectCard",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "12 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Animal<$EcologicalZone>",
          "3 Heat",
          "EcologicalZone_SpecialTile<Tharsis_5_9>",
          "2 Plant",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_2_5>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "3 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "-Production<Nor, Class<Energy>>",
          "PROD[Energy]",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }

    with(vin) {
      doTask("Pass")
    }

    with(nor) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 32, s = 0, t = 1, p = 2, e = 1, h = 16)
    vin.assertProduction(m = 12, s = 0, t = 1, p = 0, e = 1, h = 4)
    vin.assertDashMiddle(played = 14, actions = 2, tr = 28, hand = 8)
    vin.assertTags(but = 4, spt = 1, sct = 1, pot = 2, eat = 2, jot = 1, vet = 3)
    vin.assertDashRight(events = 1, tagless = 1, cities = 0)
    assertSidebar(gen = 5, temp = -24, oxygen = 5, oceans = 5, venus = 10)
    with(nor) {
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

    with(vin) {
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

    with(nor) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$Cartel"),
          "8 Pay<> FROM MC",
          "$Cartel FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Briber", "-12 MC")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$SmallAnimals"),
          "6 Pay<> FROM MC",
          "$SmallAnimals FROM ProjectCard",
          "-Production<Nor, Class<Plant>>",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTask("Pass")
    }

    with(vin) {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTasks(
          *playCardTasks("$ProtectedHabitats"),
          "5 Pay<> FROM MC",
          "$ProtectedHabitats FROM ProjectCard",
      )
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 43, s = 0, t = 2, p = 2, e = 1, h = 6)
    vin.assertProduction(m = 16, s = 0, t = 1, p = 0, e = 1, h = 5)
    vin.assertDashMiddle(played = 17, actions = 3, tr = 31, hand = 8)
    vin.assertTags(but = 4, spt = 1, sct = 1, pot = 2, eat = 3, jot = 1, vet = 3, ant = 1)
    vin.assertDashRight(events = 1, tagless = 2, cities = 0)
    assertSidebar(gen = 6, temp = -16, oxygen = 6, oceans = 6, venus = 12)
    with(vin) {
      doTasks(
          *playCardTasks("$Sabotage"),
          "1 Pay<> FROM MC",
          "$Sabotage FROM ProjectCard",
          "-7 MC<Nor>",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "4 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$VenusShuttles"), "8 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
    }

    with(nor) {
      doTasks(
          *playCardTasks("$Plantation"),
          "15 Pay<> FROM MC",
          "$Plantation FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "3 Heat",
          "GreeneryTile<Tharsis_4_5>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
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

    with(vin) {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "14 Pay<> FROM MC",
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_6_7>",
          "TerraformRating",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$IshtarMining"),
          "5 Pay<> FROM MC",
          "$IshtarMining FROM ProjectCard",
          "PROD[Titanium]",
      )
    }

    with(vin) {
      doTask("Pass")
    }

    with(nor) {
      doTask("Pass")
    }

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
    // Database save 201: immediately after both Research purchases in generation 7.
    nor.assertResources(m = 38, s = 0, t = 1, p = 6, e = 0, h = 5)
    nor.assertProduction(m = 3, s = 0, t = 1, p = 4, e = 0, h = 0)
    nor.assertDashMiddle(played = 12, actions = 3, tr = 35, hand = 6)
    nor.assertTags(but = 1, sct = 1, pot = 1, vet = 3, plt = 5, mit = 1, ant = 1, cit = 1)
    nor.assertDashRight(events = 0, tagless = 1, cities = 1)
    vin.assertResources(m = 49, s = 0, t = 3, p = 2, e = 1, h = 12)
    vin.assertProduction(m = 16, s = 0, t = 1, p = 0, e = 1, h = 5)
    vin.assertDashMiddle(played = 19, actions = 3, tr = 34, hand = 8)
    vin.assertTags(but = 4, spt = 2, sct = 1, pot = 2, eat = 4, jot = 1, vet = 3, ant = 1)
    vin.assertDashRight(events = 2, tagless = 2, cities = 0)
    assertSidebar(gen = 7, temp = -14, oxygen = 8, oceans = 8, venus = 16)
    with(nor) {
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

    with(vin) {
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "19 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers", "Action2"), "-Floater<$AerialMappers>")
      draw(TitaniumMine)
      doTasks("ProjectCard")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTask("PassBaton")
    }

    // Research Coordination supplies Interplanetary Trade's tenth distinct tag.
    with(nor) {
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "24 Pay<> FROM MC",
          "$InterplanetaryTrade FROM ProjectCard",
          "EACH @WildTagElection { Tag<@WildTagElection>? }",
          "JovianTag<WildTagElection<WildTag<ResearchCoordination>>>",
          "PROD[10 MC]",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*playCardTasks("$Thermophiles"), "9 Pay<> FROM MC", "$Thermophiles FROM ProjectCard")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_3>",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
      doTask("PassBaton")
    }

    with(nor) {
      doTask("Pass")
    }

    with(vin) {
      doTasks(*cardActionTasks("$VenusShuttles"), "7 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*playCardTasks("$MarsUniversity"), "6 Pay<> FROM MC")
      discard(PhysicsComplex)
      doTasks("$MarsUniversity FROM ProjectCard", "-ProjectCard")
      draw(VenusGovernor)
      doTask("ProjectCard")
      doTasks(
          *playCardTasks("$VenusGovernor"),
          "4 Pay<> FROM MC",
          "$VenusGovernor FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 0, tagless = 1, cities = 1)
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
    vin.assertDashRight(events = 2, tagless = 2, cities = 0)
    assertSidebar(gen = 8, temp = -10, oxygen = 9, oceans = 9, venus = 18)
    with(vin) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_3_5>",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Excentric")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$RoverConstruction"),
          "8 Pay<> FROM MC",
          "$RoverConstruction FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "18 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Tharsis_4_2>",
          "2 MC",
          "Plant",
      )
    }

    with(vin) {
      doTasks(
          *playCardTasks("$Bushes"),
          "10 Pay<> FROM MC",
          "$Bushes FROM ProjectCard",
          "PROD[2 Plant]",
          "2 Plant",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*playCardTasks("$Research"), "10 Pay<> FROM MC")
      draw(StanfordTorus, PowerPlant)
      doTasks("$Research FROM ProjectCard", "2 ProjectCard")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$Supermarkets"),
          "8 Pay<> FROM MC",
          "$Supermarkets FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$PowerPlant"),
          "3 Pay<> FROM MC",
          "$PowerPlant FROM ProjectCard",
          "PROD[Energy]",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTask("Pass")
    }

    with(nor) {
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 0, tagless = 2, cities = 2)
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
    vin.assertDashRight(events = 2, tagless = 2, cities = 1)
    assertSidebar(gen = 9, temp = -8, oxygen = 10, oceans = 9, venus = 18)
    with(nor) {
      draw(DuskLaserMining)
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_4_4>",
          "2 MC",
          "Plant",
          "2 MC",
          "2 MC",
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

    with(vin) {
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Forecaster")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers", "Action2"), "-Floater<$AerialMappers>")
      draw(HeatTrappers)
      doTasks("ProjectCard")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$DuskLaserMining"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$DuskLaserMining FROM ProjectCard",
          "-Production<Nor, Class<Energy>>",
          "PROD[Titanium]",
          "4 Titanium",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_2_3>",
          "NOW Nor { 2 MC }",
      )

      nor.doTask("2 MC")

      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_2_4>",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$Greenhouses"),
          "5 Pay<> FROM MC",
          "$Greenhouses FROM ProjectCard",
          "Plant / CityTile<Anyone>",
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

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$Trees"),
          "12 Pay<> FROM MC",
          "$Trees FROM ProjectCard",
          "PROD[3 Plant]",
          "Plant",
          "Animal<$EcologicalZone>",
          "3 Heat",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *cardActionTasks("$Thermophiles", "Action2"),
          "-2 Microbe<$Thermophiles>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    // Research Coordination supplies the eighth Plant tag counted by Robot Pollinators.
    with(nor) {
      doTasks(
          *playCardTasks("$RobotPollinators"),
          "8 Pay<> FROM MC",
          "$RobotPollinators FROM ProjectCard",
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

    with(vin) {
      doTasks(*cardActionTasks("$VenusShuttles"), "5 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_1>",
          "OxygenStep",
          "Plant",
          "Titanium",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTask("Pass")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$IndenturedWorkers"),
          "$IndenturedWorkers FROM ProjectCard",
          "IndenturedWorkers_NextCardEffect",
      )
      doTasks(
          *playCardTasks("$GiantSolarShade"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$GiantSolarShade FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
      )
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 1, tagless = 3, cities = 3)
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
    vin.assertDashRight(events = 2, tagless = 2, cities = 2)
    assertSidebar(gen = 10, temp = -4, oxygen = 14, oceans = 9, venus = 30)
    with(vin) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_5_1>",
          "NOW Nor { 2 MC }",
      )

      nor.doTask("2 MC")

      doTasks(
          "2 Plant",
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_2>",
          "0 OxygenStep",
          "Plant",
      )
    }

    with(nor) {
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

    with(vin) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*playCardTasks("$LandClaim"), "$LandClaim FROM ProjectCard", "Community<Tharsis_2_2>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*playCardTasks("$VenusianAnimals"), "15 Pay<> FROM MC")
      discard(CloudSeeding)
      draw(Gyropolis)
      doTasks(
          "$VenusianAnimals FROM ProjectCard",
          "Animal<$VenusianAnimals>",
          "-ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$FakeFloydContinuum"),
          "3 Pay<> FROM MC",
          "$FakeFloydContinuum FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    // Research Network supplies Atalanta Planitia Lab's third Science tag.
    with(vin) {
      doTasks(*playCardTasks("$AtalantaPlanitiaLab"), "10 Pay<> FROM MC")
      draw(WaterSplittingPlant, Algae)
      discard(WaterSplittingPlant)
      doTasks(
          "$AtalantaPlanitiaLab FROM ProjectCard",
          "Animal<$VenusianAnimals>",
          "2 ProjectCard",
          "-ProjectCard",
      )
      draw(NoctisCity)
      doTasks("ProjectCard")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$DeepWellHeating"),
          "12 Pay<> FROM MC",
          "$DeepWellHeating FROM ProjectCard",
          "PROD[Energy]",
          "TemperatureStep",
          "TerraformRating",
          "Ok", // oceans are already complete
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$FakeFloydContinuum"), "9 MC").expect("9 MC")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$Algae"),
          "10 Pay<> FROM MC",
          "$Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_6_5>",
          "2 MC",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTask("Pass")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$RadSuits"),
          "5 Pay<> FROM MC",
          "$RadSuits FROM ProjectCard",
          "PROD[MC]",
      )
      doTask("Pass")
    }

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
    nor.assertDashRight(events = 2, tagless = 4, cities = 5)
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
    vin.assertDashRight(events = 2, tagless = 2, cities = 3)
    assertSidebar(gen = 11, temp = 2, oxygen = 14, oceans = 9, venus = 30)
    with(nor) {
      doTasks(*cardActionTasks("$AerialMappers", "Action2"), "-Floater<$AerialMappers>")
      draw(WeatherBalloons)
      doTask("ProjectCard")
      doTasks(
          *playCardTasks("$OlympusConference"),
          "9 Pay<> FROM MC",
          "$OlympusConference FROM ProjectCard",
          "Science<$OlympusConference>",
      )
    }

    with(vin) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*playCardTasks("$IshtarExpedition"), "6 Pay<> FROM MC")
      draw(FreyjaBiodomes, GhgImportFromVenus)
      doTasks(
          "$IshtarExpedition FROM ProjectCard",
          "3 Titanium",
          "2 SearchForCard<Vin, TagFilter<Class<VenusTag>>>",
      )
    }

    with(nor) {
      doTasks(*playCardTasks("$GhgProducingBacteria"), "7 Pay<> FROM MC")
      draw(MoholeLake)
      doTasks("$GhgProducingBacteria FROM ProjectCard", "ProjectCard FROM Science")
      doTasks(
          *playCardTasks("$MoholeLake"),
          "30 Pay<> FROM MC",
          "$MoholeLake FROM ProjectCard",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>",
          "TemperatureStep",
          "3 Plant",
          "TerraformRating",
      )
    }

    with(vin) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_1_3>",
          "0 OxygenStep",
      )
      doTask("PassBaton")
    }

    with(nor) {
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
          "2 MC",
          "2 MC",
          "2 MC",
      )
    }

    with(vin) {
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*playCardTasks("$WeatherBalloons"), "10 Pay<> FROM MC")
      draw(GiantIceAsteroid)
      doTasks("$WeatherBalloons FROM ProjectCard", "Science<$OlympusConference>", "ProjectCard")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$FakeFloydContinuum"), "12 MC").expect("12 MC")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_5>",
          "0 OxygenStep",
      )
      doTask("PassBaton")
    }

    with(vin) {
      discard(GhgImportFromVenus)
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$FreyjaBiodomes"),
          "14 Pay<> FROM MC",
          "$FreyjaBiodomes FROM ProjectCard",
          "-Production<Vin, Class<Energy>>",
          "PROD[2 MC]",
          "2* Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTask("PassBaton")
    }

    // Research Network supplies Interstellar Colony Ship's fifth Science tag.
    with(vin) {
      doTasks(
          *playCardTasks("$InterstellarColonyShip"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
      )
      draw(RestrictedArea)
      doTasks("$InterstellarColonyShip FROM ProjectCard", "3 MC", "3 Heat", "ProjectCard")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$WeatherBalloons"), "Floater<$WeatherBalloons>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(
          *playCardTasks("$StanfordTorus"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$StanfordTorus FROM ProjectCard",
          "NormalCityTile<StanfordTorus_RemoteArea>",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          *playCardTasks("$NoctisCity"),
          "16 Pay<> FROM MC",
          "$NoctisCity FROM ProjectCard",
          "-Production<Vin, Class<Energy>>",
          "PROD[3 MC]",
          "NormalCityTile<Tharsis_5_3>",
          "NOW Nor { 2 MC }",
      )

      nor.doTask("2 MC")

      doTasks("2 Plant", "2 MC")
      doTask("PassBaton")
    }

    with(nor) {
      discard(GiantIceAsteroid)
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(vin) {
      discard(Gyropolis)
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(nor) {
      discard(HeatTrappers)
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(vin) {
      discard(AdvancedAlloys)
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(nor) {
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$EcologicalZone>")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*playCardTasks("$RestrictedArea"), "11 Pay<> FROM MC")
      discard(TundraFarming)
      draw(CorporateStronghold)
      doTasks(
          "$RestrictedArea FROM ProjectCard",
          "Animal<$VenusianAnimals>",
          "RestrictedArea_SpecialTile<Tharsis_7_8>",
          "Plant",
          "2 MC",
          "2 MC",
          "-ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(nor) {
      discard(TitaniumMine)
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC")
      draw(MagneticFieldDome)
      doTasks("ProjectCard")
      doTask("PassBaton")
    }

    with(nor) {
      doTask("Pass")
    }

    with(vin) {
      doTasks(*playCardTasks("$GeneRepair"), "12 Pay<> FROM MC")
      discard(MagneticFieldDome)
      draw(Teslaract)
      doTasks(
          "$GeneRepair FROM ProjectCard",
          "PROD[2 MC]",
          "Animal<$VenusianAnimals>",
          "-ProjectCard",
          "ProjectCard",
      )
      discard(InventorsGuild, CorporateStronghold, Teslaract)
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTask("Pass")
    }

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
      doTask("PassBaton")
    }

    with(vin) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_9>",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }

    assertCardTrackingComplete()
    nor.cardsHand shouldBe emptySet()
    vin.cardsHand shouldBe emptySet()
    admin.assertCounts(1 to "End", 1 to "Phase", 0 to "GameScope")

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

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Nor>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Vin>") shouldBe 10
    score.net("FirstPlace", "VictoryPoint<Nor>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Vin>") shouldBe 10
    score.net("GreeneryTile", "VictoryPoint<Nor>") shouldBe 16
    score.net("GreeneryTile", "VictoryPoint<Vin>") shouldBe 4
    score.net("CityTile", "VictoryPoint<Nor>") shouldBe 19
    score.net("CityTile", "VictoryPoint<Vin>") shouldBe 16
    score.net("Card", "VictoryPoint<Nor>") shouldBe 12
    score.net("Card", "VictoryPoint<Vin>") shouldBe 24

    nor.assertCounts(47 to "TerraformRating")
    vin.assertCounts(43 to "TerraformRating")
    nor.assertCounts(104 to "VictoryPoint")
    vin.assertCounts(107 to "VictoryPoint")
    nor.assertCounts(0 to "Victory")
    vin.assertCounts(1 to "Victory")
  }
}
