package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Archived game gf33a06d07a1c: draw identities, placement notes and checkpoints are retained
// from the existing test. Historical results image: https://tinyurl.com/39xerd7w
// The score discrepancy described at the end remains unresolved.
internal class SoloGame20230721Test : FullGameTest() {
  override val config =
      GameConfig(
          """
          ElysiumMap
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, promos:cards
          Tr63SoloObjective
          Ceres, Enceladus, Luna, Triton
          """,
          "Me",
      )

  // Drew and discarded Decomposers (cost 5) to place a city
  // Drew and discarded Land Claim (cost 1) to place a greenery
  // Drew and discarded Greenhouses (cost 6) to place a city
  // Drew and discarded Luna Metropolis (cost 21) to place a greenery
  override val soloTileLocations =
      listOf("Elysium_2_6", "Elysium_1_5", "Elysium_8_9", "Elysium_7_8")

  private val me
    get() = p1

  @Test
  internal fun soloGame20230721() {
    startGame()
    with(me) {
      doTask("14 TerraformRating")
      doTasks(
          "2 CorporationCard",
          "-CorporationCard",
          "10 ProjectCard<Selecting>",
          "4 PreludeCard",
          "-2 PreludeCard",
      )

      doTask("-SelectedColonyTile<Class<Enceladus>>")

      doTask("-6 ProjectCard<Selecting>")
    }
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
    generation12()
  }

  private fun generation1() {
    with(me) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$PhoboLog>>",
          "$PhoboLog FROM CorporationCard",
          "23 MC",
          "10 Titanium",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      // You drew Thorgate, Valley Trust, United Nations Mars Initiative and Robinson Industries
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$UnitedNationsMarsInitiative>>",
          "$UnitedNationsMarsInitiative FROM CorporationCard<Selecting>",
          "40 MC",
          "-42 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$GreatAquifer>>",
          "$GreatAquifer FROM PreludeCard",
          "OceanTile<Elysium_3_6>", // me placed ocean tile on row 3 position 6
          "TerraformRating",
          "OceanTile<Elysium_4_7>", // me placed ocean tile on row 4 position 7
          "TerraformRating",
          "Plant",
          "2 MC",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$CallistoPenalMines"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$CallistoPenalMines FROM ProjectCard",
          "PROD[3 MC]",
      )
      doTasks(
          *playCardTasks("$Sponsors"),
          "6 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("Pass")
      doTask("20 MC")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation2() {
    with(me) {
      // You drew Atmo Collectors
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          *playCardTasks("$AtmoCollectors"),
          "15 Pay<> FROM MC",
          "$AtmoCollectors FROM ProjectCard",
          "2 Floater<$AtmoCollectors>",
      )
      doTasks(
          *cardActionTasks("$AtmoCollectors", "Action2"),
          "-Floater<$AtmoCollectors>",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$RotatorImpacts"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$RotatorImpacts FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
      doTask("Pass")
      doTask("20 MC")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation3() {
    with(me) {
      // You drew Extractor Balloons, Carbonate Processing and Project Inspection
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$AtmoCollectors", "Action2"),
          "-Floater<$AtmoCollectors>",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "UseAction<$UnitedNationsMarsInitiative, Action1>",
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "3 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "-Production<Anyone, Class<Energy>>",
          "PROD[Energy]",
      )
      doTask("Pass")
      doTasks("23 MC", "Energy")
      doTask("TemperatureStep! BY Admin")
    }
  }

  private fun generation4() {
    with(me) {
      // You drew Nuclear Power, Earth Office and Energy Saving
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")

      assertProduction(m = 3, s = 0, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 16, s = 0, t = 6, p = 1, e = 1, h = 0)
      // UI says actions 2 because UNMI not currently playable
      assertDashMiddle(played = 10, actions = 3, tr = 20, hand = 5)
      assertDashRight(events = 1, tagless = 3, cities = 0, colonies = 0)
      assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 2, venus = 6)
      doTasks(*cardActionTasks("$AtmoCollectors"), "Floater<$AtmoCollectors>")
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(
          *playCardTasks("$NuclearPower"),
          "10 Pay<> FROM MC",
          "$NuclearPower FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[3 Energy]",
      )
      doTask("Pass")
      doTasks("21 MC", "4 Energy", "Heat FROM Energy")
      doTask("TemperatureStep! BY Admin")
    }
  }

  private fun generation5() {
    with(me) {
      // You drew Solar Reflectors
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          *cardActionTasks("$AtmoCollectors", "Action2"),
          "-Floater<$AtmoCollectors>",
          "2 Titanium",
      )
      // You drew Deep Well Heating
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$SolarReflectors"),
          "6 Pay<Class<Titanium>> FROM Titanium", // "overpay" 1
          "Ok", // no MC remainder
          "$SolarReflectors FROM ProjectCard",
          "PROD[5 Heat]",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ceres>",
          "8 Steel",
      )
      doTasks(
          *playCardTasks("$DeepWellHeating"),
          "6 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$DeepWellHeating FROM ProjectCard",
          "PROD[Energy]",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$CarbonateProcessing FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 Heat]",
      )
      doTasks(*playCardTasks("$EarthOffice"), "1 Pay<> FROM MC", "$EarthOffice FROM ProjectCard")
      doTask("Pass")
      doTasks("24 MC", "4 Energy", "Heat FROM Energy", "9 Heat")
      doTask("TemperatureStep! BY Admin")
    }
  }

  private fun generation6() {
    with(me) {
      // You drew Flooding and Mining Expedition
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(*cardActionTasks("$AtmoCollectors"), "Floater<$AtmoCollectors>")
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "17 MC",
      )
      doTasks(
          *playCardTasks("$ExtractorBalloons"),
          "21 Pay<> FROM MC",
          "$ExtractorBalloons FROM ProjectCard",
          "3 Floater<$ExtractorBalloons>",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$MiningExpedition"),
          "12 Pay<> FROM MC",
          "$MiningExpedition FROM ProjectCard",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
          "-2 Plant<SoloOpponent>",
      )
      doTasks(
          *playCardTasks("$Flooding"),
          "7 Pay<> FROM MC",
          "$Flooding FROM ProjectCard",
          "OceanTile<Elysium_4_6>", // me placed ocean tile on row 4 position 6
          "TerraformRating",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTask("Pass")
      doTasks("29 MC", "4 Energy", "Heat FROM Energy", "10 Heat")
      doTask("OxygenStep! BY Admin")
    }
  }

  private fun generation7() {
    with(me) {
      // You drew Geothermal Power and CEO's Favorite Project
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 4, h = 10)
      assertResources(m = 32, s = 2, t = 0, p = 2, e = 4, h = 14)
      // UI says actions 3 because UNMI not currently playable
      assertDashMiddle(played = 18, actions = 4, tr = 28, hand = 3)
      assertDashRight(events = 3, tagless = 3, cities = 0, colonies = 0)
      assertSidebar(gen = 7, temp = -20, oxygen = 2, oceans = 3, venus = 10)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "1 Pay<> FROM MC",
          "$CeosFavoriteProject FROM ProjectCard",
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$AtmoCollectors", "Action2"),
          "-Floater<$AtmoCollectors>",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$GeothermalPower"),
          "2 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$GeothermalPower FROM ProjectCard",
          "PROD[2 Energy]",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
          "5 Titanium",
      )
      doTask("Pass")
      doTasks("32 MC", "6 Energy", "Heat FROM Energy", "10 Heat")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation8() {
    with(me) {
      // You drew Interplanetary Colony Ship
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$AtmoCollectors"), "Floater<$AtmoCollectors>")
      doTasks(
          *playCardTasks("$InterplanetaryColonyShip"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$InterplanetaryColonyShip FROM ProjectCard",
          "Colony<Luna>",
          "PROD[2 MC]",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // You drew Insects, Impactor Swarm and Solarnet
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Elysium_3_7>", // me placed greenery tile on row 3 position 7
          "OxygenStep",
          "3 ProjectCard",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Solarnet"),
          "7 Pay<> FROM MC",
          "$Solarnet FROM ProjectCard",
          "2 ProjectCard", // You drew Giant Solar Shade and Luna Governor
      )
      doTasks(*playCardTasks("$LunaGovernor"), "$LunaGovernor FROM ProjectCard", "PROD[2 MC]")
      doTasks(
          *playCardTasks("$GiantSolarShade"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$GiantSolarShade FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
      )
      doTask("Pass")
      doTasks("46 MC", "6 Energy", "6 Heat FROM Energy", "10 Heat")
      doTask("OxygenStep! BY Admin")
    }
  }

  private fun generation9() {
    with(me) {
      // You drew Release of Inert Gases and Moss
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTasks(*cardActionTasks("$AtmoCollectors", "Action2"), "-Floater<$AtmoCollectors>", "4 Heat")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RotatorImpacts"), "6 Pay<> FROM MC", "Asteroid<$RotatorImpacts>")
      doTasks(
          *playCardTasks("$Moss"),
          "4 Pay<> FROM MC",
          "$Moss FROM ProjectCard",
          "PROD[Plant]",
          "-Plant",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_5_6>", // me placed city tile on row 5 position 6
          "3 Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "7 MC",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Elysium_5_7>", // me placed greenery tile on row 5 position 7
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTask("Pass")
      doTasks("51 MC", "6 Energy", "3 Heat FROM Energy", "10 Heat", "Plant")
      doTask("OxygenStep! BY Admin")
    }
  }

  private fun generation10() {
    with(me) {
      // You drew Ice Moon Colony, Trees and Dust Seals
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")

      assertProduction(m = 6, s = 0, t = 0, p = 1, e = 6, h = 10)
      assertResources(m = 49, s = 0, t = 0, p = 7, e = 6, h = 18)
      // UI says actions 3 because UNMI not currently playable
      assertDashMiddle(played = 25, actions = 4, tr = 45, hand = 7)
      assertDashRight(events = 5, tagless = 4, cities = 1, colonies = 1)
      assertSidebar(gen = 10, temp = -10, oxygen = 6, oceans = 3, venus = 24)
      doTasks(*playCardTasks("$DustSeals"), "2 Pay<> FROM MC", "$DustSeals FROM ProjectCard")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTasks(*cardActionTasks("$AtmoCollectors"), "Floater<$AtmoCollectors>")
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$IceMoonColony"),
          "23 Pay<> FROM MC",
          "$IceMoonColony FROM ProjectCard",
          "Colony<Triton>",
          "3 Titanium",
          "OceanTile<Elysium_4_4>", // me placed ocean tile on row 4 position 4
          "TerraformRating",
          "2 Plant",
      )
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Elysium_5_4>", // me placed ocean tile on row 5 position 4
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_5>", // me placed greenery tile on row 5 position 5
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Insects"),
          "9 Pay<> FROM MC",
          "$Insects FROM ProjectCard",
          "PROD[Plant]",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ceres>",
          "8 Steel",
      )
      doTask("Pass")
      doTasks("58 MC", "6 Energy", "3 Heat FROM Energy", "10 Heat", "2 Plant")
      doTask("OceanTile<Elysium_2_4>! BY Admin") // me placed ocean tile on row 2 position 4
    }
  }

  private fun generation11() {
    with(me) {
      // You drew Venus Waystation
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$VenusWaystation"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$VenusWaystation FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$ReleaseOfInertGases"),
          "14 Pay<> FROM MC",
          "$ReleaseOfInertGases FROM ProjectCard",
          "2 TerraformRating",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "7 MC",
          "2 MC",
      )
      doTasks(*cardActionTasks("$AtmoCollectors", "Action2"), "-Floater<$AtmoCollectors>", "4 Heat")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Trees"),
          "13 Pay<> FROM MC",
          "$Trees FROM ProjectCard",
          "PROD[3 Plant]",
          "Plant",
      )
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Elysium_3_5>", // me placed ocean tile on row 3 position 5
          "TerraformRating",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
      )
      // You drew Interstellar Colony Ship
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_4_5>", // me placed greenery tile on row 4 position 5
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Elysium_1_3>", // me placed ocean tile on row 1 position 3
          "TerraformRating",
          "ProjectCard",
          "2 MC",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_6_8>", // me placed city tile on row 6 position 7
          "Plant",
      )
      doTask("Pass")
      doTasks("69 MC", "6 Energy", "3 Heat FROM Energy", "10 Heat", "5 Plant")
      doTask("OxygenStep! BY Admin")
    }
  }

  private fun generation12() {
    with(me) {
      // You drew Solar Power
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")

      assertProduction(m = 7, s = 0, t = 0, p = 5, e = 6, h = 10)
      assertResources(m = 68, s = 8, t = 0, p = 8, e = 6, h = 16)
      // UI says actions 3 because UNMI not currently playable
      assertDashMiddle(played = 31, actions = 4, tr = 62, hand = 4)
      assertDashRight(events = 6, tagless = 5, cities = 2, colonies = 2)
      assertSidebar(gen = 12, temp = 0, oxygen = 9, oceans = 8, venus = 28)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_6_7>", // me placed greenery tile on row 6 position 6
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "4 MC",
          "2 MC",
      )
      doTasks(*cardActionTasks("$AtmoCollectors"), "Floater<$AtmoCollectors>")
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTasks(
          *playCardTasks("$SolarPower"),
          "6 Pay<Class<Steel>> FROM Steel", // "overpay" 1
          "Ok", // no MC remainder
          "$SolarPower FROM ProjectCard",
          "PROD[Energy]",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Elysium_5_8>", // me placed greenery tile on row 5 position 8
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_7_7>", // me placed city tile on row 7 position 5
          "Steel",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Elysium_6_6>", // me placed greenery tile on row 6 position 5
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTask("Pass")
      doTasks("77 MC", "7 Energy", "3 Heat FROM Energy", "10 Heat", "5 Plant")
      has("Victory") shouldBe true
      // Final greenery placement
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_8_8>", // me placed greenery tile on row 8 position 5
          "2 Steel",
      )
      // Decline another final greenery placement.
      doTask("PassBaton")

      assertProduction(m = 8, s = 0, t = 0, p = 5, e = 7, h = 10)
      assertResources(m = 82, s = 5, t = 0, p = 1, e = 7, h = 13)
      assertCounts(0 to "ProjectCard", 69 to "TerraformRating", 32 to "CardFront OR PlayedEvent")
      assertDashRight(events = 6, tagless = 5, cities = 3, colonies = 2)
      assertSidebar(gen = 12, temp = 4, oxygen = 12, oceans = 8, venus = 30)

      assertTags(5, 7, 0, 5, 5, 1, 3, 2, 1, 0, 0)

      val sum = Summarizer(game)
      assertCounts(69 to "TerraformRating")
      sum.net("GreeneryTile", "VictoryPoint<Me>") shouldBe 8
      sum.net("CityTile", "VictoryPoint<Me>") shouldBe 13
      sum.net("Card", "VictoryPoint<Me>") shouldBe 5
      assertCounts(95 to "VictoryPoint")
      assertCounts(82 to "MC")

      // Legacy comments claim 99 VP and report 111 on herokuapp; the discrepancy is unresolved.
      // Flooding now consumes its normal action instead of being injected outside a turn.
      (sum.signalCount("InRound", "UseAction<Me, StandardAction>") +
          sum.signalCount("InRound", "DoRequiredActions<Me>")) shouldBe 99
    }
  }
}
