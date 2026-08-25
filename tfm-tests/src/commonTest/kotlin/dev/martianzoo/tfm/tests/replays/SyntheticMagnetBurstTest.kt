package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Partial archive replay through the generation-10 World Government action:
// Synthetic Magnet Burst (ga5237bd2fb08)
// https://terraforming-mars.herokuapp.com/the-end?id=pa9f45e80d897
internal class SyntheticMagnetBurstTest : CardTrackingFullGameTest() {
  // Player-record evidence: Hellas, Corporate Era, Venus, Prelude, Prelude 2, drafting, World
  // Government, two players, and these full-random milestone and award pools.
  // Unsupported component: Builder7 and Sponsor substitute for unsupported Thawer and Briber.
  // Player-record evidence: Merger was dealt despite promo cards being disabled, so it is included
  // individually without enabling PromoCardPack.
  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, Merger

          Energizer, Builder7, Generalist, Diversifier, Terraformer29, Sponsor
          Scientist, Landscaper, Founder, Contractor, Forecaster, Incorporator
          """,
          "Pink",
          "Green",
      )
  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  @Test
  internal fun gameThroughGeneration10() {
    TfmWorkflow.Auto(game).launch()

    val pink = p1
    val green = p2

    // First player this generation is Pink
    // Good luck Pink!
    // Good luck Green!
    // Generation 1
    engine.assertCounts(1 to "Generation")

    // Pink rejected CrediCor and EcoTec; Venus Contract and Focused Organization; and Windmills,
    // Open City, and Energy Saving.
    // Pink played Tharsis Republic
    pink.playCorp(TharsisRepublic) {
      // Pink kept 7 project cards
      buyCards(
          Mine,
          RoboticWorkforce,
          GreatEscarpmentConsortium,
          DesignedMicroorganisms,
          ImmigrantCity,
          Sponsors,
          OrbitalReflectors,
      )
    }

    // Green rejected Ecoline and Morning Star Inc.; Project Eden and Floating Trade Hub; and
    // Magnetic Field Dome, Heather, Insulation, Cartel, Caretaker Contract, and Protected Habitats.
    // Green played Nirgal Enterprises
    // Green gained 1 steel production
    // Green gained 1 plant production
    // Green gained 1 energy production
    // Green kept 4 project cards
    green.playCorp(NirgalEnterprises) {
      buyCards(MineralDeposit, AquiferPumping, TectonicStressPower, RotatorImpacts)
    }

    pink.turn {
      // Pink played Recession
      // Pink gained 10 M€
      // Green lost 1 M€ production because of Pink
      // Green lost 5 M€ because of Pink
      playPrelude(Recession).expect("PROD[-Megacredit<Green>], -5 Megacredit<Green>")
      // Pink played Merger
      playPrelude(Merger) {
            // You drew Interplanetary Cinematics,Inventrix,Sagitta Frontier Services,Teractor
            // Pink played Sagitta Frontier Services
            doTask("PlayCard<Class<CorporationCard>, Class<$SagittaFrontierServices>>")
            // Pink gained 2 M€ production
            // Pink gained 1 energy production
            /* Discarded 49 cards Freyja Biodomes,Atalanta Planitia Lab,Adaptation Technology,Mining Rights,Urbanized Area,Lichen,Extractor Balloons,Forced Precipitation,Luna Metropolis,Sister Planet Support,Ants,Corroder Suits,Artificial Photosynthesis,Cloud Tourism,Sulphur-Eating Bacteria,Strip Mine,Imported Nitrogen,Standard Technology,Trans-Neptune Probe,Quantum Extractor,Mass Converter,Nitrophilic Moss,Carbonate Processing,Psychrophiles,Mining Area,Towing A Comet,Rover Construction,Extreme-Cold Fungus,Spin-Inducing Asteroid,Indentured Workers,Worms,Immigration Shuttles,Symbiotic Fungus,Livestock,Local Heat Trapping,Underground Detonations,Corporate Stronghold,SF Memorial,Nitrogen-Rich Asteroid,Io Sulphur Research,Imported GHG,Cupola City,Biomass Combustors,Special Design,Inventors' Guild,Comet,Greenhouses,Methane From Titan,Advanced Alloys */
            // Pink drew Micro-Mills
            draw(MicroMills)
            // Pink gained 4 M€ for playing Sagitta Frontier Services, which has no tags.
          }
          .expect("PROD[2, Energy], ProjectCard, -7")
    }

    green.turn {
      // Green played Nobel Prize
      playPrelude(NobelPrize) {
            // Green gained 5 M€
            /* Discarded 6 cards Dirigibles,Asteroid Mining,Viral Enhancers,Soletta,Sabotage,Big Asteroid */
            // Green drew Insects,Stratopolis
            draw(Insects, Stratopolis)
          }
          .expect("5, 2 ProjectCard")
      // Green played Suitable Infrastructure
      // Green gained 5 steel
      playPrelude(SuitableInfrastructure)
    }

    pink.turn {
      // Pink took the first action of Tharsis Republic corporation
      stdAction("HandleMandates") {
            // Pink placed city tile at 61
            placeTile(9, 7)
            // Pink placed ocean tile at 34
            placeTile(5, 6)
            // Pink drew 1 card(s)
            // You drew Industrial Center
            draw(IndustrialCenter)
            // Pink gained 3 M€
            // Pink gained 1 M€ production
          }
          .expect("-3, PROD[1]")
      // Pink played Micro-Mills
      // Pink gained 1 heat production
      // Pink gained 4 M€ for playing Micro-Mills, which has no tags.
      playProject(MicroMills, 3)
    }

    green.turn {
      // Green played Mineral Deposit
      // Green gained 5 steel
      playProject(MineralDeposit, 5)
      // Green ended turn
    }

    pink.turn {
      // Pink played Mine
      // Pink gained 1 steel production
      // Pink gained 1 M€ for playing Mine, which has exactly 1 tag.
      playProject(Mine, 4)
      // Pink played Great Escarpment Consortium
      playProject(GreatEscarpmentConsortium, 6) {
        // Pink stole 1 steel production from Green
        doTask("PROD[-Steel<Green>]")
        // Pink gained 1 steel production
        // Pink gained 4 M€ for playing Great Escarpment Consortium, which has no tags.
      }
    }

    green.turn {
      // Green played Aquifer Pumping
      // The later Aquifer Pumping action requires four of Green's ten steel to remain available.
      intentionalUnderpay()
      playProject(AquiferPumping, 6, steel = 6)
      // Green ended turn
    }

    pink.turn {
      // Pink played Sponsors
      // Pink gained 2 M€ production
      // Pink gained 1 M€ for playing Sponsors, which has exactly 1 tag.
      playProject(Sponsors, 6)
      // Pink played Robotic Workforce
      playProject(RoboticWorkforce, 9) {
            // Pink gained 1 M€ for playing Robotic Workforce, which has exactly 1 tag.
            // Pink copied Mine production with Robotic Workforce
            doTask("CopyProductionBox<$Mine>")
            // Pink gained 1 steel production
          }
          .expect("PROD[Steel], -8, 0 Mine")
    }

    green.turn {
      // Green used Aquifer Pumping action
      cardAction1(AquiferPumping) {
            pay(steel = 4)
            // Green placed ocean tile at 46
            placeTile(7, 3)
            // Green gained 2 titanium
          }
          .expect("2 Titanium")
    }
    // Green passed
    // Pink passed
    // Green declared this pass early after one action; Solarnet executes both passes in turn order.
    pink.pass()
    green.pass()
    // Pink placed ocean tile at 08
    // Pink acted as World Government and placed an ocean
    pink.wgt("OceanTile<Hellas_2_1>")

    // Generation 2
    // First player this generation is Green
    // Green bought 4 card(s)
    // You bought Giant Ice Asteroid,Investment Loan,Earth Office,Power Supply Consortium
    green.buyCards(GiantIceAsteroid, InvestmentLoan, EarthOffice, PowerSupplyConsortium)

    // Game20260820-dashboards-gen2.png was taken after Green's generation 2 purchase and
    // before Pink's.
    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 3, venus = 0)
    pink.assertResources(m = 28, s = 3, t = 0, p = 0, e = 1, h = 1)
    pink.assertProduction(m = 5, s = 3, t = 0, p = 0, e = 1, h = 1)
    green.assertResources(m = 15, s = 0, t = 2, p = 1, e = 1, h = 0)
    green.assertProduction(m = -1, s = 0, t = 0, p = 1, e = 1, h = 0)
    checkHandSizes()

    // Pink bought 4 card(s)
    // You bought Invention Contest,Decomposers,Colonizer Training Camp,Imported Hydrogen
    pink.buyCards(InventionContest, Decomposers, ColonizerTrainingCamp, ImportedHydrogen)

    green.turn {
      // Green played Earth Office
      playProject(EarthOffice, 1)
      // Green ended turn
    }
    pink.turn {
      // Pink played Designed Microorganisms
      // Pink gained 2 plant production
      playProject(DesignedMicroorganisms, 16)
      // Pink ended turn
    }
    green.turn {
      // Power Supply Consortium requires Nobel Prize's wild icon to count as Green's second power
      // tag.
      assignWildTag(NobelPrize, "PowerTag")
      // Green played Power Supply Consortium
      playProject(PowerSupplyConsortium, 5) {
        // Green stole 1 energy production from Pink
        doTask("PROD[-Energy<Pink>]")
        // Green gained 2 M€ from Suitable Infrastructure
      }
      // Green ended turn
    }
    // Pink passed
    pink.pass()
    green.turn {
      // Green played Investment Loan
      // Green lost 1 M€ production
      // Green gained 10 M€
      playProject(InvestmentLoan, 0).expect("PROD[-1], 10")
      // Green used Aquifer Pumping action
      cardAction1(AquiferPumping) {
            pay(8)
            // Green placed ocean tile at 35
            placeTile(5, 7)
            // Green gained 3 heat
            // Green gained 2 M€ from 1 ocean(s)
          }
          .expect("3 Heat, -6")
    }
    green.turn {
      // Green played Rotator Impacts
      playProject(RotatorImpacts, titanium = 2)
      // Green used Rotator Impacts action
      cardAction1(RotatorImpacts) {
        pay(6)
      }
      // Green added 1 Asteroid to Rotator Impacts
    }
    // Green passed
    green.pass()
    // Green acted as World Government and increased oxygen level
    green.wgt("OxygenStep")

    // Generation 3
    // First player this generation is Pink
    // Green bought 3 card(s)
    // You bought Mars University,Neutralizer Factory,Great Dam
    green.buyCards(MarsUniversity, NeutralizerFactory, GreatDam)
    // Pink bought 2 card(s)
    // You bought Hackers,ArchaeBacteria
    pink.buyCards(Hackers, Archaebacteria)

    // Game20260820-dashboards-gen3.png was taken after generation 3 purchases and before
    // the first action.
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 4, venus = 0)
    pink.assertResources(m = 20, s = 6, t = 0, p = 2, e = 0, h = 3)
    pink.assertProduction(m = 5, s = 3, t = 0, p = 2, e = 0, h = 1)
    green.assertResources(m = 20, s = 0, t = 0, p = 2, e = 2, h = 4)
    green.assertProduction(m = -2, s = 0, t = 0, p = 1, e = 2, h = 0)
    checkHandSizes()

    pink.turn {
      // Pink played Invention Contest
      // Pink drew 1 card(s)
      // You drew Ishtar Mining
      playProject(InventionContest, 2) { draw(IshtarMining) }
      // Pink ended turn
    }
    green.turn {
      // Green played Mars University
      playProject(MarsUniversity, 8, steel = 0) {
        // Green is using their Mars University effect to draw a card by discarding a card.
        // Green discarded Stratopolis
        discard(Stratopolis)
        // Green drew 1 card(s)
        // You drew House Printing
        draw(HousePrinting)
        doTask("-ProjectCard")
      }
      // Green ended turn
    }
    pink.turn {
      // Pink played ArchaeBacteria
      // Pink gained 1 plant production
      // Pink gained 1 M€ for playing ArchaeBacteria, which has exactly 1 tag.
      playProject(Archaebacteria, 6)
      // Pink ended turn
    }
    green.turn {
      // Green used Rotator Impacts action
      cardAction2(RotatorImpacts)
      // Green removed 1 resource(s) from Green's Rotator Impacts
      // Green ended turn
    }
    // Pink passed
    pink.pass()
    green.turn {
      // Green used Aquifer Pumping action
      cardAction1(AquiferPumping) {
        pay(8)
        // Green placed ocean tile at 26
        placeTile(4, 6)
        // Green gained 1 plant
        // Green gained 4 M€ from 2 ocean(s)
      }
    }
    // Green passed
    green.pass()
    // Pink acted as World Government and increased Venus scale
    pink.wgt("VenusStep")

    // Generation 4
    // First player this generation is Green
    // Pink bought 2 card(s)
    // You bought Titanium Mine,Ecological Zone
    pink.buyCards(TitaniumMine, EcologicalZone)
    // Green bought 4 card(s)
    // You bought Media Group,Ironworks,Research,Miranda Resort
    green.buyCards(MediaGroup, Ironworks, Research, MirandaResort)

    green.turn {
      // Green played Media Group
      playProject(MediaGroup, 3)
      // Green ended turn
    }
    pink.turn {
      // Pink played Orbital Reflectors
      // Pink gained 2 heat production
      // Pink drew 1 card(s)
      // You drew Water Splitting Plant
      playProject(OrbitalReflectors, 26) { draw(WaterSplittingPlant) }
      // Pink played Ishtar Mining
      // Pink gained 1 titanium production
      // Pink gained 1 M€ for playing Ishtar Mining, which has exactly 1 tag.
      playProject(IshtarMining, 5)
    }
    green.turn {
      // Green played Miranda Resort
      // Miranda Resort counted Nobel Prize's wild icon as Green's third earth tag.
      assignWildTag(NobelPrize, "EarthTag")
      // Green gained 3 M€ production
      // Green gained 2 M€ from Suitable Infrastructure
      playProject(MirandaResort, 12).expect("PROD[3], -10")
      // Nobel Prize's wild icon counts as Green's eighth distinct tag.
      assignWildTag(NobelPrize, "MicrobeTag")
      // Green claimed Diversifier milestone
      stdAction("ClaimMilestoneSA") { doTask("Diversifier") }
    }
    pink.turn {
      // Pink played Titanium Mine
      // Pink gained 1 titanium production
      // Pink gained 1 M€ for playing Titanium Mine, which has exactly 1 tag.
      playProject(TitaniumMine, 1, steel = 3).expect("PROD[Titanium], 0")
      // Pink ended turn
    }
    // Green passed
    green.pass()
    // Pink passed
    pink.pass()
    // Green acted as World Government and increased oxygen level
    green.wgt("OxygenStep")

    // Generation 5
    // First player this generation is Pink
    // Game20260820-dashboards-gen5.png was taken in generation 5 drafting, before purchases.
    assertSidebar(gen = 5, temp = -30, oxygen = 2, oceans = 5, venus = 8)
    pink.assertResources(m = 31, s = 9, t = 2, p = 8, e = 0, h = 7)
    pink.assertProduction(m = 5, s = 3, t = 2, p = 3, e = 0, h = 3)
    green.assertResources(m = 30, s = 0, t = 0, p = 5, e = 2, h = 8)
    green.assertProduction(m = 1, s = 0, t = 0, p = 1, e = 2, h = 0)
    checkHandSizes()

    // Green bought 3 card(s)
    // You bought Thermophiles,Rad-Chem Factory,GHG Factories
    green.buyCards(Thermophiles, RadChemFactory, GhgFactories)
    // Pink bought 3 card(s)
    // You bought Floating Habs,Nuclear Zone,Space Elevator
    pink.buyCards(FloatingHabs, NuclearZone, SpaceElevator)

    pink.turn {
      // Pink used Convert Plants standard action
      // Pink placed greenery tile at 56
      // Pink gained 2 heat
      convertPlants { placeTile(8, 7) }.expect("2 Heat")
      // Pink ended turn
    }
    green.turn {
      // Green played Great Dam
      // Green gained 2 energy production
      // Green gained 2 M€ from Suitable Infrastructure
      playProject(GreatDam, 12)
      // Green ended turn
    }
    pink.turn {
      // Pink played Space Elevator
      // The later Space Elevator action requires one of Pink's nine steel to remain available.
      intentionalUnderpay()
      // Pink gained 1 titanium production
      playProject(SpaceElevator, 5, steel = 8, titanium = 2)
      // Pink ended turn
    }
    green.turn {
      // Green used Rotator Impacts action
      cardAction1(RotatorImpacts) { pay(6) }
      // Green added 1 Asteroid to Rotator Impacts
      // Green ended turn
    }
    pink.turn {
      // Pink used Space Elevator action
      // Pink gained 5 M€
      cardAction1(SpaceElevator)
      // Pink ended turn
    }
    // Green passed
    green.pass()
    pink.turn {
      // Pink used Convert Heat standard action
      convertHeat()
      // Pink played Nuclear Zone
      // Pink gained 1 heat production
      // Pink placed Nuclear Zone tile at 20
      // Pink gained 1 plant
      // Pink drew 1 card(s)
      // You drew Earth Catapult
      playProject(NuclearZone, 10) {
            placeTile(3, 7)
            draw(EarthCatapult)
          }
          .expect("Plant")
      // Pink gained 1 M€ for playing Nuclear Zone, which has exactly 1 tag.
    }
    // Pink passed
    pink.pass()
    // Pink placed ocean tile at 27
    // Pink acted as World Government and placed an ocean
    pink.wgt("OceanTile<Hellas_4_7>")

    // Generation 6
    // First player this generation is Green
    // Pink bought 2 card(s)
    // You bought Solar Wind Power,Aerobraked Ammonia Asteroid
    pink.buyCards(SolarWindPower, AerobrakedAmmoniaAsteroid)
    // Green bought 2 card(s)
    // You bought Asteroid,Nitrite Reducing Bacteria
    green.buyCards(AsteroidCard, NitriteReducingBacteria)

    green.turn {
      // Green used Convert Heat standard action
      convertHeat()
      // Green played Asteroid
      // Green gained 2 titanium
      // Green gained 1 heat production
      // Green gained 2 M€ from Suitable Infrastructure
      // Pink lost 3 plants because of Green
      // Green gained 3 M€
      playProject(AsteroidCard, 14) { doTask("-3 Plant<Pink>") }
          .expect("2 Titanium, PROD[Heat], -3 Plant<Pink>")
    }
    pink.turn {
      // Pink played Earth Catapult
      // Pink gained 1 M€ for playing Earth Catapult, which has exactly 1 tag.
      playProject(EarthCatapult, 23)
      // Pink ended turn
    }
    green.turn {
      // Green played Ironworks
      playProject(Ironworks, 11)
      // Green ended turn
    }
    pink.turn {
      // Pink played Solar Wind Power
      // Pink gained 1 energy production
      // Pink gained 2 titanium
      playProject(SolarWindPower, titanium = 3)
      // Pink claimed Generalist milestone
      stdAction("ClaimMilestoneSA") { doTask("Generalist") }
    }
    green.turn {
      // Green used Ironworks action
      // Green gained 1 steel
      cardAction1(Ironworks)
      // Green ended turn
    }
    pink.turn {
      // Pink played Hackers
      // Pink gained 2 M€ production
      // Pink lost 1 energy production
      // Pink stole 2 M€ production from Green
      // Pink gained 4 M€ for playing Hackers, which has no tags.
      playProject(Hackers, 1) { doTask("PROD[-2 Megacredit<Green>]") }
          .expect("PROD[2, -Energy, -2 Megacredit<Green>], 3")
      // Pink used Space Elevator action
      // Pink gained 5 M€
      cardAction1(SpaceElevator)
    }
    green.turn {
      // Green used Rotator Impacts action
      // Green removed 1 resource(s) from Green's Rotator Impacts
      cardAction2(RotatorImpacts)
      // Green ended turn
    }
    pink.turn {
      // Pink played Decomposers
      // Pink added 1 Microbe to Decomposers
      // Pink gained 1 M€ for playing Decomposers, which has exactly 1 tag.
      playProject(Decomposers, 3).expect("Microbe<$Decomposers>, -2")
      // Pink played Ecological Zone
      // Pink added 2 Microbe(s) to Decomposers
      // Pink added 2 Animal(s) to Ecological Zone
      // Pink placed Ecological Zone tile at 49
      // Pink drew 1 card(s)
      // You drew Zeppelins
      playProject(EcologicalZone, 10) {
            placeTile(7, 6)
            draw(Zeppelins)
          }
          .expect("2 Microbe<$Decomposers>, 2 Animal<$EcologicalZone>")
    }
    green.turn {
      // Green funded Contractor award
      stdAction("FundAwardSA") { doTask("Contractor") }
      // Green ended turn
    }
    pink.turn {
      // Pink played Floating Habs
      // Pink gained 1 M€ for playing Floating Habs, which has exactly 1 tag.
      playProject(FloatingHabs, 3)
      // Pink played Colonizer Training Camp
      playProject(ColonizerTrainingCamp, 2, steel = 2)
    }
    // Green passed
    green.pass()
    pink.turn {
      // Pink used Sell Patents standard project
      // The log names only the count; selling Water Splitting Plant is test inference because it
      // is the only card in Pink's tracked hand that never appears in the final tableau.
      sellPatents(WaterSplittingPlant)
      // Pink sold 1 patents
      // Pink used Floating Habs action
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      // Pink added 1 Floater to Floating Habs
    }
    // Pink passed
    pink.pass()
    // Green acted as World Government and increased temperature
    green.wgt("TemperatureStep")

    // Generation 7
    // First player this generation is Pink
    // Pink bought 3 card(s)
    // You bought Lagrange Observatory,Advanced Ecosystems,Deimos Down
    pink.buyCards(LagrangeObservatory, AdvancedEcosystems, DeimosDown)
    // Green bought 2 card(s)
    // You bought Terraforming Contract,Sponsored Academies
    green.buyCards(TerraformingContract, SponsoredAcademies)

    // Game20260820-dashboards-gen7.png was taken after generation 7 purchases and before actions.
    assertSidebar(gen = 7, temp = -18, oxygen = 4, oceans = 6, venus = 10)
    pink.assertResources(m = 25, s = 3, t = 5, p = 4, e = 0, h = 9)
    pink.assertProduction(m = 7, s = 3, t = 3, p = 3, e = 0, h = 4)
    green.assertResources(m = 25, s = 1, t = 2, p = 7, e = 4, h = 3)
    green.assertProduction(m = -1, s = 0, t = 0, p = 1, e = 4, h = 1)
    checkHandSizes()

    pink.turn {
      // Pink played Deimos Down
      // Pink gained 4 steel
      // Green lost 7 plants because of Pink
      playProject(DeimosDown, 14, titanium = 5) { doTask("-7 Plant<Green>") }
          .expect("4 Steel, -7 Plant<Green>")
      // Pink claimed Terraformer29 milestone
      stdAction("ClaimMilestoneSA") { doTask("Terraformer29") }
    }
    green.turn {
      // Green played Terraforming Contract
      // Green gained 4 M€ production
      // Green gained 2 M€ from Suitable Infrastructure
      playProject(TerraformingContract, 5)
      // Green ended turn
    }
    pink.turn {
      // Pink used Space Elevator action
      // Pink gained 5 M€
      cardAction1(SpaceElevator)
      // Pink ended turn
    }
    green.turn {
      // Green used Rotator Impacts action
      cardAction1(RotatorImpacts) { pay(titanium = 2) }
      // Green added 1 Asteroid to Rotator Impacts
      // Green ended turn
    }
    // Green ended turn
    pink.turn {
      // Pink played Lagrange Observatory
      // Pink drew 1 card(s)
      // You drew Gyropolis
      playProject(LagrangeObservatory, 7) { draw(Gyropolis) }
      // Pink ended turn
    }
    green.turn {
      // Green used Ironworks action
      // Green gained 1 steel
      cardAction1(Ironworks)
      // Green ended turn
    }
    pink.turn {
      // Pink used Convert Heat standard action
      convertHeat()
      // Pink ended turn
    }
    green.turn {
      // Green used Aquifer Pumping action
      cardAction1(AquiferPumping) {
        pay(4, steel = 2)
        // Green placed ocean tile at 36
        placeTile(5, 8)
        // Green gained 4 M€ from 2 ocean(s)
      }
      // Green ended turn
    }
    // Pink passed
    pink.pass()
    green.turn {
      // Green played Tectonic Stress Power
      // Nobel Prize's wild icon counts as Green's second science tag.
      assignWildTag(NobelPrize, "ScienceTag")
      // Green gained 3 energy production
      // Green gained 2 M€ from Suitable Infrastructure
      playProject(TectonicStressPower, 18)
      // Green passed
      pass()
    }
    // Pink acted as World Government and increased temperature
    pink.wgt("TemperatureStep")

    // Generation 8
    // First player this generation is Green
    // Game20260820-dashboards-gen8.png was taken during generation 8 drafting, before purchases.
    assertSidebar(gen = 8, temp = -8, oxygen = 5, oceans = 7, venus = 10)
    pink.assertResources(m = 39, s = 9, t = 3, p = 7, e = 0, h = 5)
    pink.assertProduction(m = 7, s = 3, t = 3, p = 3, e = 0, h = 4)
    green.assertResources(m = 39, s = 0, t = 0, p = 1, e = 7, h = 4)
    green.assertProduction(m = 3, s = 0, t = 0, p = 1, e = 7, h = 1)
    checkHandSizes()

    // Green bought 2 card(s)
    // You bought Ishtar Expedition,Lava Tube Settlement
    green.buyCards(IshtarExpedition, LavaTubeSettlement)
    // Pink bought 3 card(s)
    // You bought Rad-Suits,Geothermal Power,Gene Repair
    pink.buyCards(RadSuits, GeothermalPower, GeneRepair)

    green.turn {
      // Green played Ishtar Expedition
      // Green gained 3 titanium
      /* Discarded 5 cards Magnetic Field Generators,Mangrove,Domed Crater,Mohole Area,Medical Lab */
      // Green drew Stratospheric Birds,Floating Refinery
      // Green gained 3 M€
      playProject(IshtarExpedition, 6) { draw(StratosphericBirds, FloatingRefinery) }
          .expect("3 Titanium, -3")
      // Green played Giant Ice Asteroid
      playProject(GiantIceAsteroid, 27, titanium = 3) {
            // Green placed ocean tile at 43
            doTask("OceanTile<Hellas_6_7>")
            // Green gained 4 M€ from 2 ocean(s)
            // Green placed ocean tile at 44
            doTask("OceanTile<Hellas_6_8>")
            // Green gained 1 steel
            // Green gained 6 M€ from 3 ocean(s)
            // Pink lost 6 plants because of Green
            doTask("-6 Plant<Pink>")
            // Green gained 3 M€
          }
          .expect("Steel, -6 Plant<Pink>, -14")
    }
    pink.turn {
      // Pink played Geothermal Power
      // Pink gained 2 energy production
      playProject(GeothermalPower, 1, steel = 4)
      // Pink played Immigrant City
      // The later Space Elevator action requires one of Pink's five steel to remain available.
      intentionalUnderpay()
      playProject(ImmigrantCity, 3, steel = 4) {
            // Pink placed city tile at 12
            placeTile(2, 5)
            // Pink gained 1 plant
            // Pink gained 3 M€
            // Pink gained 1 M€ production
            // Pink gained 1 M€ production
          }
          .expect("PROD[0], 0")
    }
    green.turn {
      // Green used Ironworks action
      // Green gained 1 steel
      cardAction1(Ironworks)
      // Green ended turn
    }
    pink.turn {
      // Pink used Space Elevator action
      // Pink gained 5 M€
      cardAction1(SpaceElevator)
      // Pink played Rad-Suits
      // Pink gained 1 M€ production
      // Pink gained 4 M€ for playing Rad-Suits, which has no tags.
      playProject(RadSuits, 4)
    }
    green.turn {
      // Green played Sponsored Academies
      // The card's unnamed mandatory discard is inferred to be GHG Factories, which was never
      // played and does not appear in the final hand.
      discard(GhgFactories)
      // Green drew 3 card(s)
      // You drew Commercial District,Cloud Seeding,Solarnet
      draw(CommercialDistrict, CloudSeeding, Solarnet)
      // Pink drew 1 card(s)
      // You drew GHG Producing Bacteria
      pink.draw(GhgProducingBacteria)
      playProject(SponsoredAcademies, 6) {
        // Green is using their Mars University effect to draw a card by discarding a card.
        // Green discarded Floating Refinery
        discard(FloatingRefinery)
        // Green drew 1 card(s)
        // You drew Space Mirrors
        draw(SpaceMirrors)
        doTask("-ProjectCard")
      }
      // Green ended turn
    }
    pink.turn {
      // Pink played Gene Repair
      // Pink gained 2 M€ production
      // Pink gained 1 M€ for playing Gene Repair, which has exactly 1 tag.
      playProject(GeneRepair, 10)
      // Pink ended turn
    }
    green.turn {
      // Green used Rotator Impacts action
      // Green removed 1 resource(s) from Green's Rotator Impacts
      cardAction2(RotatorImpacts)
      // Green ended turn
    }
    pink.turn {
      // Pink played GHG Producing Bacteria
      // Pink added 1 Microbe to Decomposers
      playProject(GhgProducingBacteria, 6).expect("Microbe<$Decomposers>")
      // Pink ended turn
    }
    green.turn {
      // Green played Rad-Chem Factory
      // Green lost 1 energy production
      playProject(RadChemFactory, 4, steel = 2)
      // Green ended turn
    }
    pink.turn {
      // Pink used Floating Habs action
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      // Pink added 1 Floater to Floating Habs
      // Pink ended turn
    }
    // Green passed
    green.pass()
    pink.turn {
      // Pink played Aerobraked Ammonia Asteroid
      // Pink gained 1 plant production
      // Pink gained 3 heat production
      // Pink added 2 Microbe(s) to GHG Producing Bacteria
      playProject(AerobrakedAmmoniaAsteroid, 15, titanium = 3) {
            addCardResources(GhgProducingBacteria)
          }
          .expect("PROD[Plant, 3 Heat], 2 Microbe<$GhgProducingBacteria>")
      // Pink used GHG Producing Bacteria action
      // Pink removed 2 resource(s) from Pink's GHG Producing Bacteria
      cardAction2(GhgProducingBacteria)
      // Pink passed
      pass()
    }
    // Green acted as World Government and increased temperature
    green.wgt("TemperatureStep")

    // Generation 9
    // First player this generation is Pink
    // Green bought 2 card(s)
    // You bought Land Claim,Bribed Committee
    green.buyCards(LandClaim, BribedCommittee)
    // Pink bought 3 card(s)
    // You bought Herbivores,Dawn City,Beam From A Thorium Asteroid
    pink.buyCards(Herbivores, DawnCity, BeamFromAThoriumAsteroid)

    pink.turn {
      // Pink used Greenery standard project
      // Pink placed greenery tile at 19
      // Pink gained 2 plants
      // Pink gained 4 M€ from 2 ocean(s)
      stdProject("GreenerySP", { pay(23) }) { placeTile(3, 6) }
      // Pink used Convert Plants standard action
      // Pink placed greenery tile at 55
      // Pink gained 2 heat
      convertPlants { placeTile(8, 6) }.expect("2 Heat")
    }
    green.turn {
      // Green used Convert Heat standard action
      convertHeat()
      // Green ended turn
    }
    pink.turn {
      // Pink used Convert Heat standard action
      convertHeat()
      // Pink used Space Elevator action
      // Pink gained 5 M€
      cardAction1(SpaceElevator)
    }

    // Game20260820-dashboards-gen9-early.png was taken after Space Elevator and before Ironworks.
    assertSidebar(gen = 9, temp = 6, oxygen = 8, oceans = 9, venus = 12)
    pink.assertResources(m = 21, s = 2, t = 3, p = 0, e = 1, h = 6)
    pink.assertProduction(m = 10, s = 3, t = 3, p = 4, e = 1, h = 7)
    green.assertResources(m = 41, s = 0, t = 0, p = 2, e = 6, h = 0)
    green.assertProduction(m = 3, s = 0, t = 0, p = 1, e = 6, h = 1)
    checkHandSizes()

    green.turn {
      // Green used Ironworks action
      // Green gained 1 steel
      cardAction1(Ironworks)
      // Green ended turn
    }
    pink.turn {
      // Pink played Industrial Center
      // Pink placed Industrial Center tile at 62
      // Pink gained 2 heat
      // Pink gained 1 M€ for playing Industrial Center, which has exactly 1 tag.
      playProject(IndustrialCenter, steel = 1) {
            placeTile(9, 8)
          }
          .expect("2 Heat")
      // Pink used Convert Heat standard action
      convertHeat()
    }
    green.turn {
      // Green used Air Scrapping standard project
      stdProject("AirScrappingSP")
      // Green played Neutralizer Factory
      playProject(NeutralizerFactory, 7)
    }

    pink.turn {
      // Pink played Dawn City
      // Pink gained 1 titanium production
      // Pink lost 1 energy production
      // Pink gained 3 M€
      // Pink gained 1 M€ production
      playProject(DawnCity, 4, titanium = 3).expect("PROD[Titanium, -Energy, 1], -1")
      // Pink ended turn
    }
    green.turn {
      // Green used Rotator Impacts action
      cardAction1(RotatorImpacts) { pay(6) }
      // Green added 1 Asteroid to Rotator Impacts
      // Green ended turn
    }
    pink.turn {
      // Pink used GHG Producing Bacteria action
      cardAction1(GhgProducingBacteria)
      // Pink added 1 Microbe to GHG Producing Bacteria
      // Pink ended turn
    }
    green.turn {
      // Green played Bribed Committee
      // Green gained 3 M€
      playProject(BribedCommittee, 4).expect("2 TerraformRating, -1")
      // Green ended turn
    }
    pink.turn {
      // Pink used Floating Habs action
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      // Pink added 1 Floater to Floating Habs
      // Pink ended turn
    }
    green.turn {
      // Green played Cloud Seeding
      // Green lost 1 M€ production
      // Green gained 2 plant production
      // Green gained 2 M€ from Suitable Infrastructure
      // Pink lost 1 heat production because of Green
      playProject(CloudSeeding, 11) { doTask("PROD[-Heat<Pink>]") }
          .expect("PROD[-1, 2 Plant, -Heat<Pink>], -9")
      // Green ended turn
    }
    pink.turn {
      // Pink played Zeppelins
      // Pink gained 2 M€ production
      // Pink gained 4 M€ for playing Zeppelins, which has no tags.
      playProject(Zeppelins, 11)
      // Pink ended turn
    }
    green.turn {
      // Green played Land Claim
      // Green placed land claim at 11
      // Green gained 3 M€
      playProject(LandClaim, 1) { doTask("LandClaimMarker<Hellas_2_4>") }
      // Green ended turn
    }
    // Pink passed
    pink.pass()
    green.turn {
      // Green used Sell Patents standard project
      // These are the two tracked cards that never appear in the final tableau or a named later
      // discard; assigning them to this unnamed sale is test inference.
      sellPatents(Thermophiles, NitriteReducingBacteria)
      // Green sold 2 patents
      // Green played Solarnet
      // Green drew 2 card(s)
      // You drew Water to Venus,Noctis City
      playProject(Solarnet, 7) { draw(WaterToVenus, NoctisCity) }.expect("ProjectCard")
      // Green passed
      pass()
    }

    // Game20260820-dashboards-gen9-late.png was taken after production and before World Government.
    assertSidebar(gen = 9, temp = 8, oxygen = 9, oceans = 9, venus = 16)
    pink.assertResources(m = 62, s = 4, t = 4, p = 4, e = 0, h = 7)
    pink.assertProduction(m = 13, s = 3, t = 4, p = 4, e = 0, h = 6)
    green.assertResources(m = 47, s = 1, t = 0, p = 5, e = 6, h = 3)
    green.assertProduction(m = 2, s = 0, t = 0, p = 3, e = 6, h = 1)
    checkHandSizes()

    // Pink acted as World Government and increased oxygen level
    pink.wgt("OxygenStep")

    // Generation 10
    // First player this generation is Green
    // Green bought 1 card(s)
    // You bought Business Network
    green.buyCards(BusinessNetwork)
    // Pink bought 2 card(s)
    // You bought CEO's Favorite Project,Fusion Power
    pink.buyCards(CeosFavoriteProject, FusionPower)

    green.turn {
      // Green used Ironworks action
      // Green gained 1 steel
      cardAction1(Ironworks)
      // Green played Lava Tube Settlement
      // Green gained 2 M€ production
      // Green gained 2 M€ from Suitable Infrastructure
      // Green lost 1 energy production
      // Green placed city tile at 54
      // Green drew 1 card(s)
      // You drew Venusian Insects
      // Pink gained 1 M€ production
      // Pink gained 1 M€ production
      playProject(LavaTubeSettlement, 11, steel = 2) {
            placeTile(8, 5)
            draw(VenusianInsects)
          }
          .expect("PROD[2 Megacredit<Pink>]")
    }
    pink.turn {
      // Pink played Fusion Power
      // Pink gained 3 energy production
      playProject(FusionPower, 4, steel = 4)
      // Pink ended turn
    }
    green.turn {
      // Green used Rotator Impacts action
      // Green removed 1 resource(s) from Green's Rotator Impacts
      cardAction2(RotatorImpacts)
      // Green played Business Network
      // Green lost 1 M€ production
      playProject(BusinessNetwork, 1)
    }
    pink.turn {
      // Pink played Herbivores
      // Pink added 1 Microbe to Decomposers
      // Pink added 1 Animal to Ecological Zone
      // Pink added 1 Animal to Herbivores
      // Green lost 1 plant production because of Pink
      // Pink gained 1 M€ for playing Herbivores, which has exactly 1 tag.
      playProject(Herbivores, 10) { doTask("PROD[-Plant<Green>]") }.expect("Microbe, 2 Animal")
      // Pink played Gyropolis
      // Pink gained 6 M€ production
      // Pink lost 2 energy production
      // Pink placed city tile at 05
      // Pink gained 2 plants
      // Pink gained 3 M€
      // Pink gained 1 M€ production
      // Pink gained 1 M€ production
      playProject(Gyropolis, 18) { placeTile(1, 3) }.expect("PROD[8], 2 Plant")
    }
    green.turn {
      // Green used Business Network action
      // Green bought 0 card(s)
      cardAction1(BusinessNetwork) { /* Decline buying the revealed card. */
            declineTask()
          }
          .expect("0 ProjectCard")
      // Green played Research
      playProject(Research, 11) {
        // Green drew 2 card(s)
        // You drew Energy Tapping,Ice Cap Melting
        draw(EnergyTapping, IceCapMelting)
        // Green is using their Mars University effect to draw a card by discarding a card.
        // Green discarded Ice Cap Melting
        discard(IceCapMelting)
        // Green drew 1 card(s)
        // You drew Venus Shuttles
        draw(VenusShuttles)
        // Green is using their Mars University effect to draw a card by discarding a card.
        // Green discarded Venusian Insects
        discard(VenusianInsects)
        // Green drew 1 card(s)
        // You drew Venus Soils
        draw(VenusSoils)
        doTask("-ProjectCard")
        doTask("-ProjectCard")
      }
    }
    pink.turn {
      // Pink played Imported Hydrogen
      // Pink gained 3 plants
      playProject(ImportedHydrogen, 2, titanium = 4) { doTask("3 Plant") }
      // Pink used Convert Plants standard action
      // Pink placed greenery tile at 06
      // Pink gained 1 plant
      // Pink gained 1 steel
      // Pink added 1 Animal to Herbivores
      convertPlants { placeTile(1, 4) }.expect("Steel, Animal<$Herbivores>")
    }

    green.turn {
      // Green played House Printing
      // Green gained 1 steel production
      // Green gained 2 M€ from Suitable Infrastructure
      playProject(HousePrinting, 10)
      // Green played Insects
      // Nobel Prize's wild icon counts as Green's second plant tag.
      assignWildTag(NobelPrize, "PlantTag")
      // Green gained 2 plant production
      // Green gained 2 M€ from Suitable Infrastructure
      playProject(Insects, 9)
    }
    pink.turn {
      // Pink used Space Elevator action
      // Pink gained 5 M€
      cardAction1(SpaceElevator)
      // Pink ended turn
    }
    green.turn {
      // Green used Sell Patents standard project
      // Selling Space Mirrors is test inference because it is a tracked card that never appears in
      // the final tableau or a later named discard.
      sellPatents(SpaceMirrors)
      // Green sold 1 patents
      // Green played Water to Venus
      // Green gained 3 M€
      playProject(WaterToVenus, 9).expect("VenusStep, -6")
    }
    pink.turn {
      // Pink funded Landscaper award
      stdAction("FundAwardSA", which = 2) { doTask("Landscaper") }
      // Pink ended turn
    }
    // Green passed
    green.pass()
    pink.turn {
      // Pink used Floating Habs action
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      // Pink added 1 Floater to Floating Habs
      // Pink used GHG Producing Bacteria action
      cardAction1(GhgProducingBacteria)
      // Pink added 1 Microbe to GHG Producing Bacteria
      // Pink passed
      pass()
    }

    // Game20260820-dashboards-gen10.png was taken after production and before World Government.
    assertSidebar(gen = 10, temp = 8, oxygen = 12, oceans = 9, venus = 20)
    pink.assertResources(m = 76, s = 3, t = 4, p = 6, e = 1, h = 13)
    pink.assertProduction(m = 23, s = 3, t = 4, p = 4, e = 1, h = 6)
    green.assertResources(m = 54, s = 1, t = 0, p = 9, e = 5, h = 6)
    green.assertProduction(m = 3, s = 1, t = 0, p = 4, e = 5, h = 1)
    checkHandSizes()

    // Green acted as World Government and increased Venus scale
    green.wgt("VenusStep")

    // Generation 11
    // First player this generation is Pink
    // Pink bought 2 card(s)
    // You bought Technology Demonstration,Birds
    pink.buyCards(TechnologyDemonstration, Birds)
    // Green bought 2 card(s)
    // You bought Giant Solar Shade,Algae
    green.buyCards(GiantSolarShade, Algae)

    pink.turn {
      // Pink used Greenery standard project
      // Pink placed greenery tile at 04
      // Pink gained 2 plants
      // Pink added 1 Animal to Herbivores
      stdProject("GreenerySP") { placeTile(1, 2) }
      // Pink used Convert Plants standard action
      // Pink placed greenery tile at 13
      // Pink gained 1 plant
      // Pink added 1 Animal to Herbivores
      convertPlants { placeTile(2, 6) }
    }
    green.turn {
      // Green used Convert Plants standard action
      // Green placed greenery tile at 53
      // Green gained 1 steel
      // Green gained 2 M€ from 1 ocean(s)
      convertPlants { placeTile(8, 4) }
      // Green used Business Network action
      // Green bought 0 card(s)
      cardAction1(BusinessNetwork) { /* Decline buying the revealed card. */
            declineTask()
          }
          .expect("0 ProjectCard")
    }
    pink.turn {
      // Pink played Technology Demonstration
      // Pink drew 2 card(s)
      // You drew Toll Station,Fueled Generators
      playProject(TechnologyDemonstration, titanium = 1) {
        draw(TollStation, FueledGenerators)
      }
      // Pink funded Founder award
      stdAction("FundAwardSA", which = 3) { doTask("Founder") }.expect("Award")
    }
    green.turn {
      // Green used Ironworks action
      // Green gained 1 steel
      cardAction1(Ironworks).expect("Steel")
      // Green played Commercial District
      // Green gained 4 M€ production
      // Green gained 2 M€ from Suitable Infrastructure
      // Green lost 1 energy production
      // Green placed Commercial District tile at 11
      // Green gained 1 plant
      // Green gained 1 steel
      playProject(CommercialDistrict, 10, steel = 3) {
        placeTile(2, 4)
      }
    }
    pink.turn {
      // Pink used Space Elevator action
      // Pink gained 5 M€
      cardAction1(SpaceElevator).expect("5, -Steel")
      // Pink ended turn
    }
    green.turn {
      // Green used Sell Patents standard project
      // The five named Green cards sold this generation, and their division among the four unnamed
      // sales, are test inference from the exact tracked hand and final empty hand.
      sellPatents(StratosphericBirds)
      // Green sold 1 patents
      // Green ended turn
    }
    pink.turn {
      // Pink used GHG Producing Bacteria action
      cardAction1(GhgProducingBacteria).expect("Microbe<$GhgProducingBacteria>")
      // Pink added 1 Microbe to GHG Producing Bacteria
      // Pink ended turn
    }
    green.turn {
      // Green used Sell Patents standard project
      sellPatents(EnergyTapping)
      // Green sold 1 patents
      // Green ended turn
    }
    pink.turn {
      // Pink played Advanced Ecosystems
      // Pink added 3 Microbe(s) to Decomposers
      // Pink added 2 Animal(s) to Ecological Zone
      playProject(AdvancedEcosystems, 9)
      // Pink ended turn
    }
    green.turn {
      // Green played Giant Solar Shade
      playProject(GiantSolarShade, 27)
      // Green ended turn
    }
    pink.turn {
      // Pink used Air Scrapping standard project
      stdProject("AirScrappingSP")
      // Pink ended turn
    }
    green.turn {
      // Green used Sell Patents standard project
      sellPatents(VenusShuttles)
      // Green sold 1 patents
      // Green ended turn
    }
    pink.turn {
      // Pink used Sell Patents standard project
      // The three named Pink cards sold this generation, and their order, are test inference from
      // the exact tracked hand and final empty hand.
      sellPatents(BeamFromAThoriumAsteroid)
      // Pink sold 1 patents
      // Pink ended turn
    }
    green.turn {
      // Green played Noctis City
      // Green gained 3 M€ production
      // Green gained 2 M€ from Suitable Infrastructure
      // Green placed city tile at 09
      // Green gained 2 plants
      // Green gained 2 M€ from 1 ocean(s)
      // Pink gained 1 M€ production
      // Pink gained 1 M€ production
      playProject(NoctisCity, 16, steel = 1) { placeTile(2, 2) }
      // Green ended turn
    }
    pink.turn {
      // Pink used Sell Patents standard project
      sellPatents(TollStation)
      // Pink sold 1 patents
      // Pink ended turn
    }
    green.turn {
      // Green used Sell Patents standard project
      sellPatents(VenusSoils, Algae)
      // Green sold 2 patents
      // Green passed
      // Green declared this pass early after one action; Solarnet executes it on Green's next turn.
    }
    pink.turn {
      // Pink played Birds
      // Pink added 1 Microbe to Decomposers
      // Pink added 1 Animal to Ecological Zone
      // Green lost 2 plant production because of Pink
      // Pink gained 1 M€ for playing Birds, which has exactly 1 tag.
      playProject(Birds, 8) { doTask("PROD[-2 Plant<Green>]") }
      // Pink used Birds action
      cardAction1(Birds).expect("Animal<$Birds>")
      // Pink added 1 Animal to Birds
    }
    green.pass()
    pink.turn {
      // Pink played CEO's Favorite Project
      playProject(CeosFavoriteProject, 0) { addCardResources(Birds) }
      // Pink added 1 Animal to Birds
      // Pink gained 1 M€ for playing CEO's Favorite Project, which has exactly 1 tag.
      // Pink used Sell Patents standard project
      sellPatents(FueledGenerators)
      // Pink sold 1 patents
      // Pink passed
      pass()
    }

    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    // Final greenery placement
    // Pink declines the final greenery placement.
    pink.declineTask()
    // Green declines the final greenery placement.
    green.declineTask()

    // This game id was ga5237bd2fb08
    assertCardTrackingComplete()
    pink.cardsInHand shouldBe emptySet()
    green.cardsInHand shouldBe emptySet()
    engine.assertCounts(1 to "EndPhase")

    pink.assertCounts(
        7 to "AwardTally<Pink, Landscaper>",
        8 to "AwardTally<Pink, Founder>",
        10 to "AwardTally<Pink, Contractor>",
        41 to "TerraformRating",
        100 to "VictoryPoint",
        1 to "Victory",
    )
    green.assertCounts(
        2 to "AwardTally<Green, Landscaper>",
        0 to "AwardTally<Green, Founder>",
        12 to "AwardTally<Green, Contractor>",
        51 to "TerraformRating",
        75 to "VictoryPoint",
        0 to "Victory",
    )

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Pink>") shouldBe 10
    score.net("Milestone", "VictoryPoint<Green>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Pink>") shouldBe 10
    score.net("SecondPlace", "VictoryPoint<Pink>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Green>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<Green>") shouldBe 0
    score.net("GreeneryTile", "VictoryPoint<Pink>") shouldBe 6
    score.net("GreeneryTile", "VictoryPoint<Green>") shouldBe 1
    score.net("CityTile", "VictoryPoint<Pink>") shouldBe 7
    score.net("CityTile", "VictoryPoint<Green>") shouldBe 3
    score.net("Card", "VictoryPoint<Pink>") shouldBe 26
    score.net("Card", "VictoryPoint<Green>") shouldBe 10

    pink.assertCardResources(
        9 to Decomposers,
        6 to EcologicalZone,
        4 to FloatingHabs,
        3 to GhgProducingBacteria,
        4 to Herbivores,
        2 to Birds,
    )
    pink.assertResources(m = 71, s = 5, t = 7, p = 5, e = 1, h = 20)
    pink.assertProduction(m = 25, s = 3, t = 4, p = 4, e = 1, h = 6)
    green.assertResources(m = 69, s = 1, t = 0, p = 6, e = 3, h = 8)
    green.assertProduction(m = 10, s = 1, t = 0, p = 2, e = 3, h = 1)
  }
}
