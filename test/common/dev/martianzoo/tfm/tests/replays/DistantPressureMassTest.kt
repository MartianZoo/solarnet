package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete archive replay: Distant Pressure Mass (gaf4dfdc697db)
// http://newazure.local:8080/the-end?id=paffe109dfc39
internal class DistantPressureMassTest : CardTrackingFullGameTest() {
  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, ColoniesExpansion, PromoCardPack

          Ecologist, Terraformer29, Terran, Mayor, Merchant, Researcher
          Electrician, Industrialist, Highlander, Investor, Scientist, Manufacturer
          Ceres, Ganymede, Io, Miranda, Titan
          """,
          "Keen",
          "Been",
      )
  // Solarnet's Terran is the archive's Terran5 milestone. It was available but never claimed.

  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  private val keen
    get() = p1

  private val been
    get() = p2

  @Test
  internal fun distantPressureMass() {
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
    generation11()
  }

  private fun generation1() {
    // Technically I think these discards happen during InitialResearchPhase which we don't model,
    // but we should soon...
    keen.discardUnselectedProjectCards(
        AirScrappingExpedition,
        Virus,
        IshtarExpedition,
        EnergySaving,
        NoctisCity,
        LavaFlows,
    )
    been.discardUnselectedProjectCards(
        SubCrustMeasurements,
        TradeEnvoys,
        SfMemorial,
        OrbitalCleanup,
        IceCapMelting,
        HiTechLab,
    )

    keen.playCorp(SagittaFrontierServices) {
      keen.discardProjectCardsFromDeck(
          AtalantaPlanitiaLab,
          PowerPlant,
          ArcticAlgae,
          Worms,
          MercurianAlloys,
          WeatherBalloons,
          FreyjaBiodomes,
          NitrogenRichAsteroid,
          StratosphericBirds,
          MagneticFieldDome,
          GhgProducingBacteria,
          VenusWaystation,
          CometForVenus,
          Research,
      )
      draw(CaretakerContract)
      buyCards(CuttingEdgeTechnology, SearchForLife, CommunityServices, DustSeals)
    }
    been.playCorp(Polyphemos) {
      buyCards(ExtractorBalloons, LunarExports, Flooding, CorporateStronghold).expect("-20 MC")
    }

    keen.turn {
      playPrelude(IndustrialComplex)
      playPrelude(AppliedScience).expect("6 Science, 4 MC")
    }

    been.turn {
      playPrelude(StrategicBasePlanning) {
        doTask("Colony<Ganymede>")
        placeTile(9, 7)
        placeTile(5, 6)
        draw(UndergroundCity)
      }
      playPrelude(EarlySettlement) { placeTile(2, 6) }
    }

    keen.turn { cardAction1(AppliedScience) { doTask("Titanium") } }
    been.turn {
      playProject(LunarExports, 4, titanium = 5) { doTask("PROD[5 MC]") }
    }
    keen.pass()
    been.turn {
      playProject(Flooding, 7) { placeTile(5, 7) }.expect("3 Heat, -5 MC")
      pass()
    }
    keen.wgt("VenusStep")
  }

  private fun generation2() {
    keen.buyCards(DirectedHeatUsage, BusinessNetwork, OptimalAerobraking)
    keen.discardUnselectedProjectCards(LunaMetropolis)
    been.buyCards(VenusianAnimals, MiningColony, EcologyResearch)
    been.discardUnselectedProjectCards(WarpDrive)

    been.turn { playProject(ExtractorBalloons, 21) }
    keen.turn { playProject(CuttingEdgeTechnology, 12) }
    been.turn { cardAction2(ExtractorBalloons).expect("-2 Floater, VenusStep") }
    keen.turn { playProject(DustSeals, 0) }
    been.pass()
    keen.turn {
      playProject(DirectedHeatUsage, 1)
      playProject(CommunityServices, 13)
      playProject(BusinessNetwork, 4)
      cardAction1(BusinessNetwork) {
        discardUnselectedProjectCards(SoilEnrichment)
      }
      playProject(SearchForLife, 1)
      cardAction1(SearchForLife) {
            discardProjectCardsFromDeck(EarthElevator)
            // Earth Elevator has no microbe tag.
            declineTask("Science<$SearchForLife>?")
          }
          .expect("0 Science")
      cardAction1(AppliedScience) { doTask("Titanium") }
      pass()
    }
    been.wgt("OceanTile<Hellas_4_6>")
  }

  private fun generation3() {
    keen.buyCards(MarsUniversity, BreathingFilters, MethaneFromTitan, OreProcessor)

    // Screenshot evidence: generation-3-research.png, after Keen's purchase and before Been's.
    with(keen) {
      assertResources(m = 20, s = 2, t = 4, p = 2, e = 1, h = 3)
      assertProduction(m = 6, s = 1, t = 1, p = 1, e = 1, h = 1)
      assertDashMiddle(
          played = 9,
          vp = 23,
          tr = 20,
          hand = 6,
      )
      assertTags(but = 1, sct = 2, eat = 1)
      assertDashRight(events = 0, tagless = 5, cities = 0, colonies = 0)
    }
    with(been) {
      assertResources(m = 41, s = 0, t = 0, p = 5, e = 0, h = 3)
      assertProduction(m = 10, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertDashMiddle(
          played = 6,
          vp = 22,
          tr = 23,
          hand = 5,
      )
      assertTags(but = 2, spt = 2, eat = 1, vet = 1, cit = 2)
      assertDashRight(events = 1, tagless = 1, cities = 2, colonies = 1)
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 3, venus = 4)
    engine.assertCounts(5 to "Tile")

    been.buyCards(AstraMechanica, ForcedPrecipitation)
    been.discardUnselectedProjectCards(MartianSurvey, HiredRaiders)

    keen.turn {
      cardAction1(AppliedScience) { doTask("Energy") }
      claimMilestone(cn("Merchant"))
    }
    been.turn {
      playProject(ForcedPrecipitation, 8)
      playProject(MiningColony, 20) {
        doTask("Colony<Titan>")
        addCardResources(ForcedPrecipitation)
      }
    }
    keen.turn {
      playProject(MarsUniversity, 4, steel = 2) {
        doTask("-ProjectCard")
        discard(CaretakerContract)
        draw(KelpFarming)
      }
      shouldThrow<RequirementException> {
        claimMilestone(cn("Researcher"))
      }
      assignWildTag(AppliedScience, "ScienceTag")
      claimMilestone(cn("Researcher"))
    }
    been.turn {
      cardAction1(ForcedPrecipitation)
      cardAction1(ExtractorBalloons)
    }
    keen.turn {
      cardAction1(DirectedHeatUsage) { doTask("4 MC") }
      cardAction1(BusinessNetwork) { buyCards(EarthCatapult) }
    }
    been.pass()
    keen.pass()
    keen.wgt("OxygenStep")
  }

  private fun generation4() {
    // Screenshot evidence: generation-4-research.png, before either player's purchase.
    with(keen) {
      assertResources(m = 27, s = 1, t = 5, p = 3, e = 1, h = 3)
      assertProduction(m = 6, s = 1, t = 1, p = 1, e = 1, h = 1)
      assertDashMiddle(
          played = 10,
          vp = 34,
          tr = 20,
          hand = 6,
      )
      assertTags(but = 2, sct = 3, eat = 1)
      assertDashRight(events = 0, tagless = 5, cities = 0, colonies = 0)
    }
    with(been) {
      assertResources(m = 34, s = 0, t = 1, p = 7, e = 0, h = 3)
      assertProduction(m = 10, s = 0, t = 1, p = 2, e = 0, h = 0)
      assertDashMiddle(
          played = 8,
          vp = 22,
          tr = 23,
          hand = 5,
      )
      assertTags(but = 2, spt = 3, eat = 1, vet = 2, cit = 2)
      assertDashRight(events = 1, tagless = 1, cities = 2, colonies = 2)
    }
    assertSidebar(gen = 4, temp = -30, oxygen = 1, oceans = 3, venus = 4)
    engine.assertCounts(5 to "Tile")

    been.buyCards(InventionContest)
    been.discardUnselectedProjectCards(JupiterFloatingStation, ElectroCatapult, RedShips)
    keen.buyCards(ResearchCoordination, LunarMining)
    keen.discardUnselectedProjectCards(Hospitals, SolarPower)

    been.turn {
      cardAction2(ExtractorBalloons)
      cardAction2(ForcedPrecipitation) {
            draw(MiningRights)
          }
          .expect("-2 Floater, VenusStep, ProjectCard")
    }
    keen.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    been.turn {
      stdAction("TradeSA") { doTask("Trade<Ganymede>") }.expect("5 Plant")
      convertPlants {
        placeTile(3, 7)
        draw(LunaGovernor)
      }
    }
    keen.turn { playProject(EarthCatapult, 23) }
    been.turn {
      playProject(MiningRights, 9) {
        placeTile(6, 2)
      }
    }
    keen.turn { playProject(ResearchCoordination, 2) }
    been.turn { stdProject("PowerPlantSP") }
    keen.turn {
      cardAction1(BusinessNetwork) {
        discardUnselectedProjectCards(GalileanWaystation)
      }
      cardAction1(SearchForLife) {
        discardProjectCardsFromDeck(IoSulphurResearch)
        // Io Sulphur Research has no microbe tag.
        declineTask("Science<$SearchForLife>?")
      }
    }
    been.pass()
    keen.turn {
      cardAction1(AppliedScience) { doTask("Titanium") }
      pass()
    }
    been.wgt("VenusStep")
  }

  private fun generation5() {
    been.buyCards(SpaceMirrors, SisterPlanetSupport)
    been.discardUnselectedProjectCards(MiningQuota, CrashSiteCleanup)
    keen.buyCards(FueledGenerators, CupolaCity, GiantIceAsteroid)
    keen.discardUnselectedProjectCards(FloatingRefinery)

    keen.turn { stdAction("TradeSA", 3) { doTask("Trade<Ceres>") } }
    been.turn {
      playProject(CorporateStronghold, 11) { placeTile(3, 5) }.expect("PROD[3 MC, -Energy], -9 MC")
      claimMilestone(cn("Mayor"))
    }
    keen.turn { playProject(FueledGenerators, 0) }
    been.turn {
      stdAction("TradeSA", 3) {
        doTask("Trade<Titan>")
        addCardResources(ExtractorBalloons, 1)
        addCardResources(ForcedPrecipitation, 3)
      }
    }
    keen.turn { cardAction1(AppliedScience) { doTask("Titanium") } }
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn {
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(LunarMining, 9)
    }
    been.turn { playProject(SisterPlanetSupport, 7) }
    keen.turn {
      playProject(CupolaCity, steel = 6) { placeTile(4, 4) }.expect("PROD[3 MC, -Energy], -4 Steel")
    }
    been.turn { cardAction2(ForcedPrecipitation) }
    keen.turn {
      cardAction1(BusinessNetwork) {
        discardUnselectedProjectCards(ProtectedValley)
      }
    }
    been.pass()
    keen.turn {
      cardAction1(SearchForLife) {
        discardProjectCardsFromDeck(Shuttles)
        // Shuttles has no microbe tag.
        declineTask("Science<$SearchForLife>?")
      }
      playProject(MethaneFromTitan, 9, titanium = 5)
      pass()
    }
    keen.wgt("OxygenStep")
  }

  private fun generation6() {
    keen.buyCards(LightningHarvest, WaterSplittingPlant)
    keen.discardUnselectedProjectCards(SpacePortColony, SoilFactory)
    been.buyCards(EarthOffice, SolarReflectors)
    been.discardUnselectedProjectCards(GhgImportFromVenus, ImportedNutrients)

    been.turn {
      cardAction2(ExtractorBalloons)
      cardAction2(ForcedPrecipitation)
    }
    keen.turn { stdAction("TradeSA", 3) { doTask("Trade<Io>") } }
    been.turn { convertPlants { placeTile(2, 5) } }
    keen.turn { cardAction1(BusinessNetwork) { buyCards(RestrictedArea) } }
    been.turn { playProject(EarthOffice, 1) }
    keen.turn {
      playProject(RestrictedArea, 9) {
        placeTile(3, 6)
        doTask("-ProjectCard")
        discard(OreProcessor)
        draw(IceMoonColony)
      }
    }
    been.turn { playProject(LunaGovernor, 0) }
    keen.turn {
      cardAction1(RestrictedArea) {
        draw(MolecularPrinting)
      }
    }
    been.turn {
      intentionalUnderpay()
      playProject(SolarReflectors, 23)
    }
    keen.turn { convertPlants { placeTile(4, 5) } }
    been.turn {
      intentionalUnderpay()
      playProject(SpaceMirrors, 3)
    }
    keen.turn { cardAction1(AppliedScience) { doTask("Titanium") } }
    been.turn { cardAction1(SpaceMirrors) }
    keen.turn {
      playProject(LightningHarvest, 4).expect("PROD[1 MC, Energy], -3 MC")
    }
    been.pass()
    keen.turn {
      playProject(OptimalAerobraking, 2, titanium = 1)
      convertHeat()
      stdProject("PowerPlantSP")
      pass()
    }
    been.wgt("VenusStep")
  }

  private fun generation7() {
    been.buyCards(FloaterPrototypes, UrbanDecomposers, BusinessContacts)
    been.discardUnselectedProjectCards(ProjectInspection)
    keen.buyCards(RadSuits, RadChemFactory, SterlingVents, SpecialDesign)

    keen.turn {
      playProject(RadSuits, 2)
      playProject(WaterSplittingPlant, steel = 4)
    }
    been.turn {
      playProject(VenusianAnimals, 15)
      expectProjectCards(ReleaseOfInertGases, SubterraneanReservoir, JovianLanterns)
      playProject(InventionContest, 2) {
        draw(SubterraneanReservoir)
        discardUnselectedProjectCards(ReleaseOfInertGases, JovianLanterns)
      }
    }
    keen.turn {
      playProject(GiantIceAsteroid, 25, titanium = 3) {
            doTask("-4 Plant<Been>")
            placeTile(4, 7)
            placeTile(7, 3)
          }
          .expect("-18 MC, -Titanium, Plant, 3 Heat")
    }
    been.turn {
      expectProjectCards(CorroderSuits, FusionPower, DeepWellHeating, QuantumCommunications)
      playProject(BusinessContacts, 4) {
        draw(CorroderSuits, DeepWellHeating)
        discardUnselectedProjectCards(FusionPower, QuantumCommunications)
      }
    }
    keen.turn {
      convertHeat()
      convertHeat().expect("PROD[Heat]")
    }
    been.turn { playProject(CorroderSuits, 8) { addCardResources(VenusianAnimals) } }
    keen.turn {
      cardAction1(RestrictedArea) {
        draw(MoholeArea)
      }
    }
    been.turn { stdAction("TradeSA", 3) { doTask("Trade<Ganymede>") } }
    keen.turn { playProject(SterlingVents, 1, steel = 1) }
    been.turn { convertHeat() }
    keen.turn { cardAction1(WaterSplittingPlant) }
    been.turn {
      playProject(FloaterPrototypes, 2) { addCardResources(ForcedPrecipitation) }.expect("Animal")
    }
    keen.turn { playProject(RadChemFactory, steel = 3) }
    been.turn { cardAction2(ForcedPrecipitation) }
    keen.turn { cardAction1(BusinessNetwork) { buyCards(Atmoscoop) } }
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn {
      cardAction1(SearchForLife) {
        discardProjectCardsFromDeck(Herbivores)
        // Herbivores has no microbe tag.
        declineTask("Science<$SearchForLife>?")
      }
    }
    been.pass()
    keen.pass()
    keen.wgt("VenusStep")
  }

  private fun generation8() {
    // Screenshot evidence: generation-8-research.png, before either player's purchase.
    with(keen) {
      assertResources(m = 42, s = 2, t = 5, p = 8, e = 4, h = 5)
      assertProduction(m = 10, s = 1, t = 3, p = 3, e = 4, h = 3)
      assertDashMiddle(
          played = 24,
          vp = 54,
          tr = 31,
          hand = 7,
      )
      assertTags(but = 7, spt = 2, sct = 4, pot = 3, eat = 3, jot = 1, cit = 1)
      assertDashRight(events = 1, tagless = 7, cities = 1, colonies = 0)
    }
    with(been) {
      assertResources(m = 57, s = 0, t = 4, p = 7, e = 1, h = 7)
      assertProduction(m = 20, s = 0, t = 2, p = 2, e = 1, h = 5)
      assertDashMiddle(
          played = 20,
          vp = 45,
          tr = 33,
          hand = 6,
      )
      assertTags(but = 4, spt = 5, sct = 1, pot = 1, eat = 5, vet = 5, ant = 1, cit = 3)
      assertDashRight(events = 4, tagless = 1, cities = 3, colonies = 2)
    }
    assertSidebar(gen = 8, temp = -18, oxygen = 6, oceans = 5, venus = 22)
    engine.assertCounts(14 to "Tile")

    been.buyCards(AerialMappers)
    been.discardUnselectedProjectCards(
        IndenturedWorkers,
        ProtectedHabitats,
        HermeticOrderOfMars,
    )
    keen.buyCards(ProductiveOutpost, Hackers, Harvest)
    keen.discardUnselectedProjectCards(Farming)

    been.turn {
      stdProject("GreenerySP") { placeTile(2, 4) }
      convertPlants { placeTile(1, 5) }
    }
    keen.turn {
      convertPlants { placeTile(5, 4) }
      fundAward(cn("Scientist"), 8)
    }
    been.turn {
      stdAction("TradeSA", 3) {
        doTask("Trade<Titan>")
        addCardResources(ExtractorBalloons, 1)
        addCardResources(ForcedPrecipitation, 3)
      }
      playProject(SubterraneanReservoir, 11) { placeTile(5, 8) }
    }
    keen.turn {
      playProject(Atmoscoop, 3, titanium = 5) { doTask("2 VenusStep") }
    }
    been.turn {
      cardAction2(ExtractorBalloons)
      cardAction2(ForcedPrecipitation)
    }
    keen.turn { playProject(ProductiveOutpost, 0) }
    been.turn {
      playProject(AstraMechanica, 7) {
            doWithoutAutoExec(been) {
              doTask("ProjectCard FROM PlayedEvent<Class<$InventionContest>>")
              returnToHand(InventionContest)
              doTask("ProjectCard FROM PlayedEvent<Class<$Flooding>>")
              returnToHand(Flooding)
            }
          }
          .expect("Animal")
      playProject(Flooding, 7) { placeTile(6, 8) }
    }
    keen.turn {
      cardAction1(RestrictedArea) {
        draw(AsteroidCard)
      }
    }
    been.turn {
      expectProjectCards(Bushes, MassConverter, UrbanizedArea)
      playProject(InventionContest, 2) {
        draw(UrbanizedArea)
        discardUnselectedProjectCards(Bushes, MassConverter)
      }
      playProject(DeepWellHeating, 9, steel = 2)
    }
    keen.turn {
      cardAction1(BusinessNetwork) {
        buyCards(SolarProbe)
      }
    }
    been.pass()
    keen.turn {
      cardAction1(WaterSplittingPlant)
      playProject(MolecularPrinting, 9) {
            doTask("-ProjectCard")
            discard(MoholeArea)
            draw(SpacePort)
          }
          .expect("-2 MC")
      cardAction1(SearchForLife) {
        discardProjectCardsFromDeck(BigAsteroid)
        // Big Asteroid has no microbe tag.
        declineTask("Science<$SearchForLife>?")
      }
      playProject(Hackers, 1) { doTask("PROD[-2 MC<Been>]") }.expect("PROD[2 MC, -Energy], 3 MC")
      playProject(BreathingFilters, 7) {
        doTask("-ProjectCard")
        discard(SpacePort)
        draw(VenusMagnetizer)
      }
      playProject(KelpFarming, 13)
      pass()
    }
    been.wgt("OceanTile<Hellas_6_7>")
  }

  private fun generation9() {
    been.buyCards(RimFreighters, LunarBeam)
    been.discardUnselectedProjectCards(SulphurExports, TopsoilContract)
    keen.buyCards(AdvancedAlloys, CyberiaSystems)
    keen.discardUnselectedProjectCards(VenusTradeHub, WaterToVenus)

    keen.turn {
      convertPlants { placeTile(5, 5) }
      cardAction1(WaterSplittingPlant)
    }

    // Screenshot evidence: generation-9-actions.png, after Keen's opening turn and before Been's.
    with(keen) {
      assertResources(m = 48, s = 5, t = 3, p = 0, e = 0, h = 9)
      assertProduction(m = 14, s = 1, t = 3, p = 6, e = 3, h = 3)
      assertDashMiddle(
          played = 30,
          vp = 73,
          tr = 37,
          hand = 8,
      )
      assertTags(but = 7, spt = 3, sct = 6, pot = 3, eat = 3, jot = 2, plt = 1, cit = 1)
      assertDashRight(events = 1, tagless = 9, cities = 1, colonies = 0)
    }
    with(been) {
      assertResources(m = 50, s = 0, t = 3, p = 3, e = 2, h = 13)
      assertProduction(m = 18, s = 0, t = 2, p = 2, e = 2, h = 5)
      assertDashMiddle(
          played = 23,
          vp = 59,
          tr = 41,
          hand = 7,
      )
      assertTags(but = 5, spt = 5, sct = 2, pot = 2, eat = 5, vet = 5, ant = 1, cit = 3)
      assertDashRight(events = 5, tagless = 1, cities = 3, colonies = 2)
    }
    assertSidebar(gen = 9, temp = -14, oxygen = 12, oceans = 8, venus = 30)
    engine.assertCounts(21 to "Tile")

    been.turn {
      playProject(RimFreighters, 1, titanium = 1)
      stdAction("TradeSA", 3) { doTask("Trade<Ceres>") }
    }
    keen.turn {
      cardAction1(RestrictedArea) {
        draw(LocalHeatTrapping)
      }
    }
    been.turn {
      playProject(UndergroundCity, 6, steel = 6) { placeTile(1, 4) }
      fundAward(cn("Highlander"), 14)
    }
    keen.turn {
      cardAction1(BusinessNetwork) {
        discardUnselectedProjectCards(TitanShuttles)
      }
      playProject(AdvancedAlloys, 7) {
        doTask("-ProjectCard")
        discard(CyberiaSystems)
        draw(SymbioticFungus)
      }
    }
    been.turn { playProject(LunarBeam, 10) }
    keen.turn {
      // The archive pays Solar Probe entirely in M€ despite Keen retaining usable titanium.
      assignWildTag(AppliedScience, "ScienceTag")
      intentionalUnderpay()
      playProject(SolarProbe, 7) {
            draw(Algae, CloudTourism, SpinInducingAsteroid)
            // The engine does not count Solar Probe's own science tag, so restore the third draw.
            keen.exMachina("ProjectCard")
            doTask("-ProjectCard")
            discard(SpinInducingAsteroid)
            draw(Trees)
          }
          .expect("2 ProjectCard, 3 Heat, -4 MC")
    }
    been.turn { playProject(AerialMappers, 11) }
    keen.turn {
      playProject(AsteroidCard, titanium = 3) { doTask("-3 Plant<Been>") }
          .expect("-Titanium, 3 MC, 3 Heat")
      playProject(IceMoonColony, 13, titanium = 2) {
        doTask("Colony<Ganymede>")
        placeTile(2, 1)
      }
    }
    been.turn { playProject(UrbanDecomposers, 6) }
    keen.turn { playProject(Harvest, 0) }
    been.turn { cardAction1(AerialMappers) { addCardResources(AerialMappers) } }
    keen.turn { playProject(VenusMagnetizer, 3) }
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn {
      cardAction1(SearchForLife) {
        discardProjectCardsFromDeck(GanymedeColony)
        // Ganymede Colony has no microbe tag.
        declineTask("Science<$SearchForLife>?")
      }
    }
    been.turn { convertHeat() }
    keen.turn { convertHeat() }
    been.pass()
    keen.turn {
      playProject(SpecialDesign, 2) {
        doTask("-ProjectCard")
        discard(SymbioticFungus)
        draw(Capital)
      }
      playProject(Trees, 9)
      playProject(Algae, 6)
      playProject(LocalHeatTrapping, 0) { doTask("4 Plant") }
      convertPlants { placeTile(3, 3) }
      // Test inference: Cloud Tourism is the only named, never-played card available for this
      // one-card patent sale.
      sellPatents(CloudTourism)
      stdProject("GreenerySP") { placeTile(4, 3) }
      pass()
    }
    keen.wgt("TemperatureStep")
  }

  private fun generation10() {
    been.buyCards(FieldCappedCity)
    been.discardUnselectedProjectCards(ImmigrationShuttles, VenusSoils, IcyImpactors)
    keen.buyCards(LuxuryFoods, PeroxidePower)
    keen.discardUnselectedProjectCards(CeosFavoriteProject, Sponsors)
    been.turn {
      playProject(FieldCappedCity, 23, steel = 3) { placeTile(2, 3) }
      playProject(UrbanizedArea, 10) { placeTile(3, 4) }
    }
    keen.turn {
      stdAction("TradeSA", 2) { doTask("Trade<Io>") }
      convertPlants { placeTile(6, 5) }
    }
    been.turn {
      cardAction2(AerialMappers) {
        draw(BactoviralResearch)
      }
      stdAction("TradeSA", 3) { doTask("Trade<Ganymede>") }.expect("5 Plant<Been>, Plant<Keen>")
    }
    keen.turn {
      playProject(Capital, 1, steel = 7) { placeTile(6, 6) }.expect("PROD[5 MC, -2 Energy], 3 MC")
    }
    been.turn {
      convertPlants { placeTile(9, 6) }
      playProject(EcologyResearch, 21) {
            addCardResources(VenusianAnimals)
          }
          .expect("PROD[2 Plant]")
    }
    keen.turn { convertHeat() }
    been.turn {
      convertHeat()
      convertHeat()
    }
    keen.turn {
      cardAction1(RestrictedArea) {
        draw(Greenhouses)
      }
    }
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn {
      cardAction1(BusinessNetwork) {
        buyCards(Grass)
      }
      playProject(Grass, 7)
    }
    been.pass()
    keen.turn {
      stdProject("CitySP") { placeTile(3, 2) }
      convertPlants { placeTile(4, 2) }
      playProject(PeroxidePower, 2, steel = 1)
      cardAction1(SearchForLife) {
        discardProjectCardsFromDeck(HydrogenToVenus)
        // Hydrogen to Venus has no microbe tag.
        declineTask("Science<$SearchForLife>?")
      }
      playProject(LuxuryFoods, 4)
      stdProject("AsteroidSP")
      pass()
    }
    been.wgt("TemperatureStep")
  }

  private fun generation11() {
    been.buyCards(Plantation)
    been.discardUnselectedProjectCards(Zeppelins, AsteroidMiningConsortium, Potatoes)
    keen.buyCards(NitrogenFromTitan, SubZeroSaltFish)
    keen.discardUnselectedProjectCards(CoronaExtractor, UnexpectedApplication)
    keen.turn {
      playProject(SubZeroSaltFish, 1) { doTask("PROD[-Plant<Been>]") }.expect("0 MC")
      stdAction("TradeSA", 2) {
        doTask("Trade<Miranda>")
        addCardResources(SubZeroSaltFish)
      }
    }
    been.turn {
      convertHeat()
      stdProject("CitySP") { placeTile(5, 3) }
    }
    keen.turn {
      convertHeat()
      stdProject("CitySP") { placeTile(7, 5) }
    }
    been.turn {
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede>") }
      convertPlants { placeTile(1, 3) }
    }
    keen.turn {
      convertPlants {
        placeTile(7, 6)
        draw(InvestmentLoan)
      }
      stdProject("CitySP") { placeTile(8, 8) }
    }
    been.turn {
      playProject(Plantation, 15) { placeTile(1, 2) }
      convertPlants { placeTile(2, 2) }
    }
    keen.turn {
      cardAction1(RestrictedArea) {
        draw(StaticHarvesting)
      }
      convertPlants { placeTile(7, 7) }
    }
    been.turn {
      fundAward(cn("Investor"), 20)
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    }
    keen.turn { playProject(InvestmentLoan, 1) }
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn { cardAction1(SubZeroSaltFish) }
    been.turn {
      // Test inference: Bactoviral Research is Been's only named unplayed card.
      sellPatents(BactoviralResearch)
    }
    keen.turn {
      playProject(Greenhouses, 1, steel = 1).expect("12 Plant")
      convertPlants { placeTile(7, 8) }
    }
    // The archive records Been passing as a second action; defer it to Been's next legal turn.
    been.pass()
    keen.turn {
      intentionalOverpay(1)
      playProject(NitrogenFromTitan, titanium = 6).expect("2 TerraformRating")
    }
    keen.sellPatents(StaticHarvesting)
    keen.turn {
      cardAction1(BusinessNetwork) {
        discardUnselectedProjectCards(SpaceHotels)
      }
      cardAction1(SearchForLife) {
        discardProjectCardsFromDeck(PublicBaths)
        // Public Baths has no microbe tag.
        declineTask("Science<$SearchForLife>?")
      }
      cardAction1(VenusMagnetizer)
      pass()
    }

    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    keen.convertPlants { placeTile(7, 4) }
    keen.convertPlants {
      placeTile(8, 5)
      keen.draw(SolarWindPower)
    }
    keen.declineTask()
    been.convertPlants { placeTile(6, 3) }
    been.declineTask()

    assertCardTrackingComplete()
    keen.cardsInHand shouldBe setOf(SolarWindPower)
    been.cardsInHand shouldBe emptySet()
    checkHandSizes()
    engine.assertCounts(1 to "EndPhase")

    keen.assertCounts(47 to "TerraformRating", 117 to "VictoryPoint", 1 to "Victory")
    been.assertCounts(45 to "TerraformRating", 99 to "VictoryPoint", 0 to "Victory")

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Keen>") shouldBe 10
    score.net("Milestone", "VictoryPoint<Been>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Keen>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Been>") shouldBe 10
    score.net("GreeneryTile", "VictoryPoint<Keen>") shouldBe 12
    score.net("GreeneryTile", "VictoryPoint<Been>") shouldBe 9
    score.net("CityTile", "VictoryPoint<Keen>") shouldBe 19
    score.net("CityTile", "VictoryPoint<Been>") shouldBe 23
    score.net("Card", "VictoryPoint<Keen>") shouldBe 24
    score.net("Card", "VictoryPoint<Been>") shouldBe 7

    keen.assertResources(m = 90, s = 1, t = 4, p = 1, e = 2, h = 3)
    keen.assertProduction(m = 20, s = 1, t = 3, p = 13, e = 2, h = 3)
    been.assertResources(m = 71, s = 5, t = 4, p = 0, e = 2, h = 8)
    been.assertProduction(m = 21, s = 2, t = 2, p = 4, e = 2, h = 7)
  }
}
