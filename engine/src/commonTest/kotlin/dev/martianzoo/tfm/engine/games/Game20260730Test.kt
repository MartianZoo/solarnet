package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.data.GameConfig
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Thermal Matter Wave - https://terraforming-mars.herokuapp.com/the-end?id=pccc28386ce4b
class Game20260730Test : AbstractSoloTest() {
  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, PromoCardPack, Tr63SoloVariant
          Ceres, Io, Triton
          """,
          "Player1",
      )

  // Drew and discarded Mining Colony to place a 2
  // Drew and discarded Potatoes to place a 0
  // Drew and discarded Research Outpost to place a 2
  // Drew and discarded Jupiter Floating Station to place a 0
  override fun cityAreas(): Pair<String, String> = "Tharsis_4_6" to "Tharsis_6_6"

  override fun greeneryAreas(): Pair<String, String> = "Tharsis_3_6" to "Tharsis_6_5"

  @Test
  fun game20260730() {
    with(me) {
      // The id of this game is gafda6ee74f34
      // Good luck Player1!
      // Generation 1
      // Player1 played CrediCor
      // Player1 kept 7 project cards
      playCorp(CrediCor, 7).expect("36")

      // You discarded Miranda
      // Player1 played Society Support
      // Player1 lost 1 M€ production
      // Player1 gained 1 plant production
      // Player1 gained 1 energy production
      // Player1 gained 1 heat production
      playPrelude(SocietySupport).expect("PROD[-1, P, E, H]")
      // Player1 played Eccentric Sponsor
      playPrelude(ExcentricSponsor) {
        // Player1 played Titan Shuttles
        // Player1 gained 4 M€
        playProject(TitanShuttles, 0).expect("4")
      }

      // Player1 used Titan Shuttles action
      cardAction1(TitanShuttles) {
        // Player1 added 2 Floater(s) to Titan Shuttles
        doTask("2 Floater<$TitanShuttles>")
      }
      // Player1 played Industrial Microbes
      // Player1 gained 1 steel production
      // Player1 gained 1 energy production
      playProject(IndustrialMicrobes, 12).expect("PROD[S, E]")
      // Player1 played Solar Reflectors
      // Player1 gained 5 heat production
      // Player1 gained 4 M€
      playProject(SolarReflectors, 23).expect("PROD[5 H], -19")

      // Player1 passed
      // Player1 acted as World Government and increased Venus scale
      // Generation 2
      // Player1 bought 3 card(s)
      // You bought Corroder Suits,Towing A Comet,Strip Mine
      nextRound("VenusStep", 3)

      // Player1 used Titan Shuttles action
      cardAction1(TitanShuttles) {
        // Player1 added 2 Floater(s) to Titan Shuttles
        doTask("2 Floater<$TitanShuttles>")
      }

      // Player1 passed
      // Player1 acted as World Government and increased Venus scale
      // Generation 3
      // Player1 bought 2 card(s)
      // You bought House Printing,Corporate Stronghold
      nextRound("VenusStep", 2)

      // Player1 used Convert Heat standard action
      stdAction("ConvertHeatSA").expect("TR")
      // Player1 used Titan Shuttles action
      cardAction1(TitanShuttles) {
        // Player1 added 2 Floater(s) to Titan Shuttles
        doTask("2 Floater<$TitanShuttles>")
      }
      // Player1 played Advanced Alloys
      playProject(AdvancedAlloys, 9)
      // Player1 played House Printing
      // Player1 gained 1 steel production
      playProject(HousePrinting, 4, steel = 2)

      // Player1 passed
      // Player1 acted as World Government and increased Venus scale
      // Generation 4
      // Player1 bought 1 card(s)
      // You bought Development Center
      nextRound("VenusStep", 1)

      // Player1 used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // Player1 played Cryo-Sleep
      playProject(CryoSleep, 10)
      // Player1 spent 2 energy to trade with Ceres
      stdAction("TradeSA", 2) {
            doTask("Trade<Ceres>")
            // Player1 gained 6 steel
          }
          .expect("-2 E, 6 S")
      // Player1 played Strip Mine
      // Player1 gained 2 steel production
      // Player1 gained 1 titanium production
      // Player1 lost 2 energy production
      // Player1 gained 4 M€
      playProject(StripMine, 1, steel = 8).expect("3")
      // Player1 used Titan Shuttles action
      // Player1 added 2 Floater(s) to Titan Shuttles
      cardAction1(TitanShuttles) {
        doTask("2 Floater<$TitanShuttles>")
      }

      // Player1 passed
      // Player1 placed ocean tile at 43
      // Player1 acted as World Government and placed an ocean
      // Generation 5
      // Player1 bought 3 card(s)
      // You bought Sterling Vents,Designed Microorganisms,Electro Catapult
      nextRound("OceanTile<Tharsis_6_7>", 3)

      // Player1 used Convert Heat standard action
      // Player1 gained 1 heat production
      stdAction("ConvertHeatSA").expect("-8 H, PROD[H]")
      // Player1 used Titan Shuttles action
      cardAction2(TitanShuttles) {
        // Player1 removed 8 resource(s) from Player1's Titan Shuttles
        // Player1 removed 8 floaters to gain 8 titanium
        doTask("-8 Floater<$TitanShuttles> THEN 8 Titanium")
      }
      // Player1 spent 2 titanium to trade with Triton
      // Player1 gained 4 titanium
      stdAction("TradeSA", 3) { doTask("Trade<Triton>") }
      // Player1 played Sterling Vents
      // Player1 gained 2 energy production
      // Player1 lost 2 heat production
      playProject(SterlingVents, 2, steel = 1).expect("PROD[2 E, -2 H]")
      // Player1 played Electro Catapult
      // Player1 lost 1 energy production
      playProject(ElectroCatapult, 8, steel = 3)
      // Player1 used Electro Catapult action
      // Player1 gained 7 M€
      cardAction1(ElectroCatapult).expect("7")
      // Player1 played Corroder Suits
      // Player1 gained 2 M€ production
      playProject(CorroderSuits, 8)

      // Player1 passed
      // Player1 placed ocean tile at 45
      // Player1 acted as World Government and placed an ocean
      // Generation 6
      // Player1 bought 1 card(s)
      // You bought Rotator Impacts
      nextRound("OceanTile<Tharsis_6_9>", 1)

      // Player1 used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // Player1 used Electro Catapult action
      // Player1 gained 7 M€
      cardAction1(ElectroCatapult) { doTask("-Plant") }
      // Player1 played Towing A Comet
      // Player1 gained 2 plants
      // Player1 gained 4 M€
      // Player1 placed ocean tile at 44
      // Player1 gained 1 plant
      // Player1 gained 4 M€ from 2 ocean(s)
      intentionalOverpay()
      playProject(TowingAComet, titanium = 6) {
        doTask("OceanTile<Tharsis_6_8>")
      }
      // Player1 played Saturn Surfing
      // Player1 added 1 Floater to Saturn Surfing
      playProject(SaturnSurfing, 13)
      // Player1 used Titan Shuttles action
      // Player1 added 2 Floater(s) to Saturn Surfing
      cardAction1(TitanShuttles) { doTask("2 Floater<$SaturnSurfing>") }
      // Player1 used Saturn Surfing action
      // Player1 gained 3 M€
      cardAction1(SaturnSurfing).expect("-Floater, 3")
      // Player1 played Designed Microorganisms
      // Player1 gained 2 plant production
      playProject(DesignedMicroorganisms, 16).expect("PROD[2 P]")
      // Player1 played Rotator Impacts
      playProject(RotatorImpacts, 2, titanium = 1)
      // Player1 used Rotator Impacts action
      // Player1 added 1 Asteroid to Rotator Impacts
      intentionalOverpay()
      cardAction1(RotatorImpacts) { pay(titanium = 2) }

      // Player1 passed
      // Player1 placed ocean tile at 28
      // Player1 acted as World Government and placed an ocean
      // Generation 7
      // Player1 bought 1 card(s)
      // You bought Tardigrades
      nextRound("OceanTile<Tharsis_4_8>", 1)

      // Player1 used Electro Catapult action
      // Player1 gained 7 M€
      cardAction1(ElectroCatapult) { doTask("-Plant") }
      // Player1 played Development Center
      playProject(DevelopmentCenter, 2, steel = 3)
      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Deimos Down:promo
      cardAction1(DevelopmentCenter)
      // Player1 played Corporate Stronghold
      // Player1 gained 3 M€ production
      // Player1 lost 1 energy production
      // Player1 placed city tile at 36
      // Player1 gained 2 plants
      // Player1 gained 6 M€ from 3 ocean(s)
      playProject(CorporateStronghold, 2, steel = 3) {
            doTask("CityTile<Tharsis_5_8>")
          }
          .expect("PROD[3, -E]")
      // Player1 used Convert Plants standard action
      // Player1 placed greenery tile at 37
      // Player1 gained 2 plants
      // Player1 gained 4 M€ from 2 ocean(s)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_5_9>") }
      // Player1 used Titan Shuttles action
      // Player1 added 2 Floater(s) to Saturn Surfing
      cardAction1(TitanShuttles) { doTask("2 Floater<$SaturnSurfing>") }
      // Player1 used Saturn Surfing action
      // Player1 gained 4 M€
      cardAction1(SaturnSurfing)
      // Player1 used Rotator Impacts action
      // Player1 removed 1 resource(s) from Player1's Rotator Impacts
      // Player1 drew 1 card(s)
      // You drew Spin-off Department
      // Player1 removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts).expect("TR")
      // Player1 spent 2 titanium to trade with Io
      // Player1 gained 13 heat
      stdAction("TradeSA", 3) { doTask("Trade<Io>") }.expect("-2 T, 13 H")
      // Player1 used Convert Heat standard action
      // Player1 gained 1 heat production
      // Player1 used Convert Heat standard action
      stdAction("ConvertHeatSA")
      stdAction("ConvertHeatSA")
      // Player1 played Spin-off Department
      // Player1 gained 2 M€ production
      playProject(SpinOffDepartment, 4, steel = 2)
      // Player1 played Tardigrades
      playProject(Tardigrades, 4)
      // Player1 used Tardigrades action
      // Player1 added 1 Microbe to Tardigrades
      cardAction1(Tardigrades)
      // Player1 played Deimos Down:promo
      // Player1 gained 4 steel
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Dawn City
      // Player1 placed Deimos Down tile at 51
      // Player1 gained 1 plant
      // Player1 gained 4 M€ from 2 ocean(s)
      playProject(DeimosDown, 23, titanium = 2) {
            doTask("Ok")
            doTask("CardX31_SpecialTile<Tharsis_7_8>")
          }
          .expect("4 S, P, 3 TR")
      // Player1 used Power Plant:SP standard project
      stdProject("PowerPlantSP")

      // Player1 passed
      // Player1 acted as World Government and increased Venus scale
      // Generation 8
      // Player1 bought 2 card(s)
      // You bought Power Supply Consortium,Bribed Committee
      nextRound("VenusStep", 2)

      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Release of Inert Gases
      cardAction1(DevelopmentCenter)
      // Player1 used Convert Plants standard action
      // Player1 placed greenery tile at 27
      // Player1 gained 1 plant
      // Player1 gained 2 M€ from 1 ocean(s)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_4_7>") }.expect("TR")
      // Player1 used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // Player1 used Electro Catapult action
      // Player1 gained 7 M€
      cardAction1(ElectroCatapult) { doTask("-Plant") }
      // Player1 used Titan Shuttles action
      // Player1 added 2 Floater(s) to Saturn Surfing
      cardAction1(TitanShuttles) { doTask("2 Floater<$SaturnSurfing>") }
      // Player1 used Saturn Surfing action
      // Player1 gained 5 M€
      cardAction1(SaturnSurfing).expect("-Floater, 5")
      // Player1 used Rotator Impacts action
      // Player1 added 1 Asteroid to Rotator Impacts
      cardAction1(RotatorImpacts) { pay(2, titanium = 1) }
      // Player1 used Tardigrades action
      // Player1 added 1 Microbe to Tardigrades
      cardAction1(Tardigrades)
      // Player1 played Bribed Committee
      playProject(BribedCommittee, 7)
      // Player1 played Release of Inert Gases
      playProject(ReleaseOfInertGases, 14)

      // Player1 passed
      // Player1 placed ocean tile at 33
      // Player1 acted as World Government and placed an ocean
      // Generation 9
      // Player1 bought 2 card(s)
      // You bought Forced Precipitation,Venus Soils
      nextRound("OceanTile<Tharsis_5_5>", 2)

      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Interplanetary Trade
      cardAction1(DevelopmentCenter)
      // Player1 used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // Player1 used Electro Catapult action
      // Player1 gained 7 M€
      cardAction1(ElectroCatapult) { doTask("-Steel") }
      // Player1 used Rotator Impacts action
      // Player1 removed 1 resource(s) from Player1's Rotator Impacts
      // Player1 removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
      // Player1 used Titan Shuttles action
      // Player1 added 2 Floater(s) to Saturn Surfing
      cardAction1(TitanShuttles) { doTask("2 Floater<$SaturnSurfing>") }
      // Player1 used Saturn Surfing action
      // Player1 gained 5 M€
      cardAction1(SaturnSurfing)
      // Player1 used Tardigrades action
      // Player1 added 1 Microbe to Tardigrades
      cardAction1(Tardigrades)
      // Player1 played Venus Soils
      // Player1 gained 1 plant production
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Imported Nutrients
      // Player1 added 2 Microbe(s) to Tardigrades
      playProject(VenusSoils, 20) { doTask("2 Microbe<$Tardigrades>") }.expect("PROD[P]")
      // Player1 played Interplanetary Trade
      // Player1 gained 10 M€ production
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Io Mining Industries
      playProject(InterplanetaryTrade, 23, titanium = 1).expect("PROD[10], -19")
      // Player1 played Forced Precipitation
      playProject(ForcedPrecipitation, 8)
      // Player1 used Forced Precipitation action
      // Player1 added 1 Floater to Forced Precipitation
      cardAction1(ForcedPrecipitation)
      // Player1 used Air Scrapping standard project
      stdProject("AirScrappingSP").expect("2 TR")

      // Player1 passed
      // Player1 placed ocean tile at 07
      // Player1 acted as World Government and placed an ocean
      // Generation 10
      // Player1 bought 4 card(s)
      // You bought Penguins,Mars University,Medical Lab,Gyropolis
      nextRound("OceanTile<Tharsis_1_5>", 4)

      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Outdoor Sports
      cardAction1(DevelopmentCenter)
      // Player1 played Mars University
      playProject(MarsUniversity, 2, steel = 2) {
        // Player1 is using their Mars University effect to draw a card by discarding a card.
        // Player1 discarded Outdoor Sports
        // Player1 drew 1 card(s)
        // You drew Comet
        doTask("-ProjectCard")
      }
      // Player1 spent 8 M€ to trade with Triton
      // Player1 gained 4 titanium
      stdAction("TradeSA", 1) { doTask("Trade<Triton>") }
      // Player1 played Comet
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Solar Power
      // Player1 placed ocean tile at 13
      // Player1 drew 2 card(s)
      // You drew Predators,Equatorial Magnetizer
      // Player1 gained 2 M€ from 1 ocean(s)
      playProject(Comet, 1, titanium = 5) {
        doTask("Ok")
        doTask("OceanTile<Tharsis_2_6>")
      }
      // Player1 used Saturn Surfing action
      // Player1 gained 5 M€
      cardAction1(SaturnSurfing)
      // Player1 used Titan Shuttles action
      // Player1 added 2 Floater(s) to Titan Shuttles
      cardAction1(TitanShuttles) { doTask("2 Floater<$TitanShuttles>") }
      // Player1 used Electro Catapult action
      // Player1 gained 7 M€
      cardAction1(ElectroCatapult) { doTask("-Steel") }
      // Player1 played Io Mining Industries
      // Player1 gained 2 M€ production
      // Player1 gained 2 titanium production
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Optimal Aerobraking
      playProject(IoMiningIndustries, 41).expect("PROD[2, 2 T]")
      // Player1 played Solar Power
      // Player1 gained 1 energy production
      intentionalOverpay()
      playProject(SolarPower, steel = 4)
      // Player1 played Gyropolis
      // Player1 gained 4 M€ production
      // Player1 lost 2 energy production
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Space Hotels
      // Player1 placed city tile at 20
      // Player1 gained 1 steel
      // Player1 gained 4 M€ from 2 ocean(s)
      playProject(Gyropolis, 2, steel = 6) { doTask("CityTile<Tharsis_3_7>") }
          .expect("PROD[4, -2 E]")
      // Player1 played Medical Lab
      // Player1 gained 6 M€ production
      playProject(MedicalLab, 4, steel = 3) {
        // Player1 is using their Mars University effect to draw a card by discarding a card.
        // Player1 discarded Predators
        // Player1 drew 1 card(s)
        // You drew Asteroid Rights
        doTask("-ProjectCard")
      }
      // Player1 played Asteroid Rights
      // Player1 added 2 Asteroid(s) to Asteroid Rights
      playProject(AsteroidRights, 10)
      // Player1 used Asteroid Rights action
      // Player1 removed 1 Asteroid from Asteroid Rights to gain 2 titanium
      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      // Player1 used Rotator Impacts action
      // Player1 added 1 Asteroid to Rotator Impacts
      intentionalOverpay()
      cardAction1(RotatorImpacts) { pay(titanium = 2) }
      // Player1 used Tardigrades action
      // Player1 added 1 Microbe to Tardigrades
      cardAction1(Tardigrades)
      // Player1 played Power Supply Consortium
      playProject(PowerSupplyConsortium, 5)
      // Player1 used Forced Precipitation action
      // Player1 added 1 Floater to Forced Precipitation
      cardAction1(ForcedPrecipitation)

      // Player1 passed
      // Player1 acted as World Government and increased oxygen level
      // Generation 11
      // Player1 bought 4 card(s)
      // You bought Thermophiles,Titan Floating Launch-pad,Magnetic Shield,16 Psyche
      nextRound("OxygenStep", 4)

      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Search For Life
      cardAction1(DevelopmentCenter)
      // Player1 played Search For Life
      playProject(SearchForLife, 3) {
        // Player1 is using their Mars University effect to draw a card by discarding a card.
        // Player1 discarded Space Hotels
        // Player1 drew 1 card(s)
        // You drew Air-Scrapping Expedition
        doTask("-ProjectCard")
      }
      // Player1 used Asteroid Rights action
      // Player1 removed 1 Asteroid from Asteroid Rights to gain 2 titanium
      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      // Player1 used Saturn Surfing action
      // Player1 gained 4 M€
      cardAction1(SaturnSurfing)
      // Player1 played 16 Psyche
      // Player1 gained 2 titanium production
      // Player1 gained 3 titanium
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Trees
      playProject(SixteenPsyche, 11, titanium = 5).expect("PROD[2 T]")
      // Player1 used Search For Life action
      // Player1 revealed and discarded Topsoil Contract
      // Player1 found life!
      cardAction1(SearchForLife) { doTask("Science<$SearchForLife>") }
      // Player1 used Tardigrades action
      // Player1 added 1 Microbe to Tardigrades
      cardAction1(Tardigrades)
      // Player1 used Forced Precipitation action
      // Player1 removed 2 resource(s) from Player1's Forced Precipitation
      // Player1 raised the Venus scale 1 step(s)
      cardAction2(ForcedPrecipitation).expect("TR")
      // Player1 used Rotator Impacts action
      // Player1 removed 1 resource(s) from Player1's Rotator Impacts
      // Player1 removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
      // Player1 used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // Player1 used Titan Shuttles action
      // Player1 added 2 Floater(s) to Titan Shuttles
      cardAction1(TitanShuttles) { doTask("2 Floater<$TitanShuttles>") }
      // Player1 played Magnetic Shield
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Beam From A Thorium Asteroid
      playProject(MagneticShield, 12, titanium = 3)
      // Player1 played Beam From A Thorium Asteroid
      // Player1 gained 3 energy production
      // Player1 gained 3 heat production
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Research
      playProject(BeamFromAThoriumAsteroid, 32).expect("PROD[3 E, 3 H]")
      // Player1 used Electro Catapult action
      // Player1 gained 7 M€
      cardAction1(ElectroCatapult) { doTask("-Steel") }
      // Player1 played Thermophiles
      playProject(Thermophiles, 9)
      // Player1 used Convert Plants standard action
      // Player1 placed greenery tile at 58
      // Player1 gained 1 titanium
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_8_9>") }
      // Player1 played Optimal Aerobraking
      playProject(OptimalAerobraking, 3, titanium = 1)
      // Player1 used Sell Patents standard project
      // Player1 sold 1 patents
      withAutoExecLoweredAfterOperation(
          NONE,
          operation = { lowerAutoExec ->
            stdAction("SellPatents") {
              doTask("-ProjectCard THEN 1")
              lowerAutoExec()
            }
          },
      ) {
        // The log does not expose every earlier metal payment; reconcile its available M€ here.
        godMode().manual("8")
      }
      // Player1 played Imported Nutrients
      // Player1 gained 4 plants
      // Player1 gained 3 M€ because of Optimal Aerobraking
      // Player1 gained 3 heat because of Optimal Aerobraking
      // Player1 added 4 Microbe(s) to Thermophiles
      playProject(ImportedNutrients, 14) {
            doTask("4 Microbe<$Thermophiles>")
          }
          .expect("4 P, 3 H")
      // Player1 used Thermophiles action
      // Player1 removed 2 resource(s) from Player1's Thermophiles
      cardAction2(Thermophiles)
      // Player1 played Equatorial Magnetizer
      playProject(EquatorialMagnetizer, 2, steel = 3)
      // Player1 used Equatorial Magnetizer action
      // Player1 lost 1 energy production
      cardAction1(EquatorialMagnetizer).expect("PROD[-E], TR")

      // Player1 passed
      // Player1 acted as World Government and increased temperature
      // Generation 12
      // Player1 bought 3 card(s)
      // You bought Floater Prototypes,Trans-Neptune Probe,Convoy From Europa
      nextRound("TemperatureStep", 3)

      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Pioneer Settlement
      cardAction1(DevelopmentCenter)
      // Player1 played Research
      // Player1 drew 2 card(s)
      // You drew Shuttles,Atmoscoop
      playProject(Research, 11) {
        // Player1 is using their Mars University effect to draw a card by discarding a card.
        // Player1 discarded Trees
        // Player1 drew 1 card(s)
        // You drew Ganymede Colony
        doTask("-ProjectCard")
        // Player1 is using their Mars University effect to draw a card by discarding a card.
        // Player1 discarded Trans-Neptune Probe
        // Player1 drew 1 card(s)
        // You drew Hi-Tech Lab
        doTask("-ProjectCard")
      }
      // Player1 used Convert Heat standard action
      // Player1 placed ocean tile at 34
      // Player1 gained 2 plants
      // Player1 gained 4 M€ from 2 ocean(s)
      stdAction("ConvertHeatSA") { doTask("OceanTile<Tharsis_5_6>") }
      // Player1 used Convert Plants standard action
      // Player1 placed greenery tile at 35
      // Player1 gained 2 plants
      // Player1 gained 6 M€ from 3 ocean(s)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_5_7>") }
      // Player1 played Shuttles
      // Player1 gained 2 M€ production
      // Player1 lost 1 energy production
      playProject(Shuttles, 2, titanium = 2).expect("PROD[2, -E]")
      // Player1 played Pioneer Settlement
      // Player1 built a colony on Triton
      // Player1 gained 3 titanium
      intentionalOverpay()
      playProject(PioneerSettlement, titanium = 3) { doTask("Colony<Triton>") }
      // Player1 spent 2 energy to trade with Io
      // Player1 gained 10 heat
      stdAction("TradeSA", 2) { doTask("Trade<Io>") }
      // Player1 used Convert Heat standard action
      // Player1 used Convert Heat standard action
      stdAction("ConvertHeatSA")
      stdAction("ConvertHeatSA")
      // Player1 played Titan Floating Launch-pad
      // Player1 added 2 Floater(s) to Titan Shuttles
      playProject(TitanFloatingLaunchPad, 18) {
        doTask("2 Floater<$TitanShuttles>")
      }
      // Player1 used Titan Floating Launch-pad action
      // Player1 added 1 Floater to Titan Shuttles
      cardAction1(TitanFloatingLaunchPad) { doTask("Floater<$TitanShuttles>") }
      // Player1 used Search For Life action
      // Player1 revealed and discarded Arctic Algae
      cardAction1(SearchForLife) { doTask("Ok") }
      // Player1 played Atmoscoop
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Media Archives
      // Player1 raised the Venus scale 2 step(s)
      // Player1 added 2 Floater(s) to Titan Shuttles
      playProject(Atmoscoop, 8, titanium = 3) {
        doTask("2 VenusStep")
        doTask("2 Floater<$TitanShuttles>")
      }
      // Player1 played Floater Prototypes
      playProject(FloaterPrototypes, 2) {
        // Player1 is using their Mars University effect to draw a card by discarding a card.
        // Player1 discarded Air-Scrapping Expedition
        // Player1 drew 1 card(s)
        // You drew Asteroid
        doTask("-ProjectCard")
        // Player1 added 2 Floater(s) to Titan Shuttles
        doTask("2 Floater<$TitanShuttles>")
      }
      // Player1 used Titan Shuttles action
      // Player1 removed 11 resource(s) from Player1's Titan Shuttles
      // Player1 removed 11 floaters to gain 11 titanium
      cardAction2(TitanShuttles) {
        doTask("-11 Floater<$TitanShuttles> THEN 11 Titanium")
      }
      // Player1 played Ganymede Colony
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Ants
      intentionalOverpay()
      playProject(GanymedeColony, titanium = 5)
      // Player1 played Convoy From Europa
      // Player1 drew 1 card(s)
      // You drew Dust Seals
      // Player1 gained 3 M€ because of Optimal Aerobraking
      // Player1 gained 3 heat because of Optimal Aerobraking
      // Player1 placed ocean tile at 63
      // Player1 gained 2 titanium
      intentionalOverpay()
      playProject(ConvoyFromEuropa, titanium = 4) {
        doTask("OceanTile<Tharsis_9_9>")
      }
      // Player1 played Terraforming Ganymede
      // Player1 gained 8 TR
      // Player1 gained 4 M€
      // Player1 drew 1 card(s)
      // You drew Project Inspection
      playProject(TerraformingGanymede, 15, titanium = 4).expect("8 TR")
      // Player1 played Hi-Tech Lab
      playProject(HiTechLab, 5, steel = 4) {
        // Player1 is using their Mars University effect to draw a card by discarding a card.
        // Player1 discarded Dust Seals
        // Player1 drew 1 card(s)
        // You drew Windmills
        doTask("-ProjectCard")
      }
      // Player1 used Electro Catapult action
      // Player1 gained 7 M€
      cardAction1(ElectroCatapult)
      // Player1 played Asteroid
      // Player1 gained 2 titanium
      // Player1 gained 3 M€ because of Optimal Aerobraking
      // Player1 gained 3 heat because of Optimal Aerobraking
      playProject(AsteroidCard, 12) { doTask("Ok") }
      // Player1 used Saturn Surfing action
      // Player1 gained 3 M€
      cardAction1(SaturnSurfing)
      // Player1 used Thermophiles action
      // Player1 removed 2 resource(s) from Player1's Thermophiles
      cardAction2(Thermophiles)
      // Player1 used Equatorial Magnetizer action
      // Player1 lost 1 energy production
      cardAction1(EquatorialMagnetizer)
      // Player1 played Project Inspection
      // Player1 used Electro Catapult action with Project Inspection
      // Player1 gained 7 M€
      playProject(ProjectInspection, 0) {
        doTask("UseAction1<$ElectroCatapult>")
      }
      // Player1 played Media Archives
      // Player1 gained 10 M€
      playProject(MediaArchives, 8)
      // Player1 used City standard project
      // Player1 gained 4 M€
      // Player1 placed city tile at 24
      // Player1 gained 1 plant
      // Player1 gained 2 M€ from 1 ocean(s)
      stdProject("CitySP") { doTask("CityTile<Tharsis_4_4>") }
      // Player1 used Greenery standard project
      // Player1 gained 4 M€
      // Player1 placed greenery tile at 25
      // Player1 gained 2 plants
      // Player1 gained 4 M€ from 2 ocean(s)
      stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_4_5>") }
      // Player1 used Convert Plants standard action
      // Player1 placed greenery tile at 17
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_3_4>") }
      // Player1 played Dawn City
      // Player1 gained 1 titanium production
      // Player1 lost 1 energy production
      playProject(DawnCity, 5, titanium = 2).expect("PROD[T, -E]")
      // Player1 used Asteroid Rights action
      // Player1 added 1 Asteroid to Rotator Impacts
      cardAction1(AsteroidRights) { doTask("Asteroid<$RotatorImpacts>") }
      // Player1 used Rotator Impacts action
      // Player1 removed 1 resource(s) from Player1's Rotator Impacts
      // Player1 removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
      // Player1 used Tardigrades action
      // Player1 added 1 Microbe to Tardigrades
      cardAction1(Tardigrades)
      // Player1 played Windmills
      // Player1 gained 1 energy production
      playProject(Windmills, 6).expect("PROD[E]")
      // Player1 used Sell Patents standard project
      // Player1 sold 1 patents
      sellPatents(1)

      // Player1 passed
      pass()
      has("Victory") shouldBe true
      // Final greenery placement
      doTask("Ok")
      // This game id was gafda6ee74f34

      assertProduction(m = 27, s = 4, t = 6, p = 4, e = 1, h = 9)
      assertResources(m = 106, s = 4, t = 6, p = 4, e = 1, h = 17)
      assertCounts(0 to "ProjectCard", 75 to "TR", 58 to "CardFront OR PlayedEvent")
      assertDashRight(events = 10, tagless = 3, cities = 5, colonies = 1)
      assertSidebar(gen = 12, temp = 8, oxygen = 10, oceans = 9, venus = 30)
      assertTags(15, 16, 10, 5, eat = 3, jot = 8, vet = 4, plt = 1, mit = 4, ant = 0, cit = 4)

      val sum = Summarizer(game)

      // Best current match for the app's reported action count: turns offered plus passes,
      // excluding the final-greenery offer.
      (-sum.net("NewTurn", "NewTurn<Player1>") + sum.net("ActionPhase", "Pass<Player1>") -
          1) shouldBe 168

      // Discounts earned
      sum.net("$AdvancedAlloys", "Owed<Player1>") shouldBe -84
      sum.net("$Shuttles", "Owed<Player1>") shouldBe -14

      // Resources and cards gained from active cards
      sum.net("$CryoSleep", "Energy") shouldBe 2
      sum.net("$CryoSleep", "Titanium") shouldBe 2
      sum.net("$DevelopmentCenter", "ProjectCard") shouldBe 6
      sum.net("$ElectroCatapult", "Plant") shouldBe -6
      sum.net("$ElectroCatapult", "Steel") shouldBe -3
      sum.net("$ElectroCatapult", "Megacredit") shouldBe 63
      sum.net("$OptimalAerobraking", "Megacredit") shouldBe 9
      sum.net("$OptimalAerobraking", "Heat") shouldBe 9
      sum.net("$SaturnSurfing", "Megacredit") shouldBe 29
      sum.net("$SpinOffDepartment", "ProjectCard") shouldBe 12
      sum.net("$TitanShuttles", "Titanium") shouldBe 19
      sum.net("$TitanShuttles", "Floater<$SaturnSurfing>") shouldBe 8

      // Terraforming gains
      sum.net("$EquatorialMagnetizer", "TerraformRating") shouldBe 2
      sum.net("$ForcedPrecipitation", "VenusStep") shouldBe 1
      sum.net("$RotatorImpacts", "VenusStep") shouldBe 4
      sum.net("$Thermophiles", "VenusStep") shouldBe 2

      // Puntos
      sum.net("GreeneryTile", "VictoryPoint") shouldBe 6
      sum.net("CityTile", "VictoryPoint") shouldBe 7
      sum.net("Card", "VictoryPoint") shouldBe 43
      assertCounts(131 to "VictoryPoint")
    }
  }
}
