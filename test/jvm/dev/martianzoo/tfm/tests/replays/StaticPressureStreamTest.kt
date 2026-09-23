package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.fake.FakeCanon
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete database replay: Static Pressure Stream (g1212dda978b8)
// Source: _local/replays/Game20260912/Heroku-g1212dda978b8/
// http://newazure.local:8080/the-end?id=pe1c5cff0c611
internal class StaticPressureStreamTest :
    CardTrackingFullGameTest(requireEveryProjectCardChangeNamed = true) {
  override val config =
      GameConfig(
          """
          TharsisMap
          VenusNextExpansion, PreludeExpansion, Prelude2CardPack, PromoCardPack
          FakeFloydContinuum, FakeResearchCoordination, FakeResearchNetwork, FakeWildTagUse

          Fundraiser, Philantropist, Planetologist, Sponsor, Briber, Terran
          Scientist, Landlord, Industrialist, Benefactor, Forecaster, Excentric
          """,
          "Nor",
          "Vin",
      )
  override val catalog = TfmCatalog.compose(Canon, FakeCanon)

  private val nor
    get() = p1

  private val vin
    get() = p2

  @Test
  internal fun staticPressureStream() {
    TfmWorkflow.Automatic(agents).launch()
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
    nor.playCorp(Ecoline) {
      buyCards(
          DesignedMicroorganisms,
          ResearchOutpost,
          AquiferPumping,
          UndergroundCity,
          ExtractorBalloons,
          Supermarkets,
      )
    }
    vin.playCorp(CheungShingMars) {
      buyCards(FuelFactory, CarbonateProcessing, OptimalAerobraking, CloudTourism)
    }
    nor.turn {
      playPrelude(AlbedoPlants)
      playPrelude(ProjectEden) {
        // Project Eden opens all three placement choices together.
        doTask("OceanTile<Tharsis_2_6>")
        draw(SfMemorial, EcologicalZone)
        doTask("CityTile<Tharsis_4_7>")
        doTask("GreeneryTile<Tharsis_3_7>")
      }
      // Save 6 identifies Project Eden's three discarded cards.
      discard(ExtractorBalloons, UndergroundCity, SfMemorial)
    }
    vin.turn {
      playPrelude(Supplier)
      playPrelude(FakeResearchNetwork) {
        draw(ProtectedHabitats, SisterPlanetSupport, PhysicsComplex)
      }
    }
    nor.pass()
    vin.turn {
      playProject(FuelFactory, steel = 2)
      playProject(CarbonateProcessing, steel = 2)
      pass()
    }
    nor.wgt("VenusStep")
  }

  private fun generation2() {
    vin.buyCards(Omnicourt, VenusShuttles)
    nor.buyCards(StaticHarvesting, AerialMappers)
    // Database save 18: immediately after both Research purchases in generation 2.
    nor.assertResources(m = 36, s = 1, t = 0, p = 8, e = 0, h = 6)
    nor.assertProduction(m = 0, s = 0, t = 0, p = 3, e = 0, h = 0)
    nor.assertDashMiddle(played = 3, actions = 0, vp = 24, tr = 22, hand = 7)
    nor.assertTags(plt = 3, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 51, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertProduction(m = 5, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 5, actions = 0, vp = 20, tr = 20, hand = 7)
    vin.assertTags(but = 3, pot = 1)
    vin.assertDashRight(events = 0, tagless = 1, cities = 0)
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 1, venus = 2)
    checkHandSizes()

    // Research Network's wild tag supplies Cloud Tourism's missing Earth tag.
    vin.turn {
      vin.exMachina(fakeWildTags("EarthTag"))
      playProject(CloudTourism, 11).expect("PROD[MC]")
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn {
      convertPlants { placeTile(3, 6) }
      playProject(AquiferPumping, 16, steel = 1)
    }
    // Research Network supplies Sister Planet Support's missing Earth tag.
    vin.turn {
      vin.exMachina(fakeWildTags("EarthTag"))
      playProject(SisterPlanetSupport, 7).expect("PROD[3 MC]")
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(6, 9)
      }
    }
    vin.turn { playProject(Omnicourt, 9) }
    nor.pass()
    vin.turn {
      cardAction1(CloudTourism)
      playProject(VenusShuttles, 9) { addCardResources(CloudTourism, 2) }
      vin.exMachina(fakeWildTags("VenusTag"))
      cardAction1(VenusShuttles)
      vin.assertCounts(0 to "FakeWildTagUse")
      pass()
    }
    vin.wgt("OxygenStep")
  }

  private fun generation3() {
    nor.buyCards(LocalShading, Research)
    vin.buyCards(RoboticWorkforce, SmallAnimals)
    // Database save 47: immediately after both Research purchases in generation 3.
    nor.assertResources(m = 32, s = 0, t = 0, p = 5, e = 0, h = 6)
    nor.assertProduction(m = 0, s = 0, t = 0, p = 3, e = 0, h = 0)
    nor.assertDashMiddle(played = 4, actions = 1, vp = 28, tr = 24, hand = 8)
    nor.assertTags(but = 1, plt = 3, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 33, s = 0, t = 2, p = 0, e = 0, h = 6)
    vin.assertProduction(m = 9, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 9, actions = 2, vp = 24, tr = 23, hand = 5)
    vin.assertTags(but = 4, pot = 1, eat = 1, jot = 1, vet = 3)
    vin.assertDashRight(events = 0, tagless = 1, cities = 0)
    assertSidebar(gen = 3, temp = -30, oxygen = 3, oceans = 2, venus = 4)
    checkHandSizes()

    nor.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(6, 8)
      }
      playProject(StaticHarvesting, 5)
    }
    vin.turn {
      playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$CheungShingMars>") }
      claimMilestone(cn("Fundraiser"))
    }
    nor.turn { playProject(AerialMappers, 11) }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { playProject(LocalShading, 4) }
    vin.pass()
    nor.turn {
      cardAction1(AerialMappers) { addCardResources(LocalShading) }
      cardAction2(LocalShading)
      pass()
    }
    nor.wgt("TemperatureStep")
  }

  private fun generation4() {
    nor.buyCards(FakeResearchCoordination)
    vin.buyCards(NuclearZone, MarsUniversity, SpinInducingAsteroid, Bushes)
    // Database save 75: immediately after both Research purchases in generation 4.
    nor.assertResources(m = 30, s = 0, t = 0, p = 9, e = 1, h = 6)
    nor.assertProduction(m = 1, s = 0, t = 0, p = 3, e = 1, h = 0)
    nor.assertDashMiddle(played = 7, actions = 3, vp = 30, tr = 25, hand = 6)
    nor.assertTags(but = 1, pot = 1, vet = 2, plt = 3, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 39, s = 0, t = 3, p = 0, e = 0, h = 9)
    vin.assertProduction(m = 12, s = 0, t = 1, p = 0, e = 0, h = 3)
    vin.assertDashMiddle(played = 10, actions = 2, vp = 29, tr = 23, hand = 8)
    vin.assertTags(but = 4, sct = 1, pot = 1, eat = 1, jot = 1, vet = 3)
    vin.assertDashRight(events = 0, tagless = 1, cities = 0)
    assertSidebar(gen = 4, temp = -28, oxygen = 3, oceans = 3, venus = 4)
    checkHandSizes()

    vin.turn {
      playProject(OptimalAerobraking, 1, titanium = 2)
      playProject(SpinInducingAsteroid, 13, titanium = 1) { draw(EnergyTapping) }
    }
    nor.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(4, 8)
      }
      convertPlants { placeTile(5, 8) }
    }
    vin.turn {
      playProject(NuclearZone, 10) { placeTile(5, 7) }
      // Research Network supplies Planetologist's second Jovian tag.
      vin.exMachina(fakeWildTags("JovianTag"))
      claimMilestone(cn("Planetologist"))
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn { playProject(DesignedMicroorganisms, 16) }
    vin.turn {
      vin.exMachina(fakeWildTags("VenusTag"))
      cardAction1(VenusShuttles)
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn {
      playProject(EcologicalZone, 12) {
        placeTile(5, 9)
      }
    }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { convertPlants { placeTile(2, 5) } }
    vin.turn { playProject(EnergyTapping, 3) }
    nor.turn { cardAction1(AerialMappers) { addCardResources(LocalShading) } }
    vin.pass()
    nor.turn {
      cardAction2(LocalShading)
      pass()
    }
    vin.wgt("OceanTile<Tharsis_5_5>")
  }

  private fun generation5() {
    vin.buyCards(Cartel, MethaneFromTitan, CloudSeeding)
    nor.buyCards(RobotPollinators, Plantation, InterplanetaryTrade)
    // Database save 117: immediately after both Research purchases in generation 5.
    nor.assertResources(m = 27, s = 0, t = 0, p = 6, e = 0, h = 10)
    nor.assertProduction(m = 2, s = 0, t = 0, p = 5, e = 0, h = 0)
    nor.assertDashMiddle(played = 9, actions = 3, vp = 37, tr = 28, hand = 7)
    nor.assertTags(but = 1, sct = 1, pot = 1, vet = 2, plt = 4, mit = 1, ant = 1, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 32, s = 0, t = 1, p = 2, e = 1, h = 16)
    vin.assertProduction(m = 12, s = 0, t = 1, p = 0, e = 1, h = 4)
    vin.assertDashMiddle(played = 14, actions = 2, vp = 36, tr = 28, hand = 8)
    vin.assertTags(but = 4, spt = 1, sct = 1, pot = 2, eat = 2, jot = 1, vet = 3)
    vin.assertDashRight(events = 1, tagless = 1, cities = 0)
    assertSidebar(gen = 5, temp = -24, oxygen = 5, oceans = 5, venus = 10)
    checkHandSizes()

    nor.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(5, 6)
      }
      convertPlants { placeTile(4, 6) }
    }
    vin.turn {
      convertHeat()
      convertHeat()
    }
    nor.turn { convertHeat() }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { cardAction1(AerialMappers) { addCardResources(LocalShading) } }
    vin.turn {
      vin.exMachina(fakeWildTags("EarthTag"))
      playProject(Cartel, 8)
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn { cardAction2(LocalShading) }
    vin.turn {
      vin.exMachina(fakeWildTags("VenusTag"))
      cardAction1(VenusShuttles)
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn { claimMilestone(cn("Briber")) }
    vin.turn { playProject(SmallAnimals, 6) }
    nor.pass()
    vin.turn {
      cardAction1(SmallAnimals)
      playProject(ProtectedHabitats, 5)
      pass()
    }
    nor.wgt("TemperatureStep")
  }

  private fun generation6() {
    vin.buyCards(SolarLogistics, InterstellarColonyShip, Sabotage)
    nor.buyCards(IshtarMining, Greenhouses)
    // Database save 160: immediately after both Research purchases in generation 6.
    nor.assertResources(m = 31, s = 0, t = 0, p = 6, e = 0, h = 2)
    nor.assertProduction(m = 3, s = 0, t = 0, p = 4, e = 0, h = 0)
    nor.assertDashMiddle(played = 9, actions = 3, vp = 47, tr = 31, hand = 9)
    nor.assertTags(but = 1, sct = 1, pot = 1, vet = 2, plt = 4, mit = 1, ant = 1, cit = 1)
    nor.assertDashRight(events = 0, tagless = 0, cities = 1)
    vin.assertResources(m = 43, s = 0, t = 2, p = 2, e = 1, h = 6)
    vin.assertProduction(m = 16, s = 0, t = 1, p = 0, e = 1, h = 5)
    vin.assertDashMiddle(played = 17, actions = 3, vp = 40, tr = 31, hand = 8)
    vin.assertTags(but = 4, spt = 1, sct = 1, pot = 2, eat = 3, jot = 1, vet = 3, ant = 1)
    vin.assertDashRight(events = 1, tagless = 2, cities = 0)
    assertSidebar(gen = 6, temp = -16, oxygen = 6, oceans = 6, venus = 12)
    checkHandSizes()

    vin.turn { playProject(Sabotage, 1) { doTask("-7 MC<Nor>") } }
    nor.turn { playProject(FakeResearchCoordination, 4) }
    vin.turn {
      vin.exMachina(fakeWildTags("VenusTag"))
      cardAction1(VenusShuttles)
      vin.assertCounts(0 to "FakeWildTagUse")
      stdProject("AirScrappingProject")
    }
    nor.turn {
      nor.exMachina(fakeWildTags("ScienceTag"))
      playProject(Plantation, 15) { placeTile(4, 5) }
      nor.assertCounts(0 to "FakeWildTagUse")
      convertPlants { placeTile(3, 4) }
    }
    vin.turn { cardAction1(SmallAnimals) }
    nor.turn { cardAction1(AerialMappers) { addCardResources(AerialMappers) } }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { cardAction1(LocalShading) }
    vin.turn { playProject(SolarLogistics, 14, titanium = 2) }
    nor.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(6, 7)
      }
      playProject(IshtarMining, 5)
    }
    vin.pass()
    nor.pass()
    vin.wgt("OceanTile<Tharsis_9_9>")
  }

  private fun generation7() {
    nor.buyCards()
    vin.buyCards(GeneRepair, Thermophiles)
    // Database save 201: immediately after both Research purchases in generation 7.
    nor.assertResources(m = 38, s = 0, t = 1, p = 6, e = 0, h = 5)
    nor.assertProduction(m = 3, s = 0, t = 1, p = 4, e = 0, h = 0)
    nor.assertDashMiddle(played = 12, actions = 3, vp = 53, tr = 35, hand = 6)
    nor.assertTags(but = 1, sct = 1, pot = 1, vet = 3, plt = 5, mit = 1, ant = 1, cit = 1)
    nor.assertDashRight(events = 0, tagless = 1, cities = 1)
    vin.assertResources(m = 49, s = 0, t = 3, p = 2, e = 1, h = 12)
    vin.assertProduction(m = 16, s = 0, t = 1, p = 0, e = 1, h = 5)
    vin.assertDashMiddle(played = 19, actions = 3, vp = 45, tr = 34, hand = 8)
    vin.assertTags(but = 4, spt = 2, sct = 1, pot = 2, eat = 4, jot = 1, vet = 3, ant = 1)
    vin.assertDashRight(events = 2, tagless = 2, cities = 0)
    assertSidebar(gen = 7, temp = -14, oxygen = 8, oceans = 8, venus = 16)
    checkHandSizes()

    nor.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(5, 4)
      }
      fundAward(cn("Landlord"), 8)
    }
    vin.turn { playProject(MethaneFromTitan, 19, titanium = 3) }
    nor.turn { cardAction2(AerialMappers) { draw(TitaniumMine) } }
    vin.turn { cardAction1(SmallAnimals) }
    // Research Coordination supplies Interplanetary Trade's tenth distinct tag.
    nor.turn {
      nor.exMachina(fakeWildTags("JovianTag"))
      playProject(InterplanetaryTrade, 24, titanium = 1)
      nor.assertCounts(0 to "FakeWildTagUse")
    }
    vin.turn { playProject(Thermophiles, 9) }
    nor.turn { convertPlants { placeTile(3, 3) } }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { cardAction2(LocalShading) }
    vin.turn { cardAction1(Thermophiles) { addCardResources(Thermophiles) } }
    nor.pass()
    vin.turn {
      vin.exMachina(fakeWildTags("VenusTag"))
      cardAction1(VenusShuttles)
      vin.assertCounts(0 to "FakeWildTagUse")
      convertHeat()
      playProject(MarsUniversity, 6) {
        doTask("-ProjectCard")
        discard(PhysicsComplex)
        draw(VenusGovernor)
      }
      playProject(VenusGovernor, 4)
      pass()
    }
    nor.wgt("TemperatureStep")
  }

  private fun generation8() {
    vin.buyCards(TundraFarming, VenusianAnimals)
    nor.buyCards(RoverConstruction, Trees)
    // Database save 246: immediately after both Research purchases in generation 8.
    nor.assertResources(m = 45, s = 0, t = 1, p = 5, e = 0, h = 5)
    nor.assertProduction(m = 14, s = 0, t = 1, p = 4, e = 0, h = 0)
    nor.assertDashMiddle(played = 13, actions = 3, vp = 62, tr = 37, hand = 8)
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
    vin.assertDashMiddle(played = 23, actions = 4, vp = 50, tr = 36, hand = 6)
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
    checkHandSizes()

    vin.turn {
      stdProject("CityProject") { placeTile(3, 5) }
      fundAward(cn("Excentric"), 14)
    }
    nor.turn {
      playProject(RoverConstruction, 8)
      playProject(ResearchOutpost, 18) { placeTile(4, 2) }
    }
    vin.turn { playProject(Bushes, 10) }
    nor.turn { cardAction1(AerialMappers) { addCardResources(AerialMappers) } }
    vin.turn { convertHeat() }
    nor.turn { cardAction1(LocalShading) }
    vin.turn { cardAction1(Thermophiles) { addCardResources(Thermophiles) } }
    nor.turn { playProject(Research, 10) { draw(StanfordTorus, PowerPlant) } }
    vin.turn { cardAction1(SmallAnimals) }
    nor.turn { playProject(Supermarkets, 8) }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { playProject(PowerPlant, 3) }
    vin.pass()
    nor.pass()
    vin.wgt("OxygenStep")
  }

  private fun generation9() {
    vin.buyCards(AtalantaPlanitiaLab)
    nor.buyCards(IndenturedWorkers, GiantSolarShade)
    // Database save 293: immediately after both Research purchases in generation 9.
    nor.assertResources(m = 47, s = 0, t = 2, p = 10, e = 1, h = 5)
    nor.assertProduction(m = 16, s = 0, t = 1, p = 4, e = 1, h = 0)
    nor.assertDashMiddle(played = 18, actions = 3, vp = 65, tr = 37, hand = 7)
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
    vin.assertDashMiddle(played = 24, actions = 4, vp = 63, tr = 37, hand = 6)
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
    checkHandSizes()

    nor.turn {
      stdProject("CityProject") { placeTile(4, 4) }
      convertPlants { placeTile(3, 1) }
    }
    nor.draw(DuskLaserMining)
    vin.turn { fundAward(cn("Forecaster"), 20) }
    nor.turn { cardAction2(AerialMappers) { draw(HeatTrappers) } }
    vin.turn { convertHeat() }
    nor.turn { playProject(DuskLaserMining, 1, titanium = 2) }
    vin.turn { stdProject("CityProject") { placeTile(2, 3) } }
    nor.turn { cardAction2(LocalShading) }
    vin.turn { convertPlants { placeTile(2, 4) } }
    nor.turn {
      playProject(Greenhouses, 5)
      convertPlants { placeTile(4, 3) }
    }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { convertHeat() }
    vin.turn { cardAction1(SmallAnimals) }
    nor.turn { playProject(Trees, 12) }
    vin.turn { cardAction2(Thermophiles) }
    // Research Coordination supplies the eighth Plant tag counted by Robot Pollinators.
    nor.turn {
      nor.exMachina(fakeWildTags("PlantTag"))
      playProject(RobotPollinators, 8)
      nor.assertCounts(0 to "FakeWildTagUse")
      convertPlants { placeTile(5, 2) }
    }
    vin.turn {
      vin.exMachina(fakeWildTags("VenusTag"))
      cardAction1(VenusShuttles)
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn { convertPlants { placeTile(4, 1) } }
    vin.pass()
    nor.turn {
      playProject(IndenturedWorkers, 0)
      playProject(GiantSolarShade, 3, titanium = 5)
      pass()
    }
    nor.wgt("VenusStep")
  }

  private fun generation10() {
    vin.buyCards(InventorsGuild)
    nor.buyCards(LandClaim, FakeFloydContinuum, RadSuits, DeepWellHeating)
    // Database save 357: immediately after both Research purchases in generation 10.
    nor.assertResources(m = 53, s = 0, t = 2, p = 9, e = 0, h = 4)
    nor.assertProduction(m = 18, s = 0, t = 2, p = 8, e = 0, h = 0)
    nor.assertDashMiddle(played = 24, actions = 3, vp = 90, tr = 44, hand = 7)
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
    vin.assertDashMiddle(played = 24, actions = 4, vp = 72, tr = 41, hand = 7)
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
    checkHandSizes()

    vin.turn {
      stdProject("CityProject") { placeTile(5, 1) }
      convertPlants { placeTile(6, 2) }
    }
    nor.turn {
      stdProject("CityProject") { placeTile(6, 3) }
      convertPlants { placeTile(6, 4) }
    }
    vin.turn { convertHeat() }
    nor.turn { playProject(LandClaim, 0) { doTask("Community<Tharsis_2_2>") } }
    vin.turn {
      playProject(VenusianAnimals, 15) {
        doTask("-ProjectCard")
        discard(CloudSeeding)
        draw(Gyropolis)
      }
    }

    nor.turn { playProject(FakeFloydContinuum, 3) }
    // Research Network supplies Atalanta Planitia Lab's third Science tag.
    vin.turn {
      vin.exMachina(fakeWildTags("ScienceTag"))
      playProject(AtalantaPlanitiaLab, 10) {
        draw(WaterSplittingPlant, Algae)
        doTask("-ProjectCard")
        discard(WaterSplittingPlant)
        draw(NoctisCity)
      }
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn { cardAction1(AerialMappers) { addCardResources(AerialMappers) } }
    vin.turn { cardAction1(SmallAnimals) }
    nor.turn { cardAction1(LocalShading) }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { playProject(DeepWellHeating, 12) }
    vin.turn { cardAction1(Thermophiles) { addCardResources(Thermophiles) } }
    nor.turn { cardAction1(FakeFloydContinuum).expect("9 MC") }
    vin.turn { playProject(Algae, 10) }
    nor.turn { stdProject("CityProject") { placeTile(6, 5) } }
    vin.pass()
    nor.turn {
      playProject(RadSuits, 5)
      pass()
    }
    vin.wgt("TemperatureStep")
  }

  private fun generation11() {
    nor.buyCards(GhgProducingBacteria, OlympusConference)
    vin.buyCards(IshtarExpedition, AdvancedAlloys)
    // Database save 417: immediately after both Research purchases in generation 11.
    nor.assertResources(m = 64, s = 0, t = 4, p = 14, e = 1, h = 4)
    nor.assertProduction(m = 21, s = 0, t = 2, p = 8, e = 1, h = 0)
    nor.assertDashMiddle(played = 28, actions = 4, vp = 97, tr = 45, hand = 5)
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
    vin.assertDashMiddle(played = 27, actions = 4, vp = 87, tr = 42, hand = 8)
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
    checkHandSizes()

    nor.turn {
      cardAction2(AerialMappers) { draw(WeatherBalloons) }
      playProject(OlympusConference, 9)
    }
    vin.turn {
      convertHeat()
      playProject(IshtarExpedition, 6) {
        draw(FreyjaBiodomes, GhgImportFromVenus)
      }
    }
    nor.turn {
      playProject(GhgProducingBacteria, 7) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
        draw(MoholeLake)
      }
      playProject(MoholeLake, 30)
    }
    vin.turn { convertPlants { placeTile(1, 3) } }
    nor.turn {
      stdProject("AsteroidProject")
      convertPlants { placeTile(6, 6) }
    }
    vin.turn { cardAction1(Thermophiles) { addCardResources(Thermophiles) } }
    nor.turn {
      playProject(WeatherBalloons, 10) {
        draw(GiantIceAsteroid)
      }
    }
    vin.turn { cardAction1(CloudTourism) }
    nor.turn { cardAction1(FakeFloydContinuum).expect("12 MC") }
    vin.turn { cardAction1(SmallAnimals) }
    nor.turn { convertPlants { placeTile(7, 5) } }
    vin.turn { sellPatents(GhgImportFromVenus) }
    nor.turn { cardAction2(LocalShading) }
    vin.turn {
      playProject(FreyjaBiodomes, 14) { addCardResources(VenusianAnimals, 2) }
    }
    nor.turn { cardAction1(GhgProducingBacteria) }
    // Research Network supplies Interstellar Colony Ship's fifth Science tag.
    vin.turn {
      vin.exMachina(fakeWildTags("ScienceTag"))
      playProject(InterstellarColonyShip, 1, titanium = 7) { draw(RestrictedArea) }
      vin.assertCounts(0 to "FakeWildTagUse")
    }
    nor.turn { cardAction1(WeatherBalloons) }
    vin.turn { stdProject("PowerPlantProject") }
    nor.turn { playProject(StanfordTorus, titanium = 4) }
    vin.turn { playProject(NoctisCity, 16) }
    nor.turn { sellPatents(GiantIceAsteroid) }
    vin.turn { sellPatents(Gyropolis) }
    nor.turn { sellPatents(HeatTrappers) }
    vin.turn { sellPatents(AdvancedAlloys) }
    nor.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    vin.turn {
      playProject(RestrictedArea, 11) {
        placeTile(7, 8)
        doTask("-ProjectCard")
        discard(TundraFarming)
        draw(CorporateStronghold)
      }
    }
    nor.turn { sellPatents(TitaniumMine) }
    vin.turn { cardAction1(RestrictedArea) { draw(MagneticFieldDome) } }
    nor.pass()
    vin.turn {
      playProject(GeneRepair, 12) {
        doTask("-ProjectCard")
        discard(MagneticFieldDome)
        draw(Teslaract)
      }
      sellPatents(InventorsGuild, CorporateStronghold, Teslaract)
      pass()
    }
  }

  private fun endgame() {
    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)
    nor.convertPlants { placeTile(7, 6) }
    nor.declineTask()
    vin.convertPlants { placeTile(7, 9) }
    vin.declineTask()

    assertCardTrackingComplete()
    nor.cardsHand shouldBe emptySet()
    vin.cardsHand shouldBe emptySet()
    checkHandSizes()
    admin.assertCounts(1 to "End", 1 to "Phase")

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
