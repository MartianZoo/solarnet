package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete archive replay: Thermal Matter Wave (gafda6ee74f34)
// https://terraforming-mars.herokuapp.com/the-end?id=pccc28386ce4b
// Source log and final player record: _local/replays/Game20260730/
internal class ThermalMatterWaveTest : FullGameTest() {
  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, promos:cards, Tr63SoloObjective
          Ceres, Io, Miranda, Triton
          """,
          "Player1",
      )

  override val soloTileLocations =
      listOf("Tharsis_4_6", "Tharsis_3_6", "Tharsis_6_6", "Tharsis_6_5")

  private val me
    get() = p1

  @Test
  internal fun game20260730() {
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
      doTask("-3 ProjectCard<Selecting>")
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
          "PlayCard<Class<CorporationCard>, Class<$CrediCor>>",
          "$CrediCor FROM CorporationCard",
          "57 MC",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SocietySupport>>",
          "$SocietySupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[Plant]",
          "PROD[Energy]",
          "PROD[Heat]",
      )
      // CrediCor still pays its rebate when Excentric Sponsor pays the project cost.
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>",
          "$ExcentricSponsor FROM PreludeCard",
          "PlayCard<Class<ProjectCard>, Class<$TitanShuttles>, CardLocation>",
          "-23 Owed", // Excentric Sponsor covers Titan Shuttles' full cost
          "Ok", // no MC remainder
          "$TitanShuttles FROM ProjectCard",
          "4 MC",
      )
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$TitanShuttles>")
      doTasks(
          *playCardTasks("$IndustrialMicrobes"),
          "12 Pay<> FROM MC",
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
      )
      doTasks(
          *playCardTasks("$SolarReflectors"),
          "23 Pay<> FROM MC",
          "$SolarReflectors FROM ProjectCard",
          "PROD[5 Heat]",
          "4 MC",
      )
      doTask("Pass")
      doTasks("11 MC", "Plant", "2 Energy", "6 Heat", "Steel")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation2() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$TitanShuttles>")
      doTask("Pass")
      doTasks("11 MC", "Plant", "2 Energy", "6 Heat", "Steel", "2 Heat FROM Energy")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation3() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$TitanShuttles>")
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$HousePrinting"),
          "2 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$HousePrinting FROM ProjectCard",
          "PROD[Steel]",
      )
      doTask("Pass")
      doTasks("12 MC", "Plant", "2 Energy", "6 Heat", "2 Steel", "2 Heat FROM Energy")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation4() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*playCardTasks("$CryoSleep"), "10 Pay<> FROM MC", "$CryoSleep FROM ProjectCard")
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Ceres>",
          "6 Steel",
      )
      doTasks(
          *playCardTasks("$StripMine"),
          "8 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$StripMine FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "PROD[Titanium]",
          "OxygenStep",
          "OxygenStep",
          "4 MC",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$TitanShuttles>")
      doTask("Pass")
      doTasks("15 MC", "Plant", "6 Heat", "4 Steel", "Titanium")
      doTask("OceanTile<Tharsis_6_7>! BY Admin")
    }
  }

  private fun generation5() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      // This temperature step also raises heat production.
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(
          *cardActionTasks("$TitanShuttles", "Action2"),
          "-8 Floater<$TitanShuttles>",
          "8 Titanium",
      )
      doTasks(
          "UseAction<TradeAction, Action3>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Triton>",
          "4 Titanium",
      )
      doTasks(
          *playCardTasks("$SterlingVents"),
          "1 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$SterlingVents FROM ProjectCard",
          "PROD[-2 Heat]",
          "PROD[2 Energy]",
      )
      doTasks(
          *playCardTasks("$ElectroCatapult"),
          "3 Pay<Class<Steel>> FROM Steel",
          "8 Pay<> FROM MC",
          "$ElectroCatapult FROM ProjectCard",
          "PROD[-Energy]",
      )
      doTasks(*cardActionTasks("$ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTasks(
          *playCardTasks("$CorroderSuits"),
          "8 Pay<> FROM MC",
          "$CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("Pass")
      doTasks("18 MC", "Plant", "5 Heat", "4 Steel", "Titanium", "Energy")
      doTask("OceanTile<Tharsis_6_9>! BY Admin")
    }
  }

  private fun generation6() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTasks(
          *playCardTasks("$TowingAComet"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$TowingAComet FROM ProjectCard",
          "2 Plant",
          "OxygenStep",
          "4 MC",
          "TerraformRating",
          "OceanTile<Tharsis_6_8>",
          "TerraformRating",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$SaturnSurfing"),
          "13 Pay<> FROM MC",
          "$SaturnSurfing FROM ProjectCard",
          "Floater<$SaturnSurfing>",
      )
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$SaturnSurfing>")
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "2 MC", "MC")
      doTasks(
          *playCardTasks("$DesignedMicroorganisms"),
          "16 Pay<> FROM MC",
          "$DesignedMicroorganisms FROM ProjectCard",
          "PROD[2 Plant]",
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
      doTasks("21 MC", "3 Plant", "5 Heat", "4 Steel", "Titanium", "Energy", "Heat FROM Energy")
      doTask("OceanTile<Tharsis_4_8>! BY Admin")
    }
  }

  private fun generation7() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(*cardActionTasks("$ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTasks(
          *playCardTasks("$DevelopmentCenter"),
          "3 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$DevelopmentCenter FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "3 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$CorporateStronghold FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Tharsis_5_8>",
          "2 Plant",
          "2 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_5_9>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$SaturnSurfing>")
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "3 MC", "MC")
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "UseAction<TradeAction, Action3>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Io>",
          "13 Heat",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$SpinOffDepartment"),
          "2 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$SpinOffDepartment FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTasks(*playCardTasks("$Tardigrades"), "4 Pay<> FROM MC", "$Tardigrades FROM ProjectCard")
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      // Payment reconstruction: retain one titanium for Interplanetary Trade; that allocation is
      // required to reproduce the later archived balance without a state adjustment.
      doTasks(
          *playCardTasks("$DeimosDownPromo"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "23 Pay<> FROM MC",
          "$DeimosDownPromo FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TemperatureStep",
          "4 Steel",
          "4 MC",
          "ProjectCard",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
          "Ok", // decline removing an opponent's plants
          "DeimosDownPromo_SpecialTile<Tharsis_7_8>",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
      doTask("Pass")
      doTasks("33 MC", "3 Plant", "6 Heat", "4 Steel", "Titanium", "Energy")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation8() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_7>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$SaturnSurfing>")
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "4 MC", "MC")
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "7 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
      )
      doTasks(
          *playCardTasks("$ReleaseOfInertGases"),
          "14 Pay<> FROM MC",
          "$ReleaseOfInertGases FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("Pass")
      doTasks("39 MC", "3 Plant", "6 Heat", "4 Steel", "Titanium", "Energy")
      doTask("OceanTile<Tharsis_5_5>! BY Admin")
    }
  }

  private fun generation9() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$SaturnSurfing>")
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "4 MC", "MC")
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$VenusSoils"),
          "20 Pay<> FROM MC",
          "$VenusSoils FROM ProjectCard",
          "VenusStep",
          "PROD[Plant]",
          "4 MC",
          "ProjectCard",
          "TerraformRating",
          "2 Microbe<$Tardigrades>",
      )
      // Payment reconstruction: using the retained titanium here avoids the earlier two-unit
      // overpayment and is required by the later archived balance.
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "19 Pay<> FROM MC",
          "$InterplanetaryTrade FROM ProjectCard",
          "EACH @WildTagElection { Tag<@WildTagElection>? }",
          "PROD[10 MC]",
          "4 MC",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$ForcedPrecipitation"),
          "8 Pay<> FROM MC",
          "$ForcedPrecipitation FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTask("Pass")
      doTasks("54 MC", "4 Plant", "6 Heat", "4 Steel", "Titanium", "Energy")
      doTask("OceanTile<Tharsis_1_5>! BY Admin")
    }
  }

  private fun generation10() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks("UseAction<TradeAction, Action1>", "8 Pay<> FROM MC", "Trade<Triton>", "4 Titanium")
      doTasks(
          *playCardTasks("$Comet"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "4 MC",
          "ProjectCard",
          "TerraformRating",
          "Ok", // decline removing an opponent's plants
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
          "2 ProjectCard",
          "2 MC",
      )
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "4 MC", "MC")
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$TitanShuttles>")
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTasks(
          *playCardTasks("$IoMiningIndustries"),
          "41 Pay<> FROM MC",
          "$IoMiningIndustries FROM ProjectCard",
          "PROD[2 Titanium]",
          "PROD[2 MC]",
          "4 MC",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$SolarPower"),
          "3 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$SolarPower FROM ProjectCard",
          "PROD[Energy]",
      )
      doTasks(
          *playCardTasks("$Gyropolis"),
          "6 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$Gyropolis FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[3 MC]",
          "PROD[1 MC]",
          "4 MC",
          "ProjectCard",
          "CityTile<Tharsis_3_7>",
          "Steel",
          "2 MC",
          "2 MC",
      )
      // Payment reconstruction: the steel retained on Solar Power is worth its full value here.
      doTasks(
          *playCardTasks("$MedicalLab"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$MedicalLab FROM ProjectCard",
          "PROD[6 MC]",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$AsteroidRights"),
          "10 Pay<> FROM MC",
          "$AsteroidRights FROM ProjectCard",
          "2 Asteroid<$AsteroidRights>",
      )
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$PowerSupplyConsortium"),
          "5 Pay<> FROM MC",
          "$PowerSupplyConsortium FROM ProjectCard",
          "-Production<Anyone, Class<Energy>>",
          "PROD[Energy]",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTask("Pass")
      doTasks("68 MC", "4 Plant", "6 Heat", "4 Steel", "3 Titanium", "Energy")
      doTask("OxygenStep! BY Admin")
    }
  }

  private fun generation11() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$SearchForLife"),
          "3 Pay<> FROM MC",
          "$SearchForLife FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "3 MC", "MC")
      // Payment reconstruction: retain one titanium for Magnetic Shield, where it avoids the
      // Rotator Impacts overpayment and receives full value.
      doTasks(
          *playCardTasks("$SixteenPsyche"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "11 Pay<> FROM MC",
          "$SixteenPsyche FROM ProjectCard",
          "PROD[2 Titanium]",
          "3 Titanium",
          "4 MC",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "ClaimCardReward<TagFilter<Class<MicrobeTag>>, $SearchForLife>",
          "-ProjectCard<Revealed>",
      )
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
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
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$TitanShuttles>")
      // Payment reconstruction: the titanium retained on Rotator Impacts is worth its full value
      // here, avoiding the action's two-unit overpayment.
      doTasks(
          *playCardTasks("$MagneticShield"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$MagneticShield FROM ProjectCard",
          "4 TerraformRating",
          "4 MC",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$BeamFromAThoriumAsteroid"),
          "32 Pay<> FROM MC",
          "$BeamFromAThoriumAsteroid FROM ProjectCard",
          "PROD[3 Heat]",
          "PROD[3 Energy]",
          "4 MC",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTasks(*playCardTasks("$Thermophiles"), "9 Pay<> FROM MC", "$Thermophiles FROM ProjectCard")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_9>",
          "OxygenStep",
          "Titanium",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$ImportedNutrients"),
          "14 Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
          "3 MC",
          "3 Heat",
          "4 Microbe<$Thermophiles>",
      )
      doTasks(
          *cardActionTasks("$Thermophiles", "Action2"),
          "-2 Microbe<$Thermophiles>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$EquatorialMagnetizer"),
          "3 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$EquatorialMagnetizer FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTask("Pass")
      doTasks("78 MC", "4 Plant", "9 Heat", "4 Steel", "5 Titanium", "3 Energy")
      doTask("TemperatureStep! BY Admin")
    }
  }

  private fun generation12() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
          "-ProjectCard",
          "ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Tharsis_5_6>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_5_7>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "2 MC",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Shuttles"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$Shuttles FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
      )
      doTasks(
          *playCardTasks("$PioneerSettlement"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$PioneerSettlement FROM ProjectCard",
          "PROD[-2 MC]",
          "Colony<Triton>",
          "3 Titanium",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Io>",
          "10 Heat",
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
      doTasks(
          *playCardTasks("$TitanFloatingLaunchPad"),
          "18 Pay<> FROM MC",
          "$TitanFloatingLaunchPad FROM ProjectCard",
          "2 Floater<$TitanShuttles>",
      )
      doTasks(*cardActionTasks("$TitanFloatingLaunchPad"), "Floater<$TitanShuttles>")
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // no microbe tag on the revealed card
          "-ProjectCard<Revealed>",
      )
      // Payment reconstruction: retain the titanium from Pioneer Settlement for Terraforming
      // Ganymede, where it receives full value.
      doTasks(
          *playCardTasks("$Atmoscoop"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$Atmoscoop FROM ProjectCard",
          "4 MC",
          "ProjectCard",
          "2 VenusStep",
          "TerraformRating",
          "TerraformRating",
          "2 Floater<$TitanShuttles>",
      )
      doTasks(
          *playCardTasks("$FloaterPrototypes"),
          "2 Pay<> FROM MC",
          "$FloaterPrototypes FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
          "2 Floater<$TitanShuttles>",
      )
      doTasks(
          *cardActionTasks("$TitanShuttles", "Action2"),
          "-11 Floater<$TitanShuttles>",
          "11 Titanium",
      )
      // Payment reconstruction: retain one titanium for Terraforming Ganymede rather than
      // overpaying for Ganymede Colony.
      doTasks(
          *playCardTasks("$GanymedeColony"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$GanymedeColony FROM ProjectCard",
          "CityTile<GanymedeColony_RemoteArea>",
          "4 MC",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$ConvoyFromEuropa"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$ConvoyFromEuropa FROM ProjectCard",
          "ProjectCard",
          "3 MC",
          "3 Heat",
          "OceanTile<Tharsis_9_9>",
          "TerraformRating",
          "2 Titanium",
      )
      // Payment reconstruction: the titanium retained on Pioneer Settlement and Ganymede Colony
      // is worth its full value here, avoiding three units of combined overpayment.
      doTasks(
          *playCardTasks("$TerraformingGanymede"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$TerraformingGanymede FROM ProjectCard",
          "8 TerraformRating",
          "4 MC",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$HiTechLab"),
          "4 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$HiTechLab FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "12 Pay<> FROM MC",
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "2 Titanium",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "Ok", // decline removing an opponent's plants
      )
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "2 MC", "MC")
      doTasks(
          *cardActionTasks("$Thermophiles", "Action2"),
          "-2 Microbe<$Thermophiles>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "UseAction<$ElectroCatapult, Action1>",
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
      doTasks(
          *playCardTasks("$MediaArchives"),
          "8 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "10 MC",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "4 MC",
          "PROD[MC]",
          "CityTile<Tharsis_4_4>",
          "Plant",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "4 MC",
          "GreeneryTile<Tharsis_4_5>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_4>",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$DawnCity"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$DawnCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "NormalCityTile<DawnCity_RemoteArea>",
      )
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$RotatorImpacts>")
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$Windmills"),
          "6 Pay<> FROM MC",
          "$Windmills FROM ProjectCard",
          "PROD[Energy]",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("Pass")
      doTasks("102 MC", "4 Plant", "9 Heat", "4 Steel", "6 Titanium", "Energy")
      // Decline the final greenery placement.
      doTask("PassBaton")
      assertCounts(0 to "ProjectCard")

      assertResources(m = 106, s = 4, t = 6, p = 4, e = 1, h = 17)
      assertProduction(m = 27, s = 4, t = 6, p = 4, e = 1, h = 9)
      assertCounts(0 to "ProjectCard", 58 to "CardFront OR PlayedEvent")
      assertDashRight(events = 10, tagless = 3, cities = 5, colonies = 1)
      assertSidebar(gen = 12, temp = 8, oxygen = 10, oceans = 9, venus = 30)
      assertTags(15, 16, 10, 5, eat = 3, jot = 8, vet = 4, plt = 1, mit = 4, ant = 0, cit = 4)

      val sum = Summarizer(game)

      // The source app reports 169 actions; internal flow signals do not measure that quantity.

      // Discounts earned
      // Random automatic order may attribute fewer saturated removals here; see TESTING.md.
      sum.net(
          "GrantedResourceValue<Player1, Class<Metal>, $AdvancedAlloys<Player1>>",
          "Owed<Player1>",
      ) shouldBe -92
      sum.net("$Shuttles", "Owed<Player1>") shouldBe -14
      sum.net("$CryoSleep", "Owed<Player1, Class<Energy>>") shouldBe -2
      sum.net("$CryoSleep", "Owed<Player1, Class<Titanium>>") shouldBe -2

      // Resources and cards gained from active cards
      sum.net("$DevelopmentCenter", "ProjectCard") shouldBe 6
      // sum.net("$ElectroCatapult", "Plant") shouldBe -6
      // sum.net("$ElectroCatapult", "Steel") shouldBe -3
      sum.net("$OptimalAerobraking", "Heat") shouldBe 9
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

      assertCounts(75 to "TerraformRating")
      assertCounts(131 to "VictoryPoint")
      assertCounts(1 to "Victory")
    }
  }
}
