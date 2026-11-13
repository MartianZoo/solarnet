package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Archived scenario: checkpoints are retained from the existing test; no local game log is
// available.
internal class SoloGame20230710Test : FullGameTest() {
  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, promos:cards
          fakes:cards
          Tr63SoloObjective
          Callisto, Ganymede, Luna, Miranda
          """,
          "Me",
      )

  override val soloTileLocations =
      listOf("Tharsis_4_1", "Tharsis_5_1", "Tharsis_5_8", "Tharsis_5_7")

  private val me
    get() = p1

  @Test
  internal fun soloGame20230710() {
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

      doTask("Ok") // keep all starting projects
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
          "PlayCard<Class<CorporationCard>, Class<$PharmacyUnion>>",
          "$PharmacyUnion FROM CorporationCard",
      )
      doTask("SearchForCard<TagFilter<Class<ScienceTag>>>").expect("ProjectCard")
      doTasks(
          "54 MC",
          "30 Pay<> FROM MC",
          "10 ProjectCard<Hand FROM Selecting>",
          "Disease<$PharmacyUnion>",
          "Disease<$PharmacyUnion>",
          "-4 MC",
          "-4 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$Manutech>>",
          "$Manutech FROM CorporationCard<Selecting>",
          "35 MC",
          "-42 MC",
          "PROD[Steel]",
          "Steel",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$FakeHeadStart>>",
          "$FakeHeadStart FROM PreludeCard",
          "2 Steel",
          "22 MC",
      )
      doTasks(
          *playCardTasks("$OlympusConference"),
          "3 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$OlympusConference FROM ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
          "Science<$OlympusConference>",
      )
      doTasks(
          *playCardTasks("$StandardTechnology"),
          "6 Pay<> FROM MC",
          "$StandardTechnology FROM ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
          "ProjectCard FROM Science",
      )
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
          "Science<$OlympusConference>",
          "PlayedEvent FROM $PharmacyUnion",
          "3 TerraformRating",
      )
      doTasks(
          *playCardTasks("$IndustrialMicrobes"),
          "12 Pay<> FROM MC",
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
          "Energy",
          "Steel",
      )
      doTask("Pass")
      doTasks("17 MC", "2 Steel", "Energy", "Heat FROM Energy")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation2() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      assertProduction(m = -2, s = 2, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 11, s = 3, t = 0, p = 0, e = 1, h = 1)
      assertDashMiddle(played = 8, actions = 0, tr = 19, hand = 10)
      assertTags(but = 3, sct = 3, eat = 1, mit = 1)

      // The app was showing tagless = 3, so... solarnet found its very first herokuapp bug!
      // https://github.com/terraforming-mars/terraforming-mars/issues/5847
      assertDashRight(events = 1, tagless = 2, cities = 0, colonies = 0)
      assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 2)
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$CarbonateProcessing FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 Heat]",
          "3 Heat",
      )
      doTask("Pass")
      doTasks("17 MC", "2 Steel", "Heat FROM Energy", "3 Heat")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation3() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")

      assertProduction(m = -2, s = 2, t = 0, p = 0, e = 0, h = 3)
      assertResources(m = 25, s = 3, t = 0, p = 0, e = 0, h = 8)
      assertDashMiddle(played = 9, actions = 0, tr = 19, hand = 10)
      assertTags(but = 4, sct = 3, eat = 1, mit = 1)
      assertDashRight(events = 1, tagless = 2, cities = 0, colonies = 0)
      assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 0, venus = 4)
      doTasks(
          *playCardTasks("$DeepWellHeating"),
          "3 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$DeepWellHeating FROM ProjectCard",
          "PROD[Energy]",
          "TemperatureStep",
          "Energy",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$NoctisCity"),
          "18 Pay<> FROM MC",
          "$NoctisCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Tharsis_5_3>",
          "3 MC",
          "2 Plant",
      )
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "1 Pay<> FROM MC",
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
          "Energy",
      )
      doTask("Pass")
      doTasks("21 MC", "2 Steel", "3 Heat", "2 Heat FROM Energy", "Energy")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation4() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
      doTasks(
          *playCardTasks("$EnergySaving"),
          "15 Pay<> FROM MC",
          "$EnergySaving FROM ProjectCard",
          "PROD[3 Energy]",
          "3 Energy",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Callisto>",
          "7 Energy",
      )
      doTask("Pass")
      doTasks("21 MC", "2 Steel", "3 Heat", "4 Energy", "8 Heat FROM Energy")
      doTask("OceanTile<Tharsis_5_5>! BY Admin")
    }
  }

  private fun generation5() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")

      assertProduction(m = 0, s = 2, t = 0, p = 0, e = 4, h = 3)
      assertResources(m = 26, s = 4, t = 0, p = 2, e = 4, h = 16)
      assertDashMiddle(played = 13, actions = 0, tr = 21, hand = 8)
      assertTags(but = 7, sct = 3, pot = 3, eat = 1, mit = 1, cit = 1)
      assertDashRight(events = 1, tagless = 2, cities = 1, colonies = 0)
      assertSidebar(gen = 5, temp = -26, oxygen = 0, oceans = 1, venus = 6)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
          "Heat",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "3 MC",
          "Colony<Luna>",
          "PROD[2 MC]",
          "2 MC",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "13 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$GiantSolarShade"),
          "27 Pay<> FROM MC",
          "$GiantSolarShade FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$GeothermalPower"),
          "3 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$GeothermalPower FROM ProjectCard",
          "PROD[2 Energy]",
          "2 Energy",
      )
      doTask("Pass")
      doTasks("28 MC", "2 Steel", "4 Heat", "6 Energy", "3 Heat FROM Energy")
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation6() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
          "Heat",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ganymede>",
          "6 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_3>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
      doTasks(
          *playCardTasks("$FieldCappedCity"),
          "8 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$FieldCappedCity FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[Energy]",
          "3 Plant",
          "2 MC",
          "Energy",
          "CityTile<Tharsis_7_4>",
      )
      doTask("Pass")
      doTasks("32 MC", "2 Steel", "5 Heat", "7 Energy", "4 Heat FROM Energy")
      doTask("OceanTile<Tharsis_6_7>! BY Admin")
    }
  }

  private fun generation7() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      assertProduction(m = 4, s = 2, t = 0, p = 0, e = 7, h = 5)
      assertResources(m = 40, s = 2, t = 0, p = 5, e = 7, h = 10)
      assertDashMiddle(played = 17, actions = 0, tr = 28, hand = 9)
      assertTags(but = 9, spt = 1, sct = 3, pot = 5, eat = 1, vet = 1, mit = 1, cit = 2)
      assertDashRight(events = 2, tagless = 2, cities = 2, colonies = 1)
      assertSidebar(gen = 7, temp = -20, oxygen = 1, oceans = 2, venus = 14)
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
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$ViralEnhancers"),
          "9 Pay<> FROM MC",
          "$ViralEnhancers FROM ProjectCard",
          "Plant",
          "ProjectCard FROM Science",
      )
      doTasks(
          *playCardTasks("$QuantumExtractor"),
          "13 Pay<> FROM MC",
          "$QuantumExtractor FROM ProjectCard",
          "PROD[4 Energy]",
          "Science<$OlympusConference>",
          "4 Energy",
      )
      doTasks(
          *playCardTasks("$SoilFactory"),
          "2 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$SoilFactory FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Plant]",
          "Plant",
      )
      doTask("Pass")
      doTasks("33 MC", "2 Steel", "5 Heat", "10 Energy", "11 Heat FROM Energy", "Plant")
      doTask("OxygenStep! BY Admin")
    }
  }

  private fun generation8() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
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
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "10 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "18 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TerraformRating",
          "TerraformRating",
          "-6 Plant<SoloOpponent>",
          "OceanTile<Tharsis_5_4>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "OceanTile<Tharsis_5_6>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_4>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$MagneticShield"),
          "22 Pay<> FROM MC",
          "$MagneticShield FROM ProjectCard",
          "4 TerraformRating",
      )
      doTask("Pass")
      doTasks("44 MC", "2 Steel", "5 Heat", "10 Energy", "7 Heat FROM Energy", "Plant")
      doTask("OxygenStep! BY Admin")
    }
  }

  private fun generation9() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")

      assertProduction(m = 4, s = 2, t = 0, p = 1, e = 10, h = 5)
      assertResources(m = 44, s = 4, t = 0, p = 6, e = 10, h = 14)
      assertDashMiddle(played = 23, actions = 1, tr = 40, hand = 10)
      assertTags(but = 10, spt = 3, sct = 5, pot = 6, eat = 2, vet = 1, mit = 2, cit = 2)
      assertDashRight(events = 3, tagless = 2, cities = 2, colonies = 1)
      assertSidebar(gen = 9, temp = -10, oxygen = 4, oceans = 4, venus = 14)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "4 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "ProjectCard FROM Science",
          "CityTile<Tharsis_8_6>",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$IceMoonColony"),
          "20 Pay<> FROM MC",
          "$IceMoonColony FROM ProjectCard",
          "Colony<Ganymede>",
          "PROD[Plant]",
          "Plant",
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
          "2 ProjectCard",
      )
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "3 MC",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$AsteroidRights>")
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ganymede>",
          "3 Plant",
          "Plant",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_7>",
          "OxygenStep",
          "ProjectCard",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$PermafrostExtraction"),
          "7 Pay<> FROM MC",
          "$PermafrostExtraction FROM ProjectCard",
          "OceanTile<Tharsis_9_9>",
          "TerraformRating",
          "2 Titanium",
      )
      doTask("Pass")
      doTasks("50 MC", "2 Steel", "5 Heat", "10 Energy", "7 Heat FROM Energy", "2 Plant")
      doTask("OxygenStep! BY Admin")
    }
  }

  private fun generation10() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *playCardTasks("$MiningExpedition"),
          "11 Pay<> FROM MC",
          "$MiningExpedition FROM ProjectCard",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
          "-2 Plant<SoloOpponent>",
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
          *playCardTasks("$StripMine"),
          "4 Pay<Class<Steel>> FROM Steel",
          "12 Pay<> FROM MC",
          "$StripMine FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "PROD[Titanium]",
          "OxygenStep",
          "OxygenStep",
          "2 Steel",
          "Titanium",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$SubZeroSaltFish"),
          "4 Pay<> FROM MC",
          "$SubZeroSaltFish FROM ProjectCard",
          "PROD[-Plant<SoloOpponent>]",
          "Animal<$SubZeroSaltFish>", // Viral Enhancers
      )
      doTasks(*cardActionTasks("$SubZeroSaltFish"), "Animal<$SubZeroSaltFish>")
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$GhgProducingBacteria"),
          "7 Pay<> FROM MC",
          "$GhgProducingBacteria FROM ProjectCard",
          "Science<$OlympusConference>",
          "Plant",
      )
      doTasks(
          *playCardTasks("$ImportedNitrogen"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$ImportedNitrogen FROM ProjectCard",
          "TerraformRating",
          "4 Plant",
          "3 Microbe<$GhgProducingBacteria>",
          "2 Animal<$SubZeroSaltFish>",
      )
      doTasks(
          *cardActionTasks("$GhgProducingBacteria", "Action2"),
          "-2 Microbe<$GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
      doTasks(
          *playCardTasks("$ArtificialLake"),
          "4 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$ArtificialLake FROM ProjectCard",
          "OceanTile<Tharsis_6_6>",
          "TerraformRating",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_5_2>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
      doTasks(*playCardTasks("$RefugeeCamps"), "9 Pay<> FROM MC", "$RefugeeCamps FROM ProjectCard")
      doTasks(*cardActionTasks("$RefugeeCamps"), "PROD[-MC]", "Camp<$RefugeeCamps>")
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Callisto>",
          "13 Energy",
      )
      doTask("Pass")
      doTasks(
          "60 MC",
          "4 Steel",
          "5 Heat",
          "8 Energy",
          "20 Heat FROM Energy",
          "2 Plant",
          "Titanium",
      )
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation11() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all four research cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )

      assertProduction(m = 3, s = 4, t = 1, p = 2, e = 8, h = 5)
      assertResources(m = 55, s = 4, t = 1, p = 7, e = 8, h = 27)
      assertDashMiddle(played = 33, actions = 4, tr = 57, hand = 7)
      assertTags(but = 13, spt = 4, sct = 7, pot = 6, eat = 3, vet = 1, mit = 3, ant = 1, cit = 3)
      assertDashRight(events = 6, tagless = 2, cities = 3, colonies = 2)
      assertSidebar(gen = 11, temp = 0, oxygen = 10, oceans = 8, venus = 18)
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
      doTasks(*cardActionTasks("$RefugeeCamps"), "PROD[-MC]", "Camp<$RefugeeCamps>")
      doTasks(
          *playCardTasks("$IceCapMelting"),
          "4 Pay<> FROM MC",
          "$IceCapMelting FROM ProjectCard",
          "OceanTile<Tharsis_1_4>",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "10 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "Ok", // no titanium paid
          "3 Pay<> FROM MC",
          "$TransNeptuneProbe FROM ProjectCard",
          "ProjectCard FROM Science",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "3 MC",
          "MC",
          "CityTile<Tharsis_6_5>",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$UrbanizedArea"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$UrbanizedArea FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "2 MC",
          "CityTile<Tharsis_7_5>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_5>",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$SubZeroSaltFish"), "Animal<$SubZeroSaltFish>")
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$AsteroidRights>")
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "3 MC",
          "GreeneryTile<Tharsis_8_4>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$JovianEmbassy"),
          "3 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$JovianEmbassy FROM ProjectCard",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$DawnCity"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$DawnCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "CityTile<DawnCity_RemoteArea>",
          "Titanium",
      )
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "3 MC",
          "TerraformRating",
      )
      doTask("Pass")
      doTasks(
          "70 MC",
          "4 Steel",
          "5 Heat",
          "6 Energy",
          "5 Heat FROM Energy",
          "2 Plant",
          "2 Titanium",
      )
      doTask("VenusStep! BY Admin")
    }
  }

  private fun generation12() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      assertProduction(m = 5, s = 4, t = 2, p = 2, e = 6, h = 5)
      assertResources(m = 68, s = 4, t = 3, p = 2, e = 6, h = 13)
      assertDashMiddle(played = 38, actions = 4, tr = 65, hand = 4)
      // (but = 15, spt = 6, ...)
      assertTags(15, 6, sct = 8, pot = 6, eat = 3, jot = 1, vet = 1, mit = 3, ant = 1, cit = 5)
      assertDashRight(events = 7, tagless = 2, cities = 6, colonies = 2)
      assertSidebar(gen = 12, temp = 6, oxygen = 12, oceans = 9, venus = 22)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTasks(*cardActionTasks("$SubZeroSaltFish"), "Animal<$SubZeroSaltFish>")
      doTasks(*cardActionTasks("$RefugeeCamps"), "PROD[-MC]", "Camp<$RefugeeCamps>")
      // uselessly
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTasks(
          *playCardTasks("$MagneticFieldDome"),
          "1 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$MagneticFieldDome FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[Plant]",
          "TerraformRating",
          "Plant",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "3 MC",
          "GreeneryTile<Tharsis_9_6>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$InterstellarColonyShip"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$InterstellarColonyShip FROM ProjectCard",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "4 MC",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "3 MC",
          "MC",
          "CityTile<Tharsis_9_5>",
          "Steel",
      )
      doTasks(
          *playCardTasks("$SpacePort"),
          "6 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$SpacePort FROM ProjectCard",
          "TradeFleet",
          "PROD[-Energy]",
          "PROD[4 MC]",
          "4 MC",
          "CityTile<Tharsis_6_2>",
          "Plant",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ganymede>",
          "4 Plant",
          "Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_6>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "3 MC",
          "GreeneryTile<Tharsis_7_3>",
          "0 OxygenStep", // oxygen is already maxed
      )
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "3 MC",
          "TerraformRating",
      )
      doTask("Pass")
      doTasks("79 MC", "4 Steel", "5 Heat", "3 Energy", "3 Plant", "2 Titanium")
      has("Victory") shouldBe true
      // Decline another final greenery placement.
      doTask("PassBaton")
      // Check the summary data on the you-won page
      val sum = Summarizer(game)
      assertCounts(70 to "TerraformRating")
      sum.net("GreeneryTile", "VictoryPoint<Me>") shouldBe 9
      sum.net("CityTile", "VictoryPoint<Me>") shouldBe 24
      sum.net("Card", "VictoryPoint<Me>") shouldBe 18
      assertCounts(121 to "VictoryPoint")
      assertCounts(82 to "MC")

      // Head Start's two actions occur during Prelude; the source UI says 106.
      (sum.signalCount("InRound", "UseAction<Me, StandardAction>") +
          sum.signalCount("InRound", "DoRequiredActions<Me>")) shouldBe 92
    }
  }
}
