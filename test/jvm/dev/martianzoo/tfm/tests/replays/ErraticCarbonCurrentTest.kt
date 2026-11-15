package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete database replay: Erratic Carbon Current (gbf986ef543f0)
// https://terraforming-mars.herokuapp.com/the-end?id=p6674c4a1893d
internal class ErraticCarbonCurrentTest : CardTrackingFullGameTest() {
  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, promos:cards

          RimSettler, Ecologist, Producer, Fundraiser, Philantropist, Terraformer
          Traveller, Collector, Excentric, Investor, Suburbian, Magnate
          """,
          "Blue",
          "Pink",
      )

  private val blue
    get() = p1

  private val pink
    get() = p2

  @Test
  internal fun erraticCarbonCurrent() {
    startGame()
    blue.doTask("20 TerraformRating")
    pink.doTask("20 TerraformRating")
    blue.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    pink.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    blue.doTask("-4 ProjectCard<Selecting>")
    pink.doTask("-6 ProjectCard<Selecting>")
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
  }

  private fun generation1() {
    with(blue) {
      draw(
          Potatoes,
          Hackers,
          Lichen,
          NeutralizerFactory,
          SpaceHotels,
          ExtractorBalloons,
      )
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$CrediCor>>",
          "$CrediCor FROM CorporationCard",
          "57 MC",
          "18 Pay<> FROM MC",
          "6 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(pink) {
      draw(VestaShipyard, MiningRights, BuildingIndustries, Ants)
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Ecoline>>",
          "$Ecoline FROM CorporationCard",
          "36 MC",
          "3 Plant",
          "PROD[2 Plant]",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(blue) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AlbedoPlants>>",
          "$AlbedoPlants FROM PreludeCard",
          "PROD[Plant]",
          "Plant",
          "3 Heat",
      )
      draw(Windmills, UndergroundCity, AerobrakedAmmoniaAsteroid)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ResearchNetwork>>",
          "$ResearchNetwork FROM PreludeCard",
          "PROD[MC]",
          "3 ProjectCard",
      )
    }

    with(pink) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BiosphereSupport>>",
          "$BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
      draw(AstraMechanica, Moss, SymbioticFungus)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Biolab>>",
          "$Biolab FROM PreludeCard",
          "PROD[Plant]",
          "3 ProjectCard",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$AerobrakedAmmoniaAsteroid"),
          "26 Pay<> FROM MC",
          "$AerobrakedAmmoniaAsteroid FROM ProjectCard",
          "2 Microbe",
          "PROD[3 Heat]",
          "PROD[Plant]",
          "4 MC",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$MiningRights"),
          "9 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Hellas_4_4>",
          "PROD[Steel]",
          "2 Steel",
      )
    }

    blue.doTask("Pass")
    pink.turn {
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "15 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
    pink.doTask("Pass")

    blue.doTasks("21 MC", "2 Plant", "3 Heat")
    pink.doTasks("19 MC", "5 Plant", "Steel", "Titanium")
    blue.doTask("TemperatureStep! BY Admin")
  }

  private fun generation2() {
    pink.draw(ExtremeColdFungus, MarsUniversity, BigAsteroid)
    blue.draw(SmallAsteroid, HermeticOrderOfMars)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 23 evidence: after both research purchases.
    blue.assertResources(m = 32, s = 0, t = 0, p = 3, e = 0, h = 6)
    blue.assertProduction(m = 1, s = 0, t = 0, p = 2, e = 0, h = 3)
    pink.assertResources(m = 10, s = 3, t = 1, p = 8, e = 0, h = 0)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 0)
    assertSidebar(gen = 2, temp = -28, oxygen = 0, oceans = 0, venus = 0)

    pink.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_4>",
          "OxygenStep",
          "Steel",
          "TerraformRating",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$Potatoes"),
          "2 Pay<> FROM MC",
          "$Potatoes FROM ProjectCard",
          "-2 Plant",
          "PROD[2 MC]",
          "3 Heat",
      )
    }

    pink.turn {
      discard(SymbioticFungus)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    blue.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$SmallAsteroid"),
          "10 Pay<> FROM MC",
          "$SmallAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
          "-Plant<Pink>",
      )
    }

    pink.doTask("Pass")
    blue.turn {
      doTasks(
          *playCardTasks("$Lichen"),
          "7 Pay<> FROM MC",
          "$Lichen FROM ProjectCard",
          "PROD[Plant]",
          "3 Heat",
      )

      // Test inference: Research Network's wild tag supplies the fourth bio tag.
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Ecologist",
      )
    }
    blue.doTask("Pass")

    blue.doTasks("25 MC", "3 Plant", "4 Heat")
    pink.doTasks("20 MC", "5 Plant", "Steel", "Titanium")
    pink.doTask("OxygenStep! BY Admin")
  }

  private fun generation3() {
    blue.draw(ResearchCoordination, GreatEscarpmentConsortium, Worms)
    pink.draw(NitrophilicMoss)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
    // Database save 48 evidence: after both research purchases.
    blue.assertResources(m = 21, s = 0, t = 0, p = 4, e = 0, h = 8)
    blue.assertProduction(m = 3, s = 0, t = 0, p = 3, e = 0, h = 4)
    pink.assertResources(m = 28, s = 5, t = 2, p = 5, e = 0, h = 0)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 0)
    assertSidebar(gen = 3, temp = -24, oxygen = 2, oceans = 0, venus = 0)

    blue.turn {
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "4 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "21 Pay<> FROM MC",
          "$BigAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 Titanium",
          "2 TerraformRating",
          "PROD[Heat]",
          "-4 Plant<Blue>",
      )
    }

    // Blue has no printed Earth tags yet, so Space Hotels needs both wild tags.
    blue.turn {
      doTasks(
          *playCardTasks("$SpaceHotels"),
          "12 Pay<> FROM MC",
          "$SpaceHotels FROM ProjectCard",
          "PROD[4 MC]",
      )
    }

    pink.doTask("Pass")
    blue.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    blue.doTask("Pass")

    blue.doTasks("30 MC", "3 Plant", "4 Heat")
    pink.doTasks("22 MC", "5 Plant", "Steel", "Titanium", "Heat")
    blue.doTask("OxygenStep! BY Admin")
  }

  private fun generation4() {
    pink.draw(EarthOffice, LargeConvoy)
    blue.draw(AdaptedLichen, ResearchOutpost)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 69 evidence: after both research purchases.
    blue.assertResources(m = 29, s = 0, t = 0, p = 3, e = 0, h = 4)
    blue.assertProduction(m = 7, s = 0, t = 0, p = 3, e = 0, h = 4)
    pink.assertResources(m = 23, s = 6, t = 5, p = 10, e = 0, h = 1)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 1)
    assertSidebar(gen = 4, temp = -18, oxygen = 3, oceans = 0, venus = 0)

    pink.turn {
      doTasks(
          *playCardTasks("$EarthOffice"),
          "1 Pay<> FROM MC",
          "$EarthOffice FROM ProjectCard",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "18 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Hellas_2_5>",
          "Plant",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$LargeConvoy"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "18 Pay<> FROM MC",
      )
      draw(NoctisCity, MartianSurvey)
      doTasks("$LargeConvoy FROM ProjectCard", "5 Plant", "2 ProjectCard")
      draw(Harvest)
      doTasks("OceanTile<Hellas_5_6>", "TerraformRating", "ProjectCard")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_5_5>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$HermeticOrderOfMars"),
          "9 Pay<> FROM MC",
          "$HermeticOrderOfMars FROM ProjectCard",
          "PROD[2 MC]",
          "6 MC",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
      )
      draw(Plantation)
      discard(NoctisCity)
      doTasks("$MarsUniversity FROM ProjectCard", "-ProjectCard", "ProjectCard")
    }

    blue.turn {
      doTasks(
          *playCardTasks("$AdaptedLichen"),
          "8 Pay<> FROM MC",
          "$AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
          "3 Heat",
      )
    }

    pink.doTask("Pass")
    blue.doTask("Pass")

    pink.doTasks("24 MC", "5 Plant", "Steel", "Titanium", "Heat")
    blue.doTasks("32 MC", "4 Plant", "4 Heat")
    pink.doTask("OceanTile<Hellas_6_7>! BY Admin")
  }

  private fun generation5() {
    blue.draw(NitrogenRichAsteroid)
    pink.draw(MarsNomads, BusinessNetwork, ReleaseOfInertGases)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    blue.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 98 evidence: after both research purchases.
    blue.assertResources(m = 29, s = 0, t = 0, p = 8, e = 0, h = 11)
    blue.assertProduction(m = 9, s = 0, t = 0, p = 4, e = 0, h = 4)
    pink.assertResources(m = 21, s = 3, t = 1, p = 13, e = 0, h = 2)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 1)
    assertSidebar(gen = 5, temp = -18, oxygen = 4, oceans = 2, venus = 0)

    blue.turn {
      discard(Hackers)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$NitrogenRichAsteroid"),
          "30 Pay<> FROM MC",
          "$NitrogenRichAsteroid FROM ProjectCard",
          "2 TerraformRating",
          "TemperatureStep",
          "4 MC",
          "TerraformRating",
          "PROD[4 Plant]",
      )
    }

    pink.turn {
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Hellas_4_6>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_4_5>",
          "OxygenStep",
          "Steel",
          "4 MC",
          "TerraformRating",
      )
    }

    blue.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_5>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$Harvest"),
          "4 Pay<> FROM MC",
          "$Harvest FROM ProjectCard",
          "12 MC",
      )
      doTasks(
          *playCardTasks("$MarsNomads"),
          "13 Pay<> FROM MC",
          "$MarsNomads FROM ProjectCard",
          "NomadsMarker<Hellas_8_7>",
      )
    }

    blue.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_6_6>",
          "OxygenStep",
          "4 MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Hellas_9_7 FROM Hellas_8_7>",
          "-6 MC",
          "OceanTile<Hellas_5_7>",
          "TerraformRating",
          "3 Heat",
          "6 MC",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$Windmills"),
          "5 Pay<> FROM MC",
          "$Windmills FROM ProjectCard",
          "PROD[Energy]",
      )
    }

    pink.turn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Terraformer",
      )
    }

    blue.doTask("Pass")
    pink.doTask("Pass")

    blue.doTasks("37 MC", "8 Plant", "4 Heat", "Energy")
    pink.doTasks("28 MC", "5 Plant", "Steel", "Titanium", "Heat")
    blue.doTask("OxygenStep! BY Admin")
  }

  private fun generation6() {
    pink.draw(ImportedGhg, KelpFarming, LavaTubeSettlement)
    blue.draw(Insects, PowerGrid)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 133 evidence: after both research purchases.
    blue.assertResources(m = 32, s = 0, t = 0, p = 8, e = 1, h = 7)
    blue.assertProduction(m = 9, s = 0, t = 0, p = 8, e = 1, h = 4)
    pink.assertResources(m = 19, s = 5, t = 2, p = 5, e = 0, h = 6)
    pink.assertProduction(m = -1, s = 1, t = 1, p = 5, e = 0, h = 1)
    assertSidebar(gen = 6, temp = -12, oxygen = 8, oceans = 4, venus = 0)

    pink.turn {
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "11 Pay<> FROM MC",
          "PROD[Energy]",
      )
      doTasks(
          *playCardTasks("$LavaTubeSettlement"),
          "5 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$LavaTubeSettlement FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "CityTile<Hellas_6_5>",
      )
    }

    blue.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_4>",
          "OxygenStep",
          "Plant",
          "Steel",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$ImportedGhg"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$ExtractorBalloons"),
          "20 Pay<> FROM MC",
          "$ExtractorBalloons FROM ProjectCard",
          "3 Floater<$ExtractorBalloons>",
          "4 MC",
      )
    }

    pink.turn {
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Hellas_8_6 FROM Hellas_9_7>",
          "2 Heat",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$Insects"),
          "8 Pay<> FROM MC",
          "$Insects FROM ProjectCard",
          "PROD[6 Plant]",
      )
      doTasks(
          *playCardTasks("$Worms"),
          "7 Pay<> FROM MC",
          "$Worms FROM ProjectCard",
          "PROD[2 Plant]",
      )
    }

    pink.turn {
      discard(BuildingIndustries, MartianSurvey)

      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
    }

    blue.turn {
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$Moss"),
          "4 Pay<> FROM MC",
          "$Moss FROM ProjectCard",
          "PROD[Plant]",
          "-Plant",
      )
    }

    blue.doTask("Pass")
    pink.doTask("Pass")

    blue.doTasks("39 MC", "16 Plant", "4 Heat", "Energy", "Heat FROM Energy")
    pink.doTasks("30 MC", "6 Plant", "Steel", "Titanium", "2 Heat")
    pink.doTask("OceanTile<Hellas_4_1>! BY Admin")
  }

  private fun generation7() {
    pink.draw(CarbonNanosystems, EcologicalZone)
    blue.draw(IoSulphurResearch, AiCentral)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 173 evidence: after both research purchases.
    blue.assertProduction(m = 9, s = 0, t = 0, p = 16, e = 1, h = 4)
    blue.assertResources(m = 34, s = 1, t = 0, p = 17, e = 1, h = 12)
    pink.assertResources(m = 24, s = 1, t = 2, p = 10, e = 0, h = 13)
    pink.assertProduction(m = 1, s = 1, t = 1, p = 6, e = 0, h = 2)
    assertSidebar(gen = 7, temp = -12, oxygen = 9, oceans = 5, venus = 2)

    blue.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_6>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_4>",
          "OxygenStep",
          "Plant",
          "Steel",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "12 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Animal<$EcologicalZone>",
          "EcologicalZone_SpecialTile<Hellas_2_3>",
          "Plant",
      )
      draw(FieldCappedCity)
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_6>",
          "OxygenStep",
          "ProjectCard",
          "TerraformRating",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$PowerGrid"),
          "17 Pay<> FROM MC",
          "$PowerGrid FROM ProjectCard",
          "PROD[4 Energy]",
      )
      doTasks(
          *playCardTasks("$UndergroundCity"),
          "2 Pay<Class<Steel>> FROM Steel",
          "13 Pay<> FROM MC",
          "$UndergroundCity FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "CityTile<Hellas_7_7>",
          "2 MC",
      )
    }

    pink.turn {
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Hellas_9_7 FROM Hellas_8_6>",
          "-6 MC",
          "OceanTile<Hellas_6_8>",
          "TerraformRating",
          "Steel",
          "4 MC",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    blue.turn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Producer",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "1 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
      )
      draw(ImportOfAdvancedGhg)
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // keep the offered Business Network card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    blue.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$ImportOfAdvancedGhg"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$ImportOfAdvancedGhg FROM ProjectCard",
          "PROD[2 Heat]",
      )
      discard(Ants, ExtremeColdFungus)
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
    }

    blue.turn {
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    pink.turn {
      doTasks(
          *playCardTasks("$NitrophilicMoss"),
          "8 Pay<> FROM MC",
          "$NitrophilicMoss FROM ProjectCard",
          "PROD[2 Plant]",
          "-2 Plant",
          "Animal<$EcologicalZone>",
      )
    }

    blue.doTask("Pass")
    pink.doTask("Pass")

    blue.doTasks("42 MC", "16 Plant", "4 Heat", "3 Energy", "Heat FROM Energy", "2 Steel")
    pink.doTasks("32 MC", "8 Plant", "Steel", "Titanium", "4 Heat")
    blue.doTask("OxygenStep! BY Admin")
  }

  private fun generation8() {
    pink.draw(Pets, AsteroidCard)
    blue.draw(AqueductSystems, OlympusConference, NoctisFarming, VenusSoils)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "Ok", // discard no research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 214 evidence: after both research purchases.
    blue.assertResources(m = 30, s = 2, t = 0, p = 20, e = 3, h = 9)
    blue.assertProduction(m = 9, s = 2, t = 0, p = 16, e = 3, h = 4)
    pink.assertResources(m = 26, s = 3, t = 1, p = 10, e = 0, h = 9)
    pink.assertProduction(m = 0, s = 1, t = 1, p = 8, e = 0, h = 4)
    assertSidebar(gen = 8, temp = -8, oxygen = 13, oceans = 6, venus = 2)

    pink.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_5_4>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Pets"),
          "7 Pay<> FROM MC",
          "$Pets FROM ProjectCard",
          "Animal<$Pets>",
          "Animal<$EcologicalZone>",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$OlympusConference"),
          "2 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$OlympusConference FROM ProjectCard",
          "Science<$OlympusConference>",
      )
      // Research Outpost and Olympus Conference supply the other two science tags.
      doTasks(
          *playCardTasks("$AiCentral"),
          "20 Pay<> FROM MC",
      )
      draw(InterstellarColonyShip)
      doTasks(
          "$AiCentral FROM ProjectCard",
          "PROD[-Energy]",
          "4 MC",
          "ProjectCard FROM Science",
      )
    }

    pink.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$AqueductSystems"),
          "8 Pay<> FROM MC",
      )
      draw(UndergroundDetonations, HeatTrappers, MagneticFieldGeneratorsPromo)
      doTasks(
          "$AqueductSystems FROM ProjectCard",
          "3 SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
      draw(FloatingHabs, ArcticAlgae)
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
    }

    pink.turn {
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Hellas_8_7 FROM Hellas_9_7>",
          "2 Heat",
      )
    }

    blue.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_8>",
          "OxygenStep",
          "4 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_5>",
          "OxygenStep",
          "Plant",
      )
    }

    pink.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$FloatingHabs"),
          "4 Pay<> FROM MC",
          "$FloatingHabs FROM ProjectCard",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$KelpFarming"),
          "17 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
          "Animal<$EcologicalZone>",
      )
    }

    blue.turn {
      discard(UndergroundDetonations)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *cardActionTasks("$FloatingHabs"),
          "2 Pay<> FROM MC",
          "Floater<$ExtractorBalloons>",
      )
    }

    pink.doTask("Pass")
    blue.turn {
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    blue.doTask("Pass")

    blue.doTasks("44 MC", "16 Plant", "4 Heat", "2 Energy", "3 Heat FROM Energy", "2 Steel")
    pink.doTasks("36 MC", "11 Plant", "Steel", "Titanium", "4 Heat")
    pink.doTask("OceanTile<Hellas_3_1>! BY Admin")
  }

  private fun generation9() {
    blue.draw(HiredRaiders, Algae)
    pink.draw(Virus, Comet)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 257 evidence: after both research purchases.
    blue.assertResources(m = 38, s = 2, t = 0, p = 21, e = 2, h = 8)
    blue.assertProduction(m = 9, s = 2, t = 0, p = 16, e = 2, h = 4)
    pink.assertResources(m = 32, s = 6, t = 2, p = 16, e = 0, h = 7)
    pink.assertProduction(m = 2, s = 1, t = 1, p = 11, e = 0, h = 4)
    assertSidebar(gen = 9, temp = -4, oxygen = 14, oceans = 7, venus = 4)

    blue.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Hellas_2_1>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "2 Titanium",
          "TerraformRating",
          "-3 Plant<Blue>",
      )
      doTasks(
          *playCardTasks("$Comet"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Hellas_1_1>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "-3 Plant<Blue>",
      )
    }

    blue.turn {
      draw(InventionContest, Zeppelins)

      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(
          *playCardTasks("$InventionContest"),
          "1 Pay<> FROM MC",
      )
      draw(MediaArchives)
      doTasks(
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "Science<$OlympusConference>",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$Virus"),
          "1 Pay<> FROM MC",
          "$Virus FROM ProjectCard",
          "-5 Plant<Blue>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_6_4>",
          "0 OxygenStep",
          "Steel",
      )
    }

    // Three printed science tags remain in play; Invention Contest is already an event.
    blue.turn {
      doTasks(
          *playCardTasks("$InterstellarColonyShip"),
          "23 Pay<> FROM MC",
          "$InterstellarColonyShip FROM ProjectCard",
          "4 MC",
      )
    }

    pink.turn {
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Hellas_8_6 FROM Hellas_8_7>",
          "2 Heat",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$HiredRaiders"),
          "$HiredRaiders FROM ProjectCard",
          "3 MC<Blue> FROM MC<Pink>",
      )
    }

    pink.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_5>",
          "0 OxygenStep",
      )
    }

    blue.turn {
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "8 Pay<> FROM MC",
          "Magnate",
      )
    }

    pink.turn {
      discard(FieldCappedCity)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    blue.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_8_8>",
          "OxygenStep",
          "Titanium",
      )
    }

    pink.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *playCardTasks("$CarbonNanosystems"),
          "7 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
      )
      draw(EosChasmaNationalPark)
      discard(Plantation)
      doTasks(
          "$CarbonNanosystems FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    blue.turn {
      discard(ArcticAlgae)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    pink.doTask("Pass")
    blue.turn {
      doTasks(
          *cardActionTasks("$FloatingHabs"),
          "2 Pay<> FROM MC",
          "Floater<$ExtractorBalloons>",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
    }
    blue.doTask("Pass")

    pink.doTasks("40 MC", "11 Plant", "Steel", "Titanium", "4 Heat")
    blue.doTasks("48 MC", "16 Plant", "4 Heat", "2 Energy", "2 Heat FROM Energy", "2 Steel")
    blue.doTask("TemperatureStep! BY Admin")
  }

  private fun generation10() {
    blue.draw(AirScrappingExpedition, TropicalResort)
    pink.draw(InvestmentLoan, PublicBaths)
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 307 evidence: after both research purchases.
    blue.assertResources(m = 42, s = 4, t = 1, p = 20, e = 2, h = 6)
    blue.assertProduction(m = 9, s = 2, t = 0, p = 16, e = 2, h = 4)
    pink.assertResources(m = 42, s = 1, t = 1, p = 15, e = 0, h = 5)
    pink.assertProduction(m = 2, s = 1, t = 1, p = 11, e = 0, h = 4)
    assertSidebar(gen = 10, temp = 8, oxygen = 14, oceans = 9, venus = 6)

    pink.turn {
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
      )
      draw(Decomposers)
      doTasks("VenusStep", "TerraformRating", "ProjectCard")
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    blue.turn {
      draw(ConvoyFromEuropa, Grass)

      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      // The source records an explicit unused second action.
    }

    pink.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_5_3>",
          "Animal<$Pets>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_4_2>",
          "0 OxygenStep",
          "Plant",
          "4 MC",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$AirScrappingExpedition"),
          "12 Pay<> FROM MC",
          "$AirScrappingExpedition FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 Floater<$FloatingHabs>",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
      doTasks(
          *playCardTasks("$AstraMechanica"),
          "7 Pay<> FROM MC",
      )
      draw(GreatDamPromo)
      discard(Decomposers)
      doTasks(
          "$AstraMechanica FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
          "ProjectCard FROM PlayedEvent<Class<$InvestmentLoan>>",
          "ProjectCard FROM PlayedEvent<Class<$Harvest>>",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    blue.turn {
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "14 Pay<> FROM MC",
          "Suburbian",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$Harvest"),
          "4 Pay<> FROM MC",
          "$Harvest FROM ProjectCard",
          "12 MC",
          "Animal<$EcologicalZone>",
      )
      draw(ColonizerTrainingCamp)
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Hellas_8_5 FROM Hellas_8_6>",
          "ProjectCard",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$NoctisFarming"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$NoctisFarming FROM ProjectCard",
          "PROD[MC]",
          "2 Plant",
          "3 Heat",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_6>",
          "0 OxygenStep",
          "Plant",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "20 Pay<> FROM MC",
          "Excentric",
      )
    }

    blue.turn {
      draw(Atmoscoop)

      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_7>",
          "0 OxygenStep",
          "Plant",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_9>",
          "0 OxygenStep",
          "Titanium",
          "2 MC",
      )
    }

    pink.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_4_3>",
          "0 OxygenStep",
          "Steel",
      )
      doTasks(
          *playCardTasks("$PublicBaths"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$PublicBaths FROM ProjectCard",
          "6 MC",
      )
    }

    blue.turn {
      doTasks(
          *playCardTasks("$Atmoscoop"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "$Atmoscoop FROM ProjectCard",
          "4 MC",
          "2 VenusStep",
          "2 TerraformRating",
          "2 Floater<$ExtractorBalloons>",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "2 TerraformRating",
      )
    }

    pink.turn {
      discard(EosChasmaNationalPark, GreatDamPromo, ColonizerTrainingCamp)

      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$ReleaseOfInertGases"),
          "14 Pay<> FROM MC",
          "$ReleaseOfInertGases FROM ProjectCard",
          "2 TerraformRating",
      )
    }

    blue.turn {
      discard(GreatEscarpmentConsortium)

      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$MediaArchives"),
          "7 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "17 MC",
      )
    }

    pink.doTask("Pass")
    blue.turn {
      doTasks(
          *playCardTasks("$TropicalResort"),
          "12 Pay<> FROM MC",
          "$TropicalResort FROM ProjectCard",
          "PROD[-2 Heat]",
          "PROD[3 MC]",
      )
      discard(
          Grass,
          MagneticFieldGeneratorsPromo,
          HeatTrappers,
          VenusSoils,
          IoSulphurResearch,
          ConvoyFromEuropa,
          Zeppelins,
          Algae,
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "8 MC FROM ProjectCard")
      doTasks(
          *cardActionTasks("$FloatingHabs"),
          "2 Pay<> FROM MC",
          "Floater<$FloatingHabs>",
      )
      doTasks(
          *playCardTasks("$NeutralizerFactory"),
          "6 Pay<> FROM MC",
          "$NeutralizerFactory FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
    }
    blue.doTask("Pass")

    blue.doTasks("58 MC", "16 Plant", "2 Heat", "2 Energy", "2 Heat FROM Energy", "2 Steel")
    pink.doTasks("42 MC", "11 Plant", "Steel", "Titanium", "4 Heat")
    assertSidebar(gen = 10, temp = 8, oxygen = 14, oceans = 9, venus = 18)

    pink.doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "7 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_2_2>",
        "2 Plant",
        "4 MC",
    )
    pink.doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "7 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_5_2>",
        "2 MC",
    )
    pink.doTask("PassBaton")
    blue.doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_8_7>",
        "2 Heat",
    )
    blue.doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Hellas_9_9>",
    )
    blue.doTask("PassBaton")

    assertCardTrackingComplete()
    blue.cardsHand shouldBe emptySet()
    pink.cardsHand shouldBe emptySet()
    blue.assertCounts(45 to "TerraformRating", 102 to "VictoryPoint", 1 to "Victory")
    pink.assertCounts(41 to "TerraformRating", 85 to "VictoryPoint", 0 to "Victory")
    blue.assertResources(m = 63, s = 2, t = 0, p = 0, e = 2, h = 15)
    blue.assertProduction(m = 13, s = 2, t = 0, p = 16, e = 2, h = 2)
    pink.assertResources(m = 48, s = 1, t = 2, p = 1, e = 0, h = 9)
    pink.assertProduction(m = 1, s = 1, t = 1, p = 11, e = 0, h = 4)
  }

  override val cardArrivalOrder =
      mapOf(
          // Source save 0: Blue's third corporation was Splice, Pink's was Mons Insurance.
          cn("CorporationCard") to
              mapOf(
                  cn("Blue") to listOf(Viron, CrediCor),
                  cn("Pink") to listOf(Ecoline, Inventrix),
              ),
          cn("PreludeCard") to
              mapOf(
                  cn("Blue") to listOf(UnmiContractor, AlbedoPlants, NewPartner, ResearchNetwork),
                  cn("Pink") to
                      listOf(MetalsCompany, BiosphereSupport, GiantSolarCollector, Biolab),
              ),
      )

  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(cn("Blue") to listOf(Viron), cn("Pink") to listOf(Inventrix)),
          cn("PreludeCard") to
              mapOf(
                  cn("Blue") to listOf(UnmiContractor, NewPartner),
                  cn("Pink") to listOf(MetalsCompany, GiantSolarCollector),
              ),
      )

  // Source save 0 and the final Research drafts in saves 22, 47, 68, 97, 132, 172, 213,
  // 256, and 306 identify the setup and Research offers. The source also identifies the
  // Business Network offers. Test assignments: Herbivores and Predators are invented
  // Invention Contest rejects; the archive names only the retained Media Archives.
  override val selectingArrivalOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("Blue") to
                      listOf(
                          // Setup
                          Hackers,
                          Lichen,
                          NeutralizerFactory,
                          OrbitalCleanup,
                          Potatoes,
                          Trees,
                          Supermarkets,
                          CityParks,
                          SpaceHotels,
                          ExtractorBalloons,
                          // Generation 2
                          SmallAsteroid,
                          HermeticOrderOfMars,
                          CallistoPenalMines,
                          EnergyMarket,
                          // Generation 3
                          ResearchCoordination,
                          GreatEscarpmentConsortium,
                          Worms,
                          CaretakerContract,
                          // Generation 4
                          AdaptedLichen,
                          EnergyTapping,
                          ResearchOutpost,
                          OutdoorSports,
                          // Generation 5
                          NitrogenRichAsteroid,
                          DeuteriumExport,
                          ElectroCatapult,
                          BeamFromAThoriumAsteroid,
                          // Generation 6
                          Insects,
                          PowerGrid,
                          JovianEmbassy,
                          SoilFactory,
                          // Generation 7
                          IoSulphurResearch,
                          AiCentral,
                          SpaceElevator,
                          IndustrialCenter,
                          // Generation 8
                          OlympusConference,
                          NoctisFarming,
                          AqueductSystems,
                          VenusSoils,
                          // Generation 9
                          HiredRaiders,
                          Algae,
                          MiningExpedition,
                          Livestock,
                          // Invention Contest
                          MediaArchives,
                          Herbivores,
                          Predators,
                          // Generation 10
                          AirScrappingExpedition,
                          TropicalResort,
                          ProtectedValley,
                          AcquiredCompany,
                      ),
                  cn("Pink") to
                      listOf(
                          // Setup
                          DustSeals,
                          RadSuits,
                          Shuttles,
                          MiningRights,
                          BuildingIndustries,
                          Penguins,
                          VestaShipyard,
                          BactoviralResearch,
                          UrbanizedArea,
                          Ants,
                          // Generation 2
                          BigAsteroid,
                          MarsUniversity,
                          ExtremeColdFungus,
                          Bushes,
                          // Generation 3
                          Casinos,
                          NitrophilicMoss,
                          QuantumExtractor,
                          ArtificialLake,
                          // Generation 4
                          EarthOffice,
                          LargeConvoy,
                          EquatorialMagnetizer,
                          SulphurExports,
                          // Generation 5
                          ReleaseOfInertGases,
                          BusinessNetwork,
                          MarsNomads,
                          BioPrintingFacility,
                          // Generation 6
                          LavaTubeSettlement,
                          KelpFarming,
                          ImportedGhg,
                          Ironworks,
                          // Generation 7
                          EcologicalZone,
                          CarbonNanosystems,
                          CeosFavoriteProject,
                          VenusWaystation,
                          // Business Network, generation 7
                          ImportOfAdvancedGhg,
                          // Generation 8
                          AsteroidCard,
                          Pets,
                          FreyjaBiodomes,
                          WaterImportFromEuropa,
                          // Business Network, generation 8
                          LawSuit,
                          // Generation 9
                          Comet,
                          PeroxidePower,
                          Virus,
                          DesignedMicroorganisms,
                          // Business Network, generation 9
                          MiningQuota,
                          // Generation 10
                          PublicBaths,
                          InvestmentLoan,
                          OreProcessor,
                          LightningHarvest,
                          // Business Network, generation 10
                          AerialMappers,
                      ),
              ),
      )
  override val selectingDepartureOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("Blue") to
                      listOf(
                          // Setup
                          OrbitalCleanup,
                          Trees,
                          Supermarkets,
                          CityParks,
                          // Generation 2
                          CallistoPenalMines,
                          EnergyMarket,
                          // Generation 3
                          CaretakerContract,
                          // Generation 4
                          EnergyTapping,
                          OutdoorSports,
                          // Generation 5
                          DeuteriumExport,
                          ElectroCatapult,
                          BeamFromAThoriumAsteroid,
                          // Generation 6
                          JovianEmbassy,
                          SoilFactory,
                          // Generation 7
                          SpaceElevator,
                          IndustrialCenter,
                          // Generation 9
                          MiningExpedition,
                          Livestock,
                          // Invention Contest
                          Herbivores,
                          Predators,
                          // Generation 10
                          ProtectedValley,
                          AcquiredCompany,
                      ),
                  cn("Pink") to
                      listOf(
                          // Setup
                          DustSeals,
                          RadSuits,
                          Shuttles,
                          Penguins,
                          BactoviralResearch,
                          UrbanizedArea,
                          // Generation 2
                          Bushes,
                          // Generation 3
                          Casinos,
                          QuantumExtractor,
                          ArtificialLake,
                          // Generation 4
                          EquatorialMagnetizer,
                          SulphurExports,
                          // Generation 5
                          BioPrintingFacility,
                          // Generation 6
                          Ironworks,
                          // Generation 7
                          CeosFavoriteProject,
                          VenusWaystation,
                          // Generation 8
                          FreyjaBiodomes,
                          WaterImportFromEuropa,
                          // Business Network, generation 8
                          LawSuit,
                          // Generation 9
                          PeroxidePower,
                          DesignedMicroorganisms,
                          // Business Network, generation 9
                          MiningQuota,
                          // Generation 10
                          OreProcessor,
                          LightningHarvest,
                          // Business Network, generation 10
                          AerialMappers,
                      ),
              ),
      )
}
