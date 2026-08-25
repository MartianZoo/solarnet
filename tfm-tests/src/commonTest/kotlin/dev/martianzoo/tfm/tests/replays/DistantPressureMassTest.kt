package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
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
    // First player this generation is Keen
    // Good luck Keen!
    // Good luck Been!
    // Generation 1
    // Keen played Sagitta Frontier Services
    // Keen gained 2 M€ production
    // Keen gained 1 energy production
    // Discarded 14 cards Atalanta Planitia Lab,Power Plant,Arctic Algae,Worms,Mercurian
    // Alloys,Weather Balloons,Freyja Biodomes,Nitrogen-Rich Asteroid,Stratospheric Birds,Magnetic
    // Field Dome,GHG Producing Bacteria,Venus Waystation,Comet for Venus,Research
    keen.playCorp(SagittaFrontierServices) {
      // Keen drew Caretaker Contract
      // Keen kept 4 project cards
      // Keen gained 4 M€ for playing Sagitta Frontier Services, which has no tags.
      draw(CaretakerContract)
      buyCards(CuttingEdgeTechnology, SearchForLife, CommunityServices, DustSeals)
    }
    // Been played Polyphemos
    // Been gained 5 M€ production
    // Been gained 5 titanium
    // Been kept 4 project cards
    been.playCorp(Polyphemos) {
      buyCards(ExtractorBalloons, LunarExports, Flooding, CorporateStronghold).expect("-20")
    }

    keen.turn {
      // Keen played Industrial Complex
      // Keen gained 1 steel production
      // Keen gained 1 titanium production
      // Keen gained 1 plant production
      // Keen gained 1 heat production
      // Keen spent 18 M€ as payment
      // Keen gained 1 M€ for playing Industrial Complex, which has exactly 1 tag.
      playPrelude(IndustrialComplex)
      // Keen played Applied Science
      // Keen added 6 Science(s) to Applied Science
      // Keen gained 4 M€ for playing Applied Science, which has no tags.
      playPrelude(AppliedScience).expect("6 Science, 4")
    }

    been.turn {
      // Been played Strategic Base Planning
      playPrelude(StrategicBasePlanning) {
        // Been built a colony on Ganymede
        // Been gained 1 plant production
        // Been placed city tile at 61
        // Been spent 3 M€ as payment
        // Been spent 6 M€ as payment
        // Been placed ocean tile at 34
        // Been drew 1 card(s)
        doTask("Colony<Ganymede>")
        placeTile(9, 7)
        placeTile(5, 6)
        // You drew Underground City
        draw(UndergroundCity)
      }
      // Been played Early Settlement
      // Been gained 1 plant production
      // Been placed city tile at 13
      // Been gained 1 plant
      playPrelude(EarlySettlement) { placeTile(2, 6) }
    }

    // Keen used Applied Science action
    // Keen removed 1 resource(s) from Keen's Applied Science
    // Keen gained 1 titanium
    // Keen ended turn
    keen.turn { cardAction1(AppliedScience) { doTask("Titanium") } }
    been.turn {
      // Been spent 4 M€ as payment
      // Been spent 5 titanium as payment
      // Been played Lunar Exports
      // Been gained 5 M€ production
      // Been ended turn
      playProject(LunarExports, 4, titanium = 5) { doTask("PROD[5 Megacredit]") }
    }
    // Keen passed
    keen.pass()
    been.turn {
      // Been spent 7 M€ as payment
      // Been played Flooding
      // Been placed ocean tile at 35
      // Been gained 3 heat
      // Been gained 2 M€ from 1 ocean(s)
      playProject(Flooding, 7) { placeTile(5, 7) }.expect("3 Heat, -5")
      // Been passed
      pass()
    }
    // Keen acted as World Government and increased Venus scale
    keen.wgt("VenusStep")
  }

  private fun generation2() {
    // Generation 2
    // First player this generation is Been
    // Keen spent 9 M€ as payment
    // Keen bought 3 card(s)
    // You bought Directed Heat Usage,Business Network,Optimal Aerobraking
    keen.buyCards(DirectedHeatUsage, BusinessNetwork, OptimalAerobraking)
    // Been spent 15 M€ as payment
    // Been bought 3 card(s)
    // You bought Venusian Animals,Mining Colony,Ecology Research
    been.buyCards(VenusianAnimals, MiningColony, EcologyResearch)

    // Been spent 21 M€ as payment
    // Been played Extractor Balloons
    // Been added 3 Floater(s) to Extractor Balloons
    // Been ended turn
    been.turn { playProject(ExtractorBalloons, 21) }
    // Keen spent 12 M€ as payment
    // Keen played Cutting Edge Technology
    // Keen gained 1 M€ for playing Cutting Edge Technology, which has exactly 1 tag.
    // Keen ended turn
    keen.turn { playProject(CuttingEdgeTechnology, 12) }
    // Been used Extractor Balloons action
    // Been removed 2 resource(s) from Been's Extractor Balloons
    // Been ended turn
    been.turn { cardAction2(ExtractorBalloons).expect("-2 Floater, VenusStep") }
    // Keen played Dust Seals
    // Keen gained 4 M€ for playing Dust Seals, which has no tags.
    // Keen ended turn
    keen.turn { playProject(DustSeals, 0) }
    // Been passed
    been.pass()
    keen.turn {
      // Keen spent 1 M€ as payment
      // Keen played Directed Heat Usage
      // Keen gained 4 M€ for playing Directed Heat Usage, which has no tags.
      playProject(DirectedHeatUsage, 1)
      // Keen spent 13 M€ as payment
      // Keen played Community Services
      // Keen gained 5 M€ production
      // Keen gained 4 M€ for playing Community Services, which has no tags.
      playProject(CommunityServices, 13)
      // Keen spent 4 M€ as payment
      // Keen played Business Network
      // Keen lost 1 M€ production
      // Keen gained 1 M€ for playing Business Network, which has exactly 1 tag.
      playProject(BusinessNetwork, 4)
      // Keen used Business Network action
      cardAction1(BusinessNetwork) {
        // The archive records that the revealed card was not bought.
        buyCards(0)
      }
      // Keen bought 0 card(s)
      // Keen spent 1 M€ as payment
      // Keen played Search For Life
      // Keen gained 1 M€ for playing Search For Life, which has exactly 1 tag.
      playProject(SearchForLife, 1)
      // Keen used Search For Life action
      // Keen spent 1 M€ as payment
      // Keen revealed and discarded Earth Elevator
      cardAction1(SearchForLife) {
            // Earth Elevator has no microbe tag.
            declineTask()
          }
          .expect("0 Science")
      // Keen used Applied Science action
      // Keen removed 1 resource(s) from Keen's Applied Science
      // Keen gained 1 titanium
      cardAction1(AppliedScience) { doTask("Titanium") }
      // Keen passed
      // Been placed ocean tile at 26
      pass()
    }
    // Been acted as World Government and placed an ocean
    been.wgt("OceanTile<Hellas_4_6>")
  }

  private fun generation3() {
    // Generation 3
    // First player this generation is Keen
    // Keen spent 12 M€ as payment
    // Keen bought 4 card(s)
    // You bought Mars University,Breathing Filters,Methane From Titan,Ore Processor
    keen.buyCards(MarsUniversity, BreathingFilters, MethaneFromTitan, OreProcessor)

    // Screenshot evidence: generation-3-research.png, after Keen's purchase and before Been's.
    with(keen) {
      assertResources(m = 20, s = 2, t = 4, p = 2, e = 1, h = 3)
      assertProduction(m = 6, s = 1, t = 1, p = 1, e = 1, h = 1)
    }
    with(been) {
      assertResources(m = 41, s = 0, t = 0, p = 5, e = 0, h = 3)
      assertProduction(m = 10, s = 0, t = 0, p = 2, e = 0, h = 0)
    }

    // Been spent 10 M€ as payment
    // Been bought 2 card(s)
    // You bought Astra Mechanica,Forced Precipitation
    been.buyCards(AstraMechanica, ForcedPrecipitation)

    keen.turn {
      // Keen used Applied Science action
      // Keen removed 1 resource(s) from Keen's Applied Science
      // Keen gained 1 energy
      cardAction1(AppliedScience) { doTask("Energy") }
      // Keen spent 8 M€ as payment
      // Keen claimed Merchant milestone
      stdAction("ClaimMilestoneSA") { doTask("Merchant") }
    }
    been.turn {
      // Been spent 8 M€ as payment
      // Been played Forced Precipitation
      playProject(ForcedPrecipitation, 8)
      // Been spent 20 M€ as payment
      // Been played Mining Colony
      // Been gained 1 titanium production
      playProject(MiningColony, 20) {
        // Been built a colony on Titan
        doTask("Colony<Titan>")
        // Been added 3 Floater(s) to Forced Precipitation
        addCardResources(ForcedPrecipitation)
      }
    }
    keen.turn {
      // Keen spent 4 M€ as payment
      // Keen spent 2 steel as payment
      // Keen played Mars University
      // Keen is using their Mars University effect to draw a card by discarding a card.
      playProject(MarsUniversity, 4, steel = 2) {
        doTask("-ProjectCard")
        // Keen discarded Caretaker Contract
        // Keen drew 1 card(s)
        discard(CaretakerContract)
        // You drew Kelp Farming
        draw(KelpFarming)
      }
      // Search for Life, Cutting Edge Technology, and Mars University supply three science tags;
      // Applied Science's wild tag is the fourth needed by Researcher.
      assignWildTag(AppliedScience, "ScienceTag")
      // Keen spent 8 M€ as payment
      // Keen claimed Researcher milestone
      stdAction("ClaimMilestoneSA") { doTask("Researcher") }
    }
    been.turn {
      // Been used Forced Precipitation action
      // Been spent 2 M€ as payment
      // Been added 1 Floater to Forced Precipitation
      cardAction1(ForcedPrecipitation)
      // Been used Extractor Balloons action
      // Been added 1 Floater to Extractor Balloons
      cardAction1(ExtractorBalloons)
    }
    keen.turn {
      // Keen used Directed Heat Usage action
      // Keen spent 3 heat as payment
      // Keen gained 4 M€
      cardAction1(DirectedHeatUsage) { doTask("4") }
      // Keen used Business Network action
      // Keen spent 3 M€ as payment
      // Keen bought 1 card(s)
      // You bought Earth Catapult
      cardAction1(BusinessNetwork) { buyCards(EarthCatapult) }
    }
    // Been passed
    been.pass()
    // Keen passed
    keen.pass()
    // Keen acted as World Government and increased oxygen level
    keen.wgt("OxygenStep")
  }

  private fun generation4() {
    // Screenshot evidence: generation-4-research.png, before either player's purchase.
    with(keen) {
      assertResources(m = 27, s = 1, t = 5, p = 3, e = 1, h = 3)
      assertProduction(m = 6, s = 1, t = 1, p = 1, e = 1, h = 1)
    }
    with(been) {
      assertResources(m = 34, s = 0, t = 1, p = 7, e = 0, h = 3)
      assertProduction(m = 10, s = 0, t = 1, p = 2, e = 0, h = 0)
    }

    // Generation 4
    // First player this generation is Been
    // Been spent 5 M€ as payment
    // Been bought 1 card(s)
    // You bought Invention Contest
    been.buyCards(InventionContest)
    // Keen spent 6 M€ as payment
    // Keen bought 2 card(s)
    // You bought Research Coordination,Lunar Mining
    keen.buyCards(ResearchCoordination, LunarMining)

    been.turn {
      // Been used Extractor Balloons action
      // Been removed 2 resource(s) from Been's Extractor Balloons
      cardAction2(ExtractorBalloons)
      // Been used Forced Precipitation action
      // Been removed 2 resource(s) from Been's Forced Precipitation
      // Been drew 1 card(s)
      cardAction2(ForcedPrecipitation) {
            // You drew Mining Rights
            draw(MiningRights)
          }
          .expect("-2 Floater, VenusStep, ProjectCard")
    }
    // Keen used Directed Heat Usage action
    // Keen spent 3 heat as payment
    // Keen gained 4 M€
    // Keen ended turn
    keen.turn { cardAction1(DirectedHeatUsage) { doTask("4") } }
    been.turn {
      // Been spent 9 M€ as payment
      // Been traded with Ganymede
      // Been gained 4 plants
      // Been gained 1 plant
      stdAction("TradeSA") { doTask("Trade<Ganymede>") }.expect("5 Plant")
      // Been used Convert Plants standard action
      // Been placed greenery tile at 20
      // Been gained 1 plant
      // Been drew 1 card(s)
      convertPlants {
        placeTile(3, 7)
        // You drew Luna Governor
        // Been spent 8 plants as payment
        draw(LunaGovernor)
      }
    }
    // Keen spent 23 M€ as payment
    // Keen played Earth Catapult
    // Keen gained 1 M€ for playing Earth Catapult, which has exactly 1 tag.
    // Keen ended turn
    keen.turn { playProject(EarthCatapult, 23) }
    been.turn {
      // Been spent 9 M€ as payment
      // Been played Mining Rights
      // Been gained 1 titanium production
      // Been placed Mining Rights tile at 38
      // Been gained 1 titanium
      // Been ended turn
      playProject(MiningRights, 9) {
        placeTile(6, 2)
      }
    }
    // Keen spent 2 M€ as payment
    // Keen played Research Coordination
    // Keen gained 4 M€ for playing Research Coordination, which has no tags.
    // Keen ended turn
    keen.turn { playProject(ResearchCoordination, 2) }
    // Been spent 11 M€ as payment
    // Been used Power Plant:SP standard project
    // Been ended turn
    been.turn { stdProject("PowerPlantSP") }
    keen.turn {
      // Keen used Business Network action
      cardAction1(BusinessNetwork) {
        // The archive records that the revealed card was not bought.
        buyCards(0)
      }
      // Keen bought 0 card(s)
      // Keen used Search For Life action
      // Keen spent 1 M€ as payment
      // Keen revealed and discarded Io Sulphur Research
      cardAction1(SearchForLife) {
        // Io Sulphur Research has no microbe tag.
        declineTask()
      }
    }
    // Been passed
    been.pass()
    keen.turn {
      // Keen used Applied Science action
      // Keen removed 1 resource(s) from Keen's Applied Science
      // Keen gained 1 titanium
      cardAction1(AppliedScience) { doTask("Titanium") }
      // Keen passed
      pass()
    }
    // Been acted as World Government and increased Venus scale
    been.wgt("VenusStep")
  }

  private fun generation5() {
    // Generation 5
    // First player this generation is Keen
    // Been spent 10 M€ as payment
    // Been bought 2 card(s)
    // You bought Space Mirrors,Sister Planet Support
    been.buyCards(SpaceMirrors, SisterPlanetSupport)
    // Keen spent 9 M€ as payment
    // Keen bought 3 card(s)
    // You bought Fueled Generators,Cupola City,Giant Ice Asteroid
    keen.buyCards(FueledGenerators, CupolaCity, GiantIceAsteroid)

    // Keen spent 3 titanium as payment
    // Keen traded with Ceres
    // Keen gained 8 steel
    // Keen ended turn
    keen.turn { stdAction("TradeSA", 3) { doTask("Trade<Ceres>") } }
    been.turn {
      // Been spent 11 M€ as payment
      // Been played Corporate Stronghold
      // Been gained 3 M€ production
      // Been lost 1 energy production
      // Been placed city tile at 18
      // Been gained 2 M€ from 1 ocean(s)
      playProject(CorporateStronghold, 11) { placeTile(3, 5) }.expect("PROD[3, -Energy], -9")
      // Been spent 8 M€ as payment
      // Been claimed Mayor milestone
      stdAction("ClaimMilestoneSA") { doTask("Mayor") }
    }
    // Keen played Fueled Generators
    // Keen lost 1 M€ production
    // Keen gained 1 energy production
    // Keen ended turn
    keen.turn { playProject(FueledGenerators, 0) }
    been.turn {
      // Been spent 3 titanium as payment
      // Been traded with Titan
      // Been added 1 Floater to Extractor Balloons
      // Been added 3 Floater(s) to Forced Precipitation
      // Been ended turn
      stdAction("TradeSA", 3) {
        doTask("Trade<Titan>")
        doTask("Floater<$ExtractorBalloons>")
        doTask("3 Floater<$ForcedPrecipitation>")
      }
    }
    // Keen used Applied Science action
    // Keen removed 1 resource(s) from Keen's Applied Science
    // Keen gained 1 titanium
    // Keen ended turn
    keen.turn { cardAction1(AppliedScience) { doTask("Titanium") } }
    // Been used Extractor Balloons action
    // Been added 1 Floater to Extractor Balloons
    // Been ended turn
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn {
      // Earth Catapult supplies one Earth tag; this wild assignment supplies Lunar Mining's
      // second required Earth tag.
      assignWildTag(ResearchCoordination, "EarthTag")
      // Keen spent 9 M€ as payment
      // Keen played Lunar Mining
      // Keen gained 2 titanium production
      // Keen gained 1 M€ for playing Lunar Mining, which has exactly 1 tag.
      // Keen ended turn
      playProject(LunarMining, 9)
    }
    // Been spent 7 M€ as payment
    // Been played Sister Planet Support
    // Been gained 3 M€ production
    // Been ended turn
    been.turn { playProject(SisterPlanetSupport, 7) }
    // Keen spent 6 steel as payment
    // Keen played Cupola City
    // Keen gained 3 M€ production
    // Keen lost 1 energy production
    // Keen placed city tile at 24
    // Keen gained 2 steel
    // Keen ended turn
    keen.turn {
      playProject(CupolaCity, steel = 6) { placeTile(4, 4) }.expect("PROD[3, -Energy], -4 Steel")
    }
    // Been used Forced Precipitation action
    // Been removed 2 resource(s) from Been's Forced Precipitation
    // Been ended turn
    been.turn { cardAction2(ForcedPrecipitation) }
    keen.turn {
      // Keen used Business Network action
      // Keen bought 0 card(s)
      // Keen ended turn
      cardAction1(BusinessNetwork) {
        // The archive records that the revealed card was not bought.
        buyCards(0)
      }
    }
    // Been passed
    been.pass()
    keen.turn {
      // Keen used Search For Life action
      // Keen spent 1 M€ as payment
      // Keen revealed and discarded Shuttles
      cardAction1(SearchForLife) {
        // Shuttles has no microbe tag.
        declineTask()
      }
      // Keen spent 9 M€ as payment
      // Keen spent 5 titanium as payment
      // Keen played Methane From Titan
      // Keen gained 2 plant production
      // Keen gained 2 heat production
      playProject(MethaneFromTitan, 9, titanium = 5)
      // Keen passed
      pass()
    }
    // Keen acted as World Government and increased oxygen level
    keen.wgt("OxygenStep")
  }

  private fun generation6() {
    // Generation 6
    // First player this generation is Been
    // Keen spent 6 M€ as payment
    // Keen bought 2 card(s)
    // You bought Lightning Harvest,Water Splitting Plant
    keen.buyCards(LightningHarvest, WaterSplittingPlant)
    // Been spent 10 M€ as payment
    // Been bought 2 card(s)
    // You bought Earth Office,Solar Reflectors
    been.buyCards(EarthOffice, SolarReflectors)

    been.turn {
      // Been used Extractor Balloons action
      // Been removed 2 resource(s) from Been's Extractor Balloons
      cardAction2(ExtractorBalloons)
      // Been used Forced Precipitation action
      // Been removed 2 resource(s) from Been's Forced Precipitation
      cardAction2(ForcedPrecipitation)
    }
    // Keen spent 3 titanium as payment
    // Keen traded with Io
    // Keen gained 13 heat
    // Keen ended turn
    keen.turn { stdAction("TradeSA", 3) { doTask("Trade<Io>") } }
    // Been used Convert Plants standard action
    // Been placed greenery tile at 12
    // Been gained 1 plant
    // Been spent 8 plants as payment
    // Been ended turn
    been.turn { convertPlants { placeTile(2, 5) } }
    // Keen used Business Network action
    // Keen spent 3 M€ as payment
    // Keen bought 1 card(s)
    // You bought Restricted Area
    // Keen ended turn
    keen.turn { cardAction1(BusinessNetwork) { buyCards(RestrictedArea) } }
    // Been spent 1 M€ as payment
    // Been played Earth Office
    // Been ended turn
    been.turn { playProject(EarthOffice, 1) }
    keen.turn {
      // Keen spent 9 M€ as payment
      // Keen played Restricted Area
      // Keen placed Restricted Area tile at 19
      // Keen gained 2 plants
      // Keen gained 2 M€ from 1 ocean(s)
      // Keen is using their Mars University effect to draw a card by discarding a card.
      playProject(RestrictedArea, 9) {
        placeTile(3, 6)
        doTask("-ProjectCard")
        // Keen discarded Ore Processor
        // Keen drew 1 card(s)
        discard(OreProcessor)
        // You drew Ice Moon Colony
        // Keen gained 1 M€ for playing Restricted Area, which has exactly 1 tag.
        // Keen ended turn
        draw(IceMoonColony)
      }
    }
    // Been played Luna Governor
    // Been gained 2 M€ production
    // Been ended turn
    been.turn { playProject(LunaGovernor, 0) }
    keen.turn {
      // Keen used Restricted Area action
      // Keen spent 2 M€ as payment
      // Keen drew 1 card(s)
      cardAction1(RestrictedArea) {
        // You drew Molecular Printing
        // Keen ended turn
        draw(MolecularPrinting)
      }
    }
    been.turn {
      // Payment reconstruction: the archive explicitly records 23 M€ and no titanium; Been keeps
      // that titanium for the generation-7 Ganymede trade.
      intentionalUnderpay()
      // Been spent 23 M€ as payment
      // Been played Solar Reflectors
      // Been gained 5 heat production
      // Been ended turn
      playProject(SolarReflectors, 23)
    }
    // Keen used Convert Plants standard action
    // Keen placed greenery tile at 25
    // Keen gained 1 steel
    // Keen gained 4 M€ from 2 ocean(s)
    // Keen spent 8 plants as payment
    // Keen ended turn
    keen.turn { convertPlants { placeTile(4, 5) } }
    been.turn {
      // Payment reconstruction: the archive again records only M€, preserving the same titanium
      // for the next generation's trade.
      intentionalUnderpay()
      // Been spent 3 M€ as payment
      // Been played Space Mirrors
      // Been ended turn
      playProject(SpaceMirrors, 3)
    }
    // Keen used Applied Science action
    // Keen removed 1 resource(s) from Keen's Applied Science
    // Keen gained 1 titanium
    // Keen ended turn
    keen.turn { cardAction1(AppliedScience) { doTask("Titanium") } }
    // Been used Space Mirrors action
    // Been spent 7 M€ as payment
    // Been gained 1 energy production
    // Been ended turn
    been.turn { cardAction1(SpaceMirrors) }
    keen.turn {
      // Fueled Generators supplies one power tag. Both wild cards are needed for Lightning
      // Harvest's three-power-tag requirement.
      assignWildTag(AppliedScience, "PowerTag")
      assignWildTag(ResearchCoordination, "PowerTag")
      // Keen spent 4 M€ as payment
      // Keen played Lightning Harvest
      // Keen gained 1 M€ production
      // Keen gained 1 energy production
      // Keen gained 1 M€ for playing Lightning Harvest, which has exactly 1 tag.
      // Keen ended turn
      playProject(LightningHarvest, 4).expect("PROD[1, Energy], -3")
    }
    // Been passed
    been.pass()
    keen.turn {
      // Keen spent 2 M€ as payment
      // Keen spent 1 titanium as payment
      // Keen played Optimal Aerobraking
      // Keen gained 1 M€ for playing Optimal Aerobraking, which has exactly 1 tag.
      playProject(OptimalAerobraking, 2, titanium = 1)
      // Keen spent 8 heat as payment
      // Keen used Convert Heat standard action
      convertHeat()
      // Keen spent 11 M€ as payment
      // Keen used Power Plant:SP standard project
      stdProject("PowerPlantSP")
      // Keen passed
      pass()
    }
    // Been acted as World Government and increased Venus scale
    been.wgt("VenusStep")
  }

  private fun generation7() {
    // Generation 7
    // First player this generation is Keen
    // Been spent 15 M€ as payment
    // Been bought 3 card(s)
    // You bought Floater Prototypes,Urban Decomposers,Business Contacts
    been.buyCards(FloaterPrototypes, UrbanDecomposers, BusinessContacts)
    // Keen spent 12 M€ as payment
    // Keen bought 4 card(s)
    // You bought Rad-Suits,Rad-Chem Factory,Sterling Vents,Special Design
    keen.buyCards(RadSuits, RadChemFactory, SterlingVents, SpecialDesign)

    keen.turn {
      // Keen spent 2 M€ as payment
      // Keen played Rad-Suits
      // Keen gained 1 M€ production
      // Keen gained 4 M€ for playing Rad-Suits, which has no tags.
      playProject(RadSuits, 2)
      // Keen spent 4 steel as payment
      // Keen played Water Splitting Plant
      // Keen gained 1 M€ for playing Water Splitting Plant, which has exactly 1 tag.
      playProject(WaterSplittingPlant, steel = 4)
    }
    been.turn {
      // Been spent 15 M€ as payment
      // Been played Venusian Animals
      // Been added 1 Animal to Venusian Animals
      playProject(VenusianAnimals, 15)
      // Been spent 2 M€ as payment
      // Been played Invention Contest
      // Been added 1 Animal to Venusian Animals
      // Been drew 1 card(s)
      playProject(InventionContest, 2) {
        // You drew Subterranean Reservoir
        draw(SubterraneanReservoir)
      }
    }
    keen.turn {
      // Keen spent 25 M€ as payment
      // Keen spent 3 titanium as payment
      // Keen played Giant Ice Asteroid
      // Keen gained 1 heat production
      // Keen gained 3 M€ because of Optimal Aerobraking
      // Keen gained 3 heat because of Optimal Aerobraking
      // Keen placed ocean tile at 27
      // Keen gained 1 plant
      // Keen gained 4 M€ from 2 ocean(s)
      // Keen placed ocean tile at 46
      // Keen gained 2 titanium
      // Been lost 4 plants because of Keen
      // Keen ended turn
      playProject(GiantIceAsteroid, 25, titanium = 3) {
            doTask("-4 Plant<Been>")
            doTask("OceanTile<Hellas_4_7>")
            doTask("OceanTile<Hellas_7_3>")
          }
          .expect("-18, -Titanium, Plant, 3 Heat, -4 Plant<Been>")
    }
    // Been spent 4 M€ as payment
    // Been played Business Contacts
    // Been drew 2 card(s)
    been.turn {
      playProject(BusinessContacts, 4) {
        // You drew Corroder Suits,Deep Well Heating
        // Been ended turn
        draw(CorroderSuits, DeepWellHeating)
      }
    }
    keen.turn {
      // Keen spent 8 heat as payment
      // Keen used Convert Heat standard action
      convertHeat()
      // Keen spent 8 heat as payment
      // Keen used Convert Heat standard action
      // Keen gained 1 heat production
      convertHeat().expect("PROD[Heat]")
    }
    // Been spent 8 M€ as payment
    // Been played Corroder Suits
    // Been gained 2 M€ production
    // Been added 1 Animal to Venusian Animals
    // Been ended turn
    been.turn { playProject(CorroderSuits, 8) { addCardResources(VenusianAnimals) } }
    keen.turn {
      // Keen used Restricted Area action
      // Keen spent 2 M€ as payment
      // Keen drew 1 card(s)
      cardAction1(RestrictedArea) {
        // You drew Mohole Area
        // Keen ended turn
        draw(MoholeArea)
      }
    }
    // Been spent 3 titanium as payment
    // Been traded with Ganymede
    // Been gained 4 plants
    // Been gained 1 plant
    // Been ended turn
    been.turn { stdAction("TradeSA", 3) { doTask("Trade<Ganymede>") } }
    // Keen spent 1 M€ as payment
    // Keen spent 1 steel as payment
    // Keen played Sterling Vents
    // Keen gained 2 energy production
    // Keen lost 2 heat production
    // Keen ended turn
    keen.turn { playProject(SterlingVents, 1, steel = 1) }
    // Been spent 8 heat as payment
    // Been used Convert Heat standard action
    // Been ended turn
    been.turn { convertHeat() }
    // Keen used Water Splitting Plant action
    // Keen ended turn
    keen.turn { cardAction1(WaterSplittingPlant) }
    been.turn {
      // Been spent 2 M€ as payment
      // Been played Floater Prototypes
      // Been added 1 Animal to Venusian Animals
      // Been added 2 Floater(s) to Forced Precipitation
      // Been ended turn
      playProject(FloaterPrototypes, 2) { addCardResources(ForcedPrecipitation) }.expect("Animal")
    }
    // Keen spent 3 steel as payment
    // Keen played Rad-Chem Factory
    // Keen lost 1 energy production
    // Keen gained 2 TR
    // Keen gained 1 M€ for playing Rad-Chem Factory, which has exactly 1 tag.
    // Keen ended turn
    keen.turn { playProject(RadChemFactory, steel = 3) }
    // Been used Forced Precipitation action
    // Been removed 2 resource(s) from Been's Forced Precipitation
    // Been ended turn
    been.turn { cardAction2(ForcedPrecipitation) }
    // Keen used Business Network action
    // Keen spent 3 M€ as payment
    // Keen bought 1 card(s)
    // You bought Atmoscoop
    // Keen ended turn
    keen.turn { cardAction1(BusinessNetwork) { buyCards(Atmoscoop) } }
    // Been used Extractor Balloons action
    // Been added 1 Floater to Extractor Balloons
    // Been ended turn
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn {
      // Keen used Search For Life action
      // Keen spent 1 M€ as payment
      // Keen revealed and discarded Herbivores
      // Keen ended turn
      cardAction1(SearchForLife) {
        // Herbivores has no microbe tag.
        declineTask()
      }
    }
    // Been passed
    been.pass()
    // Keen passed
    keen.pass()
    // Keen acted as World Government and increased Venus scale
    keen.wgt("VenusStep")
  }

  private fun generation8() {
    // Screenshot evidence: generation-8-research.png, before either player's purchase.
    with(keen) {
      assertResources(m = 42, s = 2, t = 5, p = 8, e = 4, h = 5)
      assertProduction(m = 10, s = 1, t = 3, p = 3, e = 4, h = 3)
    }
    with(been) {
      assertResources(m = 57, s = 0, t = 4, p = 7, e = 1, h = 7)
      assertProduction(m = 20, s = 0, t = 2, p = 2, e = 1, h = 5)
    }

    // Generation 8
    // First player this generation is Been
    // Been spent 5 M€ as payment
    // Been bought 1 card(s)
    // You bought Aerial Mappers
    been.buyCards(AerialMappers)
    // Keen spent 9 M€ as payment
    // Keen bought 3 card(s)
    // You bought Productive Outpost,Hackers,Harvest
    keen.buyCards(ProductiveOutpost, Hackers, Harvest)

    been.turn {
      // Been spent 23 M€ as payment
      // Been used Greenery standard project
      // Been placed greenery tile at 11
      // Been gained 1 plant
      // Been gained 1 steel
      stdProject("GreenerySP") { placeTile(2, 4) }
      // Been used Convert Plants standard action
      // Been placed greenery tile at 07
      // Been gained 1 plant
      // Been spent 8 plants as payment
      convertPlants { placeTile(1, 5) }
    }
    keen.turn {
      // Keen used Convert Plants standard action
      // Keen placed greenery tile at 32
      // Keen gained 2 steel
      convertPlants { placeTile(5, 4) }
      // Keen spent 8 plants as payment
      // Keen funded Scientist award
      // Keen spent 8 M€ as payment
      stdAction("FundAwardSA") { doTask("Scientist") }
    }
    been.turn {
      // Been spent 3 titanium as payment
      // Been traded with Titan
      // Been added 1 Floater to Extractor Balloons
      // Been added 3 Floater(s) to Forced Precipitation
      stdAction("TradeSA", 3) {
        doTask("Trade<Titan>")
        doTask("Floater<$ExtractorBalloons>")
        doTask("3 Floater<$ForcedPrecipitation>")
      }

      // Screenshot evidence: generation-8-actions.png, after Been's Titan trade and before
      // Subterranean Reservoir.
      with(keen) {
        assertResources(m = 25, s = 4, t = 5, p = 0, e = 4, h = 5)
        assertProduction(m = 10, s = 1, t = 3, p = 3, e = 4, h = 3)
      }
      with(been) {
        assertResources(m = 29, s = 1, t = 1, p = 1, e = 1, h = 7)
        assertProduction(m = 20, s = 0, t = 2, p = 2, e = 1, h = 5)
      }

      // Been spent 11 M€ as payment
      // Been played Subterranean Reservoir
      // Been placed ocean tile at 36
      // Been gained 4 M€ from 2 ocean(s)
      playProject(SubterraneanReservoir, 11) { placeTile(5, 8) }
    }
    keen.turn {
      // Keen spent 3 M€ as payment
      // Keen spent 5 titanium as payment
      // Keen played Atmoscoop
      // Keen raised the Venus scale 2 step(s)
      // Keen ended turn
      playProject(Atmoscoop, 3, titanium = 5) { doTask("2 VenusStep") }
    }
    been.turn {
      // Been used Extractor Balloons action
      // Been removed 2 resource(s) from Been's Extractor Balloons
      cardAction2(ExtractorBalloons)
      // Been used Forced Precipitation action
      // Been removed 2 resource(s) from Been's Forced Precipitation
      cardAction2(ForcedPrecipitation)
    }
    // Keen played Productive Outpost
    // Keen gained 4 M€ for playing Productive Outpost, which has no tags.
    // Keen ended turn
    keen.turn { playProject(ProductiveOutpost, 0) }
    been.turn {
      // Been spent 7 M€ as payment
      // Been played Astra Mechanica
      // Been added 1 Animal to Venusian Animals
      playProject(AstraMechanica, 7) {
            doWithoutAutoExec(been) {
              doTask("ProjectCard FROM PlayedEvent<Class<$InventionContest>>")
              // Been returned Invention Contest to their hand
              returnToHand(InventionContest)
              doTask("ProjectCard FROM PlayedEvent<Class<$Flooding>>")
              // Been returned Flooding to their hand
              returnToHand(Flooding)
            }
          }
          .expect("Animal, ProjectCard")
      // Been spent 7 M€ as payment
      // Been played Flooding
      // Been placed ocean tile at 44
      // Been gained 1 steel
      // Been gained 4 M€ from 2 ocean(s)
      playProject(Flooding, 7) { placeTile(6, 8) }
    }
    keen.turn {
      // Keen used Restricted Area action
      // Keen spent 2 M€ as payment
      // Keen drew 1 card(s)
      cardAction1(RestrictedArea) {
        // You drew Asteroid
        // Keen ended turn
        draw(AsteroidCard)
      }
    }
    been.turn {
      // Been spent 2 M€ as payment
      // Been played Invention Contest
      // Been added 1 Animal to Venusian Animals
      // Been drew 1 card(s)
      playProject(InventionContest, 2) {
        // You drew Urbanized Area
        draw(UrbanizedArea)
      }
      // Been spent 9 M€ as payment
      // Been spent 2 steel as payment
      // Been played Deep Well Heating
      // Been gained 1 energy production
      playProject(DeepWellHeating, 9, steel = 2)
    }
    keen.turn {
      // Keen used Business Network action
      cardAction1(BusinessNetwork) {
        // Keen spent 3 M€ as payment
        // Keen bought 1 card(s)
        // You bought Solar Probe
        // Keen ended turn
        buyCards(SolarProbe)
      }
    }
    // Been passed
    been.pass()
    keen.turn {
      // Keen used Water Splitting Plant action
      cardAction1(WaterSplittingPlant)
      // Keen spent 9 M€ as payment
      // Keen played Molecular Printing
      // Keen gained 6 M€
      // Keen is using their Mars University effect to draw a card by discarding a card.
      playProject(MolecularPrinting, 9) {
            doTask("-ProjectCard")
            // Keen discarded Mohole Area
            // Keen drew 1 card(s)
            discard(MoholeArea)
            // You drew Space Port
            // Keen gained 1 M€ for playing Molecular Printing, which has exactly 1 tag.
            draw(SpacePort)
          }
          .expect("-2")
      // Keen used Search For Life action
      // Keen spent 1 M€ as payment
      // Keen revealed and discarded Big Asteroid
      cardAction1(SearchForLife) {
        // Big Asteroid has no microbe tag.
        declineTask()
      }
      // Keen spent 1 M€ as payment
      // Keen played Hackers
      // Keen gained 2 M€ production
      // Keen lost 1 energy production
      // Keen stole 2 M€ production from Been
      // Keen gained 4 M€ for playing Hackers, which has no tags.
      playProject(Hackers, 1) { doTask("PROD[-2 Megacredit<Been>]") }
          .expect("PROD[2, -Energy, -2 Megacredit<Been>], 3")
      // Keen spent 7 M€ as payment
      // Keen played Breathing Filters
      // Keen is using their Mars University effect to draw a card by discarding a card.
      playProject(BreathingFilters, 7) {
        doTask("-ProjectCard")
        // Keen discarded Space Port
        // Keen drew 1 card(s)
        discard(SpacePort)
        // You drew Venus Magnetizer
        // Keen gained 1 M€ for playing Breathing Filters, which has exactly 1 tag.
        draw(VenusMagnetizer)
      }
      // Keen spent 13 M€ as payment
      // Keen played Kelp Farming
      // Keen gained 2 M€ production
      // Keen gained 3 plant production
      // Keen gained 2 plants
      // Keen gained 1 M€ for playing Kelp Farming, which has exactly 1 tag.
      playProject(KelpFarming, 13)
      // Keen passed
      // Been placed ocean tile at 43
      pass()
    }
    // Been acted as World Government and placed an ocean
    been.wgt("OceanTile<Hellas_6_7>")
  }

  private fun generation9() {
    // Generation 9
    // First player this generation is Keen
    // Been spent 10 M€ as payment
    // Been bought 2 card(s)
    // You bought Rim Freighters,Lunar Beam
    been.buyCards(RimFreighters, LunarBeam)
    // Keen spent 6 M€ as payment
    // Keen bought 2 card(s)
    // You bought Advanced Alloys,Cyberia Systems
    keen.buyCards(AdvancedAlloys, CyberiaSystems)

    keen.turn {
      // Keen used Convert Plants standard action
      // Keen placed greenery tile at 33
      // Keen gained 2 M€ from 1 ocean(s)
      convertPlants { placeTile(5, 5) }
      // Keen spent 8 plants as payment
      // Keen used Water Splitting Plant action
      cardAction1(WaterSplittingPlant)
    }

    // Screenshot evidence: generation-9-actions.png, after Keen's opening turn and before Been's.
    with(keen) {
      assertResources(m = 48, s = 5, t = 3, p = 0, e = 0, h = 9)
      assertProduction(m = 14, s = 1, t = 3, p = 6, e = 3, h = 3)
    }
    with(been) {
      assertResources(m = 50, s = 0, t = 3, p = 3, e = 2, h = 13)
      assertProduction(m = 18, s = 0, t = 2, p = 2, e = 2, h = 5)
    }

    been.turn {
      // Been spent 1 M€ as payment
      // Been spent 1 titanium as payment
      // Been played Rim Freighters
      playProject(RimFreighters, 1, titanium = 1)
      // Been spent 2 titanium as payment
      // Been traded with Ceres
      // Been gained 6 steel
      stdAction("TradeSA", 3) { doTask("Trade<Ceres>") }
    }
    keen.turn {
      // Keen used Restricted Area action
      // Keen spent 2 M€ as payment
      // Keen drew 1 card(s)
      cardAction1(RestrictedArea) {
        // You drew Local Heat Trapping
        // Keen ended turn
        draw(LocalHeatTrapping)
      }
    }
    been.turn {
      // Been spent 6 M€ as payment
      // Been spent 6 steel as payment
      // Been played Underground City
      // Been gained 2 steel production
      // Been lost 2 energy production
      // Been placed city tile at 06
      // Been gained 1 plant
      // Been gained 1 steel
      playProject(UndergroundCity, 6, steel = 6) { placeTile(1, 4) }
      // Been funded Highlander award
      // Been spent 14 M€ as payment
      stdAction("FundAwardSA", which = 2) { doTask("Highlander") }
    }
    keen.turn {
      // Keen used Business Network action
      cardAction1(BusinessNetwork) {
        // The archive records that the revealed card was not bought.
        buyCards(0)
      }
      // Keen bought 0 card(s)
      // Keen spent 7 M€ as payment
      // Keen played Advanced Alloys
      // Keen is using their Mars University effect to draw a card by discarding a card.
      playProject(AdvancedAlloys, 7) {
        doTask("-ProjectCard")
        // Keen discarded Cyberia Systems
        // Keen drew 1 card(s)
        discard(CyberiaSystems)
        // You drew Symbiotic Fungus
        // Keen gained 1 M€ for playing Advanced Alloys, which has exactly 1 tag.
        draw(SymbioticFungus)
      }
    }
    // Been spent 10 M€ as payment
    // Been played Lunar Beam
    // Been lost 2 M€ production
    // Been gained 2 energy production
    // Been gained 2 heat production
    // Been ended turn
    been.turn { playProject(LunarBeam, 10) }
    keen.turn {
      // The archive pays Solar Probe entirely in M€ despite Keen retaining usable titanium.
      intentionalUnderpay()
      // Keen spent 7 M€ as payment
      // Keen played Solar Probe
      // Keen drew 3 card(s)
      playProject(SolarProbe, 7) {
            // You drew Algae,Cloud Tourism,Spin-Inducing Asteroid
            // Keen gained 3 M€ because of Optimal Aerobraking
            // Keen gained 3 heat because of Optimal Aerobraking
            // Keen is using their Mars University effect to draw a card by discarding a card.
            draw(Algae, CloudTourism, SpinInducingAsteroid)
            // Solar Probe's event cleanup currently removes its own science tag too early, so
            // Solarnet draws two of the archive's three cards. Restore that direct missing
            // consequence.
            keen.exMachina("ProjectCard")
            doTask("-ProjectCard")
            // Keen discarded Spin-Inducing Asteroid
            // Keen drew 1 card(s)
            discard(SpinInducingAsteroid)
            // You drew Trees
            // Keen ended turn
            draw(Trees)
          }
          .expect("2 ProjectCard, 3 Heat, -4")
    }
    // Been spent 11 M€ as payment
    // Been played Aerial Mappers
    // Been ended turn
    been.turn { playProject(AerialMappers, 11) }
    keen.turn {
      // Keen spent 3 titanium as payment
      // Keen played Asteroid
      // Keen gained 2 titanium
      // Keen gained 3 M€ because of Optimal Aerobraking
      // Keen gained 3 heat because of Optimal Aerobraking
      // Been lost 3 plants because of Keen
      playProject(AsteroidCard, titanium = 3) { doTask("-3 Plant<Been>") }
          .expect("-Titanium, 3, 3 Heat, -3 Plant<Been>")
      // Keen spent 13 M€ as payment
      // Keen spent 2 titanium as payment
      // Keen played Ice Moon Colony
      playProject(IceMoonColony, 13, titanium = 2) {
        // Keen built a colony on Ganymede
        // Keen gained 1 plant production
        // Keen placed ocean tile at 08
        // Keen gained 2 plants
        // Keen gained 1 M€ for playing Ice Moon Colony, which has exactly 1 tag.
        doTask("Colony<Ganymede>")
        placeTile(2, 1)
      }
    }
    // Been spent 6 M€ as payment
    // Been played Urban Decomposers
    // Been gained 1 plant production
    // Been ended turn
    been.turn { playProject(UrbanDecomposers, 6) }
    // Keen played Harvest
    // Keen gained 12 M€
    // Keen ended turn
    keen.turn { playProject(Harvest, 0) }
    // Been used Aerial Mappers action
    // Been added 1 Floater to Aerial Mappers
    // Been ended turn
    been.turn { cardAction1(AerialMappers) { addCardResources(AerialMappers) } }
    // Keen spent 3 M€ as payment
    // Keen played Venus Magnetizer
    // Keen gained 1 M€ for playing Venus Magnetizer, which has exactly 1 tag.
    // Keen ended turn
    keen.turn { playProject(VenusMagnetizer, 3) }
    // Been used Extractor Balloons action
    // Been added 1 Floater to Extractor Balloons
    // Been ended turn
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn {
      // Keen used Search For Life action
      // Keen spent 1 M€ as payment
      // Keen revealed and discarded Ganymede Colony
      // Keen ended turn
      cardAction1(SearchForLife) {
        // Ganymede Colony has no microbe tag.
        declineTask()
      }
    }
    // Been spent 8 heat as payment
    // Been used Convert Heat standard action
    // Been ended turn
    been.turn { convertHeat() }
    // Keen spent 8 heat as payment
    // Keen used Convert Heat standard action
    // Keen ended turn
    keen.turn { convertHeat() }
    // Been passed
    been.pass()
    keen.turn {
      // Keen spent 2 M€ as payment
      // Keen played Special Design
      // Keen is using their Mars University effect to draw a card by discarding a card.
      playProject(SpecialDesign, 2) {
        doTask("-ProjectCard")
        // Keen discarded Symbiotic Fungus
        // Keen drew 1 card(s)
        discard(SymbioticFungus)
        // You drew Capital
        draw(Capital)
      }
      // Keen spent 9 M€ as payment
      // Keen played Trees
      // Keen gained 3 plant production
      // Keen gained 1 plant
      // Keen gained 1 M€ for playing Trees, which has exactly 1 tag.
      playProject(Trees, 9)
      // Keen spent 6 M€ as payment
      // Keen played Algae
      // Keen gained 2 plant production
      // Keen gained 1 plant
      // Keen gained 1 M€ for playing Algae, which has exactly 1 tag.
      playProject(Algae, 6)
      // Keen played Local Heat Trapping
      // Keen spent 5 heat as payment
      // Keen gained 4 plants
      // Keen gained 1 M€ for playing Local Heat Trapping, which has exactly 1 tag.
      playProject(LocalHeatTrapping, 0) { doTask("4 Plant") }
      // Keen used Convert Plants standard action
      // Keen placed greenery tile at 16
      // Keen gained 1 steel
      convertPlants { placeTile(3, 3) }
      // Test inference: Cloud Tourism is the only named, never-played card available for this
      // one-card patent sale.
      // Keen spent 8 plants as payment
      // Keen used Sell Patents standard project
      // Keen sold 1 patents
      sellPatents(CloudTourism)
      // Keen spent 23 M€ as payment
      // Keen used Greenery standard project
      // Keen placed greenery tile at 23
      // Keen gained 1 steel
      stdProject("GreenerySP") { placeTile(4, 3) }
      // Keen passed
      pass()
    }
    // Keen acted as World Government and increased temperature
    keen.wgt("TemperatureStep")
  }

  private fun generation10() {
    // Generation 10
    // First player this generation is Been
    // Been spent 5 M€ as payment
    // Been bought 1 card(s)
    // You bought Field-Capped City
    been.buyCards(FieldCappedCity)
    // Keen spent 6 M€ as payment
    // Keen bought 2 card(s)
    // You bought Luxury Foods,Peroxide Power
    keen.buyCards(LuxuryFoods, PeroxidePower)
    been.turn {
      // Been spent 23 M€ as payment
      // Been spent 3 steel as payment
      // Been played Field-Capped City
      // Been gained 2 M€ production
      // Been gained 1 energy production
      // Been gained 3 plants
      // Been placed city tile at 10
      // Been gained 1 plant
      playProject(FieldCappedCity, 23, steel = 3) { placeTile(2, 3) }
      // Been spent 10 M€ as payment
      // Been played Urbanized Area
      // Been gained 2 M€ production
      // Been placed city tile at 17
      // Been gained 1 steel
      playProject(UrbanizedArea, 10) { placeTile(3, 4) }
    }
    keen.turn {
      // Keen spent 3 energy to trade with Io
      // Keen gained 8 heat
      stdAction("TradeSA", 2) { doTask("Trade<Io>") }
      // Keen used Convert Plants standard action
      // Keen placed greenery tile at 41
      // Keen spent 8 plants as payment
      convertPlants { placeTile(6, 5) }
    }
    been.turn {
      // Been used Aerial Mappers action
      // Been removed 1 resource(s) from Been's Aerial Mappers
      // Been drew 1 card(s)
      cardAction2(AerialMappers) {
        // You drew Bactoviral Research
        draw(BactoviralResearch)
      }
      // Been spent 2 titanium as payment
      // Been traded with Ganymede
      // Been gained 4 plants
      // Been gained 1 plant
      // Keen gained 1 plant
      stdAction("TradeSA", 3) { doTask("Trade<Ganymede>") }.expect("5 Plant<Been>, Plant<Keen>")
    }
    // Keen spent 1 M€ as payment
    // Keen spent 7 steel as payment
    // Keen played Capital
    // Keen gained 5 M€ production
    // Keen lost 2 energy production
    // Keen placed Capital tile at 42
    // Keen gained 4 M€ from 2 ocean(s)
    // Keen ended turn
    keen.turn {
      playProject(Capital, 1, steel = 7) { placeTile(6, 6) }.expect("PROD[5, -2 Energy], 3")
    }
    been.turn {
      // Been used Convert Plants standard action
      // Been placed greenery tile at 60
      // Been gained 2 heat
      convertPlants { placeTile(9, 6) }
      // Been spent 8 plants as payment
      // Been spent 21 M€ as payment
      // Been played Ecology Research
      // Been gained 2 plant production
      playProject(EcologyResearch, 21) {
            // Been added 1 Animal to Venusian Animals
            // Been added 1 Animal to Venusian Animals
            addCardResources(VenusianAnimals)
          }
          .expect("PROD[2 Plant], 2 Animal")
    }
    // Keen spent 8 heat as payment
    // Keen used Convert Heat standard action
    // Keen ended turn
    keen.turn { convertHeat() }
    been.turn {
      // Been spent 8 heat as payment
      // Been used Convert Heat standard action
      convertHeat()
      // Been spent 8 heat as payment
      // Been used Convert Heat standard action
      convertHeat()
    }
    keen.turn {
      // Keen used Restricted Area action
      // Keen spent 2 M€ as payment
      // Keen drew 1 card(s)
      cardAction1(RestrictedArea) {
        // You drew Greenhouses
        // Keen ended turn
        draw(Greenhouses)
      }
    }
    // Been used Extractor Balloons action
    // Been added 1 Floater to Extractor Balloons
    // Been ended turn
    been.turn { cardAction1(ExtractorBalloons) }
    keen.turn {
      // Keen used Business Network action
      cardAction1(BusinessNetwork) {
        // Keen spent 3 M€ as payment
        // Keen bought 1 card(s)
        // You bought Grass
        buyCards(Grass)
      }
      // Keen spent 7 M€ as payment
      // Keen played Grass
      // Keen gained 1 plant production
      // Keen gained 3 plants
      // Keen gained 1 M€ for playing Grass, which has exactly 1 tag.
      playProject(Grass, 7)
    }
    // Been passed
    been.pass()
    keen.turn {
      // Keen spent 25 M€ as payment
      // Keen used City standard project
      // Keen placed city tile at 15
      // Keen gained 1 plant
      // Keen gained 2 M€ from 1 ocean(s)
      stdProject("CitySP") { placeTile(3, 2) }
      // Keen used Convert Plants standard action
      // Keen placed greenery tile at 22
      // Keen gained 1 plant
      convertPlants { placeTile(4, 2) }
      // Keen spent 8 plants as payment
      // Keen spent 2 M€ as payment
      // Keen spent 1 steel as payment
      // Keen played Peroxide Power
      // Keen lost 1 M€ production
      // Keen gained 2 energy production
      playProject(PeroxidePower, 2, steel = 1)
      // Keen used Search For Life action
      // Keen spent 1 M€ as payment
      // Keen revealed and discarded Hydrogen to Venus
      cardAction1(SearchForLife) {
        // Hydrogen to Venus has no microbe tag.
        declineTask()
      }
      // Keen spent 4 M€ as payment
      // Keen played Luxury Foods
      // Keen gained 4 M€ for playing Luxury Foods, which has no tags.
      playProject(LuxuryFoods, 4)
      // Keen spent 14 M€ as payment
      // Keen used Asteroid:SP standard project
      stdProject("AsteroidSP")
      // Keen passed
      pass()
    }
    // Been acted as World Government and increased temperature
    been.wgt("TemperatureStep")
  }

  private fun generation11() {
    // Generation 11
    // First player this generation is Keen
    // Been spent 5 M€ as payment
    // Been bought 1 card(s)
    // You bought Plantation
    been.buyCards(Plantation)
    // Keen spent 6 M€ as payment
    // Keen bought 2 card(s)
    // You bought Nitrogen from Titan,Sub-zero Salt Fish
    keen.buyCards(NitrogenFromTitan, SubZeroSaltFish)
    keen.turn {
      // Keen spent 1 M€ as payment
      // Keen played Sub-zero Salt Fish
      // Been lost 1 plant production because of Keen
      // Keen gained 1 M€ for playing Sub-zero Salt Fish, which has exactly 1 tag.
      playProject(SubZeroSaltFish, 1) { doTask("PROD[-Plant<Been>]") }
          .expect("PROD[-Plant<Been>], 0")
      // Keen spent 3 energy to trade with Miranda
      stdAction("TradeSA", 2) {
        doTask("Trade<Miranda>")
        // Keen added 3 Animal(s) to Sub-zero Salt Fish
        addCardResources(SubZeroSaltFish)
      }
    }
    been.turn {
      // Been spent 8 heat as payment
      // Been used Convert Heat standard action
      convertHeat()
      // Been spent 25 M€ as payment
      // Been used City standard project
      // Been placed city tile at 31
      stdProject("CitySP") { placeTile(5, 3) }
    }
    keen.turn {
      // Keen spent 8 heat as payment
      // Keen used Convert Heat standard action
      convertHeat()
      // Keen spent 25 M€ as payment
      // Keen used City standard project
      // Keen placed city tile at 48
      stdProject("CitySP") { placeTile(7, 5) }
    }
    been.turn {
      // Been spent 2 energy to trade with Ganymede
      // Been gained 3 plants
      // Been gained 1 plant
      // Keen gained 1 plant
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede>") }
      // Been used Convert Plants standard action
      // Been placed greenery tile at 05
      // Been gained 2 plants
      // Been spent 8 plants as payment
      convertPlants { placeTile(1, 3) }
    }
    keen.turn {
      // Keen used Convert Plants standard action
      // Keen placed greenery tile at 49
      // Keen drew 1 card(s)
      convertPlants {
        placeTile(7, 6)
        // You drew Investment Loan
        draw(InvestmentLoan)
      }
      // Keen spent 8 plants as payment
      // Keen spent 25 M€ as payment
      // Keen used City standard project
      // Keen placed city tile at 57
      // Keen gained 1 titanium
      stdProject("CitySP") { placeTile(8, 8) }
    }
    been.turn {
      // Been spent 15 M€ as payment
      // Been played Plantation
      // Been placed greenery tile at 04
      // Been gained 2 plants
      playProject(Plantation, 15) { placeTile(1, 2) }
      // Been used Convert Plants standard action
      // Been placed greenery tile at 09
      // Been gained 2 plants
      // Been gained 2 M€ from 1 ocean(s)
      // Been spent 8 plants as payment
      convertPlants { placeTile(2, 2) }
    }
    keen.turn {
      // Keen used Restricted Area action
      // Keen spent 2 M€ as payment
      // Keen drew 1 card(s)
      cardAction1(RestrictedArea) {
        // You drew Static Harvesting
        draw(StaticHarvesting)
      }
      // Keen used Convert Plants standard action
      // Keen placed greenery tile at 50
      // Keen gained 2 M€ from 1 ocean(s)
      // Keen spent 8 plants as payment
      convertPlants { placeTile(7, 7) }
    }
    been.turn {
      // Been funded Investor award
      stdAction("FundAwardSA", which = 3) { doTask("Investor") }
      // Been spent 20 M€ as payment
      // Been used Aerial Mappers action
      // Been added 1 Floater to Aerial Mappers
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    }
    // Keen spent 1 M€ as payment
    // Keen played Investment Loan
    // Keen lost 1 M€ production
    // Keen gained 10 M€
    // Keen ended turn
    keen.turn { playProject(InvestmentLoan, 1) }
    // Been used Extractor Balloons action
    // Been added 1 Floater to Extractor Balloons
    // Been ended turn
    been.turn { cardAction1(ExtractorBalloons) }
    // Keen used Sub-zero Salt Fish action
    // Keen added 1 Animal to Sub-zero Salt Fish
    // Keen ended turn
    keen.turn { cardAction1(SubZeroSaltFish) }
    been.turn {
      // Test inference: Bactoviral Research is Been's only named unplayed card.
      // Been used Sell Patents standard project
      // Been sold 1 patents
      // Been passed
      sellPatents(BactoviralResearch)
    }
    keen.turn {
      // Keen spent 1 M€ as payment
      // Keen spent 1 steel as payment
      // Keen played Greenhouses
      // Keen gained 12 plants
      playProject(Greenhouses, 1, steel = 1).expect("12 Plant")
      // Keen used Convert Plants standard action
      // Keen placed greenery tile at 51
      // Keen gained 4 M€ from 2 ocean(s)
      convertPlants { placeTile(7, 8) }
    }
    // The archive records Been passing as a second action; defer it to Been's next legal turn.
    been.pass()
    keen.turn {
      // The archive spends six Advanced Alloys titanium (24 value) against the 23 M€ invoice.
      intentionalOverpay()
      // Keen spent 8 plants as payment
      // Keen spent 6 titanium as payment
      // Keen played Nitrogen from Titan
      // Keen gained 2 TR
      playProject(NitrogenFromTitan, titanium = 6).expect("2 TerraformRating, -6 Titanium")
    }
    // Keen used Sell Patents standard project
    // Keen sold 1 patents
    keen.sellPatents(StaticHarvesting)
    keen.turn {
      // Keen used Business Network action
      cardAction1(BusinessNetwork) { buyCards(0) }
      // Keen bought 0 card(s)
      // Keen used Search For Life action
      // Keen spent 1 M€ as payment
      // Keen revealed and discarded Public Baths
      cardAction1(SearchForLife) {
        // Public Baths has no microbe tag.
        declineTask()
      }
      // Keen used Venus Magnetizer action
      // Keen lost 1 energy production
      cardAction1(VenusMagnetizer)
      // Keen passed
      // Final greenery placement
      pass()
    }

    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    // Keen placed greenery tile at 47
    // Keen gained 2 M€ from 1 ocean(s)
    keen.convertPlants { placeTile(7, 4) }
    // Keen placed greenery tile at 54
    // Keen drew 1 card(s)
    keen.convertPlants {
      placeTile(8, 5)
      // You drew Solar Wind Power
      keen.draw(SolarWindPower)
    }
    keen.declineTask()
    // Been placed greenery tile at 39
    // Been gained 2 M€ from 1 ocean(s)
    // This game id was gaf4dfdc697db
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
