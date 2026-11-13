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
internal class SoloGame20230611Test : FullGameTest() {
  override val soloTileLocations = listOf("Hellas_5_1", "Hellas_6_2", "Hellas_8_4", "Hellas_9_5")

  private val me
    get() = p1

  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, promos:cards, Tr63SoloObjective
          -WorldGovernmentRule
          """,
          "Me",
      )

  @Test
  internal fun letsPlay() {
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
      doTask("-5 ProjectCard<Selecting>")
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
          "PlayCard<Class<CorporationCard>, Class<$ValleyTrust>>",
          "$ValleyTrust FROM CorporationCard",
          "37 MC",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )

      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 22, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertDashMiddle(played = 1, actions = 0, tr = 14, hand = 5)
      assertTags(eat = 1) // ...
      assertDashRight(events = 0, tagless = 0, cities = 0)
      assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Biolab>>",
          "$Biolab FROM PreludeCard",
          "PROD[Plant]",
          "3 ProjectCard",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$NewPartner>>",
          "$NewPartner FROM PreludeCard",
          "PROD[MC]",
          "2 PreludeCard<Selecting>",
          "PlayCard<Class<PreludeCard>, Class<$BusinessEmpire>>",
          "$BusinessEmpire FROM PreludeCard<Selecting>",
          "PROD[6 MC]",
          "-6 MC",
      )
      doTasks(
          "DoRequiredActions",
          "3 PreludeCard<Selecting>",
          "PlayCard<Class<PreludeCard>, Class<$GalileanMining>>",
          "$GalileanMining FROM PreludeCard<Selecting>",
          "PROD[2 Titanium]",
          "-5 MC",
      )
      doTasks(
          *playCardTasks("$IndenturedWorkers"),
          "$IndenturedWorkers FROM ProjectCard",
          "IndenturedWorkers_NextCardEffect",
      )
      doTasks(
              *playCardTasks("$IndustrialMicrobes"),
              "4 Pay<> FROM MC",
              "$IndustrialMicrobes FROM ProjectCard",
              "PROD[Energy]",
              "PROD[Steel]",
          )
          .expect("MicrobeTag")
      doTask("Pass")
      doTasks("21 MC", "Plant", "2 Titanium", "Energy", "Steel")
    }
  }

  private fun generation2() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )

      assertProduction(m = 7, s = 1, t = 2, p = 1, e = 1, h = 0)
      assertResources(m = 22, s = 1, t = 2, p = 1, e = 1, h = 0)
      assertDashMiddle(played = 7, actions = 0, tr = 14, hand = 8)
      assertTags(but = 1, sct = 1, eat = 2, jot = 1, mit = 1) // ...
      assertDashRight(events = 1, tagless = 1, cities = 0)
      assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 0)
      doTasks(
              *playCardTasks("$AcquiredCompany"),
              "10 Pay<> FROM MC",
              "$AcquiredCompany FROM ProjectCard",
              "PROD[3 MC]",
          )
          .expect("EarthTag")
      doTask("Pass")
      doTasks("24 MC", "Plant", "2 Titanium", "Energy", "Steel", "Heat FROM Energy")
    }
  }

  private fun generation3() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "2 Titanium",
          "TerraformRating",
          "-2 Plant<SoloOpponent>",
      )
      doTasks(
          *playCardTasks("$PeroxidePower"),
          "2 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
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
          *playCardTasks("$Solarnet"),
          "7 Pay<> FROM MC",
          "$Solarnet FROM ProjectCard",
          "2 ProjectCard",
      )
      doTask("Pass")
      doTasks("24 MC", "Plant", "2 Titanium", "3 Energy", "Steel", "Heat FROM Energy")
    }
  }

  private fun generation4() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-3 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )

      assertProduction(m = 9, s = 1, t = 2, p = 1, e = 3, h = 0)
      assertResources(m = 29, s = 1, t = 4, p = 3, e = 3, h = 2)
      assertDashMiddle(played = 12, actions = 1, tr = 15, hand = 8)
      assertTags(but = 2, sct = 1, pot = 1, eat = 3, jot = 1, vet = 1, mit = 1) // ...
      assertDashRight(events = 2, tagless = 2, cities = 0)
      assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 0, venus = 0)
      doTasks(
          *playCardTasks("$RegolithEaters"),
          "11 Pay<> FROM MC",
          "$RegolithEaters FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *playCardTasks("$SubCrustMeasurements"),
          "1 Pay<Class<Steel>> FROM Steel",
          "16 Pay<> FROM MC",
          "$SubCrustMeasurements FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTask("Pass")
      doTasks("24 MC", "Plant", "2 Titanium", "3 Energy", "Steel", "3 Heat FROM Energy")
    }
  }

  private fun generation5() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
      doTasks(
          *playCardTasks("$SmallAsteroid"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$SmallAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "-2 Plant<SoloOpponent>",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(
          *playCardTasks("$MagneticFieldDome"),
          "1 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$MagneticFieldDome FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[Plant]",
          "TerraformRating",
      )
      doTask("Pass")
      doTasks("28 MC", "2 Plant", "2 Titanium", "Energy", "Steel", "3 Heat FROM Energy", "Heat")
    }
  }

  private fun generation6() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )

      assertProduction(m = 9, s = 1, t = 2, p = 2, e = 1, h = 1)
      assertResources(m = 22, s = 1, t = 5, p = 6, e = 1, h = 9)
      assertDashMiddle(played = 16, actions = 3, tr = 19, hand = 10)
      assertTags(but = 4, sct = 3, pot = 1, eat = 4, jot = 1, vet = 1, mit = 2)
      assertDashRight(events = 3, tagless = 2, cities = 0)
      assertSidebar(gen = 6, temp = -24, oxygen = 0, oceans = 0, venus = 2)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(
          *cardActionTasks("$RegolithEaters", "Action2"),
          "-2 Microbe<$RegolithEaters>",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "Ok", // no steel paid
          "1 Pay<> FROM MC",
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
      )
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "3 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<SoloOpponent>]",
      )
      doTasks(
          *playCardTasks("$MagneticShield"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$MagneticShield FROM ProjectCard",
          "4 TerraformRating",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTask("Pass")
      doTasks("33 MC", "2 Plant", "2 Titanium", "3 Energy", "Steel", "Heat FROM Energy", "Heat")
    }
  }

  private fun generation7() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-1 ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_9_7>",
          "OxygenStep",
          "-6 MC",
          "TerraformRating",
          "OceanTile<Hellas_5_6>",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "2 Pay<Class<Steel>> FROM Steel",
          "12 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Hellas_7_6>",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$Cartel"),
          "7 Pay<> FROM MC",
          "$Cartel FROM ProjectCard",
          "PROD[5 MC]",
      )
      doTasks(
          *playCardTasks("$Supercapacitors"),
          "3 Pay<> FROM MC",
          "$Supercapacitors FROM ProjectCard",
          "PROD[MC]",
      )
      doTask("Pass")
      doTasks("41 MC", "2 Plant", "2 Titanium", "3 Energy", "Steel", "3 Heat FROM Energy", "Heat")
      // The player chose to convert only one of three energy with Supercapacitors.
      doTask("2 Energy FROM Heat")
    }
  }

  private fun generation8() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-1 ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )

      assertProduction(m = 14, s = 1, t = 2, p = 2, e = 3, h = 1)
      assertResources(m = 33, s = 1, t = 4, p = 2, e = 5, h = 5)
      assertDashMiddle(played = 22, actions = 3, tr = 27, hand = 14)
      assertDashRight(events = 3, tagless = 2, cities = 1)
      assertSidebar(gen = 8, temp = -22, oxygen = 2, oceans = 1, venus = 2)
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(
          *playCardTasks("$AtalantaPlanitiaLab"),
          "7 Pay<> FROM MC",
          "$AtalantaPlanitiaLab FROM ProjectCard",
          "2 ProjectCard",
      )
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "1 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$CarbonateProcessing FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 Heat]",
      )
      doTasks(
          *playCardTasks("$EquatorialMagnetizer"),
          "10 Pay<> FROM MC",
          "$EquatorialMagnetizer FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "10 Pay<> FROM MC",
          "$CorporateStronghold FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Hellas_5_5>",
          "2 MC",
      )
      doTask("Pass")
      doTasks("46 MC", "2 Plant", "3 Titanium", "Steel", "5 Heat FROM Energy", "4 Heat")
      // The player chose to convert none of five energy with Supercapacitors.
      doTask("5 Energy FROM Heat")
    }
  }

  private fun generation9() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(
          *cardActionTasks("$RegolithEaters", "Action2"),
          "-2 Microbe<$RegolithEaters>",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Archaebacteria"),
          "5 Pay<> FROM MC",
          "$Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
      )
      doTasks(
          *playCardTasks("$PowerGrid"),
          "17 Pay<> FROM MC",
          "$PowerGrid FROM ProjectCard",
          "PROD[5 Energy]",
      )
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTasks(
          *playCardTasks("$AiCentral"),
          "1 Pay<Class<Steel>> FROM Steel",
          "16 Pay<> FROM MC",
          "$AiCentral FROM ProjectCard",
          "PROD[-Energy]",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTask("Pass")
      doTasks("49 MC", "3 Plant", "3 Titanium", "Steel", "5 Heat", "5 Heat FROM Energy", "3 Energy")
      // The player chose to convert only two of five energy with Supercapacitors.
      doTask("3 Energy FROM Heat")
    }
  }

  private fun generation10() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTask("-4 ProjectCard<Selecting>")

      assertProduction(m = 17, s = 1, t = 3, p = 3, e = 3, h = 5)
      assertResources(m = 56, s = 1, t = 6, p = 7, e = 6, h = 8)
      assertDashMiddle(played = 30, actions = 5, tr = 32, hand = 10)
      assertDashRight(events = 3, tagless = 2, cities = 2)
      assertSidebar(gen = 10, temp = -20, oxygen = 3, oceans = 1, venus = 4)
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$StripMine"),
          "1 Pay<Class<Steel>> FROM Steel",
          "22 Pay<> FROM MC",
          "$StripMine FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "PROD[Titanium]",
          "OxygenStep",
          "OxygenStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Potatoes"),
          "1 Pay<> FROM MC",
          "$Potatoes FROM ProjectCard",
          "-2 Plant",
          "PROD[2 MC]",
      )
      doTasks(
          *playCardTasks("$MirandaResort"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$MirandaResort FROM ProjectCard",
          "PROD[5 MC]",
      )
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Hellas_6_7>",
          "TerraformRating",
          "2 MC",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTask("Pass")
      doTasks("62 MC", "3 Plant", "4 Titanium", "3 Steel", "5 Heat", "6 Heat FROM Energy")
      // The player chose to convert only three of six energy with Supercapacitors.
      doTask("3 Energy FROM Heat")
    }
  }

  private fun generation11() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "-1 ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_6_6>",
          "OxygenStep",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$WaterSplittingPlant FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "6 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
      )
      doTasks(
          *playCardTasks("$QuantumExtractor"),
          "10 Pay<> FROM MC",
          "$QuantumExtractor FROM ProjectCard",
          "PROD[4 Energy]",
      )
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTasks(
          *playCardTasks("$ImportedGhg"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
      )
      doTasks(
          *playCardTasks("$NitrogenRichAsteroid"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "10 Pay<> FROM MC",
          "$NitrogenRichAsteroid FROM ProjectCard",
          "2 TerraformRating",
          "TemperatureStep",
          "PROD[Plant]",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Hellas_7_7>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
      doTask("Pass")
      doTasks("74 MC", "4 Plant", "4 Titanium", "3 Steel", "6 Heat", "3 Energy")
    }
  }

  private fun generation12() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTask("-4 ProjectCard<Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTasks(
          *cardActionTasks("$RegolithEaters", "Action2"),
          "-2 Microbe<$RegolithEaters>",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Greenhouses"),
          "2 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$Greenhouses FROM ProjectCard",
          "4 Plant",
      )
      doTasks(
          *playCardTasks("$TerraformingGanymede"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "18 Pay<> FROM MC",
          "$TerraformingGanymede FROM ProjectCard",
          "4 TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "6 MC FROM ProjectCard")
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "1 Pay<> FROM MC",
          "$TransNeptuneProbe FROM ProjectCard",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_6_5>",
          "OxygenStep",
          "TerraformRating",
      )

      assertProduction(m = 24, s = 3, t = 4, p = 4, e = 2, h = 6)
      assertResources(m = 8, s = 1, t = 0, p = 0, e = 0, h = 1)
      assertDashMiddle(played = 41, actions = 1, tr = 64, hand = 0)
      assertDashRight(events = 6, tagless = 2, cities = 2)
      assertSidebar(gen = 12, temp = -10, oxygen = 11, oceans = 2, venus = 16)
      doTask("Pass")
      doTasks("88 MC", "4 Plant", "4 Titanium", "3 Steel", "6 Heat", "2 Energy")
      has("Victory") shouldBe true

      // Final plant conversion would happen, but...
      assertCounts(4 to "Plant")
      assertCounts(12 to "Tile") // checking for the heck of it
      // Decline the final greenery placement.
      doTask("PassBaton")
      assertCounts(78 to "VictoryPoint") // wow that was not good
    }
  }
}
