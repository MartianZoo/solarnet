package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class SoloGame0721Test : AbstractSoloTest() {
  override fun setup(): GameSetup {
    val colonyTiles = setOf(cn("Ceres"), cn("Luna"), cn("Triton")) // and Enceladus
    return GameSetup(Canon, "BREVPCTX", 2, colonyTiles)
  }

  // TODO calculate the areas based on the cards drawn

  // Drew and discarded Decomposers (cost 5) to place a 2
  // Drew and discarded Land Claim (cost 1) to place a 0
  // Drew and discarded Greenhouses (cost 6) to place a 2
  // Drew and discarded Luna Metropolis (cost 21) to place a 0
  override fun cityAreas(): Pair<String, String> = "Elysium_2_6" to "Elysium_8_9"
  override fun greeneryAreas(): Pair<String, String> = "Elysium_1_5" to "Elysium_7_8"

  @Test
  fun game() {
    with(me) {
      // The id of this game is gf33a06d07a1c
      // Good luck me!
      // Generation 1
      // me's titanium amount increased by 10
      // me played PhoboLog
      // me kept 4 project cards
      playCorp("Phobolog", 4).expect("11, 10 T")

      // You discarded Enceladus

      phase("Prelude")
      // me played Merger
      val UnitedNationsMarsInitiative = "UnitedNationsMarsInitiative"
      playPrelude("Merger") {
        // You drew Thorgate, Valley Trust, United Nations Mars Initiative and Robinson Industries
        // You drew Thorgate, Valley Trust, United Nations Mars Initiative and Robinson Industries
        // me played United Nations Mars Initiative
        doTask("PlayCard<Class<CorporationCard>, Class<$UnitedNationsMarsInitiative>>")
        // TODO playCorp
      }
      // me played Great Aquifer
      playPrelude("GreatAquifer") {
        // me placed ocean tile on row 3 position 6
        doFirstTask("OceanTile<Elysium_3_6>")
        // me placed ocean tile on row 4 position 7
        // me's plants amount increased by 1
        doFirstTask("OceanTile<Elysium_4_7>")
      }.expect("Plant, 2")

      phase("Action")

      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)

      // me played Callisto Penal Mines
      // me's megacredits production increased by 3
      playProject("CallistoPenalMines", titanium = 6)

      // me played Sponsors
      // me's megacredits production increased by 2
      playProject("Sponsors", 6)

      // me passed
      // me acted as World Government and increased Venus scale
      // Generation 2
      // me bought 1 card(s)
      // You drew Atmo Collectors
      nextRound("VenusStep", 1)

      // me played Atmo Collectors
      val AtmoCollectors = "AtmoCollectors"
      playProject(AtmoCollectors, 15) {
        // me added 2 floater(s) to Atmo Collectors
        doTask("2 Floater<$AtmoCollectors>")
      }
      // me used Atmo Collectors action
      cardAction2(AtmoCollectors) {
        // me removed 1 resource(s) from me's Atmo Collectors
        // me's titanium amount increased by 2
        doTask("2 Titanium")
      }
      // me played Rotator Impacts
      val RotatorImpacts = "RotatorImpacts"
      playProject(RotatorImpacts, 2, titanium = 1)
      // me used Rotator Impacts action
      // me added 1 asteroid(s) to Rotator Impacts
      cardAction1(RotatorImpacts) {
        pay(2, titanium = 1)
      }.expect("Asteroid")

      // me passed
      // me acted as World Government and increased Venus scale
      // Generation 3
      // me bought 3 card(s)
      // You drew Extractor Balloons, Carbonate Processing and Project Inspection
      nextRound("VenusStep", 3)

      // me used Rotator Impacts action
      // me removed 1 resource(s) from me's Rotator Impacts
      // me removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts).expect("TR")
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative).expect("TR")
      // me used Atmo Collectors action
      cardAction2(AtmoCollectors) {
        // me removed 1 resource(s) from me's Atmo Collectors
        // me's titanium amount increased by 2
        doTask("2 Titanium")
      }
      // me played Project Inspection
      playProject("ProjectInspection", 0) {
        // me used United Nations Mars Initiative action with Project Inspection
        doTask("UseAction1<$UnitedNationsMarsInitiative>")
      }
      // me played Energy Tapping
      playProject("EnergyTapping", 3) { doTask("PROD[-Energy<P2>]") }

      // me passed
      // me acted as World Government and increased temperature
      // Generation 4
      // me bought 3 card(s)
      // You drew Nuclear Power, Earth Office and Energy Saving
      nextRound("TemperatureStep", 3)

      assertProduction(m = 3, s = 0, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 16, s = 0, t = 6, p = 1, e = 1, h = 0)
      // UI says actions 2 because UNMI not currently playable
      assertDashMiddle(played = 10, actions = 3, vp = 21, tr = 20, hand = 5)
      assertDashRight(events = 1, tagless = 3, cities = 0, colonies = 0)
      assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 2, venus = 6)

      // me used Atmo Collectors action
      // me added 1 floater(s) to Atmo Collectors
      cardAction1(AtmoCollectors)
      // me used Rotator Impacts action
      // me added 1 asteroid(s) to Rotator Impacts
      cardAction1(RotatorImpacts) {
        pay(2, titanium = 1)
      }
      // me played Nuclear Power
      // me's megacredits production decreased by 2
      // me's energy production increased by 3
      playProject("NuclearPower", 10).expect("PROD[-2, 3E]")

      // me passed
      // me acted as World Government and increased temperature
      // Generation 5
      // me bought 1 card(s)
      // You drew Solar Reflectors
      nextRound("TemperatureStep", 1)

      // me used Atmo Collectors action
      cardAction2(AtmoCollectors) {
        // me removed 1 resource(s) from me's Atmo Collectors
        // me's titanium amount increased by 2
        doTask("2 Titanium")
      }
      // me used Rotator Impacts action
      // me removed 1 resource(s) from me's Rotator Impacts
      // me drew 1 card(s)
      // You drew Deep Well Heating
      // me removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts).expect("Card")
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)
      // me played Solar Reflectors
      // me's heat production increased by 5
      playProject("SolarReflectors", titanium = 6) // "overpay" 1
      // me spent 3 energy to trade with Ceres
      // me's steel amount increased by 8
      stdAction("TradeSA", 2) { doTask("Trade<Ceres, TradeFleetA>") }.expect("-3 Energy, 8 Steel")
      // me played Deep Well Heating
      // me's energy production increased by 1
      // me's heat production increased by 1
      playProject("DeepWellHeating", 1, steel = 6).expect("PROD[E, H], TR")
      // me played Carbonate Processing
      // me's energy production decreased by 1
      // me's heat production increased by 3
      playProject("CarbonateProcessing", 2, steel = 2)
      // me played Earth Office
      playProject("EarthOffice", 1)

      // me passed
      // me acted as World Government and increased temperature
      // Generation 6
      // me bought 2 card(s)
      // You drew Flooding and Mining Expedition
      nextRound("TemperatureStep", 2)

      // me used Convert Heat standard action
      // me's heat production increased by 1
      stdAction("ConvertHeatSA").expect("PROD[Heat]")
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)
      // me used Rotator Impacts action
      // me added 1 asteroid(s) to Rotator Impacts
      cardAction1(RotatorImpacts) {
        pay(2, titanium = 1)
      }
      // me used Atmo Collectors action
      // me added 1 floater(s) to Atmo Collectors
      cardAction1(AtmoCollectors)
      // me spent 3 energy to trade with Luna
      // me's megacredits amount increased by 17
      stdAction("TradeSA", 2) { doTask("Trade<Luna, TradeFleetA>") }.expect("17")
      // me played Extractor Balloons
      // me added 3 floater(s) to Extractor Balloons
      val ExtractorBalloons = "ExtractorBalloons"
      playProject(ExtractorBalloons, 21)
      // me used Extractor Balloons action
      // me removed 2 resource(s) from me's Extractor Balloons
      // me raised the Venus scale 1 step(s)
      cardAction2(ExtractorBalloons).expect("TR")
      // me played Mining Expedition
      // me's steel amount increased by 2
      playProject("MiningExpedition", 12) { doTask("-2 Plant<P2>") }.expect("2 Steel, TR")

      // TODO had to hack around this - Flooding should have been banned
      // me played Flooding
      // me placed ocean tile on row 4 position 6
      // me's plants amount increased by 1
      godMode().manual("-7 THEN OceanTile<Elysium_4_6>").expect("Plant, -3")
      godMode().manual("PlayedEvent<Class<Conscription>> FROM ProjectCard") // ewww!

      // me passed
      // me acted as World Government and increased oxygen level
      // Generation 7
      // me bought 2 card(s)
      // You drew Geothermal Power and CEO's Favorite Project
      nextRound("OxygenStep", 2)

      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 4, h = 10)
      assertResources(m = 32, s = 2, t = 0, p = 2, e = 4, h = 14)
      // UI says actions 3 because UNMI not currently playable
      assertDashMiddle(played = 18, actions = 4, vp = 28, tr = 28, hand = 3)
      assertDashRight(events = 3, tagless = 3, cities = 0, colonies = 0)
      assertSidebar(gen = 7, temp = -20, oxygen = 2, oceans = 3, venus = 10)

      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)
      // me used Extractor Balloons action
      // me added 1 floater(s) to Extractor Balloons
      cardAction1(ExtractorBalloons)
      // me played CEO's Favorite Project
      playProject("CeosFavoriteProject", 1) {
        // me added 1 asteroid(s) to Rotator Impacts
        doTask("Asteroid<RotatorImpacts>")
      }
      // me used Rotator Impacts action
      // me removed 1 resource(s) from me's Rotator Impacts
      // me removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
      // me used Atmo Collectors action
      // me removed 1 resource(s) from me's Atmo Collectors
      cardAction2(AtmoCollectors) {
        // me's titanium amount increased by 2
        doTask("2 Titanium")
      }
      // me played Geothermal Power
      // me's energy production increased by 2
      playProject("GeothermalPower", 7, steel = 2)
      // me spent 3 energy to trade with Triton
      // me's titanium amount increased by 5
      stdAction("TradeSA", 2) { doTask("Trade<Triton, TradeFleetA>") }.expect("5 T")

      // me passed
      // me acted as World Government and increased Venus scale
      // Generation 8
      // me bought 1 card(s)
      // You drew Interplanetary Colony Ship
      nextRound("VenusStep", 1)

      // me used Extractor Balloons action
      // me removed 2 resource(s) from me's Extractor Balloons
      // me raised the Venus scale 1 step(s)
      cardAction2(ExtractorBalloons)
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)
      // me used Rotator Impacts action
      // me removed 1 resource(s) from me's Rotator Impacts
      // me removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
      // me used Atmo Collectors action
      // me added 1 floater(s) to Atmo Collectors
      cardAction1(AtmoCollectors)
      // me played Interplanetary Colony Ship
      playProject("InterplanetaryColonyShip", 5, titanium = 1) {
        // me built a colony on Luna
        // me's megacredits production increased by 2
        doTask("Colony<Luna>")
      }.expect("PROD[2]")
      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used Greenery standard project
      stdProject("GreenerySP") {
        // me placed greenery tile on row 3 position 7
        // me drew 3 card(s)
        // You drew Insects, Impactor Swarm and Solarnet
        doTask("GreeneryTile<Elysium_3_7>")
      }.expect("3 Card")
      // me played Solarnet
      // me drew 2 card(s)
      playProject("Solarnet", 7)
      // You drew Giant Solar Shade and Luna Governor
      // me played Luna Governor
      // me's megacredits production increased by 2
      playProject("LunaGovernor", 0)
      // me played Giant Solar Shade
      playProject("GiantSolarShade", 3, titanium = 6)

      // me passed
      // me acted as World Government and increased oxygen level
      // Generation 9
      // me bought 2 card(s)
      // You drew Release of Inert Gases and Moss
      nextRound("OxygenStep", 2)

      // me used Extractor Balloons action
      // me added 1 floater(s) to Extractor Balloons
      cardAction1(ExtractorBalloons)
      // me used Atmo Collectors action
      // me removed 1 resource(s) from me's Atmo Collectors
      cardAction2(AtmoCollectors) {
        // me's heat amount increased by 4
        doTask("4 Heat")
      }
      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)
      // me used Rotator Impacts action
      // me added 1 asteroid(s) to Rotator Impacts
      cardAction1(RotatorImpacts) {
        pay(6)
      }
      // me played Moss
      // me's plants production increased by 1
      playProject("Moss", 4)
      // me used City standard project
      stdProject("CitySP") {
        // me placed city tile on row 5 position 6
        // me's plants amount increased by 3
        doTask("CityTile<Elysium_5_6>")
      }.expect("3 Plant")
      stdAction("TradeSA", 2) {
        // me spent 3 energy to trade with Luna
        // me's megacredits amount increased by 7
        // me's megacredits amount increased by 2
        doTask("Trade<Luna, TradeFleetA>")
      }.expect("9")
      // me used Greenery standard project
      stdProject("GreenerySP") {
        // me placed greenery tile on row 5 position 7
        // me's plants amount increased by 2
        doTask("GreeneryTile<Elysium_5_7>")
      }

      // me passed
      // me acted as World Government and increased oxygen level
      // Generation 10
      // me bought 3 card(s)
      // You drew Ice Moon Colony, Trees and Dust Seals
      nextRound("OxygenStep", 3)

      assertProduction(m = 6, s = 0, t = 0, p = 1, e = 6, h = 10)
      assertResources(m = 49, s = 0, t = 0, p = 7, e = 6, h = 18)
      // UI says actions 3 because UNMI not currently playable
      assertDashMiddle(played = 25, actions = 4, vp = 49, tr = 45, hand = 7)
      assertDashRight(events = 5, tagless = 4, cities = 1, colonies = 1)
      assertSidebar(gen = 10, temp = -10, oxygen = 6, oceans = 3, venus = 24)

      // me played Dust Seals
      playProject("DustSeals", 2)
      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)
      // me used Extractor Balloons action
      // me added 1 floater(s) to Extractor Balloons
      cardAction1(ExtractorBalloons)
      // me used Atmo Collectors action
      // me added 1 floater(s) to Atmo Collectors
      cardAction1(AtmoCollectors)
      // me used Rotator Impacts action
      // me removed 1 resource(s) from me's Rotator Impacts
      // me removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
      // me played Ice Moon Colony
      playProject("IceMoonColony", 23) {
        // me built a colony on Triton
        // me's titanium amount increased by 3
        doTask("Colony<Triton>")
        // me placed ocean tile on row 4 position 4
        // me's plants amount increased by 2
        doTask("OceanTile<Elysium_4_4>")
      }.expect("3T, 2P")
      // me used Aquifer standard project
      stdProject("AquiferSP") {
        // me placed ocean tile on row 5 position 4
        // me's plants amount increased by 2
        doTask("OceanTile<Elysium_5_4>")
      }
      // me used Convert Plants standard action
      stdAction("ConvertPlantsSA") {
        // me placed greenery tile on row 5 position 5
        // me's plants amount increased by 2
        doTask("GreeneryTile<Elysium_5_5>")
      }
      // me played Insects
      // me's plants production increased by 1
      playProject("Insects", 9).expect("PROD[P]")
      // me spent 3 energy to trade with Ceres
      stdAction("TradeSA", 2) {
        // me's steel amount increased by 8
        doTask("Trade<Ceres, TradeFleetA>")
      }.expect("8 Steel")

      // me passed
      // me placed ocean tile on row 2 position 4
      // me acted as World Government and placed an ocean
      // Generation 11
      // me bought 1 card(s)
      // You drew Venus Waystation
      nextRound("OceanTile<Elysium_2_4>", 1)

      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)
      // me used Extractor Balloons action
      // me removed 2 resource(s) from me's Extractor Balloons
      // me raised the Venus scale 1 step(s)
      cardAction2(ExtractorBalloons)
      // me played Venus Waystation 2 1 &
      playProject("VenusWaystation", 1, titanium = 2)
      // me played Release of Inert Gases
      playProject("ReleaseOfInertGases", 14)
      // me spent 3 energy to trade with Luna
      stdAction("TradeSA", 2) {
        // me's megacredits amount increased by 7
        // me's megacredits amount increased by 2
        doTask("Trade<Luna, TradeFleetA>")
      }.expect("9")
      // me used Atmo Collectors action
      // me removed 1 resource(s) from me's Atmo Collectors
      cardAction2(AtmoCollectors) {
        // me's heat amount increased by 4
        doTask("4 Heat")
      }
      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me played Trees
      // me's plants production increased by 3
      // me's plants amount increased by 1
      playProject("Trees", 13)
      // me used Aquifer standard project
      stdProject("AquiferSP") {
        // me placed ocean tile on row 3 position 5
        // me's plants amount increased by 1
        doTask("OceanTile<Elysium_3_5>")
      }
      // me used Convert Plants standard action
      stdAction("ConvertPlantsSA") {
        // me placed greenery tile on row 4 position 5
        // me's plants amount increased by 1
        doTask("GreeneryTile<Elysium_4_5>")
        // me placed ocean tile on row 1 position 3
        // me drew 1 card(s)
        // You drew Interstellar Colony Ship
        doTask("OceanTile<Elysium_1_3>")
      }
          .expect("-7 Plant, 8, Card, 3 TR")
      // me used Rotator Impacts action1 2 ***
      // me added 1 asteroid(s) to Rotator Impacts
      cardAction1(RotatorImpacts) {
        pay(2, titanium = 1)
      }
      // me used City standard project
      stdProject("CitySP") {
        // me placed city tile on row 6 position 7
        // me's plants amount increased by 1
        doTask("CityTile<Elysium_6_8>")
      }

      // me passed
      // me acted as World Government and increased oxygen level
      // Generation 12
      // me bought 1 card(s)
      // You drew Solar Power
      nextRound("OxygenStep", 1)

      assertProduction(m = 7, s = 0, t = 0, p = 5, e = 6, h = 10)
      assertResources(m = 68, s = 8, t = 0, p = 8, e = 6, h = 16)
      // UI says actions 3 because UNMI not currently playable
      assertDashMiddle(played = 31, actions = 4, vp = 75, tr = 62, hand = 4)
      assertDashRight(events = 6, tagless = 5, cities = 2, colonies = 2)
      assertSidebar(gen = 12, temp = 0, oxygen = 9, oceans = 8, venus = 28)

      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used Convert Heat standard action
      stdAction("ConvertHeatSA")
      // me used United Nations Mars Initiative action
      cardAction1(UnitedNationsMarsInitiative)
      // me used Rotator Impacts action
      // me removed 1 resource(s) from me's Rotator Impacts
      // me removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
      // me used Convert Plants standard action
      stdAction("ConvertPlantsSA") {
        // me placed greenery tile on row 6 position 6
        // me's plants amount increased by 1
        doTask("GreeneryTile<Elysium_6_7>")
      }
      // me spent 3 energy to trade with Luna
      stdAction("TradeSA", 2) {
        // me's megacredits amount increased by 4
        // me's megacredits amount increased by 2
        doTask("Trade<Luna, TradeFleetA>")
      }.expect("6")
      // me used Atmo Collectors action
      // me added 1 floater(s) to Atmo Collectors
      cardAction1(AtmoCollectors)
      // me used Extractor Balloons action
      // me added 1 floater(s) to Extractor Balloons
      cardAction1(ExtractorBalloons)
      // me played Solar Power
      // me's energy production increased by 1
      playProject("SolarPower", 0, steel = 6) // "overpay" 1
      // me used Greenery standard project
      stdProject("GreenerySP") {
        // me placed greenery tile on row 5 position 8
        // me's plants amount increased by 2
        doTask("GreeneryTile<Elysium_5_8>")
      }
      // me used City standard project
      stdProject("CitySP") {
        // me placed city tile on row 7 position 5
        // me's steel amount increased by 1
        doTask("CityTile<Elysium_7_7>")
      }
      // me used Greenery standard project
      stdProject("GreenerySP") {
        // me placed greenery tile on row 6 position 5
        // me's plants amount increased by 1
        doTask("GreeneryTile<Elysium_6_6>")
      }
      // me used Sell Patents standard project
      // me sold 3 patents
      sellPatents(3)

      // me passed
      pass()
      engine.phase("Production")

      assertThat(has("63 TR")) // victory!

      // Final greenery placement

      // TODO wow this is a stupid hack. Make sure final greeneries don't raise oxygen!
      // In general this trick could fail if it triggers an ocean tile placement
      opponent.godMode().manual("14 OxygenStep")

      godMode().manual("UseAction1<ConvertPlantsSA>") {
        // me placed greenery tile on row 8 position 5
        // me's steel amount increased by 2
        doTask("GreeneryTile<Elysium_8_8>")
      }

      engine.phase("End")

      // This game id was gf33a06d07a1c
      // herokuapp results image: https://tinyurl.com/39xerd7w

      assertProduction(m = 8, s = 0, t = 0, p = 5, e = 7, h = 10)
      assertResources(m = 82, s = 5, t = 0, p = 1, e = 7, h = 13)
      assertCounts(0 to "ProjectCard", 69 to "TR", 32 to "CardFront + PlayedEvent")
      assertDashRight(events = 6, tagless = 5, cities = 3, colonies = 2)
      assertSidebar(gen = 12, temp = 4, oxygen = 14, oceans = 8, venus = 30) // TODO oxygen = 12

      assertTags(5, 7, 0, 5, 5, 1, 3, 2, 1, 0, 0)

      val sum = Summarizer(game)
      assertCounts(69 to "TerraformRating")
      assertThat(sum.net("GreeneryTile", "VictoryPoint<P1>")).isEqualTo(8)
      assertThat(sum.net("CityTile", "VictoryPoint<P1>")).isEqualTo(13)
      assertThat(sum.net("Card", "VictoryPoint<P1>")).isEqualTo(5)
      assertCounts(95 to "VictoryPoint")
      assertCounts(82 to "Megacredit")
    }
  }
}
