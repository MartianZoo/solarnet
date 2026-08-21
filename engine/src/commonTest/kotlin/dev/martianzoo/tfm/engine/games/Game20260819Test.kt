package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// NOTE: Solar Fusion Stream - https://terraforming-mars.herokuapp.com/the-end?id=pc2de3208e4ca
class Game20260819Test : CardTrackingFullGameTest() {
  // NOTE: The archived metadata specifies Elysium, Corporate Era, Prelude, promo cards, drafting,
  // NOTE: fast mode, three players, and the following limited-synergy milestone and award pools.
  // NOTE: Hydrologist is not implemented; unclaimed Terraformer is a same-role setup substitute.
  // NOTE: Thresholds distinguish Builder and Terraformer from the variants in other active bundles.
  override val config =
      GameConfig(
          """
          ElysiumMapOption
          PreludeExpansion, MilestonesAwardsExpansion, PromoCardPack

          Builder7, Philantropist, Spacefarer, Terraformer29, Energizer
          Incorporator, Botanist, Founder, Benefactor, Banker
          """,
          "JR",
          "KB",
          "ER",
      )

  @Test
  fun game20260819() {
    TfmWorkflow.Auto(game).launch()

    val JR = p1
    val KB = p2
    val ER = p3

    // First player this generation is JR
    // Good luck JR!
    // Good luck KB!
    // Good luck ER!
    // Generation 1
    engine.assertCounts(1 to "Generation")

    // NOTE: JR rejected Teractor and PolderTECH Dutch; UNMI Contractor and Acquired Space Agency;
    // NOTE: Crash Site Cleanup, Outdoor Sports, Interstellar Colony Ship, Tropical Resort,
    // NOTE: Physics Complex, and Weather Balloons.
    // JR played Tharsis Republic
    // JR kept 4 project cards
    JR.playCorp(TharsisRepublic) {
      JR.buyCards(MethaneFromTitan, TechnologyDemonstration, FueledGenerators, LavaTubeSettlement)
    }

    // NOTE: KB rejected Utopia Invest and Recyclon; Great Aquifer and Polar Industries;
    // NOTE: Asteroid Deflection System, Decomposers, Black Polar Dust, Comet Aiming, and Astra
    // NOTE: Mechanica.
    // NOTE: The log identifies three of KB's five kept projects. Imported GHG and Corporate
    // NOTE: Stronghold are the two never-played candidates from that deal.
    // KB played Mons Insurance
    // KB gained 4 M€ production
    // JR lost 2 M€ production
    // ER lost 2 M€ production
    // KB kept 5 project cards
    KB.playCorp(MonsInsurance) {
          KB.buyCards(
              EnergyTapping,
              StaticHarvesting,
              StandardTechnology,
              ImportedGhg,
              CorporateStronghold,
          )
        }
        .expect("PROD[4 M<KB>, -2 M<JR>, -2 M<ER>]")

    // NOTE: ER rejected Factorum and Viron; Mohole and Huge Asteroid; and Meat Industry,
    // NOTE: Supercapacitors, Orbital Cleanup, Ice Cap Melting, and Towing A Comet.
    // ER played Tycho Magnetics
    // ER gained 1 energy production
    // ER kept 5 project cards
    ER.playCorp(TychoMagnetics) {
      ER.buyCards(
          AsteroidCard,
          IndustrialCenter,
          WaterImportFromEuropa,
          CommercialDistrict,
          MirandaResort,
      )
    }

    JR.turn {
      // JR played Smelting Plant
      // JR gained 5 steel
      playPrelude(SmeltingPlant)
      // JR played Self-Sufficient Settlement
      // JR gained 2 M€ production
      // JR placed city tile at 20
      // JR drew 3 card(s)
      // You drew Kaguya Tech,Space Elevator,Magnetic Shield
      // JR gained 3 M€
      // JR gained 1 M€ production
      playPrelude(SelfSufficientSettlement) {
        draw(KaguyaTech, SpaceElevator, MagneticShield)
        doTask("CityTile<Elysium_3_7>")
      }
    }

    KB.turn {
      // KB played Research Network
      // KB gained 1 M€ production
      // KB drew 3 card(s)
      // You drew Research Outpost,Restricted Area,Acquired Company
      playPrelude(ResearchNetwork) {
            draw(ResearchOutpost, RestrictedArea, AcquiredCompany)
          }
          .expect("PROD[M], WildTag")
      // KB played Established Methods
      // KB gained 30 M€
      // KB used Power Plant:SP standard project
      // KB used Power Plant:SP standard project
      playPrelude(FakeEstablishedMethods) {
            doTask("UseAction1<UseStandardProjectSA>")
            doTask("UseAction1<PowerPlantSP>")
            pay(11)
            doTask("UseAction1<UseStandardProjectSA>")
            doTask("UseAction1<PowerPlantSP>")
            pay(11)
          }
          .expect("8 M")
    }

    ER.turn {
      // ER played Galilean Mining
      // ER gained 2 titanium production
      // NOTE: ER lost 5 M€.
      playPrelude(GalileanMining)
      // ER played Aquifer Turbines
      // ER gained 2 energy production
      // ER placed ocean tile at 27
      // ER gained 1 plant
      // NOTE: ER lost 3 M€.
      playPrelude(AquiferTurbines) { doTask("OceanTile<Elysium_4_7>") }.expect("PROD[2 E], P, TR")
    }

    // JR took the first action of Tharsis Republic corporation
    // JR placed city tile at 34
    // JR gained 3 plants
    // JR gained 3 M€
    // JR gained 1 M€ production
    JR.stdAction("HandleMandates") { doTask("CityTile<Elysium_5_6>") }.expect("3 P, 3 M, PROD[M]")
    // JR played Methane From Titan
    // JR gained 2 plant production
    // JR gained 2 heat production
    JR.playProject(MethaneFromTitan, 28)
    // KB played Research Outpost
    // KB placed city tile at 31
    // KB gained 2 plants
    // JR gained 1 M€ production
    KB.playProject(ResearchOutpost, 18) { doTask("CityTile<Elysium_5_3>") }
        .expect("2 P<KB>, PROD[M<JR>]")
    // KB played Acquired Company
    // KB gained 3 M€ production
    KB.playProject(AcquiredCompany, 9)
    // ER played Industrial Center
    // ER placed Industrial Center tile at 28
    // ER gained 1 plant
    // ER gained 1 steel
    // ER gained 2 M€ from 1 ocean(s)
    ER.playProject(IndustrialCenter, 4) { doTask("IndustrialCenter_SpecialTile<Elysium_4_8>") }
        .expect("P, S")
    // ER used Industrial Center action
    // ER gained 1 steel production
    ER.cardAction1(IndustrialCenter)
    // JR played Technology Demonstration
    // JR drew 2 card(s)
    // You drew Energy Market,Sterling Vents
    JR.playProject(TechnologyDemonstration, 5) { JR.draw(EnergyMarket, SterlingVents) }
    // JR played Fueled Generators
    // JR lost 1 M€ production
    // JR gained 1 energy production
    JR.playProject(FueledGenerators, 1)
    // KB played Restricted Area
    // KB placed Restricted Area tile at 62
    // KB drew 1 card(s)
    // You drew Peroxide Power
    KB.playProject(RestrictedArea, 10) {
      KB.draw(PeroxidePower)
      doTask("RestrictedArea_SpecialTile<Elysium_9_8>")
    }
    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Designed Microorganisms
    KB.cardAction1(RestrictedArea) { KB.draw(DesignedMicroorganisms) }
    // ER passed
    ER.pass()
    // JR passed
    JR.pass()
    // KB played Energy Tapping
    // KB stole 1 energy production from ER
    KB.playProject(EnergyTapping, 2) { doTask("PROD[-E<ER>]") }.expect("PROD[E<KB>, -E<ER>]")
    // KB passed
    KB.pass()

    // Generation 2
    // First player this generation is KB
    // ER bought 1 card(s)
    // You bought Vesta Shipyard
    ER.buyCards(VestaShipyard)
    // KB bought 2 card(s)
    // You bought Olympus Conference,Noctis Farming
    KB.buyCards(OlympusConference, NoctisFarming)
    // JR bought 3 card(s)
    // You bought Immigrant City,Natural Preserve,Steelworks
    JR.buyCards(ImmigrantCity, NaturalPreserve, Steelworks)

    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Adapted Lichen
    KB.cardAction1(RestrictedArea) { KB.draw(AdaptedLichen) }
    // KB played Peroxide Power
    // KB lost 1 M€ production
    // KB gained 2 energy production
    KB.playProject(PeroxidePower, 6)
    // ER used Tycho Magnetics action
    // ER spent 2 energy
    // ER drew 1 card(s)
    // You drew Red Ships
    ER.cardAction1(TychoMagnetics) {
      doTask("-2 E")
      ER.draw(RedShips)
    }
    // ER played Vesta Shipyard
    // ER gained 1 titanium production
    ER.playProject(VestaShipyard, 9, titanium = 2)
    // JR played Immigrant City
    // JR placed city tile at 16
    // JR drew 1 card(s)
    // You drew Pets
    // JR gained 3 M€
    // JR gained 1 M€ production
    // JR gained 1 M€ production
    // NOTE: JR lost 1 energy production.
    // NOTE: JR lost 2 M€ production.
    JR.playProject(ImmigrantCity, 3, steel = 5) {
      JR.draw(Pets)
      doTask("CityTile<Elysium_3_3>")
    }
    // JR played Pets
    // JR added 1 Animal to Pets
    JR.playProject(Pets, 10)
    // KB played Static Harvesting
    // KB gained 1 energy production
    // KB gained 3 M€
    KB.doTask("BuildingTag<WildTagUse<$ResearchNetwork>>")
    KB.playProject(StaticHarvesting, 4).expect("-M")
    // KB claimed Energizer milestone
    KB.stdAction("ClaimMilestoneSA") { doTask("Energizer") }
    // ER used Industrial Center action
    // ER gained 1 steel production
    ER.cardAction1(IndustrialCenter)
    // ER passed
    ER.declineSecondAction()
    // JR played Sterling Vents
    // JR gained 2 energy production
    // JR lost 2 heat production
    JR.playProject(SterlingVents, 5)
    // JR passed
    JR.declineSecondAction()
    // KB passed
    KB.pass()
    // NOTE: Heroku records ER's and JR's illegal second-action passes here. Decline those second
    // NOTE: actions in Solarnet, then perform the no-effect passes at the next legal first-action
    // NOTE: boundary.
    ER.pass()
    JR.pass()

    // Generation 3
    // First player this generation is ER
    // JR bought 1 card(s)
    // You bought Field-Capped City
    JR.buyCards(FieldCappedCity)
    // ER bought 2 card(s)
    // You bought Robotic Workforce,Strip Mine
    ER.buyCards(RoboticWorkforce, StripMine)
    // KB bought 3 card(s)
    // You bought Soil Factory,Quantum Extractor,Nitrogen-Rich Asteroid
    KB.buyCards(SoilFactory, QuantumExtractor, NitrogenRichAsteroid)

    // ER used Tycho Magnetics action
    // ER spent 2 energy
    // ER drew 1 card(s)
    // You drew Lightning Harvest
    ER.cardAction1(TychoMagnetics) {
      doTask("-2 E")
      ER.draw(LightningHarvest)
    }
    // ER used Industrial Center action
    // ER gained 1 steel production
    ER.cardAction1(IndustrialCenter)
    // JR played Natural Preserve
    // JR gained 1 M€ production
    // JR placed Natural Preserve tile at 53
    // JR gained 2 steel
    JR.playProject(NaturalPreserve, 9) { doTask("NaturalPreserve_SpecialTile<Elysium_8_4>") }
    // JR claimed Builder7 milestone
    JR.stdAction("ClaimMilestoneSA") { doTask("Builder7") }
    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Martian Lumber Corp
    KB.cardAction1(RestrictedArea) { KB.draw(MartianLumberCorp) }
    // KB played Olympus Conference
    // KB added 1 Science to Olympus Conference
    KB.playProject(OlympusConference, 9, steel = 0)
    // ER passed
    ER.pass()
    // JR played Energy Market
    JR.playProject(EnergyMarket, 3)
    // JR passed
    JR.declineSecondAction()
    // KB played Quantum Extractor
    // KB gained 4 energy production
    // KB removed a resource from Olympus Conference to draw a card
    // KB drew 1 card(s)
    // You drew Earth Office
    KB.doTask("ScienceTag<WildTagUse<$ResearchNetwork>>")
    KB.playProject(QuantumExtractor, 12) {
      KB.draw(EarthOffice)
      doTask("ProjectCard FROM Science<$OlympusConference>")
    }
    // KB played Earth Office
    KB.playProject(EarthOffice, 0)
    // NOTE: Heroku records JR's pass as a second action; defer it to this legal boundary.
    JR.pass()
    // KB passed
    KB.pass()

    // Generation 4
    // First player this generation is JR
    // KB bought 4 card(s)
    // You bought Bactoviral Research,Investment Loan,Ice Asteroid,Mass Converter
    KB.buyCards(BactoviralResearch, InvestmentLoan, IceAsteroid, MassConverter)
    // JR bought 1 card(s)
    // You bought Interplanetary Trade
    JR.buyCards(InterplanetaryTrade)
    // ER bought 4 card(s)
    // You bought Greenhouses,Dusk Laser Mining,Inventors' Guild,Deep Well Heating
    ER.buyCards(Greenhouses, DuskLaserMining, InventorsGuild, DeepWellHeating)

    // JR used Convert Plants standard action
    // JR placed greenery tile at 13
    // JR gained 2 steel
    JR.convertPlants { doTask("GreeneryTile<Elysium_2_6>") }
    // JR played Kaguya Tech
    // JR gained 2 M€ production
    // JR drew 1 card(s)
    // You drew Hermetic Order of Mars
    // JR placed city tile at 13
    // JR gained 2 steel
    // JR gained 3 M€
    // JR gained 1 M€ production
    // JR gained 1 M€ production
    // JR added 1 Animal to Pets
    JR.playProject(KaguyaTech, 10) {
      JR.draw(HermeticOrderOfMars)
      doTask("CityTile<Elysium_2_6> FROM GreeneryTile<Elysium_2_6>")
    }
    // KB played Mass Converter
    // KB gained 6 energy production
    // KB added 1 Science to Olympus Conference
    KB.doTask("ScienceTag<WildTagUse<$ResearchNetwork>>")
    KB.playProject(MassConverter, 7)
    // KB played Investment Loan
    // KB lost 1 M€ production
    // KB gained 10 M€
    KB.playProject(InvestmentLoan, 0)
    // ER used Tycho Magnetics action
    // ER spent 2 energy
    // ER drew 1 card(s)
    // You drew Giant Space Mirror
    ER.cardAction1(TychoMagnetics) {
      doTask("-2 E")
      ER.draw(GiantSpaceMirror)
    }
    // ER played Inventors' Guild
    ER.playProject(InventorsGuild, 9)
    // JR played Lava Tube Settlement
    // JR gained 2 M€ production
    // JR lost 1 energy production
    // JR placed city tile at 14
    // JR gained 2 titanium
    // JR gained 3 M€
    // JR gained 1 M€ production
    // JR gained 1 M€ production
    // JR added 1 Animal to Pets
    JR.playProject(LavaTubeSettlement, 3, steel = 6) { doTask("CityTile<Elysium_3_1>") }
    // JR played Hermetic Order of Mars
    // JR gained 2 M€ production
    // JR gained 22 M€
    JR.playProject(HermeticOrderOfMars, 10)
    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Capital
    KB.cardAction1(RestrictedArea) { KB.draw(Capital) }
    // KB played Standard Technology
    // KB removed a resource from Olympus Conference to draw a card
    // KB drew 1 card(s)
    // You drew Hackers
    KB.playProject(StandardTechnology, 5) {
      KB.draw(Hackers)
      doTask("ProjectCard FROM Science<$OlympusConference>")
    }
    // ER used Inventors' Guild action
    // ER bought 0 card(s)
    ER.cardAction1(InventorsGuild) { doTask("Ok") }
    // ER played Dusk Laser Mining
    // ER gained 1 titanium production
    // ER lost 1 energy production
    // ER gained 4 titanium
    ER.playProject(DuskLaserMining, 2, titanium = 2)
    // JR played Interplanetary Trade
    // JR gained 9 M€ production
    JR.playProject(InterplanetaryTrade, 21, titanium = 2)
    // JR passed
    JR.declineSecondAction()
    // KB played Adapted Lichen
    // KB gained 1 plant production
    KB.playProject(AdaptedLichen, 8)
    // KB played Hackers
    // KB gained 2 M€ production
    // KB lost 1 energy production
    // KB stole 2 M€ production from JR
    // JR received 1 M€ from Mons Insurance owner (KB)
    KB.playProject(Hackers, 2) { doTask("PROD[-2 M<JR>]") }
    // ER played Water Import From Europa
    // NOTE: ER recalls using all eight titanium and retaining 11 M€ after this play.
    ER.playProject(WaterImportFromEuropa, 1, titanium = 8).expect("-M, -8 T")
    ER.assertCounts(11 to "M", 0 to "T")
    // NOTE: Deep Well Heating is the only never-played card in ER's hand here.
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(DeepWellHeating)
    JR.pass()
    // KB passed
    KB.pass()
    // ER used Water Import From Europa action
    // ER placed ocean tile at 24
    // ER gained 2 plants
    ER.cardAction1(WaterImportFromEuropa) {
          ER.pay(12)
          doTask("OceanTile<Elysium_4_4>")
        }
        .expect("-12 M, 2 P")
    // ER passed
    ER.pass()

    // Generation 5
    // First player this generation is KB
    // JR bought 3 card(s)
    // You bought ArchaeBacteria,Supermarkets,Medical Lab
    JR.buyCards(Archaebacteria, Supermarkets, MedicalLab)
    // KB bought 3 card(s)
    // You bought Mining Rights,Business Contacts,Hired Raiders
    KB.buyCards(MiningRights, BusinessContacts, HiredRaiders)
    // ER bought 2 card(s)
    // You bought Dust Seals,Sponsors
    ER.buyCards(DustSeals, Sponsors)

    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Equatorial Magnetizer
    KB.cardAction1(RestrictedArea) { KB.draw(EquatorialMagnetizer) }
    // KB played Business Contacts
    // KB drew 2 card(s)
    // You drew Titanium Mine,Law Suit
    KB.playProject(BusinessContacts, 3) { KB.draw(TitaniumMine, LawSuit) }
    // ER used Tycho Magnetics action
    // ER spent 1 energy
    // ER drew 1 card(s)
    // You drew Solar Wind Power
    ER.cardAction1(TychoMagnetics) {
      doTask("-E")
      ER.draw(SolarWindPower)
    }
    // ER used Inventors' Guild action
    // ER bought 0 card(s)
    ER.cardAction1(InventorsGuild) { doTask("Ok") }
    // JR claimed Philantropist milestone
    JR.stdAction("ClaimMilestoneSA") { doTask("Philantropist") }
    // JR played Space Elevator
    // JR gained 1 titanium production
    JR.playProject(SpaceElevator, 27)
    // KB played Hired Raiders
    // KB stole 2 steel from ER
    // ER received 3 M€ from Mons Insurance owner (KB)
    KB.playProject(HiredRaiders, 0) { doTask("2 S<KB> FROM S<ER>") }
        .expect("2 S<KB>, -2 S<ER>, -3 M<KB>, 3 M<ER>")
    // KB played Titanium Mine
    // KB gained 1 titanium production
    KB.playProject(TitaniumMine, 2, steel = 2)
    // ER played Dust Seals
    ER.playProject(DustSeals, 2)
    // ER played Sponsors
    // ER gained 2 M€ production
    ER.playProject(Sponsors, 6)
    // JR played ArchaeBacteria
    // JR gained 1 plant production
    JR.playProject(Archaebacteria, 6)
    // JR passed
    JR.declineSecondAction()
    // NOTE: Fixture inference: Imported GHG is one of KB's two unidentified opening keeps.
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(ImportedGhg)
    // KB played Equatorial Magnetizer
    KB.playProject(EquatorialMagnetizer, 10)
    // ER used Water Import From Europa action
    // ER placed ocean tile at 32
    // ER gained 2 plants
    // ER gained 2 M€ from 1 ocean(s)
    ER.cardAction1(WaterImportFromEuropa) {
          ER.pay(titanium = 4)
          doTask("OceanTile<Elysium_5_4>")
        }
        .expect("2 M, -4 T, 2 P")
    // ER played Commercial District
    // ER gained 4 M€ production
    // ER lost 1 energy production
    // ER placed Commercial District tile at 23
    // ER gained 1 plant
    // ER gained 4 M€ from 2 ocean(s)
    ER.playProject(CommercialDistrict, 0, steel = 8) {
      doTask("CommercialDistrict_SpecialTile<Elysium_4_3>")
    }
    JR.pass()
    // KB used Equatorial Magnetizer action
    // KB lost 1 energy production
    KB.cardAction1(EquatorialMagnetizer)
    KB.declineSecondAction()
    // KB passed
    // ER passed
    ER.pass()
    KB.pass()

    // Generation 6
    // First player this generation is ER
    // ER bought 2 card(s)
    // You bought Media Group,Cyberia Systems
    ER.buyCards(MediaGroup, CyberiaSystems)
    // KB bought 4 card(s)
    // You bought AI Central,Earth Catapult,Regolith Eaters,Small Asteroid
    KB.buyCards(AiCentral, EarthCatapult, RegolithEaters, SmallAsteroid)
    // JR bought 2 card(s)
    // You bought Space Mirrors,Zeppelins
    JR.buyCards(SpaceMirrors, Zeppelins)

    // ER used Inventors' Guild action
    // ER bought 0 card(s)
    ER.cardAction1(InventorsGuild) { doTask("Ok") }
    // ER played Giant Space Mirror
    // ER gained 3 energy production
    ER.playProject(GiantSpaceMirror, 5, titanium = 4).expect("-5 M, -4 T, PROD[3 E]")
    // JR used Convert Heat standard action
    JR.convertHeat()
    // JR played Field-Capped City
    // JR gained 2 M€ production
    // JR gained 1 energy production
    // JR gained 3 plants
    // JR placed city tile at 41
    // JR gained 1 plant
    // JR gained 2 M€ from 1 ocean(s)
    // JR gained 3 M€
    // JR gained 1 M€ production
    // JR gained 1 M€ production
    // JR added 1 Animal to Pets
    JR.playProject(FieldCappedCity, 29) { doTask("CityTile<Elysium_6_5>") }
        .expect("PROD[4 M], 4 P, Animal")
    // KB used Convert Heat standard action
    KB.convertHeat()
    // KB used Convert Heat standard action
    // KB gained 1 heat production
    KB.convertHeat()
    // ER used Water Import From Europa action
    // ER placed ocean tile at 26
    // ER gained 1 plant
    // ER gained 2 M€ from 1 ocean(s)
    ER.cardAction1(WaterImportFromEuropa) {
      ER.pay(12)
      doTask("OceanTile<Elysium_4_6>")
    }
    // ER used Convert Plants standard action
    // ER placed greenery tile at 36
    // ER gained 2 plants
    // ER gained 2 M€ from 1 ocean(s)
    ER.convertPlants { doTask("GreeneryTile<Elysium_5_8>") }
    // JR used Convert Plants standard action
    // JR placed greenery tile at 33
    // JR gained 2 plants
    // JR gained 4 M€ from 2 ocean(s)
    JR.convertPlants { doTask("GreeneryTile<Elysium_5_5>") }

    // NOTE: _local/Game20260819/Game20260819-dashboard-gen6.png was taken here, after JR's
    // NOTE: generation-6 greenery and before JR played Zeppelins.
    assertSidebar(gen = 6, temp = -24, oxygen = 5, oceans = 4)
    ER.assertResources(m = 23, s = 3, t = 0, p = 2, e = 0, h = 0)
    ER.assertProduction(m = 4, s = 3, t = 4, p = 0, e = 3, h = 0)
    JR.assertResources(m = 18, s = 0, t = 1, p = 4, e = 1, h = 0)
    JR.assertProduction(m = 24, s = 0, t = 1, p = 3, e = 2, h = 0)
    KB.assertResources(m = 17, s = 0, t = 1, p = 4, e = 14, h = 18)
    KB.assertProduction(m = 8, s = 0, t = 1, p = 1, e = 14, h = 1)
    ER.assertCounts(25 to "TR")
    JR.assertCounts(25 to "TR")
    KB.assertCounts(23 to "TR")
    checkHandSizes()

    // JR played Zeppelins
    // JR gained 7 M€ production
    JR.playProject(Zeppelins, 13)
    // KB used Convert Heat standard action
    KB.convertHeat()
    // KB used Convert Heat standard action
    // KB gained 1 heat production
    KB.convertHeat()
    // ER played Red Ships
    ER.playProject(RedShips, 2)
    // ER used Red Ships action
    // ER gained 7 M€
    ER.cardAction1(RedShips).expect("7 M")
    // JR played Space Mirrors
    JR.playProject(SpaceMirrors, titanium = 1)
    // JR passed
    JR.declineSecondAction()
    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Industrial Microbes
    KB.cardAction1(RestrictedArea) { KB.draw(IndustrialMicrobes) }
    // KB used Equatorial Magnetizer action
    // KB lost 1 energy production
    KB.cardAction1(EquatorialMagnetizer)
    // ER played Greenhouses
    // ER gained 7 plants
    ER.playProject(Greenhouses, 0, steel = 3)
    // ER used Convert Plants standard action
    // ER placed greenery tile at 37
    // ER gained 1 plant
    // ER gained 1 titanium
    ER.convertPlants { doTask("GreeneryTile<Elysium_5_9>") }.expect("T")
    JR.pass()
    // NOTE: Fixture inference: Corporate Stronghold is KB's other unidentified opening keep.
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(CorporateStronghold)
    // KB played Ice Asteroid
    // KB placed ocean tile at 18
    // KB gained 1 plant
    // KB gained 2 M€ from 1 ocean(s)
    // KB placed ocean tile at 19
    // KB gained 6 M€ from 3 ocean(s)
    KB.playProject(IceAsteroid, 15, titanium = 1) {
          doTask("OceanTile<Elysium_3_5>")
          doTask("OceanTile<Elysium_3_6>")
        }
        .expect("-7 M, 2 TR, P")
    // ER played Strip Mine
    // ER gained 2 steel production
    // ER gained 1 titanium production
    // ER lost 2 energy production
    ER.playProject(StripMine, 25)
    // ER passed
    ER.declineSecondAction()
    // KB played Noctis Farming
    // KB gained 1 M€ production
    // KB gained 2 plants
    KB.playProject(NoctisFarming, 9)
    KB.declineSecondAction()
    ER.pass()
    // KB passed
    KB.pass()

    // Generation 7
    // First player this generation is JR
    // JR bought 2 card(s)
    // You bought Mine,Homeostasis Bureau
    JR.buyCards(Mine, HomeostasisBureau)
    // ER bought 4 card(s)
    // You bought Special Design,Advanced Alloys,Subterranean Reservoir,Trees
    ER.buyCards(SpecialDesign, AdvancedAlloys, SubterraneanReservoir, Trees)
    // KB bought 2 card(s)
    // You bought Big Asteroid,Research
    KB.buyCards(BigAsteroid, Research)

    // JR used Space Mirrors action
    // JR gained 1 energy production
    JR.cardAction1(SpaceMirrors)
    // JR funded Banker award
    JR.stdAction("FundAwardSA") { doTask("Banker") }
    // KB used Convert Plants standard action
    // KB placed greenery tile at 30
    // KB gained 2 plants
    KB.convertPlants { doTask("GreeneryTile<Elysium_5_2>") }
    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Potatoes
    KB.cardAction1(RestrictedArea) { KB.draw(Potatoes) }
    // ER used Tycho Magnetics action
    // ER spent 1 energy
    // ER drew 1 card(s)
    // You drew Deimos Down:promo
    ER.cardAction1(TychoMagnetics) {
      doTask("-E")
      ER.draw(DeimosDownPromo)
    }
    // ER used Inventors' Guild action
    // ER bought 0 card(s)
    ER.cardAction1(InventorsGuild) { doTask("Ok") }
    // JR played Mine
    // JR gained 1 steel production
    JR.playProject(Mine, 4)
    // JR played Supermarkets
    // JR gained 2 M€ production
    JR.playProject(Supermarkets, 9)
    // KB used Equatorial Magnetizer action
    // KB lost 1 energy production
    KB.cardAction1(EquatorialMagnetizer)
    // NOTE: Fixture inference: Capital is the first of KB's three unplayed generation-7 sales.
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(Capital)
    // ER played Advanced Alloys
    ER.playProject(AdvancedAlloys, 9)
    // ER used Water Import From Europa action
    // ER placed ocean tile at 05
    // ER drew 1 card(s)
    // You drew Gene Repair
    ER.cardAction1(WaterImportFromEuropa) {
          ER.pay(titanium = 3)
          ER.draw(GeneRepair)
          doTask("OceanTile<Elysium_1_3>")
        }
        .expect("TR, ProjectCard")
    // JR played Magnetic Shield
    JR.playProject(MagneticShield, 21, titanium = 1)
    // JR passed
    JR.declineSecondAction()
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(LawSuit)
    // KB used Convert Heat standard action
    KB.convertHeat()
    // ER played Media Group
    ER.playProject(MediaGroup, 6)
    // ER played Asteroid
    // ER gained 2 titanium
    // JR lost 3 plants because of ER
    // JR received 3 M€ from Mons Insurance owner (KB)
    // ER gained 3 M€
    ER.playProject(AsteroidCard, 2, titanium = 3) { doTask("-3 P<JR>") }
        .expect("-T<ER>, -3 P<JR>, -3 M<KB>, 3 M<JR>")
    JR.pass()
    // KB played Designed Microorganisms
    // KB gained 2 plant production
    // KB added 1 Science to Olympus Conference
    engine.assertCounts(8 to "TemperatureStep")
    KB.playProject(DesignedMicroorganisms, 15)
    // KB used Convert Heat standard action
    KB.convertHeat()
    // ER played Solar Wind Power
    // ER gained 1 energy production
    // ER gained 2 titanium
    ER.playProject(SolarWindPower, 3, titanium = 2).expect("0 T")
    // ER used Red Ships action
    // ER gained 8 M€
    ER.cardAction1(RedShips).expect("8 M")
    // KB played Potatoes
    // KB gained 2 M€ production
    // NOTE: KB lost 2 plants.
    KB.playProject(Potatoes, 1)
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(IndustrialMicrobes)
    // ER played Subterranean Reservoir
    // ER placed ocean tile at 12
    // ER gained 4 M€ from 2 ocean(s)
    // ER gained 3 M€
    ER.playProject(SubterraneanReservoir, 11) { doTask("OceanTile<Elysium_2_5>") }.expect("-4 M")
    // ER played Miranda Resort
    // ER gained 2 M€ production
    ER.playProject(MirandaResort, 4, titanium = 2)
    // KB passed
    KB.pass()
    // NOTE: Fixture inference: Cyberia Systems is ER's only unplayed card not needed later.
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(CyberiaSystems)
    // ER played Lightning Harvest
    // ER gained 1 M€ production
    // ER gained 1 energy production
    ER.playProject(LightningHarvest, 8)
    // ER passed
    ER.pass()

    // Generation 8
    // First player this generation is KB
    // ER bought 4 card(s)
    // You bought Mars University,Great Dam:promo,GHG Factories,Meltworks
    ER.buyCards(MarsUniversity, GreatDamPromo, GhgFactories, Meltworks)
    // JR bought 3 card(s)
    // You bought Algae,Phobos Space Haven,Robot Pollinators
    JR.buyCards(Algae, PhobosSpaceHaven, RobotPollinators)
    // KB bought 2 card(s)
    // You bought Lichen,Tardigrades
    KB.buyCards(Lichen, Tardigrades)

    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Domed Crater
    KB.cardAction1(RestrictedArea) { KB.draw(DomedCrater) }
    // KB used Equatorial Magnetizer action
    // KB lost 1 energy production
    KB.cardAction1(EquatorialMagnetizer)
    // ER used Tycho Magnetics action
    // ER spent 3 energy
    // ER drew 1 card(s)
    // You drew Bio Printing Facility
    ER.cardAction1(TychoMagnetics) {
      doTask("-3 E")
      ER.draw(BioPrintingFacility)
    }
    // ER used Water Import From Europa action
    // ER placed ocean tile at 06
    // ER gained 1 steel
    // ER gained 4 M€ from 2 ocean(s)
    ER.cardAction1(WaterImportFromEuropa) {
          ER.pay(titanium = 3)
          ER.draw(SolarLogistics)
          doTask("OceanTile<Elysium_1_4>")
        }
        .expect("4 M, -3 T")
    // JR used Space Mirrors action
    // JR gained 1 energy production
    JR.cardAction1(SpaceMirrors)
    // JR used Space Elevator action
    // JR gained 5 M€
    JR.cardAction1(SpaceElevator).expect("5 M, -S")
    // KB played Earth Catapult
    KB.playProject(EarthCatapult, 19)
    // KB played Big Asteroid
    // KB gained 4 titanium
    KB.playProject(BigAsteroid, 14, titanium = 2) { doTask("Ok") }.expect("2 T")
    // ER played Great Dam:promo
    // ER gained 2 energy production
    // ER placed Great Dam tile at 07
    // ER drew 1 card(s)
    // You drew Solar Logistics
    // ER gained 4 M€ from 2 ocean(s)
    ER.playProject(GreatDamPromo, steel = 5) { doTask("GreatDamPromo_SpecialTile<Elysium_1_5>") }
        .expect("4 M, PROD[2 E], 0 ProjectCard")
    // ER played Solar Logistics
    // ER gained 2 titanium
    ER.playProject(SolarLogistics, 12, titanium = 2).expect("0 T")
    // JR played Algae
    // JR gained 2 plant production
    // JR gained 1 plant
    JR.playProject(Algae, 10)
    // JR used Convert Plants standard action
    // JR placed greenery tile at 25
    // JR gained 1 plant
    // JR gained 6 M€ from 3 ocean(s)
    JR.convertPlants { doTask("GreeneryTile<Elysium_4_5>") }
    // KB used Convert Heat standard action
    KB.convertHeat()
    // KB used Convert Heat standard action
    KB.convertHeat()
    // ER used Inventors' Guild action
    // ER bought 0 card(s)
    ER.cardAction1(InventorsGuild) { doTask("Ok") }
    // ER played Mars University
    // ER is using their Mars University effect to draw a card by discarding a card.
    // ER discarded Meltworks
    // ER drew 1 card(s)
    // You drew Aqueduct Systems
    ER.playProject(MarsUniversity, 2, steel = 2) {
      ER.draw(AqueductSystems)
      doTask("-ProjectCard")
      ER.discard(Meltworks)
    }
    // JR played Robot Pollinators
    // JR gained 1 plant production
    // JR gained 2 plants
    JR.playProject(RobotPollinators, 9)
    // JR played Medical Lab
    // JR gained 6 M€ production
    JR.playProject(MedicalLab, 13)
    // KB played Nitrogen-Rich Asteroid
    // KB gained 4 plant production
    // ER drew 1 card(s)
    // You drew Insulation
    KB.playProject(NitrogenRichAsteroid, 12, titanium = 4) {
      ER.draw(Insulation)
      doTask("PROD[4 Plant]")
    }
    // KB played Small Asteroid
    // ER drew 1 card(s)
    // You drew Shuttles
    // JR lost 2 plants because of KB
    KB.playProject(SmallAsteroid, 3) {
      ER.draw(Shuttles)
      doTask("-2 P<JR>")
    }
    // ER played GHG Factories
    // ER lost 1 energy production
    // ER gained 4 heat production
    // NOTE: Fixture payment inference: ER spent a fourth steel for the remaining 2 M€ so the
    // NOTE: later Gene Repair payment and final dashboard can both be reproduced.
    ER.intentionalOverpay()
    ER.playProject(GhgFactories, steel = 4)
    // ER played Robotic Workforce
    // ER is using their Mars University effect to draw a card by discarding a card.
    // ER discarded Aqueduct Systems
    // ER drew 1 card(s)
    // You drew Lake Marineris
    // ER copied GHG Factories production with Robotic Workforce
    // ER lost 1 energy production
    // ER gained 4 heat production
    ER.playProject(RoboticWorkforce, 9) {
          ER.draw(LakeMarineris)
          doTask("-ProjectCard")
          ER.discard(AqueductSystems)
          doTask("CopyProductionBox<$GhgFactories>")
        }
        .expect("PROD[-E, 4 H], -ProjectCard")
    // JR played Phobos Space Haven
    // JR gained 1 titanium production
    // JR gained 3 M€
    // JR gained 1 M€ production
    // JR added 1 Animal to Pets
    JR.playProject(PhobosSpaceHaven, 22, titanium = 1)
    // JR funded Founder award
    JR.stdAction("FundAwardSA") { doTask("Founder") }
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(DomedCrater)
    // KB played Tardigrades
    KB.playProject(Tardigrades, 1)
    // ER played Gene Repair
    // ER gained 2 M€ production
    // ER is using their Mars University effect to draw a card by discarding a card.
    // ER discarded Lake Marineris
    // ER drew 1 card(s)
    // You drew Power Grid
    ER.playProject(GeneRepair, 12) {
      ER.draw(PowerGrid)
      doTask("-ProjectCard")
      ER.discard(LakeMarineris)
    }
    // ER used Red Ships action
    // ER gained 9 M€
    ER.cardAction1(RedShips).expect("9 M")
    // JR used Sell Patents standard project
    // JR sold 2 patents
    JR.sellPatents(Steelworks, HomeostasisBureau)
    // JR passed
    JR.declineSecondAction()
    // KB used Tardigrades action
    // KB added 1 Microbe to Tardigrades
    KB.cardAction1(Tardigrades)
    // KB passed
    KB.declineSecondAction()
    // ER played Shuttles
    // ER gained 2 M€ production
    // ER lost 1 energy production
    ER.playProject(Shuttles, 2, titanium = 2)
    // ER passed
    ER.declineSecondAction()
    JR.pass()
    KB.pass()
    ER.pass()

    // Generation 9
    // First player this generation is ER
    // KB bought 3 card(s)
    // You bought Symbiotic Fungus,Ants,Worms
    KB.buyCards(SymbioticFungus, Ants, Worms)
    // ER bought 3 card(s)
    // You bought Small Animals,Public Baths,Callisto Penal Mines
    ER.buyCards(SmallAnimals, PublicBaths, CallistoPenalMines)
    // JR bought 1 card(s)
    // You bought Cloud Seeding
    JR.buyCards(CloudSeeding)

    // ER played Deimos Down:promo
    // ER gained 4 steel
    // ER drew 1 card(s)
    // You drew Biomass Combustors
    // ER placed Deimos Down tile at 61
    // ER drew 1 card(s)
    // You drew Heat Trappers
    // JR lost 6 plants because of ER
    // JR received 3 M€ from Mons Insurance owner (KB)
    // ER gained 3 M€
    ER.playProject(DeimosDownPromo, 9, titanium = 5) {
      ER.draw(BiomassCombustors, HeatTrappers)
      doTask("DeimosDownPromo_SpecialTile<Elysium_9_7>")
      doTask("-6 P<JR>")
    }
    // ER used Convert Heat standard action
    ER.convertHeat()
    // JR used Space Elevator action
    // JR gained 5 M€
    JR.cardAction1(SpaceElevator).expect("5 M, -S")
    // JR used Space Mirrors action
    // JR gained 1 energy production
    JR.cardAction1(SpaceMirrors)
    // KB used Convert Plants standard action
    // KB placed greenery tile at 39
    // KB gained 1 plant
    KB.convertPlants { doTask("GreeneryTile<Elysium_6_3>") }
    // KB used Restricted Area action
    // KB drew 1 card(s)
    // You drew Kelp Farming
    KB.cardAction1(RestrictedArea) { KB.draw(KelpFarming) }
    // ER used Inventors' Guild action
    // ER bought 1 card(s)
    // You bought Magnetic Field Dome
    ER.cardAction1(InventorsGuild) { ER.buyCards(MagneticFieldDome) }
    // ER played Small Animals
    // KB lost 1 plant production because of ER
    ER.playProject(SmallAnimals, 6) { doTask("PROD[-P<KB>]") }
    // JR used Greenery standard project
    // JR placed greenery tile at 42
    // JR gained 1 plant
    JR.stdProject("GreenerySP") { doTask("GreeneryTile<Elysium_6_6>") }
    // JR used Greenery standard project
    // JR placed greenery tile at 15
    JR.stdProject("GreenerySP") { doTask("GreeneryTile<Elysium_3_2>") }
    // KB played Research
    // KB drew 2 card(s)
    // You drew Public Plans,Invention Contest
    // KB removed a resource from Olympus Conference to draw a card
    // KB drew 1 card(s)
    // You drew Rego Plastics
    // KB added 1 Science to Olympus Conference
    KB.playProject(Research, 8) {
      KB.draw(PublicPlans, InventionContest, RegoPlastics)
      doTask("ProjectCard FROM Science<$OlympusConference>")
    }
    // KB used Equatorial Magnetizer action
    // KB lost 1 energy production
    KB.cardAction1(EquatorialMagnetizer)
    // ER played Bio Printing Facility
    ER.playProject(BioPrintingFacility, 1, steel = 2)
    // ER used Bio Printing Facility action
    // ER added 1 Animal to Small Animals
    ER.cardAction1(BioPrintingFacility) { doTask("Animal<$SmallAnimals>") }
    // JR used Greenery standard project
    // JR placed greenery tile at 22
    // JR gained 1 plant
    JR.stdProject("GreenerySP") { doTask("GreeneryTile<Elysium_4_2>") }
    // JR passed
    JR.declineSecondAction()
    // KB played Invention Contest
    // KB removed a resource from Olympus Conference to draw a card
    // KB drew 1 card(s)
    // You drew Martian Rails
    // KB drew 1 card(s)
    // You drew Imported Nutrients
    KB.playProject(InventionContest, megacredits = 0) {
      KB.draw(MartianRails, ImportedNutrients)
      doTask("ProjectCard FROM Science<$OlympusConference>")
    }
    // KB used Tardigrades action
    // KB added 1 Microbe to Tardigrades
    KB.cardAction1(Tardigrades)
    // ER played Magnetic Field Dome
    // ER gained 1 plant production
    // ER lost 2 energy production
    // NOTE: Fixture payment inference: ER spent a second steel for the remaining 2 M€ so the
    // NOTE: later Callisto Penal Mines payment and final dashboard can both be reproduced.
    ER.intentionalOverpay()
    ER.playProject(MagneticFieldDome, steel = 2)
    // ER used Small Animals action
    // ER added 1 Animal to Small Animals
    ER.cardAction1(SmallAnimals)
    // NOTE: Heroku records JR's pass as a second action; defer it to this legal boundary.
    JR.pass()
    // KB played Public Plans
    // KB gained 14 M€ because of Public Plans
    // KB revealed Soil Factory,Martian Lumber Corp,Bactoviral Research,Mining Rights,AI
    // Central,Regolith Eaters,Lichen,Imported Nutrients,Martian Rails,Rego Plastics,Kelp
    // Farming,Worms,Ants,Symbiotic Fungus
    KB.playProject(PublicPlans, 4) { doTask("14") }.expect("10 M")
    // KB played Ants
    KB.playProject(Ants, 6)
    // ER used Red Ships action
    // ER gained 9 M€
    ER.cardAction1(RedShips).expect("9 M")
    // ER played Public Baths
    // ER gained 6 M€
    ER.playProject(PublicBaths, megacredits = 0, steel = 2).expect("6 M")
    // KB used Ants action
    // KB removed 1 resource(s) from KB's Tardigrades
    // KB added 1 Microbe to Ants
    KB.cardAction1(Ants)
    // KB funded Benefactor award
    KB.stdAction("FundAwardSA") { doTask("Benefactor") }
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(SpecialDesign)
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(Trees)
    // KB played Bactoviral Research
    // KB drew 1 card(s)
    // You drew Permafrost Extraction
    // KB added 1 Science to Olympus Conference
    // KB added 11 Microbe(s) to Ants
    KB.doTask("ScienceTag<WildTagUse<$ResearchNetwork>>")
    KB.playProject(BactoviralResearch, 7) {
      KB.draw(PermafrostExtraction)
      doTask("11 Microbe<$Ants>")
    }
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(MiningRights)
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(Insulation)
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(PowerGrid)
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(AiCentral)
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(RegolithEaters)
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(BiomassCombustors)
    // ER used City standard project
    // ER placed city tile at 38
    // ER gained 1 steel
    // JR gained 1 M€ production
    // JR gained 1 M€ production
    // JR added 1 Animal to Pets
    // NOTE: ER gained 1 M€ production.
    ER.stdProject("CitySP") { doTask("CityTile<Elysium_6_2>") }.expect("S<ER>, PROD[2 M<JR>]")
    // KB played Worms
    // KB gained 3 plant production
    KB.doTask("MicrobeTag<WildTagUse<$ResearchNetwork>>")
    KB.playProject(Worms, 5).expect("PROD[3 P]")
    // KB played Imported Nutrients
    // KB gained 4 plants
    // ER drew 1 card(s)
    // You drew Trans-Neptune Probe
    // KB added 4 Microbe(s) to Ants
    KB.playProject(ImportedNutrients, 1, titanium = 1) {
      ER.draw(TransNeptuneProbe)
      doTask("4 Microbe<$Ants>")
    }
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(HeatTrappers)
    // ER played Callisto Penal Mines
    // ER gained 3 M€ production
    ER.playProject(CallistoPenalMines, 22)
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(Lichen)
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(MartianRails)
    // ER used Sell Patents standard project
    // ER sold 1 patents
    ER.sellPatents(TransNeptuneProbe)
    // ER passed
    ER.declineSecondAction()
    // KB used Sell Patents standard project
    // KB sold 4 patents
    KB.sellPatents(RegoPlastics, KelpFarming, SymbioticFungus, PermafrostExtraction)
    // KB played Soil Factory
    // KB gained 1 plant production
    // KB lost 1 energy production
    KB.playProject(SoilFactory, 6)
    ER.pass()
    // KB used Sell Patents standard project
    // KB sold 1 patents
    KB.sellPatents(MartianLumberCorp)
    assertSidebar(gen = 9, temp = 8, oxygen = 14, oceans = 9)
    // KB passed
    KB.pass()

    // Final greenery placement
    ER.doTask("Ok")
    // JR placed greenery tile at 35
    // JR gained 2 plants
    // JR gained 4 M€ from 2 ocean(s)
    JR.convertPlants { doTask("GreeneryTile<Elysium_5_7>") }
    JR.doTask("Ok")
    // KB placed greenery tile at 47
    // KB gained 1 steel
    // KB placed greenery tile at 54
    KB.convertPlants { doTask("GreeneryTile<Elysium_7_4>") }
    KB.convertPlants { doTask("GreeneryTile<Elysium_8_5>") }
    KB.doTask("Ok")

    // This game id was g4ce040d78bb6
    assertCardTrackingComplete()
    JR.cardsInHand shouldBe setOf(CloudSeeding)
    KB.cardsInHand shouldBe emptySet()
    ER.cardsInHand shouldBe emptySet()

    val score = Summarizer(game)
    // NOTE: Heroku's player-colored award scores are Founder 5/4/3, Benefactor 33/43/38, and
    // NOTE: Banker 42/11/15 for JR/KB/ER. GrossHack raises each internal Banker tally by five.
    JR.assertCounts(
        5 to "AwardTally<JR, Founder>",
        33 to "AwardTally<JR, Benefactor>",
        47 to "AwardTally<JR, Banker>",
        33 to "TR",
        84 to "VP",
        1 to "Victory",
    )
    KB.assertCounts(
        4 to "AwardTally<KB, Founder>",
        43 to "AwardTally<KB, Benefactor>",
        16 to "AwardTally<KB, Banker>",
    )
    ER.assertCounts(
        3 to "AwardTally<ER, Founder>",
        38 to "AwardTally<ER, Benefactor>",
        20 to "AwardTally<ER, Banker>",
    )
    KB.assertCounts(43 to "TR", 75 to "VP", 0 to "Victory")
    ER.assertCounts(38 to "TR", 67 to "VP", 0 to "Victory")
    score.net("Milestone", "VP<JR>") shouldBe 10
    score.net("Milestone", "VP<KB>") shouldBe 5
    score.net("Milestone", "VP<ER>") shouldBe 0
    score.net("FirstPlace", "VP<JR>") shouldBe 10
    score.net("SecondPlace", "VP<JR>") shouldBe 0
    score.net("FirstPlace", "VP<KB>") shouldBe 5
    score.net("SecondPlace", "VP<KB>") shouldBe 2
    score.net("FirstPlace", "VP<ER>") shouldBe 0
    score.net("SecondPlace", "VP<ER>") shouldBe 4
    score.net("GreeneryTile", "VP<JR>") shouldBe 6
    score.net("GreeneryTile", "VP<KB>") shouldBe 4
    score.net("GreeneryTile", "VP<ER>") shouldBe 2
    score.net("CityTile", "VP<JR>") shouldBe 9
    score.net("CityTile", "VP<KB>") shouldBe 3
    score.net("CityTile", "VP<ER>") shouldBe 2
    score.net("Card", "VP<JR>") shouldBe 16
    score.net("Card", "VP<KB>") shouldBe 13
    score.net("Card", "VP<ER>") shouldBe 21

    JR.assertResources(m = 81, s = 1, t = 4, p = 3, e = 5, h = 10)
    JR.assertProduction(m = 42, s = 1, t = 2, p = 6, e = 5, h = 0)
    KB.assertResources(m = 59, s = 1, t = 1, p = 1, e = 9, h = 28)
    KB.assertProduction(m = 11, s = 0, t = 1, p = 10, e = 9, h = 2)
    ER.assertResources(m = 54, s = 9, t = 5, p = 3, e = 0, h = 8)
    ER.assertProduction(m = 15, s = 5, t = 5, p = 1, e = 0, h = 8)
  }
}
