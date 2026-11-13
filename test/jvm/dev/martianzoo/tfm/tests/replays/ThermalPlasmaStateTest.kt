package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Sources: _local/replays/Game20260817/full-log-pb64886c6e682.txt and
// _local/replays/Game20260817/player-pb64886c6e682.json. The complete Research offers come from the
// earlier clone of the same source game at /the-end?id=pe59a6b631bd6.
internal class ThermalPlasmaStateTest : FullGameTest() {
  override val config =
      GameConfig(
          """
          ElysiumMap
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, TerraLabsResearch, promos:cards
          Tr63SoloObjective
          Ganymede, Luna, Miranda, Pluto
          """,
          "Me",
      )

  // Drew and discarded Orbital Reflectors to place a 2
  // Drew and discarded Lake Marineris to place a 0
  // Drew and discarded Topsoil Contract to place a 2
  // Drew and discarded Space Station to place a 0
  override val soloTileLocations =
      listOf("Elysium_6_4", "Elysium_7_5", "Elysium_8_7", "Elysium_9_8")

  private val me
    get() = p1

  @Test
  internal fun thermalPlasmaState() {
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
      doTask("-SelectedColonyTile<Class<Miranda>>")
      doTask("-2 ProjectCard<Selecting>")
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
          "PlayCard<Class<CorporationCard>, Class<$TerraLabsResearch>>",
          "$TerraLabsResearch FROM CorporationCard",
          "14 MC",
          "-TerraformRating",
          "8 Pay<> FROM MC",
          "8 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BusinessEmpire>>",
          "$BusinessEmpire FROM PreludeCard",
          "PROD[6 MC]",
          "-6 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
          "$MetalsCompany FROM PreludeCard",
          "PROD[MC]",
          "PROD[Steel]",
          "PROD[Titanium]",
      )

      assertCounts(0 to "MC")
      doTask("Pass")
      doTasks("18 MC", "Steel", "Titanium")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation2() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")

      assertCounts(17 to "MC")
      doTask("Pass")
      doTasks("18 MC", "Steel", "Titanium")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation3() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "2 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "Ok", // no steel paid
          "1 Pay<> FROM MC",
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
      )
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
      )
      doTasks(
          *playCardTasks("$AiCentral"),
          "2 Pay<Class<Steel>> FROM Steel",
          "17 Pay<> FROM MC",
          "$AiCentral FROM ProjectCard",
          "PROD[-Energy]",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )

      assertCounts(0 to "MC")
      doTask("Pass")
      doTasks("17 MC", "Steel", "2 Titanium")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation4() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "3 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$GeothermalPower"),
          "1 Pay<Class<Steel>> FROM Steel",
          "9 Pay<> FROM MC",
          "$GeothermalPower FROM ProjectCard",
          "PROD[2 Energy]",
      )

      assertCounts(0 to "MC")
      doTask("Pass")
      doTasks("17 MC", "Steel", "2 Titanium", "3 Energy")
      doTask("TemperatureStep! BY Admin")
    }
  }

  private fun generation5() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "3 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*playCardTasks("$MediaGroup"), "6 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "3 MC",
          "UseAction<$AiCentral, Action1>",
          "2 ProjectCard",
      )
      doTasks(
          *playCardTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$RotatorImpacts FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "13 MC",
      )
      doTasks(
          *playCardTasks("$RestrictedArea"),
          "11 Pay<> FROM MC",
          "$RestrictedArea FROM ProjectCard",
          "RestrictedArea_SpecialTile<Elysium_3_7>",
          "3 ProjectCard",
      )
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Conscription"),
          "5 Pay<> FROM MC",
          "$Conscription FROM ProjectCard",
          "Conscription_NextCardEffect",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$LunarBeam"),
          "$LunarBeam FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[2 Heat]",
          "PROD[2 Energy]",
      )
      doTasks(
          *playCardTasks("$ArtificialPhotosynthesis"),
          "12 Pay<> FROM MC",
          "$ArtificialPhotosynthesis FROM ProjectCard",
          "PROD[2 Energy]",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Extremophiles"),
          "3 Pay<> FROM MC",
          "$Extremophiles FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$Extremophiles>")

      assertCounts(0 to "MC")
      doTask("Pass")
      doTasks("15 MC", "Steel", "2 Titanium", "7 Energy", "2 Heat")
      doTask("TemperatureStep! BY Admin")
    }
  }

  private fun generation6() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "4 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Pluto>",
          "4 ProjectCard",
      )
      doTasks(
          *playCardTasks("$IndenturedWorkers"),
          "$IndenturedWorkers FROM ProjectCard",
          "IndenturedWorkers_NextCardEffect",
          "3 MC",
      )
      doTasks(*playCardTasks("$ForcedPrecipitation"), "$ForcedPrecipitation FROM ProjectCard")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Steelworks"),
          "2 Pay<Class<Steel>> FROM Steel",
          "11 Pay<> FROM MC",
          "$Steelworks FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$ImportOfAdvancedGhg"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$ImportOfAdvancedGhg FROM ProjectCard",
          "PROD[2 Heat]",
          "3 MC",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$SulphurEatingBacteria"),
          "6 Pay<> FROM MC",
          "$SulphurEatingBacteria FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$SulphurEatingBacteria>")

      assertCounts(0 to "MC")
      doTask("Pass")
      doTasks("17 MC", "Steel", "2 Titanium", "7 Energy", "4 Heat")
      doTask("OceanTile<Elysium_3_6>! BY Admin")
    }
  }

  private fun generation7() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "4 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ganymede>",
          "6 Plant",
      )
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$Solarnet"),
          "7 Pay<> FROM MC",
          "$Solarnet FROM ProjectCard",
          "2 ProjectCard",
      )
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-3 Microbe<$SulphurEatingBacteria>",
          "9 MC",
      )
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "21 Pay<> FROM MC",
          "$BigAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 Titanium",
          "3 MC",
          "TerraformRating",
          "PROD[Heat]",
          "TerraformRating",
          "Ok", // decline removing an opponent's plants
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$LunaGovernor"),
          "4 Pay<> FROM MC",
          "$LunaGovernor FROM ProjectCard",
          "PROD[2 MC]",
      )

      assertCounts(2 to "MC")
      doTask("Pass")
      doTasks("21 MC", "Steel", "2 Titanium", "7 Energy", "5 Heat")
      doTask("OceanTile<Elysium_2_4>! BY Admin")
    }
  }

  private fun generation8() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "2 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "7 MC",
      )
      doTasks(
          *playCardTasks("$OlympusConference"),
          "5 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$OlympusConference FROM ProjectCard",
          "Science<$OlympusConference>",
      )
      doTasks(
          *playCardTasks("$StaticHarvesting"),
          "5 Pay<> FROM MC",
          "$StaticHarvesting FROM ProjectCard",
          "PROD[Energy]",
          "5 MC",
      )
      doTasks(
          *playCardTasks("$ProtectedGrowth"),
          "2 Pay<> FROM MC",
          "$ProtectedGrowth FROM ProjectCard",
          "5 Plant",
          "3 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_4_8>",
          "OxygenStep",
          "Plant",
          "Steel",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$AtalantaPlanitiaLab"),
          "10 Pay<> FROM MC",
          "$AtalantaPlanitiaLab FROM ProjectCard",
          "2 ProjectCard",
          "ProjectCard FROM Science",
      )
      doTasks(
          *playCardTasks("$ImportedGhg"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
          "3 MC",
      )
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *playCardTasks("$RegolithEaters"),
          "13 Pay<> FROM MC",
          "$RegolithEaters FROM ProjectCard",
          "Science<$OlympusConference>",
      )
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$RegolithEaters>")
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Worms"),
          "8 Pay<> FROM MC",
          "$Worms FROM ProjectCard",
          "PROD[2 Plant]",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")

      assertCounts(2 to "MC")
      doTask("Pass")
      doTasks("26 MC", "Steel", "2 Titanium", "8 Energy", "7 Heat", "2 Plant")
      doTask("TemperatureStep! BY Admin")
    }
  }

  private fun generation9() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "3 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "4 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
      )
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(
          *playCardTasks("$AirScrappingExpedition"),
          "13 Pay<> FROM MC",
          "$AirScrappingExpedition FROM ProjectCard",
          "VenusStep",
          "3 MC",
          "TerraformRating",
          "3 Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$RegolithEaters", "Action2"),
          "-2 Microbe<$RegolithEaters>",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$RegolithEaters>")
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *playCardTasks("$SoilEnrichment"),
          "6 Pay<> FROM MC",
          "$SoilEnrichment FROM ProjectCard",
          "5 Plant",
          "3 MC",
          "-Microbe<$Extremophiles>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_8>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*playCardTasks("$Thermophiles"), "9 Pay<> FROM MC", "$Thermophiles FROM ProjectCard")
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")

      assertCounts(0 to "MC")
      doTask("Pass")
      doTasks("32 MC", "Steel", "2 Titanium", "8 Energy", "7 Heat", "2 Plant", "4 Heat FROM Energy")
      doTask("TemperatureStep! BY Admin")
    }
  }

  private fun generation10() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "4 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
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
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$RegolithEaters>")
      doTasks(
          *cardActionTasks("$RegolithEaters", "Action2"),
          "-2 Microbe<$RegolithEaters>",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$CarbonateProcessing FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 Heat]",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Pluto>",
          "3 ProjectCard",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "6 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$BlackPolarDust"),
          "15 Pay<> FROM MC",
          "$BlackPolarDust FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[3 Heat]",
          "OceanTile<Elysium_1_3>",
          "TerraformRating",
          "ProjectCard",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "7 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "7 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$JovianEmbassy"),
          "7 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$JovianEmbassy FROM ProjectCard",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$Windmills"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$Windmills FROM ProjectCard",
          "PROD[Energy]",
      )

      assertCounts(6 to "MC")
      doTask("Pass")
      doTasks("40 MC", "Steel", "2 Titanium", "8 Energy", "13 Heat", "2 Plant", "Heat FROM Energy")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation11() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "7 MC",
      )
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(
          *playCardTasks("$InventionContest"),
          "2 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
          "3 MC",
          "ProjectCard FROM Science",
      )
      doTasks(
          *playCardTasks("$TowingAComet"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$TowingAComet FROM ProjectCard",
          "2 Plant",
          "OxygenStep",
          "3 MC",
          "TerraformRating",
          "OceanTile<Elysium_4_6>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$LavaFlows"),
          "18 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "3 MC",
          "TerraformRating",
          "TerraformRating",
          "LavaFlows_SpecialTile<Elysium_3_1>",
          "2 Titanium",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Elysium_4_7>",
          "TerraformRating",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_7>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
      doTasks(
          *cardActionTasks("$Thermophiles", "Action2"),
          "-2 Microbe<$Thermophiles>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$Thermophiles>")
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *playCardTasks("$ExtractorBalloons"),
          "21 Pay<> FROM MC",
          "$ExtractorBalloons FROM ProjectCard",
          "3 Floater<$ExtractorBalloons>",
      )
      doTasks(
          *playCardTasks("$Airliners"),
          "11 Pay<> FROM MC",
          "$Airliners FROM ProjectCard",
          "PROD[2 MC]",
          "2 Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )

      assertCounts(0 to "MC")
      doTask("Pass")
      doTasks(
          "53 MC",
          "Steel",
          "2 Titanium",
          "8 Energy",
          "13 Heat",
          "2 Plant",
          "5 Heat FROM Energy",
      )
      doTask("OceanTile<Elysium_4_4>! BY Admin")
    }
  }

  private fun generation12() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_6>",
          "OxygenStep",
          "3 Plant",
          "2 MC",
          "TerraformRating",
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
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$RegolithEaters>")
      doTasks(
          *cardActionTasks("$RegolithEaters", "Action2"),
          "-2 Microbe<$RegolithEaters>",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *playCardTasks("$IoSulphurResearch"),
          "17 Pay<> FROM MC",
          "$IoSulphurResearch FROM ProjectCard",
          "Science<$OlympusConference>",
          "3 ProjectCard",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$FusionPower"),
          "6 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$FusionPower FROM ProjectCard",
          "PROD[3 Energy]",
          "ProjectCard FROM Science",
      )
      doTasks(
          *playCardTasks("$InventorsGuild"),
          "9 Pay<> FROM MC",
          "$InventorsGuild FROM ProjectCard",
          "Science<$OlympusConference>",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      // The app omitted Air Raid's 5 M€ transfer from the log.
      doTasks(
          *playCardTasks("$AirRaid"),
          "$AirRaid FROM ProjectCard",
          "-Floater",
          "3 MC",
          "5 MC FROM MC<SoloOpponent>",
      )
      doTasks(
          *playCardTasks("$BactoviralResearch"),
          "10 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
          "ProjectCard FROM Science",
          "14 Microbe<$SulphurEatingBacteria>",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-19 Microbe<$SulphurEatingBacteria>",
          "57 MC",
      )
      doTasks(
          *playCardTasks("$ReleaseOfInertGases"),
          "14 Pay<> FROM MC",
          "$ReleaseOfInertGases FROM ProjectCard",
          "2 TerraformRating",
          "3 MC",
      )
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Elysium_2_5>",
          "TerraformRating",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$Mangrove"),
          "12 Pay<> FROM MC",
          "$Mangrove FROM ProjectCard",
          "GreeneryTile<Elysium_3_5>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_4_5>",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$Algae"),
          "10 Pay<> FROM MC",
          "$Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ganymede>",
          "5 Plant",
      )
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Elysium_5_4>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_5>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_3_4>",
          "OxygenStep",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      assertCounts(3 to "MC")
      doTask("Pass")
      doTasks("67 MC", "Steel", "2 Titanium", "11 Energy", "13 Heat", "4 Plant", "Heat FROM Energy")
      // Decline the final greenery placement.
      doTask("PassBaton")
      assertCounts(0 to "ProjectCard")
      // Final state and score come from /api/player?id=pb64886c6e682.
      assertProduction(m = 3, s = 1, t = 2, p = 4, e = 11, h = 13)
      assertResources(m = 70, s = 1, t = 4, p = 4, e = 11, h = 14)
      assertCounts(64 to "TerraformRating", 0 to "ProjectCard", 18 to "PlayedEvent")
      assertSidebar(gen = 12, temp = 8, oxygen = 14, oceans = 8, venus = 30)
      has("Victory") shouldBe true

      val sum = Summarizer(game)
      sum.net("GreeneryTile", "VictoryPoint<Me>") shouldBe 7
      sum.net("CityTile", "VictoryPoint<Me>") shouldBe 4
      sum.net("Card", "VictoryPoint<Me>") shouldBe 9
      assertCounts(84 to "VictoryPoint")
    }
  }
}

/*
 * Implementation note: complete Research deck order from the earlier source-game clone:
 *
 * RadSuits, SoilFactory, FueledGenerators, NeptunianPowerConsultants, AiCentral, Research,
 * SecurityFleet, ImportOfAdvancedGhg, UrbanDecomposers, FreyjaBiodomes, ProductiveOutpost,
 * Solarnet, InterplanetaryTrade, MolecularPrinting, LunarBeam, LawSuit, NoctisFarming,
 * VestaShipyard, SolarWindPower, ArtificialPhotosynthesis, Steelworks, Heather,
 * AcquiredCompany, LavaTubeSettlement, FoodFactory, Conscription, SulphurEatingBacteria,
 * IndenturedWorkers, Harvest, StaticHarvesting, LunaGovernor, JetStreamMicroscrappers,
 * InvestmentLoan, TundraFarming, TradingColony, RegolithEaters, Herbivores, Satellites,
 * MineralDeposit, FuelFactory, BusinessNetwork, Potatoes, DirectedHeatUsage,
 * CarbonateProcessing, PioneerSettlement, InterstellarColonyShip, Archaebacteria,
 * OutdoorSports, MiningRights, Airliners, CrashSiteCleanup, QuantumExtractor,
 * AsteroidHollowing, InventorsGuild
 */
