package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete archive replay: Solar Fusion Stream (g4ce040d78bb6)
// https://terraforming-mars.herokuapp.com/the-end?id=pc2de3208e4ca
internal class SolarFusionStreamTest : CardTrackingFullGameTest() {
  // Player-record evidence: Elysium, Corporate Era, Prelude, promo cards, drafting, fast mode,
  // three players, and these limited-synergy milestone and award pools.
  // Unsupported component: unclaimed Terraformer substitutes for unclaimed Hydrologist.
  // The thresholds select the archived Builder and Terraformer variants.
  override val config =
      GameConfig(
          """
          ElysiumMap
          PreludeExpansion, PromoCardPack

          Builder7, Philantropist, Spacefarer, Terraformer29, Energizer
          Incorporator, Botanist, Founder, Benefactor, Banker
          """,
          "JR",
          "KB",
          "ER",
      )
  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  @Test
  internal fun game20260819() {
    TfmWorkflow.Auto(game).launch()

    val JR = p1
    val KB = p2
    val ER = p3

    engine.assertCounts(1 to "Generation")

    // Player-record evidence: JR rejected Teractor and PolderTECH Dutch; UNMI Contractor and
    // Acquired Space Agency; Crash Site Cleanup, Outdoor Sports, Interstellar Colony Ship,
    // Tropical Resort, Physics Complex, and Weather Balloons.
    JR.playCorp(TharsisRepublic) {
      JR.buyCards(MethaneFromTitan, TechnologyDemonstration, FueledGenerators, LavaTubeSettlement)
    }

    // Player-record evidence: KB rejected Utopia Invest and Recyclon; Great Aquifer and Polar
    // Industries; Asteroid Deflection System, Decomposers, Black Polar Dust, Comet Aiming, and
    // Astra Mechanica.
    // Test inference: the log identifies three of KB's five kept projects. Imported GHG and
    // Corporate Stronghold are the two never-played candidates from that deal.
    KB.playCorp(MonsInsurance) {
          KB.buyCards(
              EnergyTapping,
              StaticHarvesting,
              StandardTechnology,
              ImportedGhg,
              CorporateStronghold,
          )
        }
        .expect("PROD[-2 Megacredit<JR>, -2 Megacredit<ER>]")

    // Player-record evidence: ER rejected Factorum and Viron; Mohole and Huge Asteroid; and Meat
    // Industry, Supercapacitors, Orbital Cleanup, Ice Cap Melting, and Towing A Comet.
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
      playPrelude(SmeltingPlant)
      playPrelude(SelfSufficientSettlement) {
        draw(KaguyaTech, SpaceElevator, MagneticShield)
        placeTile(3, 7)
      }
    }

    KB.turn {
      playPrelude(ResearchNetwork) {
            draw(ResearchOutpost, RestrictedArea, AcquiredCompany)
          }
          .expect("PROD[1], WildTag")
      // Unsupported component: Fake Established Methods models the archived card's two standard
      // projects, but not its unused unaffordable-second-project fallback.
      playPrelude(FakeEstablishedMethods) {
            doTask("UseAction<PowerPlantSP, First>")
            pay(11)
            doTask("UseAction<PowerPlantSP, First>")
            pay(11)
          }
          .expect("8")
    }

    ER.turn {
      // Payment reconstruction: ER spent five.
      playPrelude(GalileanMining)
      // Payment reconstruction: ER spent three.
      playPrelude(AquiferTurbines) { placeTile(4, 7) }.expect("PROD[2 Energy], Plant")
    }

    JR.stdAction("HandleMandates") { placeTile(5, 6) }.expect("3 Plant, 3, PROD[1]")
    JR.playProject(MethaneFromTitan, 28)
    KB.playProject(ResearchOutpost, 18) { placeTile(5, 3) }.expect("PROD[Megacredit<JR>]")
    KB.playProject(AcquiredCompany, 9)
    ER.playProject(IndustrialCenter, 4) { placeTile(4, 8) }.expect("Plant, Steel")
    ER.cardAction1(IndustrialCenter)
    JR.playProject(TechnologyDemonstration, 5) { JR.draw(EnergyMarket, SterlingVents) }
    JR.playProject(FueledGenerators, 1)
    KB.playProject(RestrictedArea, 10) {
      KB.draw(PeroxidePower)
      placeTile(9, 8)
    }
    KB.cardAction1(RestrictedArea) { KB.draw(DesignedMicroorganisms) }
    ER.pass()
    JR.pass()
    KB.playProject(EnergyTapping, 2) { doTask("PROD[-Energy<ER>]") }
        .expect("PROD[Energy<KB>, -Energy<ER>]")
    KB.pass()

    ER.buyCards(VestaShipyard)
    KB.buyCards(OlympusConference, NoctisFarming)
    JR.buyCards(ImmigrantCity, NaturalPreserve, Steelworks)

    KB.cardAction1(RestrictedArea) { KB.draw(AdaptedLichen) }
    KB.playProject(PeroxidePower, 6)
    ER.cardAction1(TychoMagnetics, x = 2) { ER.draw(RedShips) }
    ER.playProject(VestaShipyard, 9, titanium = 2)
    // Consequence reconstruction: JR lost one energy production and two money production.
    JR.playProject(ImmigrantCity, 3, steel = 5) {
      JR.draw(Pets)
      placeTile(3, 3)
    }
    JR.playProject(Pets, 10)
    KB.assignWildTag(ResearchNetwork, "BuildingTag")
    KB.playProject(StaticHarvesting, 4).expect("-1")
    KB.stdAction("ClaimMilestoneSA") { doTask("Energizer") }
    ER.cardAction1(IndustrialCenter)
    ER.declineSecondAction()
    JR.playProject(SterlingVents, 5)
    JR.declineSecondAction()
    KB.pass()
    // Chronology: Heroku records ER's and JR's illegal second-action passes here. Decline those
    // second actions in Solarnet, then perform the no-effect passes at the next legal first-action
    // boundary.
    ER.pass()
    JR.pass()

    JR.buyCards(FieldCappedCity)
    ER.buyCards(RoboticWorkforce, StripMine)
    KB.buyCards(SoilFactory, QuantumExtractor, NitrogenRichAsteroid)

    ER.cardAction1(TychoMagnetics, x = 2) { ER.draw(LightningHarvest) }
    ER.cardAction1(IndustrialCenter)
    JR.playProject(NaturalPreserve, 9) { placeTile(8, 4) }
    JR.stdAction("ClaimMilestoneSA") { doTask("Builder7") }
    KB.cardAction1(RestrictedArea) { KB.draw(MartianLumberCorp) }
    KB.playProject(OlympusConference, 9, steel = 0)
    ER.pass()
    JR.playProject(EnergyMarket, 3)
    JR.declineSecondAction()
    KB.assignWildTag(ResearchNetwork, "ScienceTag")
    KB.playProject(QuantumExtractor, 12) {
      KB.draw(EarthOffice)
      doTask("ProjectCard FROM Science<$OlympusConference>")
    }
    KB.playProject(EarthOffice, 0)
    // Chronology: Heroku records JR's pass as a second action; defer it to this legal boundary.
    JR.pass()
    KB.pass()

    KB.buyCards(BactoviralResearch, InvestmentLoan, IceAsteroid, MassConverter)
    JR.buyCards(InterplanetaryTrade)
    ER.buyCards(Greenhouses, DuskLaserMining, InventorsGuild, DeepWellHeating)

    JR.convertPlants { placeTile(2, 6) }
    JR.playProject(KaguyaTech, 10) {
      JR.draw(HermeticOrderOfMars)
      doTask("CityTile<Elysium_2_6> FROM GreeneryTile<Elysium_2_6>")
    }
    KB.assignWildTag(ResearchNetwork, "ScienceTag")
    KB.playProject(MassConverter, 7)
    KB.playProject(InvestmentLoan, 0)
    ER.cardAction1(TychoMagnetics, x = 2) { ER.draw(GiantSpaceMirror) }
    ER.playProject(InventorsGuild, 9)
    JR.playProject(LavaTubeSettlement, 3, steel = 6) { placeTile(3, 1) }
    JR.playProject(HermeticOrderOfMars, 10)
    KB.cardAction1(RestrictedArea) { KB.draw(Capital) }
    KB.playProject(StandardTechnology, 5) {
      KB.draw(Hackers)
      doTask("ProjectCard FROM Science<$OlympusConference>")
    }
    ER.cardAction1(InventorsGuild) { /* Decline buying the revealed card. */
      ER.buyCards(0)
    }
    ER.playProject(DuskLaserMining, 2, titanium = 2)
    JR.playProject(InterplanetaryTrade, 21, titanium = 2)
    JR.declineSecondAction()
    KB.playProject(AdaptedLichen, 8)
    KB.playProject(Hackers, 2) { doTask("PROD[-2 Megacredit<JR>]") }
    // User recollection: ER used all eight titanium and retained 11 M€ after this play.
    ER.playProject(WaterImportFromEuropa, 1, titanium = 8)
    ER.assertCounts(11 to "Megacredit", 0 to "Titanium")
    // Test inference: Deep Well Heating is the only never-played card in ER's hand here.
    ER.sellPatents(DeepWellHeating)
    JR.pass()
    KB.pass()
    ER.cardAction1(WaterImportFromEuropa) {
          ER.pay(12)
          placeTile(4, 4)
        }
        .expect("2 Plant")
    ER.pass()

    JR.buyCards(Archaebacteria, Supermarkets, MedicalLab)
    KB.buyCards(MiningRights, BusinessContacts, HiredRaiders)
    ER.buyCards(DustSeals, Sponsors)

    KB.cardAction1(RestrictedArea) { KB.draw(EquatorialMagnetizer) }
    KB.playProject(BusinessContacts, 3) { KB.draw(TitaniumMine, LawSuit) }
    ER.cardAction1(TychoMagnetics, x = 1) { ER.draw(SolarWindPower) }
    ER.cardAction1(InventorsGuild) { /* Decline buying the revealed card. */
      ER.buyCards(0)
    }
    JR.stdAction("ClaimMilestoneSA") { doTask("Philantropist") }
    JR.playProject(SpaceElevator, 27)
    KB.playProject(HiredRaiders, 0) { doTask("2 Steel<KB> FROM Steel<ER>") }
        .expect("-3 Megacredit<KB>, 3 Megacredit<ER>")
    KB.playProject(TitaniumMine, 2, steel = 2)
    ER.playProject(DustSeals, 2)
    ER.playProject(Sponsors, 6)
    JR.playProject(Archaebacteria, 6)
    JR.declineSecondAction()
    // Test inference: Imported GHG is one of KB's two unidentified opening keeps.
    KB.sellPatents(ImportedGhg)
    KB.playProject(EquatorialMagnetizer, 10)
    ER.cardAction1(WaterImportFromEuropa) {
      ER.pay(titanium = 4)
      placeTile(5, 4)
    }
    ER.playProject(CommercialDistrict, 0, steel = 8) {
          placeTile(4, 3)
        }
        .expect("4, Plant")
    JR.pass()
    KB.cardAction1(EquatorialMagnetizer)
    KB.declineSecondAction()
    ER.pass()
    KB.pass()

    ER.buyCards(MediaGroup, CyberiaSystems)
    KB.buyCards(AiCentral, EarthCatapult, RegolithEaters, SmallAsteroid)
    JR.buyCards(SpaceMirrors, Zeppelins)

    ER.cardAction1(InventorsGuild) { /* Decline buying the revealed card. */
      ER.buyCards(0)
    }
    ER.playProject(GiantSpaceMirror, 5, titanium = 4).expect("PROD[3 Energy]")
    JR.convertHeat()
    JR.playProject(FieldCappedCity, 29) { placeTile(6, 5) }
    KB.convertHeat()
    KB.convertHeat()
    ER.cardAction1(WaterImportFromEuropa) {
      ER.pay(12)
      placeTile(4, 6)
    }
    ER.convertPlants { placeTile(5, 8) }
    JR.convertPlants { placeTile(5, 5) }

    // Screenshot evidence: Game20260819-dashboard-gen6.png was taken here, after JR's generation-6
    // greenery and before JR played Zeppelins.
    assertSidebar(gen = 6, temp = -24, oxygen = 5, oceans = 4)
    ER.assertResources(m = 23, s = 3, t = 0, p = 2, e = 0, h = 0)
    ER.assertProduction(m = 4, s = 3, t = 4, p = 0, e = 3, h = 0)
    JR.assertResources(m = 18, s = 0, t = 1, p = 4, e = 1, h = 0)
    JR.assertProduction(m = 24, s = 0, t = 1, p = 3, e = 2, h = 0)
    KB.assertResources(m = 17, s = 0, t = 1, p = 4, e = 14, h = 18)
    KB.assertProduction(m = 8, s = 0, t = 1, p = 1, e = 14, h = 1)
    ER.assertCounts(25 to "TerraformRating")
    JR.assertCounts(25 to "TerraformRating")
    KB.assertCounts(23 to "TerraformRating")
    checkHandSizes()

    JR.playProject(Zeppelins, 13)
    KB.convertHeat()
    KB.convertHeat()
    ER.playProject(RedShips, 2)
    ER.cardAction1(RedShips)
    JR.playProject(SpaceMirrors, titanium = 1)
    JR.declineSecondAction()
    KB.cardAction1(RestrictedArea) { KB.draw(IndustrialMicrobes) }
    KB.cardAction1(EquatorialMagnetizer)
    ER.playProject(Greenhouses, 0, steel = 3)
    ER.convertPlants { placeTile(5, 9) }.expect("Titanium")
    JR.pass()
    // Test inference: Corporate Stronghold is KB's other unidentified opening keep.
    KB.sellPatents(CorporateStronghold)
    KB.playProject(IceAsteroid, 15, titanium = 1) {
      doTask("OceanTile<Elysium_3_5>")
      doTask("OceanTile<Elysium_3_6>")
    }
    ER.playProject(StripMine, 25)
    ER.declineSecondAction()
    KB.playProject(NoctisFarming, 9)
    KB.declineSecondAction()
    ER.pass()
    KB.pass()

    JR.buyCards(Mine, HomeostasisBureau)
    ER.buyCards(SpecialDesign, AdvancedAlloys, SubterraneanReservoir, Trees)
    KB.buyCards(BigAsteroid, Research)

    JR.cardAction1(SpaceMirrors)
    JR.stdAction("FundAwardSA") { doTask("Banker") }
    KB.convertPlants { placeTile(5, 2) }
    KB.cardAction1(RestrictedArea) { KB.draw(Potatoes) }
    ER.cardAction1(TychoMagnetics, x = 1) { ER.draw(DeimosDownPromo) }
    ER.cardAction1(InventorsGuild) { /* Decline buying the revealed card. */
      ER.buyCards(0)
    }
    JR.playProject(Mine, 4)
    JR.playProject(Supermarkets, 9)
    KB.cardAction1(EquatorialMagnetizer)
    // Test inference: Capital is the first of KB's three unplayed generation-7 sales.
    KB.sellPatents(Capital)
    ER.playProject(AdvancedAlloys, 9)
    ER.cardAction1(WaterImportFromEuropa) {
          ER.pay(titanium = 3)
          ER.draw(GeneRepair)
          placeTile(1, 3)
        }
        .expect("TerraformRating, ProjectCard")
    JR.playProject(MagneticShield, 21, titanium = 1)
    JR.declineSecondAction()
    KB.sellPatents(LawSuit)
    KB.convertHeat()
    ER.playProject(MediaGroup, 6)
    ER.playProject(AsteroidCard, 2, titanium = 3) { doTask("-3 Plant<JR>") }
        .expect("-3 Megacredit<KB>, 3 Megacredit<JR>")
    JR.pass()
    engine.assertCounts(8 to "TemperatureStep")
    KB.playProject(DesignedMicroorganisms, 15)
    KB.convertHeat()
    ER.playProject(SolarWindPower, 3, titanium = 2).expect("0 Titanium")
    ER.cardAction1(RedShips).expect("8")
    // Consequence reconstruction: KB lost two plants.
    KB.playProject(Potatoes, 1)
    KB.sellPatents(IndustrialMicrobes)
    ER.playProject(SubterraneanReservoir, 11) { placeTile(2, 5) }
    ER.playProject(MirandaResort, 4, titanium = 2).expect("-4, -2 Titanium")
    KB.pass()
    // Test inference: Cyberia Systems is ER's only unplayed card not needed later.
    ER.sellPatents(CyberiaSystems)
    ER.playProject(LightningHarvest, 8)
    ER.pass()

    ER.buyCards(MarsUniversity, GreatDamPromo, GhgFactories, Meltworks)
    JR.buyCards(Algae, PhobosSpaceHaven, RobotPollinators)
    KB.buyCards(Lichen, Tardigrades)

    KB.cardAction1(RestrictedArea) { KB.draw(DomedCrater) }
    KB.cardAction1(EquatorialMagnetizer)
    ER.cardAction1(TychoMagnetics, x = 3) { ER.draw(BioPrintingFacility) }
    ER.cardAction1(WaterImportFromEuropa) {
      ER.pay(titanium = 3)
      ER.draw(SolarLogistics)
      placeTile(1, 4)
    }
    JR.cardAction1(SpaceMirrors)
    JR.cardAction1(SpaceElevator)
    KB.playProject(EarthCatapult, 19)
    KB.playProject(BigAsteroid, 14, titanium = 2) { /* Decline removing an opponent's plants. */
          declineTask()
        }
        .expect("2 Titanium")
    ER.playProject(GreatDamPromo, steel = 5) { placeTile(1, 5) }.expect("0 ProjectCard")
    ER.playProject(SolarLogistics, 12, titanium = 2).expect("0 Titanium")
    JR.playProject(Algae, 10).expect("0 ProjectCard<ER>")
    JR.convertPlants { placeTile(4, 5) }
    KB.convertHeat()
    KB.convertHeat()
    ER.cardAction1(InventorsGuild) { /* Decline buying the revealed card. */
      ER.buyCards(0)
    }
    ER.playProject(MarsUniversity, 2, steel = 2) {
      ER.draw(AqueductSystems)
      doTask("-ProjectCard")
      ER.discard(Meltworks)
    }
    JR.playProject(RobotPollinators, 9)
    JR.playProject(MedicalLab, 13)
    KB.playProject(NitrogenRichAsteroid, 12, titanium = 4) {
          ER.draw(Insulation)
          doTask("PROD[4 Plant]")
        }
        .expect("PROD[4 Plant<KB>], ProjectCard<ER>")
    KB.playProject(SmallAsteroid, 3) {
      ER.draw(Shuttles)
      doTask("-2 Plant<JR>")
    }
    // Payment reconstruction: ER spent a fourth steel for the remaining two so the later Gene
    // Repair payment and final dashboard can both be reproduced.
    ER.intentionalOverpay(1)
    ER.playProject(GhgFactories, steel = 4)
    ER.playProject(RoboticWorkforce, 9) {
      ER.draw(LakeMarineris)
      doTask("-ProjectCard")
      ER.discard(AqueductSystems)
      doTask("CopyProductionBox<$GhgFactories>")
    }
    JR.playProject(PhobosSpaceHaven, 22, titanium = 1)
    JR.stdAction("FundAwardSA", which = 2) { doTask("Founder") }
    KB.sellPatents(DomedCrater)
    KB.playProject(Tardigrades, 1)
    ER.playProject(GeneRepair, 12) {
      ER.draw(PowerGrid)
      doTask("-ProjectCard")
      ER.discard(LakeMarineris)
    }
    ER.cardAction1(RedShips)
    JR.sellPatents(Steelworks, HomeostasisBureau)
    JR.declineSecondAction()
    KB.cardAction1(Tardigrades)
    KB.declineSecondAction()
    ER.playProject(Shuttles, 2, titanium = 2)
    ER.declineSecondAction()
    JR.pass()
    KB.pass()
    ER.pass()

    KB.buyCards(SymbioticFungus, Ants, Worms)
    ER.buyCards(SmallAnimals, PublicBaths, CallistoPenalMines)
    JR.buyCards(CloudSeeding)

    ER.playProject(DeimosDownPromo, 9, titanium = 5) {
      ER.draw(BiomassCombustors, HeatTrappers)
      placeTile(9, 7)
      doTask("-6 Plant<JR>")
    }
    ER.convertHeat()
    JR.cardAction1(SpaceElevator)
    JR.cardAction1(SpaceMirrors)
    KB.convertPlants { placeTile(6, 3) }
    KB.cardAction1(RestrictedArea) { KB.draw(KelpFarming) }
    ER.cardAction1(InventorsGuild) { ER.buyCards(MagneticFieldDome) }
    ER.playProject(SmallAnimals, 6) { doTask("PROD[-Plant<KB>]") }
    JR.stdProject("GreenerySP") { placeTile(6, 6) }
    JR.stdProject("GreenerySP") { placeTile(3, 2) }
    KB.playProject(Research, 8) {
      KB.draw(PublicPlans, InventionContest, RegoPlastics)
      doTask("ProjectCard FROM Science<$OlympusConference>")
    }
    KB.cardAction1(EquatorialMagnetizer)
    ER.playProject(BioPrintingFacility, 1, steel = 2)
    ER.cardAction1(BioPrintingFacility) { addCardResources(SmallAnimals) }
    JR.stdProject("GreenerySP") { placeTile(4, 2) }
    JR.declineSecondAction()
    KB.playProject(InventionContest, megacredits = 0) {
      KB.draw(MartianRails, ImportedNutrients)
      doTask("ProjectCard FROM Science<$OlympusConference>")
    }
    KB.cardAction1(Tardigrades)
    // Payment reconstruction: ER spent a second steel for the remaining two so the later Callisto
    // Penal Mines payment and final dashboard can both be reproduced.
    ER.intentionalOverpay(1)
    ER.playProject(MagneticFieldDome, steel = 2)
    ER.cardAction1(SmallAnimals)
    // Chronology: Heroku records JR's pass as a second action; defer it to this legal boundary.
    JR.pass()
    KB.playProject(PublicPlans, 4) {
          doTask("Revealed")
          doTask("14 ProjectCard<Revealed FROM Hand>")
        }
        .expect("10")
    KB.playProject(Ants, 6)
    ER.cardAction1(RedShips)
    ER.playProject(PublicBaths, megacredits = 0, steel = 2)
    KB.cardAction1(Ants)
    KB.stdAction("FundAwardSA", which = 3) { doTask("Benefactor") }
    ER.sellPatents(SpecialDesign)
    ER.sellPatents(Trees)
    KB.assignWildTag(ResearchNetwork, "ScienceTag")
    KB.playProject(BactoviralResearch, 7) {
      KB.draw(PermafrostExtraction)
      addCardResources(Ants)
    }
    KB.sellPatents(MiningRights)
    ER.sellPatents(Insulation)
    ER.sellPatents(PowerGrid)
    KB.sellPatents(AiCentral)
    KB.sellPatents(RegolithEaters)
    ER.sellPatents(BiomassCombustors)
    // Consequence reconstruction: ER gained one money production.
    ER.stdProject("CitySP") { placeTile(6, 2) }
    KB.assignWildTag(ResearchNetwork, "MicrobeTag")
    KB.playProject(Worms, 5)
    KB.playProject(ImportedNutrients, 1, titanium = 1) {
      ER.draw(TransNeptuneProbe)
      addCardResources(Ants)
    }
    ER.sellPatents(HeatTrappers)
    ER.playProject(CallistoPenalMines, 22)
    KB.sellPatents(Lichen)
    KB.sellPatents(MartianRails)
    ER.sellPatents(TransNeptuneProbe)
    ER.declineSecondAction()
    KB.sellPatents(RegoPlastics, KelpFarming, SymbioticFungus, PermafrostExtraction)
    KB.playProject(SoilFactory, 6)
    ER.pass()
    KB.sellPatents(MartianLumberCorp)
    assertSidebar(gen = 9, temp = 8, oxygen = 14, oceans = 9)
    KB.pass()

    // Decline ER's final greenery placement.
    ER.declineTask()
    JR.convertPlants { placeTile(5, 7) }
    // Decline another final greenery placement for JR.
    JR.declineTask()
    KB.convertPlants { placeTile(7, 4) }
    KB.convertPlants { placeTile(8, 5) }
    // Decline another final greenery placement for KB.
    KB.declineTask()

    assertCardTrackingComplete()
    JR.cardsInHand shouldBe setOf(CloudSeeding)
    KB.cardsInHand shouldBe emptySet()
    ER.cardsInHand shouldBe emptySet()

    JR.assertResources(m = 81, s = 1, t = 4, p = 3, e = 5, h = 10)
    JR.assertProduction(m = 42, s = 1, t = 2, p = 6, e = 5, h = 0)
    KB.assertResources(m = 59, s = 1, t = 1, p = 1, e = 9, h = 28)
    KB.assertProduction(m = 11, s = 0, t = 1, p = 10, e = 9, h = 2)
    ER.assertResources(m = 54, s = 9, t = 5, p = 3, e = 0, h = 8)
    ER.assertProduction(m = 15, s = 5, t = 5, p = 1, e = 0, h = 8)

    JR.assertCounts(
        5 to "AwardTally<JR, Founder>",
        33 to "AwardTally<JR, Benefactor>",
    )
    KB.assertCounts(
        4 to "AwardTally<KB, Founder>",
        43 to "AwardTally<KB, Benefactor>",
    )
    ER.assertCounts(
        3 to "AwardTally<ER, Founder>",
        38 to "AwardTally<ER, Benefactor>",
    )
    // Player-record evidence: Banker tallies are 42/11/15 for JR/KB/ER. Do not assert the raw
    // internal tallies, which include the engine's five-unit production offset.

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<JR>") shouldBe 10
    score.net("Milestone", "VictoryPoint<KB>") shouldBe 5
    score.net("Milestone", "VictoryPoint<ER>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<JR>") shouldBe 10
    score.net("SecondPlace", "VictoryPoint<JR>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<KB>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<KB>") shouldBe 2
    score.net("FirstPlace", "VictoryPoint<ER>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<ER>") shouldBe 4
    score.net("GreeneryTile", "VictoryPoint<JR>") shouldBe 6
    score.net("GreeneryTile", "VictoryPoint<KB>") shouldBe 4
    score.net("GreeneryTile", "VictoryPoint<ER>") shouldBe 2
    score.net("CityTile", "VictoryPoint<JR>") shouldBe 9
    score.net("CityTile", "VictoryPoint<KB>") shouldBe 3
    score.net("CityTile", "VictoryPoint<ER>") shouldBe 2
    score.net("Card", "VictoryPoint<JR>") shouldBe 16
    score.net("Card", "VictoryPoint<KB>") shouldBe 13
    score.net("Card", "VictoryPoint<ER>") shouldBe 21

    JR.assertCounts(33 to "TerraformRating")
    KB.assertCounts(43 to "TerraformRating")
    ER.assertCounts(38 to "TerraformRating")
    JR.assertCounts(84 to "VictoryPoint")
    KB.assertCounts(75 to "VictoryPoint")
    ER.assertCounts(67 to "VictoryPoint")
    JR.assertCounts(1 to "Victory")
    KB.assertCounts(0 to "Victory")
    ER.assertCounts(0 to "Victory")
  }
}
