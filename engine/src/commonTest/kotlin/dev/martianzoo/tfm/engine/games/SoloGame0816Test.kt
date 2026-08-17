package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SoloGame0816Test : AbstractSoloTest() {
  override val config =
      GameConfig(
          """
          ElysiumMapOption
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, TurmoilCardPack, PromoCardPack
          Tr63SoloVariant
          Ganymede, Luna, Pluto
          """,
          "Me",
      )

  // Drew and discarded Orbital Reflectors to place a 2
  // Drew and discarded Lake Marineris to place a 0
  // Drew and discarded Topsoil Contract to place a 2
  // Drew and discarded Space Station to place a 0
  override fun cityAreas(): Pair<String, String> = "Elysium_6_4" to "Elysium_8_7"

  override fun greeneryAreas(): Pair<String, String> = "Elysium_7_5" to "Elysium_9_8"

  @Test
  fun soloGame0816() {
    with(me) {
      // The id of this game is g5ce0a315821d
      // Good luck You!
      // Generation 1
      // You played TerraLabs Research
      // You kept 8 project cards
      playCorp(cn("TerraLabsResearch"), 8)

      // You discarded Miranda

      // You played Metals Company
      // You gained 1 M€ production
      // You gained 1 steel production
      // You gained 1 titanium production
      playPrelude(cn("MetalsCompany")).expect("PROD[M, S, T]")
      // You played Galilean Mining
      // You gained 2 titanium production
      playPrelude(cn("GalileanMining")).expect("PROD[2 T]")

      // You passed
      // You acted as World Government and increased Venus scale
      // Generation 2
      // You bought 1 card(s)
      // You bought Solarnet
      nextRound("VenusStep", 1)

      // You used Sell Patents standard project
      // You sold 2 patents
      sellPatents(2)
      // You played Neptunian Power Consultants
      val NeptunianPowerConsultants = cn("NeptunianPowerConsultants")
      playProject(NeptunianPowerConsultants, 14)

      // You passed
      // You acted as World Government and increased Venus scale
      // Generation 3
      // You bought 2 card(s)
      // You bought Lunar Beam,Vesta Shipyard
      nextRound("VenusStep", 2)

      // You played Vesta Shipyard
      // You gained 1 titanium production
      playProject(cn("VestaShipyard"), titanium = 5).expect("PROD[T]")

      // You passed
      // You acted as World Government and increased Venus scale
      // Generation 4
      // You bought 4 card(s)
      // You bought Heather,Artificial Photosynthesis,Steelworks,Solar Wind Power
      nextRound("VenusStep", 4)

      // You played Solar Wind Power
      // You gained 1 energy production
      // You gained 2 titanium
      playProject(cn("SolarWindPower"), 2, titanium = 3).expect("PROD[E], -T")
      // You played Artificial Photosynthesis
      // You gained 2 energy production
      playProject(cn("ArtificialPhotosynthesis"), 12) { doTask("PROD[2 Energy]") }
          .expect("PROD[2 E]")
      // You played Fueled Generators
      // You lost 1 M€ production
      // You gained 1 energy production
      playProject(cn("FueledGenerators"), 1).expect("PROD[-M, E]")

      // You passed
      // You placed ocean tile at 19
      // You acted as World Government and placed an ocean
      // You gained 1 energy production
      // You added 1 Hydroelectric resource to Neptunian Power Consultants
      pass()
      doTask("OceanTile<Elysium_3_6>! BY Engine")
      doTask("NeptunianOption")
      pay(1, steel = 2)
      // Generation 5
      // You bought 3 card(s)
      // You bought Acquired Company,Lava Tube Settlement,Conscription
      buyCards(3)

      // You played Acquired Company
      // You gained 3 M€ production
      playProject(cn("AcquiredCompany"), 10).expect("PROD[3 M]")
      // You spent 3 energy to trade with Luna
      // You gained 13 M€
      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }.expect("-3 E, 13 M")
      // You played Research
      // You drew 2 card(s)
      // You drew Media Group,Eos Chasma National Park
      playProject(cn("Research"), 11).expect("ProjectCard")

      // You passed
      // You placed ocean tile at 27
      // You acted as World Government and placed an ocean
      // You gained 1 energy production
      // You added 1 Hydroelectric resource to Neptunian Power Consultants
      pass()
      doTask("OceanTile<Elysium_4_7>! BY Engine")
      doTask("NeptunianOption")
      pay(1, steel = 2)
      // Generation 6
      // You bought 4 card(s)
      // You bought Harvest,Static Harvesting,Sulphur-Eating Bacteria,Indentured Workers
      buyCards(4)

      // You played Media Group
      playProject(cn("MediaGroup"), 6)
      // You played Import of Advanced GHG
      // You gained 2 heat production
      // You gained 3 M€
      playProject(cn("ImportOfAdvancedGhg"), titanium = 3).expect("PROD[2 H], 3 M")
      // You played Conscription
      // You gained 3 M€
      playProject(cn("Conscription"), 5).expect("-2 M")
      // You played AI Central
      // You lost 1 energy production
      val AiCentral = cn("AiCentral")
      playProject(AiCentral, 3, steel = 1).expect("PROD[-E]")
      // You used AI Central action
      // You drew 2 card(s)
      // You drew Stratopolis,Rotator Impacts
      cardAction1(AiCentral).expect("2 ProjectCard")
      // You played Rotator Impacts
      val RotatorImpacts = cn("RotatorImpacts")
      playProject(RotatorImpacts, titanium = 2)
      // You used Rotator Impacts action
      // You added 1 Asteroid to Rotator Impacts
      cardAction1(RotatorImpacts) { pay(titanium = 2) }.expect("Asteroid")
      // You played Indentured Workers
      // You gained 3 M€
      playProject(cn("IndenturedWorkers"), 0).expect("3 M")
      // You played Lunar Beam
      // You lost 2 M€ production
      // You gained 2 energy production
      // You gained 2 heat production
      playProject(cn("LunarBeam"), 5).expect("PROD[-2 M, 2 E, 2 H]")
      // You spent 3 energy to trade with Pluto
      // You drew 4 card(s)
      // You drew Geothermal Power,Building Industries,Forced Precipitation,Project Inspection
      stdAction("TradeSA", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 4 ProjectCard")
      // You played Project Inspection
      // You used AI Central action with Project Inspection
      // You drew 2 card(s)
      // You drew Great Escarpment Consortium,Restricted Area
      // You gained 3 M€
      playProject(cn("ProjectInspection"), 0) { doTask("UseAction1<$AiCentral>") }
          .expect("ProjectCard, 3 M")
      // You used Sell Patents standard project
      // You sold 2 patents
      sellPatents(2)
      // You played Great Escarpment Consortium
      // You gained 1 steel production
      playProject(cn("GreatEscarpmentConsortium"), 6) {
            doTask("PROD[-Steel<SoloOpponent>]")
          }
          .expect("PROD[S<Me>]")

      // You passed
      // You acted as World Government and increased temperature
      // Generation 7
      // You bought 4 card(s)
      // You bought Luna Governor,Jet Stream Microscrappers,Investment Loan,Tundra Farming
      nextRound("TemperatureStep", 4)

      // You used AI Central action
      // You drew 2 card(s)
      // You drew Callisto Penal Mines,Shuttles
      cardAction1(AiCentral).expect("2 ProjectCard")
      // You used Rotator Impacts action
      // You removed 1 resource(s) from You's Rotator Impacts
      // You drew 1 card(s)
      // You drew Extremophiles
      cardAction2(RotatorImpacts).expect("-Asteroid, ProjectCard, TR")
      // You played Investment Loan
      // You lost 1 M€ production
      // You gained 10 M€
      // You gained 3 M€
      playProject(cn("InvestmentLoan"), 3).expect("10 M, PROD[-M]")
      // You played Lava Tube Settlement
      // You gained 2 M€ production
      // You lost 1 energy production
      // You placed city tile at 20
      // You drew 3 card(s)
      // You drew Local Shading,Soil Enrichment,Trans-Neptune Probe
      // You gained 4 M€ from 2 ocean(s)
      playProject(cn("LavaTubeSettlement"), 11, steel = 2) {
            doTask("CityTile<Elysium_3_7>")
          }
          .expect("PROD[2 M, -E], 2 ProjectCard, -7 M")
      // You played Callisto Penal Mines
      // You gained 3 M€ production
      playProject(cn("CallistoPenalMines"), titanium = 8).expect("PROD[3 M]")
      // You used Sell Patents standard project
      // You sold 3 patents
      sellPatents(3)
      // You played Sulphur-Eating Bacteria
      val SulphurEatingBacteria = cn("SulphurEatingBacteria")
      playProject(SulphurEatingBacteria, 6)
      // You used Sulphur-Eating Bacteria action
      // You added 1 Microbe to Sulphur-Eating Bacteria
      cardAction1(SulphurEatingBacteria).expect("Microbe<$SulphurEatingBacteria>")
      // You played Extremophiles
      val Extremophiles = cn("Extremophiles")
      playProject(Extremophiles, 3)
      // You used Extremophiles action
      // You added 1 Microbe to Sulphur-Eating Bacteria
      cardAction1(Extremophiles) { doTask("Microbe<$SulphurEatingBacteria>") }
          .expect("Microbe<$SulphurEatingBacteria>")
      // You played Luna Governor
      // You gained 2 M€ production
      playProject(cn("LunaGovernor"), 4).expect("PROD[2 M]")
      // You spent 3 energy to trade with Ganymede
      // You gained 6 plants
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede>") }.expect("-3 E, 6 P")

      // You passed
      // You acted as World Government and increased temperature
      // Generation 8
      // You bought 4 card(s)
      // You bought Trading Colony,Regolith Eaters,Herbivores,Satellites
      nextRound("TemperatureStep", 4)

      // You used Convert Heat standard action
      // You gained 1 heat production
      stdAction("ConvertHeatSA").expect("TR, PROD[H]")
      // You used AI Central action
      // You drew 2 card(s)
      // You drew Protected Growth,Worms
      cardAction1(AiCentral).expect("2 ProjectCard")
      // You used Extremophiles action
      // You added 1 Microbe to Sulphur-Eating Bacteria
      cardAction1(Extremophiles) { doTask("Microbe<$SulphurEatingBacteria>") }
      // You used Sulphur-Eating Bacteria action
      // You added 1 Microbe to Sulphur-Eating Bacteria
      cardAction1(SulphurEatingBacteria).expect("Microbe<$SulphurEatingBacteria>")
      // You used Rotator Impacts action
      // You added 1 Asteroid to Rotator Impacts
      cardAction1(RotatorImpacts) { pay(titanium = 2) }.expect("Asteroid")
      // You played Steelworks
      val Steelworks = cn("Steelworks")
      playProject(Steelworks, 11, steel = 2)
      // You used Steelworks action
      // You gained 2 steel
      cardAction1(Steelworks).expect("-4 E, 2 S, TR")
      // You played Building Industries
      // You gained 2 steel production
      // You lost 1 energy production
      playProject(cn("BuildingIndustries"), 2, steel = 2).expect("PROD[2 S, -E]")
      // You played Satellites
      // You gained 5 M€ production
      playProject(cn("Satellites"), 1, titanium = 3).expect("PROD[5 M]")
      // You used Sell Patents standard project
      // You sold 2 patents
      sellPatents(2)

      // You passed
      // You placed ocean tile at 11
      // You acted as World Government and placed an ocean
      // You gained 1 energy production
      // You added 1 Hydroelectric resource to Neptunian Power Consultants
      // Generation 9
      // You bought 4 card(s)
      // You bought Mineral Deposit,Fuel Factory,Potatoes,Business Network
      pass()
      doTask("OceanTile<Elysium_2_4>! BY Engine")
      doTask("NeptunianOption")
      pay(1, steel = 2)
      buyCards(4)

      // You used AI Central action
      // You drew 2 card(s)
      // You drew Industrial Center,Big Asteroid
      cardAction1(AiCentral).expect("2 ProjectCard")
      // You used Steelworks action
      // You gained 2 steel
      cardAction1(Steelworks).expect("-4 E, 2 S, TR")
      // You used Rotator Impacts action
      // You removed 1 resource(s) from You's Rotator Impacts
      cardAction2(RotatorImpacts).expect("-Asteroid, TR")
      // You used Convert Heat standard action
      stdAction("ConvertHeatSA").expect("TR")
      // You played Solarnet
      // You drew 2 card(s)
      // You drew Methane From Titan,Weather Balloons
      playProject(cn("Solarnet"), 7).expect("ProjectCard")
      // You used Extremophiles action
      // You added 1 Microbe to Sulphur-Eating Bacteria
      cardAction1(Extremophiles) { doTask("Microbe<$SulphurEatingBacteria>") }
      // You used Sulphur-Eating Bacteria action
      // You removed 5 microbes from Sulphur-Eating Bacteria to gain 15 M€
      cardAction2(SulphurEatingBacteria) {
            doTask("-5 Microbe<$SulphurEatingBacteria> THEN 15 M")
          }
          .expect("-5 Microbe<$SulphurEatingBacteria>, 15 M")
      // You played Mineral Deposit
      // You gained 5 steel
      // You gained 3 M€
      playProject(cn("MineralDeposit"), 5).expect("5 S, -2 M")
      // You played Geothermal Power
      // You gained 2 energy production
      playProject(cn("GeothermalPower"), 1, steel = 5).expect("PROD[2 E]")
      // You played Fuel Factory
      // You gained 1 M€ production
      // You gained 1 titanium production
      // You lost 1 energy production
      playProject(cn("FuelFactory"), steel = 3).expect("PROD[M, T, -E]")
      // You played Business Network
      // You lost 1 M€ production
      val BusinessNetwork = cn("BusinessNetwork")
      playProject(BusinessNetwork, 4).expect("PROD[-M]")
      // You used Business Network action
      // You bought 1 card(s)
      // You bought Urbanized Area
      cardAction1(BusinessNetwork) { buyCards(1) }
      // You used Sell Patents standard project
      // You sold 2 patents
      sellPatents(2)
      // You played Big Asteroid
      // You gained 4 titanium
      // You gained 1 heat production
      // You gained 3 M€
      playProject(cn("BigAsteroid"), 15, titanium = 4) {
            doTask("-4 Plant<SoloOpponent>")
          }
          .expect("0 T, PROD[H], 2 TR, -12 M")
      // You played Static Harvesting
      // You gained 1 energy production
      // You gained 7 M€
      playProject(cn("StaticHarvesting"), 5).expect("PROD[E], 2 M")
      // You played Protected Growth
      // You gained 6 plants
      // You gained 3 M€
      playProject(cn("ProtectedGrowth"), 2).expect("6 P, M")
      // You used Sell Patents standard project
      // You sold 1 patents
      sellPatents(1)
      // You used Convert Plants standard action
      // You placed greenery tile at 28
      // You gained 1 plant
      // You gained 1 steel
      // You gained 2 M€ from 1 ocean(s)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Elysium_4_8>") }
          .expect("-7 P, S, 2 M, TR")
      // You played Regolith Eaters
      val RegolithEaters = cn("RegolithEaters")
      playProject(RegolithEaters, 13)
      // You used Regolith Eaters action
      // You added 1 Microbe to Regolith Eaters
      cardAction1(RegolithEaters).expect("Microbe<$RegolithEaters>")
      // You played Soil Enrichment
      // You removed 1 resource(s) from You's Regolith Eaters
      // You removed 1 microbe from Regolith Eaters to gain 5 plants
      // You gained 3 M€
      playProject(cn("SoilEnrichment"), 6).expect("-Microbe<$RegolithEaters>, 5 P, -3 M")
      // You used Convert Plants standard action
      // You placed greenery tile at 36
      // You gained 2 plants
      // You gained 2 M€ from 1 ocean(s)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Elysium_5_8>") }.expect("-6 P, 2 M, TR")
      // You used Sell Patents standard project
      // You sold 1 patents
      sellPatents(1)
      // You played Worms
      // You gained 2 plant production
      playProject(cn("Worms"), 8).expect("PROD[2 P<Me>]")

      // You passed
      // You placed ocean tile at 24
      // You acted as World Government and placed an ocean
      // You gained 1 energy production
      // You added 1 Hydroelectric resource to Neptunian Power Consultants
      // Generation 10
      // You bought 4 card(s)
      // You bought Carbonate Processing,Directed Heat Usage,Pioneer Settlement,Interstellar Colony
      // Ship
      pass()
      doTask("OceanTile<Elysium_4_4>! BY Engine")
      doTask("NeptunianOption")
      pay(1, steel = 2)
      buyCards(4)

      // You used Convert Heat standard action
      stdAction("ConvertHeatSA").expect("TR")
      // You used AI Central action
      // You drew 2 card(s)
      // You drew Medical Lab,Black Polar Dust
      cardAction1(AiCentral).expect("2 ProjectCard")
      // You used Business Network action
      // You bought 1 card(s)
      // You bought Air-Scrapping Expedition
      cardAction1(BusinessNetwork) { buyCards(1) }
      // You used Steelworks action
      // You gained 2 steel
      cardAction1(Steelworks).expect("-4 E, 2 S, TR")
      // You used Regolith Eaters action
      // You added 1 Microbe to Regolith Eaters
      cardAction1(RegolithEaters).expect("Microbe<$RegolithEaters>")
      // You used Sulphur-Eating Bacteria action
      // You added 1 Microbe to Sulphur-Eating Bacteria
      cardAction1(SulphurEatingBacteria).expect("Microbe<$SulphurEatingBacteria>")
      // You used Extremophiles action
      // You added 1 Microbe to Regolith Eaters
      cardAction1(Extremophiles) { doTask("Microbe<$RegolithEaters>") }
      // You used Rotator Impacts action
      // You added 1 Asteroid to Rotator Impacts
      cardAction1(RotatorImpacts) { pay(titanium = 2) }.expect("Asteroid")
      // You played Shuttles
      // You gained 2 M€ production
      // You lost 1 energy production
      playProject(cn("Shuttles"), 1, titanium = 3).expect("PROD[2 M, -E]")
      // You played Carbonate Processing
      // You lost 1 energy production
      // You gained 3 heat production
      playProject(cn("CarbonateProcessing"), steel = 3).expect("PROD[-E, 3 H]")
      // You spent 3 energy to trade with Pluto
      // You drew 3 card(s)
      // You drew Venus Governor,Imported GHG,Olympus Conference
      stdAction("TradeSA", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 3 ProjectCard")
      // You played Olympus Conference
      // You added 1 Science to Olympus Conference
      val OlympusConference = cn("OlympusConference")
      playProject(OlympusConference, 4, steel = 3).expect("Science<$OlympusConference>")
      // You played Imported GHG
      // You gained 1 heat production
      // You gained 3 heat
      // You gained 3 M€
      playProject(cn("ImportedGhg"), 2, titanium = 1).expect("PROD[H], 3 H, M")
      // You used Convert Heat standard action
      stdAction("ConvertHeatSA").expect("TR")
      // You played Heather
      // You gained 1 plant production
      // You gained 1 plant
      playProject(cn("Heather"), 6).expect("PROD[P], P")
      // You used Sell Patents standard project
      // You sold 3 patents
      sellPatents(3)
      // You played Forced Precipitation
      val ForcedPrecipitation = cn("ForcedPrecipitation")
      playProject(ForcedPrecipitation, 8)
      // You used Sell Patents standard project
      // You sold 1 patents
      sellPatents(1)
      // You played Air-Scrapping Expedition
      // You added 3 Floater(s) to Forced Precipitation
      // You gained 3 M€
      playProject(cn("AirScrappingExpedition"), 13) {
            doTask("3 Floater<$ForcedPrecipitation>")
          }
          .expect("3 Floater<$ForcedPrecipitation>, -10 M")
      // You used Forced Precipitation action
      // You removed 2 resource(s) from You's Forced Precipitation
      cardAction2(ForcedPrecipitation).expect("-2 Floater<$ForcedPrecipitation>, TR")
      // You played Trans-Neptune Probe
      // You removed a resource from Olympus Conference to draw a card
      // You drew 1 card(s)
      // You drew Atalanta Planitia Lab
      playProject(cn("TransNeptuneProbe"), 1, titanium = 1) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }

      // You passed
      // You acted as World Government and increased oxygen level
      // Generation 11
      // You bought 0 card(s)
      nextRound("OxygenStep", 0)

      // You used AI Central action
      // You drew 2 card(s)
      // You drew Trees,Minority Refuge
      cardAction1(AiCentral).expect("2 ProjectCard")
      // You used Business Network action
      // You bought 1 card(s)
      // You bought Towing A Comet
      cardAction1(BusinessNetwork) { buyCards(1) }
      // You used Convert Heat standard action
      stdAction("ConvertHeatSA").expect("TR")
      // You used Rotator Impacts action
      // You removed 1 resource(s) from You's Rotator Impacts
      cardAction2(RotatorImpacts).expect("-Asteroid, 2 TR")
      // You used Steelworks action
      // You gained 2 steel
      cardAction1(Steelworks).expect("-4 E, 2 S, TR")
      // You spent 3 energy to trade with Luna
      // You gained 17 M€
      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }.expect("-3 E, 17 M")
      // You used Regolith Eaters action
      // You removed 2 resource(s) from You's Regolith Eaters
      cardAction2(RegolithEaters).expect("-2 Microbe<$RegolithEaters>, 2 TR")
      // You used Extremophiles action
      // You added 1 Microbe to Regolith Eaters
      cardAction1(Extremophiles) { doTask("Microbe<$RegolithEaters>") }
      // You used Sulphur-Eating Bacteria action
      // You added 1 Microbe to Sulphur-Eating Bacteria
      cardAction1(SulphurEatingBacteria).expect("Microbe<$SulphurEatingBacteria>")
      // You played Towing A Comet
      // You gained 2 plants
      // You placed ocean tile at 26
      // You gained 1 plant
      // You gained 4 M€ from 2 ocean(s)
      // You gained 1 energy production
      // You added 1 Hydroelectric resource to Neptunian Power Consultants
      // You gained 3 M€
      playProject(cn("TowingAComet"), titanium = 7) {
        doTask("OceanTile<Elysium_4_6>")
        doTask("NeptunianOption")
        pay(1, steel = 2)
      }
      // You played Black Polar Dust
      // You lost 2 M€ production
      // You gained 3 heat production
      // You placed ocean tile at 18
      // You gained 1 plant
      // You gained 6 M€ from 3 ocean(s)
      // You declined to use the Neptunian Power Consultants effect
      playProject(cn("BlackPolarDust"), 15) {
            doTask("OceanTile<Elysium_3_5>")
            doTask("Ok")
          }
          .expect("PROD[-2 M, 3 H], P, -9 M")
      // You used Convert Plants standard action
      // You placed greenery tile at 35
      // You gained 2 plants
      // You gained 4 M€ from 2 ocean(s)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Elysium_5_7>") }.expect("-6 P, 4 M, TR")
      // You used Convert Plants standard action
      // You placed greenery tile at 34
      // You gained 3 plants
      // You gained 2 M€ from 1 ocean(s)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Elysium_5_6>") }.expect("-5 P, 2 M, TR")
      // You used Forced Precipitation action
      // You added 1 Floater to Forced Precipitation
      cardAction1(ForcedPrecipitation).expect("Floater<$ForcedPrecipitation>")
      // You played Harvest
      // You gained 12 M€
      // You gained 3 M€
      playProject(cn("Harvest"), 4).expect("11 M")
      // You played Atalanta Planitia Lab
      // You drew 2 card(s)
      // You drew Trade Envoys,Rego Plastics
      // You added 1 Science to Olympus Conference
      playProject(cn("AtalantaPlanitiaLab"), 10).expect("ProjectCard, Science<$OlympusConference>")
      // You played Restricted Area
      // You removed a resource from Olympus Conference to draw a card
      // You drew 1 card(s)
      // You drew Extractor Balloons
      // You placed Restricted Area tile at 17
      // You gained 6 M€ from 3 ocean(s)
      val RestrictedArea = cn("RestrictedArea")
      playProject(RestrictedArea, 11) {
            doTask("ProjectCard FROM Science<$OlympusConference>")
            doTask("Tile199<Elysium_3_4>")
          }
          .expect("-5 M")
      // You used Restricted Area action
      // You drew 1 card(s)
      // You drew Thermophiles
      cardAction1(RestrictedArea).expect("ProjectCard")
      // You used Sell Patents standard project
      // You sold 5 patents
      sellPatents(5)
      // You used Asteroid:SP standard project
      stdProject("AsteroidSP").expect("TR")
      // You used Asteroid:SP standard project
      stdProject("AsteroidSP").expect("TR")
      // You used Asteroid:SP standard project
      stdProject("AsteroidSP").expect("TR")
      // You played Trees
      // You gained 3 plant production
      // You gained 1 plant
      playProject(cn("Trees"), 13).expect("PROD[3 P], P")

      // You passed
      // You acted as World Government and increased Venus scale
      // Generation 12
      // You bought 1 card(s)
      // You bought Inventors' Guild
      nextRound("VenusStep", 1)

      // You used Convert Heat standard action
      stdAction("ConvertHeatSA").expect("TR")
      // You used Convert Heat standard action
      stdAction("ConvertHeatSA") {
            // You placed ocean tile at 05
            // You drew 1 card(s)
            // You drew Windmills
            // You gained 2 M€ from 1 ocean(s)
            doTask("OceanTile<Elysium_1_3>")
            // You declined to use the Neptunian Power Consultants effect
            doTask("Ok")
          }
          .expect("ProjectCard, 2 M, 2 TR")
      // You used Convert Plants standard action
      // You placed greenery tile at 25
      // You gained 1 plant
      // You gained 6 M€ from 3 ocean(s)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Elysium_4_5>") }.expect("-7 P, 6 M, TR")
      // You used Restricted Area action
      // You drew 1 card(s)
      // You drew Adaptation Technology
      cardAction1(RestrictedArea).expect("ProjectCard")
      // You used AI Central action
      // You drew 2 card(s)
      // You drew Space Mirrors,Fusion Power
      cardAction1(AiCentral).expect("2 ProjectCard")
      // You used Business Network action
      // You bought 1 card(s)
      // You bought Business Contacts
      cardAction1(BusinessNetwork) { buyCards(1) }
      // You used Forced Precipitation action
      // You removed 2 resource(s) from You's Forced Precipitation
      cardAction2(ForcedPrecipitation).expect("-2 Floater<$ForcedPrecipitation>, TR")
      // You used Steelworks action
      // You gained 2 steel
      cardAction1(Steelworks).expect("-4 E, 2 S, TR")
      // You used Extremophiles action
      // You added 1 Microbe to Regolith Eaters
      cardAction1(Extremophiles) { doTask("Microbe<$RegolithEaters>") }
      // You used Regolith Eaters action
      // You removed 2 resource(s) from You's Regolith Eaters
      cardAction2(RegolithEaters).expect("-2 Microbe<$RegolithEaters>, TR")
      // You played Rego Plastics
      playProject(cn("RegoPlastics"), steel = 5)
      // You played Business Contacts
      // You drew 2 card(s)
      // You drew Bribed Committee,Lava Flows
      // You gained 3 M€
      playProject(cn("BusinessContacts"), 7).expect("ProjectCard, -4 M")
      // You played Bribed Committee
      // You gained 3 M€
      playProject(cn("BribedCommittee"), 7).expect("-4 M, 2 TR")
      // You used Sell Patents standard project
      // You sold 2 patents
      sellPatents(2)
      // You played Lava Flows
      // You placed Lava Flows tile at 14
      // You gained 2 titanium
      // You gained 3 M€
      playProject(cn("LavaFlows"), 18) { doTask("Tile140<Elysium_3_1>") }.expect("2 T, 2 TR, -15 M")
      // You played Interstellar Colony Ship
      // You gained 3 M€
      playProject(cn("InterstellarColonyShip"), 1, titanium = 7).expect("2 M")
      // You spent 3 energy to trade with Pluto
      // You drew 2 card(s)
      // You drew Jovian Embassy,Bactoviral Research
      stdAction("TradeSA", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 2 ProjectCard")
      // You played Fusion Power
      // You gained 3 energy production
      // You added 1 Science to Olympus Conference
      playProject(cn("FusionPower"), 2, steel = 4).expect("PROD[3 E], Science<$OlympusConference>")
      // You played Bactoviral Research
      // You drew 1 card(s)
      // You drew Beam From A Thorium Asteroid
      // You removed a resource from Olympus Conference to draw a card
      // You drew 1 card(s)
      // You drew Release of Inert Gases
      // You added 13 Microbe(s) to Sulphur-Eating Bacteria
      playProject(cn("BactoviralResearch"), 10) {
            doTask("ProjectCard FROM Science<$OlympusConference>")
            doTask("13 Microbe<$SulphurEatingBacteria>")
          }
          .expect("ProjectCard, 13 Microbe<$SulphurEatingBacteria>")
      // You played Release of Inert Gases
      // You gained 3 M€
      playProject(cn("ReleaseOfInertGases"), 14).expect("2 TR, -11 M")
      // You used Sulphur-Eating Bacteria action
      // You removed 15 microbes from Sulphur-Eating Bacteria to gain 45 M€
      cardAction2(SulphurEatingBacteria) {
            doTask("-15 Microbe<$SulphurEatingBacteria> THEN 45 M")
          }
          .expect("-15 Microbe<$SulphurEatingBacteria>, 45 M")
      // You played Jovian Embassy
      playProject(cn("JovianEmbassy"), 11, steel = 1)
      // You used Sell Patents standard project
      // You sold 4 patents
      sellPatents(4)
      // You used Asteroid:SP standard project
      stdProject("AsteroidSP").expect("TR")
      // You used Asteroid:SP standard project
      stdProject("AsteroidSP").expect("TR")
      // You used Asteroid:SP standard project
      stdProject("AsteroidSP").expect("0 TR")
      // You passed
      // Final greenery placement
      // Final greenery phase is skipped since you did not complete the win condition.
      pass()

      // Final state and score come from /api/player?id=p7b3c852a63cc.
      assertProduction(m = 10, s = 4, t = 5, p = 6, e = 11, h = 13)
      assertResources(m = 81, s = 4, t = 5, p = 9, e = 11, h = 14)
      assertCounts(58 to "TerraformRating", 0 to "ProjectCard")
      assertCounts(5 to "Hydroelectric<$NeptunianPowerConsultants>")
      assertSidebar(gen = 12, temp = 8, oxygen = 14, oceans = 7, venus = 20)
      has("Victory") shouldBe false

      engine.phase("End")
      val sum = Summarizer(game)
      sum.net("GreeneryTile", "VictoryPoint<Me>") shouldBe 5
      sum.net("CityTile", "VictoryPoint<Me>") shouldBe 1
      sum.net("Card", "VictoryPoint<Me>") shouldBe 19
      assertCounts(83 to "VictoryPoint")

      // This archive was cloned from the same source game after completion.
      // This game was a clone from game #g5ce0a315821d
    }
  }
}
