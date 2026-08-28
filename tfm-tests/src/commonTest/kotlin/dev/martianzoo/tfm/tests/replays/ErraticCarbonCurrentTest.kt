package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.CardDefinition
import dev.martianzoo.tfm.canon.CardDefinition.CardData
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val marsNomads = cn("MarsNomads")

// Mars Nomads' moving, non-tile map marker is not yet modeled. The replay supplies each sourced
// placement bonus explicitly while this stand-in preserves the card play and once-per-round action.
private val marsNomadsDefinition =
    CardDefinition(
        CardData(
            name = "MarsNomads",
            deck = "PROJECT",
            actions = listOf("-> Ok"),
            cost = 13,
            projectKind = "ACTIVE",
        )
    )

private val erraticCarbonCurrentCatalog =
    TfmCatalog.compose(
        Canon,
        object : TfmCatalog() {
          override val explicitClassDeclarations = setOf(marsNomadsDefinition.asClassDeclaration)
          override val cardDefinitions = setOf(marsNomadsDefinition)
        },
    )

// Complete database replay: Erratic Carbon Current (gbf986ef543f0)
// https://terraforming-mars.herokuapp.com/the-end?id=p6674c4a1893d
internal class ErraticCarbonCurrentTest : CardTrackingFullGameTest() {
  override val catalog = erraticCarbonCurrentCatalog

  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, PromoCardPack
          MarsNomads

          RimSettler, Ecologist, Producer, Fundraiser, Philantropist, Terraformer29
          Traveller, Collector, Excentric, Investor, Suburbian, Magnate
          """,
          "Blue",
          "Pink",
      )

  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  private val blue
    get() = p1

  private val pink
    get() = p2

  @Test
  internal fun erraticCarbonCurrent() {
    TfmWorkflow.Auto(game).launch()
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
  }

  private fun generation1() {
    blue.discardUnselectedProjectCards(OrbitalCleanup, Trees, Supermarkets, CityParks)
    pink.discardUnselectedProjectCards(
        DustSeals,
        RadSuits,
        Shuttles,
        Penguins,
        BactoviralResearch,
        UrbanizedArea,
    )

    blue.playCorp(CrediCor) {
      buyCards(Potatoes, Hackers, Lichen, NeutralizerFactory, SpaceHotels, ExtractorBalloons)
    }
    pink.playCorp(Ecoline) { buyCards(VestaShipyard, MiningRights, BuildingIndustries, Ants) }

    blue.turn {
      playPrelude(AlbedoPlants)
      playPrelude(ResearchNetwork) {
        draw(Windmills, UndergroundCity, AerobrakedAmmoniaAsteroid)
      }
    }
    pink.turn {
      playPrelude(BiosphereSupport)
      playPrelude(Biolab) { draw(AstraMechanica, Moss, SymbioticFungus) }
    }

    blue.turn { playProject(AerobrakedAmmoniaAsteroid, 26) }
    pink.turn { playProject(MiningRights, 9) { placeTile(4, 4) } }
    blue.pass()
    pink.turn {
      playProject(VestaShipyard, 15)
      pass()
    }
    blue.wgt("TemperatureStep")
  }

  private fun generation2() {
    pink.buyCards(BigAsteroid, MarsUniversity, ExtremeColdFungus)
    pink.discardUnselectedProjectCards(Bushes)
    blue.buyCards(SmallAsteroid, HermeticOrderOfMars)
    blue.discardUnselectedProjectCards(CallistoPenalMines, EnergyMarket)

    // Database save 23 evidence: after both research purchases.
    blue.assertResources(m = 32, s = 0, t = 0, p = 3, e = 0, h = 6)
    blue.assertProduction(m = 1, s = 0, t = 0, p = 2, e = 0, h = 3)
    pink.assertResources(m = 10, s = 3, t = 1, p = 8, e = 0, h = 0)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 0)
    assertSidebar(gen = 2, temp = -28, oxygen = 0, oceans = 0, venus = 0)

    pink.turn { convertPlants { placeTile(3, 4) } }
    blue.turn { playProject(Potatoes, 2) }
    pink.turn { sellPatents(SymbioticFungus) }
    blue.turn {
      convertHeat()
      playProject(SmallAsteroid, 10) { doTask("-Plant<Pink>") }
    }
    pink.pass()
    blue.turn {
      playProject(Lichen, 7)
      // Test inference: Research Network's wild tag supplies the fourth bio tag.
      assignWildTag("PlantTag")
      claimMilestone(cn("Ecologist"))
    }
    blue.pass()
    pink.wgt("OxygenStep")
  }

  private fun generation3() {
    blue.buyCards(ResearchCoordination, GreatEscarpmentConsortium, Worms)
    blue.discardUnselectedProjectCards(CaretakerContract)
    pink.buyCards(NitrophilicMoss)
    pink.discardUnselectedProjectCards(Casinos, QuantumExtractor, ArtificialLake)

    // Database save 48 evidence: after both research purchases.
    blue.assertResources(m = 21, s = 0, t = 0, p = 4, e = 0, h = 8)
    blue.assertProduction(m = 3, s = 0, t = 0, p = 3, e = 0, h = 4)
    pink.assertResources(m = 28, s = 5, t = 2, p = 5, e = 0, h = 0)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 0)
    assertSidebar(gen = 3, temp = -24, oxygen = 2, oceans = 0, venus = 0)

    blue.turn { playProject(ResearchCoordination, 4) }
    pink.turn {
      playProject(BigAsteroid, 21, titanium = 2) { doTask("-4 Plant<Blue>") }
    }
    blue.turn {
      // Blue has no printed Earth tags yet, so Space Hotels needs both wild tags.
      assignWildTag("EarthTag")
      assignWildTag("EarthTag")
      playProject(SpaceHotels, 12)
    }
    pink.pass()
    blue.turn {
      convertHeat()
      pass()
    }
    blue.wgt("OxygenStep")
  }

  private fun generation4() {
    pink.buyCards(EarthOffice, LargeConvoy)
    pink.discardUnselectedProjectCards(EquatorialMagnetizer, SulphurExports)
    blue.buyCards(AdaptedLichen, ResearchOutpost)
    blue.discardUnselectedProjectCards(EnergyTapping, OutdoorSports)

    // Database save 69 evidence: after both research purchases.
    blue.assertResources(m = 29, s = 0, t = 0, p = 3, e = 0, h = 4)
    blue.assertProduction(m = 7, s = 0, t = 0, p = 3, e = 0, h = 4)
    pink.assertResources(m = 23, s = 6, t = 5, p = 10, e = 0, h = 1)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 1)
    assertSidebar(gen = 4, temp = -18, oxygen = 3, oceans = 0, venus = 0)

    pink.turn { playProject(EarthOffice, 1) }
    blue.turn { playProject(ResearchOutpost, 18) { placeTile(2, 5) } }
    pink.turn {
      playProject(LargeConvoy, 18, titanium = 5) {
        doTask("5 Plant")
        draw(NoctisCity, MartianSurvey)
        placeTile(5, 6)
        draw(Harvest)
      }
      convertPlants { placeTile(5, 5) }
    }
    blue.turn { playProject(HermeticOrderOfMars, 9) }
    pink.turn {
      playProject(MarsUniversity, steel = 4) {
        doTask("-ProjectCard")
        discard(NoctisCity)
        draw(Plantation)
      }
    }
    blue.turn { playProject(AdaptedLichen, 8) }
    pink.pass()
    blue.pass()
    pink.wgt("OceanTile<Hellas_6_7>")
  }

  private fun generation5() {
    blue.buyCards(NitrogenRichAsteroid)
    blue.discardUnselectedProjectCards(
        DeuteriumExport,
        ElectroCatapult,
        BeamFromAThoriumAsteroid,
    )
    pink.buyCards(ReleaseOfInertGases, BusinessNetwork, marsNomads)
    pink.discardUnselectedProjectCards(BioPrintingFacility)

    // Database save 98 evidence: after both research purchases.
    blue.assertResources(m = 29, s = 0, t = 0, p = 8, e = 0, h = 11)
    blue.assertProduction(m = 9, s = 0, t = 0, p = 4, e = 0, h = 4)
    pink.assertResources(m = 21, s = 3, t = 1, p = 13, e = 0, h = 2)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 1)
    assertSidebar(gen = 5, temp = -18, oxygen = 4, oceans = 2, venus = 0)

    blue.turn {
      sellPatents(Hackers)
      playProject(NitrogenRichAsteroid, 30) { doTask("PROD[4 Plant]") }
    }
    pink.turn {
      stdProject("AquiferSP") { placeTile(4, 6) }
      convertPlants { placeTile(4, 5) }
    }
    blue.turn { convertPlants { placeTile(3, 5) } }
    pink.turn {
      playProject(Harvest, 4)
      playProject(marsNomads, 13)
    }
    blue.turn { convertHeat() }
    pink.turn {
      convertPlants { placeTile(6, 6) }
      cardAction1(marsNomads)
    }
    // Unsupported component: moving Mars Nomads granted the destination's complete bonus.
    pink.exMachina("-6 MC, OceanTile<Hellas_5_7>, TerraformRating, 3 Heat, 6 MC")
    blue.turn { playProject(Windmills, 5) }
    pink.turn { claimMilestone(cn("Terraformer29")) }
    blue.pass()
    pink.pass()
    blue.wgt("OxygenStep")
  }

  private fun generation6() {
    pink.buyCards(ImportedGhg, KelpFarming, LavaTubeSettlement)
    pink.discardUnselectedProjectCards(Ironworks)
    blue.buyCards(Insects, PowerGrid)
    blue.discardUnselectedProjectCards(JovianEmbassy, SoilFactory)

    // Database save 133 evidence: after both research purchases.
    blue.assertResources(m = 32, s = 0, t = 0, p = 8, e = 1, h = 7)
    blue.assertProduction(m = 9, s = 0, t = 0, p = 8, e = 1, h = 4)
    pink.assertResources(m = 19, s = 5, t = 2, p = 5, e = 0, h = 6)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 1)
    assertSidebar(gen = 6, temp = -12, oxygen = 8, oceans = 4, venus = 0)

    pink.turn {
      stdProject("PowerPlantSP")
      playProject(LavaTubeSettlement, 5, steel = 5) { placeTile(6, 5) }
    }
    blue.turn {
      convertPlants { placeTile(2, 4) }.expect("-7 Plant, Steel")
    }
    pink.turn { playProject(ImportedGhg, 1, titanium = 1) }
    blue.turn { playProject(ExtractorBalloons, 20) }
    pink.turn { cardAction1(marsNomads) }
    // Unsupported component: the Nomads' new area supplied two heat.
    pink.exMachina("2 Heat")
    blue.turn {
      assignWildTag("PlantTag")
      assignWildTag("PlantTag")
      playProject(Insects, 8).expect("PROD[6 Plant]")
      assignWildTag("MicrobeTag")
      assignWildTag("MicrobeTag")
      playProject(Worms, 7).expect("PROD[2 Plant]")
    }
    pink.turn { sellPatents(BuildingIndustries, MartianSurvey) }
    blue.turn { cardAction2(ExtractorBalloons) }
    pink.turn { playProject(Moss, 4) }
    blue.pass()
    pink.pass()
    pink.wgt("OceanTile<Hellas_4_1>")
  }

  private fun generation7() {
    pink.buyCards(CarbonNanosystems, EcologicalZone)
    pink.discardUnselectedProjectCards(CeosFavoriteProject, VenusWaystation)
    blue.buyCards(IoSulphurResearch, AiCentral)
    blue.discardUnselectedProjectCards(SpaceElevator, IndustrialCenter)

    // Database save 173 evidence: after both research purchases.
    blue.assertProduction(m = 9, s = 0, t = 0, p = 16, e = 1, h = 4)
    blue.assertResources(m = 34, s = 1, t = 0, p = 17, e = 1, h = 12)
    pink.assertResources(m = 24, s = 1, t = 2, p = 10, e = 0, h = 13)
    pink.assertProduction(m = 1, s = 1, t = 1, p = 6, e = 0, h = 2)
    assertSidebar(gen = 7, temp = -12, oxygen = 9, oceans = 5, venus = 2)

    blue.turn {
      convertPlants { placeTile(3, 6) }
      convertPlants { placeTile(1, 4) }
    }
    pink.turn {
      playProject(EcologicalZone, 12) { placeTile(2, 3) }
      convertPlants {
        placeTile(7, 6)
        draw(FieldCappedCity)
      }
    }
    blue.turn {
      assignWildTag("PowerTag")
      assignWildTag("PowerTag")
      playProject(PowerGrid, 17).expect("PROD[4 Energy]")
      playProject(UndergroundCity, 13, steel = 2) { placeTile(7, 7) }
    }
    pink.turn {
      cardAction1(marsNomads)
      convertHeat()
    }
    // Unsupported component: the Nomads' destination bonus placed an ocean and granted steel.
    pink.exMachina("-2 MC, Steel, OceanTile<Hellas_6_8>, TerraformRating")
    blue.turn { claimMilestone(cn("Producer")) }
    pink.turn {
      playProject(BusinessNetwork, 1)
      cardAction1(BusinessNetwork) { buyCards(ImportOfAdvancedGhg) }
    }
    blue.turn { convertHeat() }
    pink.turn {
      playProject(ImportOfAdvancedGhg, titanium = 2)
      sellPatents(Ants, ExtremeColdFungus)
    }
    blue.turn { cardAction1(ExtractorBalloons) }
    pink.turn { playProject(NitrophilicMoss, 8) }
    blue.pass()
    pink.pass()
    blue.wgt("OxygenStep")
  }

  private fun generation8() {
    pink.buyCards(Pets, AsteroidCard)
    pink.discardUnselectedProjectCards(FreyjaBiodomes, WaterImportFromEuropa)
    blue.buyCards(AqueductSystems, OlympusConference, NoctisFarming, VenusSoils)

    // Database save 214 evidence: after both research purchases.
    blue.assertResources(m = 30, s = 2, t = 0, p = 20, e = 3, h = 9)
    blue.assertProduction(m = 9, s = 2, t = 0, p = 16, e = 3, h = 4)
    pink.assertResources(m = 26, s = 3, t = 1, p = 10, e = 0, h = 9)
    pink.assertProduction(m = 0, s = 1, t = 1, p = 8, e = 0, h = 4)
    assertSidebar(gen = 8, temp = -8, oxygen = 13, oceans = 6, venus = 2)

    pink.turn {
      convertPlants { placeTile(5, 4) }
      playProject(Pets, 7)
    }
    blue.turn {
      playProject(OlympusConference, 5, steel = 2).expect("Science<$OlympusConference>")
      // Research Outpost and Olympus Conference supply the other two science tags.
      assignWildTag("ScienceTag")
      playProject(AiCentral, 20) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
        draw(InterstellarColonyShip)
      }
    }
    pink.turn {
      cardAction1(BusinessNetwork) {
        discardUnselectedProjectCards(LawSuit)
      }
    }
    blue.turn {
      playProject(AqueductSystems, 8) {
        discardProjectCardsFromDeck(
            ProjectInspection,
            VenusianAnimals,
            Stratopolis,
            Sponsors,
            CyberiaSystems,
            Fish,
            NeptunianPowerConsultants,
            WaterToVenus,
            InterplanetaryTrade,
            Birds,
            SpecialDesign,
            Sabotage,
            SnowAlgae,
            ViralEnhancers,
            TollStation,
            TransNeptuneProbe,
            Cartel,
            PowerSupplyConsortium,
            SolarLogistics,
            WavePower,
            ProtectedGrowth,
            Farming,
            Satellites,
        )
        draw(UndergroundDetonations, HeatTrappers, MagneticFieldGeneratorsPromo)
      }
      cardAction1(AiCentral) { draw(FloatingHabs, ArcticAlgae) }
    }
    pink.turn { cardAction1(marsNomads) }
    // Unsupported component: the Nomads' new area supplied two heat.
    pink.exMachina("2 Heat")
    blue.turn {
      convertPlants { placeTile(7, 8) }
      convertPlants { placeTile(1, 5) }
    }
    pink.turn { convertHeat() }
    blue.turn { playProject(FloatingHabs, 4) }
    pink.turn { playProject(KelpFarming, 17) }
    blue.turn {
      sellPatents(UndergroundDetonations)
      cardAction1(FloatingHabs) { addCardResources(ExtractorBalloons) }
    }
    pink.pass()
    blue.turn {
      cardAction2(ExtractorBalloons)
      convertHeat()
      pass()
    }
    pink.wgt("OceanTile<Hellas_3_1>")
  }

  private fun generation9() {
    blue.buyCards(HiredRaiders, Algae)
    blue.discardUnselectedProjectCards(MiningExpedition, Livestock)
    pink.buyCards(Comet, Virus)
    pink.discardUnselectedProjectCards(PeroxidePower, DesignedMicroorganisms)

    // Database save 257 evidence: after both research purchases.
    blue.assertResources(m = 38, s = 2, t = 0, p = 21, e = 2, h = 8)
    blue.assertProduction(m = 9, s = 2, t = 0, p = 16, e = 2, h = 4)
    pink.assertResources(m = 32, s = 6, t = 2, p = 16, e = 0, h = 7)
    pink.assertProduction(m = 2, s = 1, t = 1, p = 11, e = 0, h = 4)
    assertSidebar(gen = 9, temp = -4, oxygen = 14, oceans = 7, venus = 4)

    blue.turn {
      convertHeat()
      stdProject("AsteroidSP") { placeTile(2, 1) }
    }
    pink.turn {
      playProject(AsteroidCard, 8, titanium = 2) { doTask("-3 Plant<Blue>") }
      playProject(Comet, 15, titanium = 2) {
        placeTile(1, 1)
        doTask("-3 Plant<Blue>")
      }
    }
    blue.turn {
      cardAction1(AiCentral) { draw(InventionContest, Zeppelins) }
      expectProjectCards(MediaArchives, Mine, StratosphericBirds)
      playProject(InventionContest, 1) {
        draw(MediaArchives)
        discardUnselectedProjectCards(Mine, StratosphericBirds)
      }
    }
    pink.turn {
      playProject(Virus, 1) { doTask("-5 Plant<Blue>") }
      convertPlants { placeTile(6, 4) }
    }
    blue.turn {
      // Three printed science tags remain in play; Invention Contest is already an event.
      assignWildTag("ScienceTag")
      assignWildTag("ScienceTag")
      playProject(InterstellarColonyShip, 23)
    }
    // Unsupported component: this Nomads destination supplies the heat spent by the next action.
    pink.exMachina("2 Heat")
    pink.turn {
      cardAction1(marsNomads)
      convertHeat()
    }
    blue.turn {
      playProject(HiredRaiders, 0) { doTask("3 MC<Blue> FROM MC<Pink>") }
    }
    pink.turn { convertPlants { placeTile(7, 5) } }
    blue.turn { fundAward(cn("Magnate"), 8) }
    pink.turn { sellPatents(FieldCappedCity) }
    blue.turn { convertPlants { placeTile(8, 8) } }
    pink.turn {
      cardAction1(BusinessNetwork) { discardUnselectedProjectCards(MiningQuota) }
      playProject(CarbonNanosystems, steel = 7) {
        doTask("-ProjectCard")
        discard(Plantation)
        draw(EosChasmaNationalPark)
      }
    }
    blue.turn { sellPatents(ArcticAlgae) }
    pink.pass()
    blue.turn {
      cardAction1(FloatingHabs) { addCardResources(ExtractorBalloons) }
      cardAction2(ExtractorBalloons)
      pass()
    }
    blue.wgt("TemperatureStep")
  }

  private fun generation10() {
    blue.buyCards(AirScrappingExpedition, TropicalResort)
    blue.discardUnselectedProjectCards(ProtectedValley, AcquiredCompany)
    pink.buyCards(InvestmentLoan, PublicBaths)
    pink.discardUnselectedProjectCards(OreProcessor, LightningHarvest)

    // Database save 307 evidence: after both research purchases.
    blue.assertResources(m = 42, s = 4, t = 1, p = 20, e = 2, h = 6)
    blue.assertProduction(m = 9, s = 2, t = 0, p = 16, e = 2, h = 4)
    pink.assertResources(m = 42, s = 1, t = 1, p = 15, e = 0, h = 5)
    pink.assertProduction(m = 2, s = 1, t = 1, p = 11, e = 0, h = 4)
    assertSidebar(gen = 10, temp = 8, oxygen = 14, oceans = 9, venus = 6)

    pink.turn {
      stdProject("AirScrappingSP")
      draw(Decomposers)
      cardAction1(BusinessNetwork) { discardUnselectedProjectCards(AerialMappers) }
    }
    blue.turn {
      cardAction1(AiCentral) { draw(ConvoyFromEuropa, Grass) }
      // The source records an explicit unused second action.
    }
    pink.turn {
      stdProject("CitySP") { placeTile(5, 3) }
      convertPlants { placeTile(4, 2) }
    }
    blue.turn {
      playProject(AirScrappingExpedition, 12) { addCardResources(FloatingHabs, 3) }
    }
    pink.turn {
      playProject(InvestmentLoan, 0)
      playProject(AstraMechanica, 7) {
        doWithoutAutoExec(pink) {
          doTask("ProjectCard FROM PlayedEvent<Class<$InvestmentLoan>>")
          returnToHand(InvestmentLoan)
          doTask("ProjectCard FROM PlayedEvent<Class<$Harvest>>")
          returnToHand(Harvest)
        }
        doTask("-ProjectCard")
        discard(Decomposers)
        draw(GreatDamPromo)
      }
    }
    blue.turn { fundAward(cn("Suburbian"), 14) }
    pink.turn {
      playProject(Harvest, 4)
      cardAction1(marsNomads)
    }
    // Unsupported component: this Nomads destination supplied a project card.
    pink.exMachina("ProjectCard")
    pink.draw(ColonizerTrainingCamp)
    blue.turn {
      playProject(NoctisFarming, 1, steel = 4)
      convertPlants { placeTile(2, 6) }
    }
    pink.turn {
      playProject(InvestmentLoan, 0)
      fundAward(cn("Excentric"), 20)
    }
    blue.turn {
      convertPlants {
        placeTile(3, 7)
        draw(Atmoscoop)
      }
      convertPlants { placeTile(7, 9) }
    }
    pink.turn {
      convertPlants { placeTile(4, 3) }
      playProject(PublicBaths, 2, steel = 2)
    }
    blue.turn {
      playProject(Atmoscoop, 15, titanium = 2) {
        doTask("2 VenusStep")
        addCardResources(ExtractorBalloons, 2)
      }
      cardAction2(ExtractorBalloons)
    }
    pink.turn {
      sellPatents(EosChasmaNationalPark, GreatDamPromo, ColonizerTrainingCamp)
      playProject(ReleaseOfInertGases, 14)
    }
    blue.turn {
      sellPatents(GreatEscarpmentConsortium)
      playProject(MediaArchives, 7)
    }
    pink.pass()
    blue.turn {
      playProject(TropicalResort, 12)
      sellPatents(
          Grass,
          MagneticFieldGeneratorsPromo,
          HeatTrappers,
          VenusSoils,
          IoSulphurResearch,
          ConvoyFromEuropa,
          Zeppelins,
          Algae,
      )
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      playProject(NeutralizerFactory, 6)
      pass()
    }

    assertSidebar(gen = 10, temp = 8, oxygen = 14, oceans = 9, venus = 18)

    pink.convertPlants { placeTile(2, 2) }
    pink.convertPlants { placeTile(5, 2) }
    pink.declineTask()
    blue.convertPlants { placeTile(8, 7) }
    blue.convertPlants { placeTile(9, 9) }
    blue.declineTask()

    assertCardTrackingComplete()
    blue.cardsInHand shouldBe emptySet()
    pink.cardsInHand shouldBe emptySet()
    checkHandSizes()
    engine.assertCounts(1 to "EndPhase")

    blue.assertCounts(45 to "TerraformRating", 102 to "VictoryPoint", 1 to "Victory")
    pink.assertCounts(41 to "TerraformRating", 85 to "VictoryPoint", 0 to "Victory")

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Blue>") shouldBe 10
    score.net("Milestone", "VictoryPoint<Pink>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Blue>") shouldBe 10
    score.net("FirstPlace", "VictoryPoint<Pink>") shouldBe 5
    score.net("GreeneryTile", "VictoryPoint<Blue>") shouldBe 12
    score.net("GreeneryTile", "VictoryPoint<Pink>") shouldBe 12
    score.net("CityTile", "VictoryPoint<Blue>") shouldBe 11
    score.net("CityTile", "VictoryPoint<Pink>") shouldBe 11
    score.net("Card", "VictoryPoint<Blue>") shouldBe 14
    score.net("Card", "VictoryPoint<Pink>") shouldBe 11

    blue.assertResources(m = 63, s = 2, t = 0, p = 0, e = 2, h = 15)
    blue.assertProduction(m = 13, s = 2, t = 0, p = 16, e = 2, h = 2)
    pink.assertResources(m = 48, s = 1, t = 2, p = 1, e = 0, h = 9)
    pink.assertProduction(m = 1, s = 1, t = 1, p = 11, e = 0, h = 4)
  }
}
