package dev.martianzoo.tfm.tests.curiosities

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.replays.FullGameTest
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** A legal solo opening built to maximize the MC gained during one standard-project action. */
internal class MaximumStandardProjectTest : FullGameTest() {
  override val firstGlobalEvents =
      listOf(
          "DemocraticReform",
          "MinimalImpactPolicy",
          "FreeAcademiaTreaty",
          "PateraBoring",
          "ScienceSummit",
          "TerraformingLobbying",
          "CharismaticWgPresident",
          "SolarnetGlobalEvent",
          "BioengineeringBoom",
          "SelfSufficiencyProgram",
          "RedResistance",
      )
  override val config =
      GameConfig(
          """
          HellasMap
          CorporateEraExpansion, PreludeExpansion, prelude2:cards, promos:cards
          TurmoilExpansion
          """,
          "Me",
      )

  override val soloTileLocations = listOf("Hellas_5_1", "Hellas_6_2", "Hellas_8_4", "Hellas_9_5")

  private val me
    get() = p1

  @Test
  internal fun `one greenery standard project can gain fifty one mc`() {
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
      doTask("Ok") // keep all offered cards
    }
    admin.count("Coming<DemocraticReform>") shouldBe 1
    (admin.count("PartyLeader<MarsFirst, Neutral>") > 0) shouldBe true
    admin.count("Distant<MinimalImpactPolicy>") shouldBe 1
    (admin.count("PartyLeader<Reds, Neutral>") > 0) shouldBe true
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

    me.count("Science<Spire>") shouldBe 12
    admin.count("TemperatureStep") shouldBe 14
    admin.count("OxygenStep") shouldBe 7
    admin.count("OceanTile") shouldBe 8
    admin.count("Ruling<Greens>") shouldBe 1

    // 27 ocean adjacency + 6 Terraforming Deal + 4 CrediCor + 4 Greens + 3 each
    // from Standard Technology and Homeostasis Bureau + 2 each from Suitable Infrastructure and
    // Meat Industry.
    me.completeOperation {
          doTasks(
              *standardProjectTasks("GreeneryProject"),
              "12 PayFromCard<Spire> FROM Science<Spire>",
              "Ok", // twelve science cover the bill
          )
          doTask("7 MC") // CrediCor and Standard Technology.
          doTasks("GreeneryTile<Hellas_3_6>", "2 Plant", "Animal<Herbivores>")
          // Greens, Lakefront Resorts, three adjacent oceans, and Meat Industry.
          doTask("15 MC")
          doTasks("OxygenStep", "TerraformRating", "TemperatureStep", "TerraformRating")
          doTask("7 MC") // Two TR rewards from Terraforming Deal, plus Homeostasis Bureau.
          doTasks("OceanTile<Hellas_5_7>", "3 Heat", "2 Plant", "TerraformRating")
          doTask("20 MC") // Lakefront Resorts, six adjacent oceans, and Terraforming Deal.
          doTask("PROD[MC]")
          doTask("2 MC")
        }
        .expect("51 MC")
  }

  private fun generation1() {
    with(me) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Spire>>",
          "$Spire FROM CorporationCard",
          "50 MC",
          "Science<$Spire>",
          "30 Pay<> FROM MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$LakefrontResorts>>",
          "$LakefrontResorts FROM CorporationCard<Selecting>",
          "54 MC",
          "-42 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$NewPartner>>",
          "$NewPartner FROM PreludeCard",
          "PROD[MC]",
          "2 PreludeCard<Selecting>",
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "$BoardOfDirectors FROM PreludeCard<Selecting>",
          "4 Director<$BoardOfDirectors>",
      )

      count("MC") shouldBe 32
      doTasks("DoRequiredActions", "4 ProjectCard", "-3 ProjectCard")
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
          "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
          "$DoubleDown FROM PreludeCard",
          "CopyPrelude<$Merger>",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$CrediCor>>",
          "$CrediCor FROM CorporationCard<Selecting>",
          "57 MC",
          "-42 MC",
      )
      doTasks(*playCardTasks("$MediaGroup"), "6 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "7 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
          "Science<$Spire>",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$InventionContest"),
          "2 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
          "Science<$Spire>",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$TechnologyDemonstration"),
          "5 Pay<> FROM MC",
          "$TechnologyDemonstration FROM ProjectCard",
          "2 ProjectCard",
          "Science<$Spire>",
          "3 MC",
          "3 MC",
          "3 Heat",
      )
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "7 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
          "Science<$Spire>",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$Virus"),
          "1 Pay<> FROM MC",
          "$Virus FROM ProjectCard",
          "Science<$Spire>",
          "3 MC",
          "-5 Plant<SoloOpponent>",
      )
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
          "Science<$Spire>",
      )
      doTasks(
          *playCardTasks("$StandardTechnology"),
          "6 Pay<> FROM MC",
          "$StandardTechnology FROM ProjectCard",
      )
      doTask("Pass")
      doTask("14 MC")
      admin.count("PartyLeader<Greens, Neutral>") shouldBe 1
      admin.count("Distant<FreeAcademiaTreaty>") shouldBe 1
      (admin.count("PartyLeader<Scientists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation2() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "14 Pay<> FROM MC",
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "2 Titanium",
          "Science<$Spire>",
          "3 MC",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "-3 Plant<SoloOpponent>",
      )
      doTasks(*playCardTasks("$EarthOffice"), "1 Pay<> FROM MC", "$EarthOffice FROM ProjectCard")
      doTasks(
          *playCardTasks("$SpaceHotels"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$SpaceHotels FROM ProjectCard",
          "PROD[4 MC]",
          "Science<$Spire>",
      )
      doTask("Pass")
      doTask("18 MC")
      // Turmoil: Democratic Reform.
      doTasks("0 Plant", "0 Steel")
      doTasks("OceanTile<Hellas_1_1> BY Admin", "PROD[MC]")
      admin.count("PartyLeader<Unity, Neutral>") shouldBe 1
      admin.count("Distant<PateraBoring>") shouldBe 1
      (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation3() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(
          *playCardTasks("$AcquiredCompany"),
          "7 Pay<> FROM MC",
          "$AcquiredCompany FROM ProjectCard",
          "PROD[3 MC]",
      )
      doTask("Pass")
      doTask("22 MC")
      // Turmoil: Minimal Impact Policy.
      doTasks("-OceanTile", "EACH Influence<Me> { StandardResource<Me>! }")
      admin.count("PartyLeader<Scientists, Neutral>") shouldBe 1
      admin.count("Distant<ScienceSummit>") shouldBe 1
      (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation4() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$Sponsors"),
          "3 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTasks(
          *playCardTasks("$MediaArchives"),
          "5 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "6 MC",
      )
      doTask("Pass")
      doTask("23 MC")
      // Turmoil: Free Academia Treaty.
      doTask("0 MC")
      admin.count("PartyLeader<Kelvinists, Neutral>") shouldBe 1
      admin.count("Distant<TerraformingLobbying>") shouldBe 1
      (admin.count("PartyFollower<Kelvinists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation5() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$HomeostasisBureau"),
          "16 Pay<> FROM MC",
          "$HomeostasisBureau FROM ProjectCard",
          "PROD[2 Heat]",
      )
      doTask("Pass")
      doTasks("22 MC", "2 Heat")
      // Turmoil: Patera Boring.
      doTask("PROD[0 Heat]")
      (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
      admin.count("Distant<CharismaticWgPresident>") shouldBe 1
      (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation6() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$ArcticAlgae"),
          "12 Pay<> FROM MC",
          "$ArcticAlgae FROM ProjectCard",
          "Plant",
      )
      doTasks(
          *playCardTasks("$ImportedGhg"),
          "4 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
          "Science<$Spire>",
          "3 MC",
          "3 MC",
          "3 Heat",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$LavaFlows"),
          "18 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "3 MC",
          "TerraformRating",
          "PROD[Heat]",
          "3 MC",
          "TerraformRating",
          "3 MC",
          "LavaFlows_SpecialTile<Hellas_2_2>",
          "2 Plant",
          "0 MC",
      )
      expectChanges(
          "SolarnetGlobalEvent, Distant<SolarnetGlobalEvent>, 2 PartyFollower<Unity, Neutral>"
      ) {
        doTask("Pass")
        doTasks("24 MC", "4 Heat")
        // Turmoil: Science Summit.
        doTasks(
            "EACH Card@ResourceHolder<Player>(HAS CardResource) { CardResource<Card@ResourceHolder>* BY Admin }",
            "0 ProjectCard",
        )
      }
      admin.count("Distant<SolarnetGlobalEvent>") shouldBe 1
    }
  }

  private fun generation7() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "27 Pay<> FROM MC",
          "$BigAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 Titanium",
          "4 MC",
          "Science<$Spire>",
          "3 MC",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "3 MC",
          "TerraformRating",
          "3 MC",
          "-4 Plant<SoloOpponent>",
      )
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "24 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 MC",
          "Science<$Spire>",
          "3 MC",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "3 MC",
          "TerraformRating",
          "3 MC",
          "-Plant<SoloOpponent>",
          "OceanTile<Hellas_4_6>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "Plant",
          "0 MC",
          "OceanTile<Hellas_4_7>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "Plant",
          "1 MC",
          "2 MC",
      )
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
          "PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>",
          "$TerraformingDeal FROM PreludeCard",
      )
      doTask("Pass")
      doTasks("32 MC", "4 Heat")
      // Turmoil: Terraforming Lobbying.
      // Both Generous Funding awards are zero; select the TR award before acknowledging each.
      selectTask("2! MC<Me> / 5 (TerraformRating<Me> - 15) MAX 5")
      doTasks("0 MC", "0 MC")
      admin.count("PartyLeader<Unity, Neutral>") shouldBe 1
      admin.count("Distant<BioengineeringBoom>") shouldBe 1
      (admin.count("PartyLeader<Greens, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation8() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$IceAsteroid"),
          "23 Pay<> FROM MC",
          "$IceAsteroid FROM ProjectCard",
          "4 MC",
          "Science<$Spire>",
          "3 MC",
          "3 MC",
          "3 Heat",
          "OceanTile<Hellas_5_6>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "ProjectCard",
          "1 MC",
          "2 MC",
          "2 MC",
          "OceanTile<Hellas_5_8>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "1 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$Comet"),
          "21 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "4 MC",
          "Science<$Spire>",
          "3 MC",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "3 MC",
          "2 MC",
          "-3 Plant",
          "OceanTile<Hellas_6_7>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "1 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$TowingAComet"),
          "23 Pay<> FROM MC",
          "$TowingAComet FROM ProjectCard",
          "2 Plant",
          "OxygenStep",
          "4 MC",
          "Science<$Spire>",
          "3 MC",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "2 MC",
          "OceanTile<Hellas_6_8>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "Steel",
      )
      // Lakefront Resorts, two adjacent oceans, and Terraforming Deal.
      doTask("8 MC")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_2>",
          "OxygenStep",
          "2 Plant",
          "0 MC",
          "TerraformRating",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_3>",
          "OxygenStep",
          "2 Plant",
          "0 MC",
          "TerraformRating",
          "2 MC",
      )
      doTasks(
          *cardActionTasks("$BoardOfDirectors"),
          "PreludeCard",
          "-12 MC",
          "-Director<$BoardOfDirectors>",
          "PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>",
          "$SuitableInfrastructure FROM PreludeCard",
          "5 Steel",
      )
      expectChanges(
          "SelfSufficiencyProgram, Distant<SelfSufficiencyProgram>, 2 PartyFollower<Greens, Neutral>"
      ) {
        doTask("Pass")
        doTasks("45 MC", "4 Heat")
        // Turmoil: Charismatic WG President.
        doTasks("4 MC", "0 MC")
      }
      admin.count("Distant<SelfSufficiencyProgram>") shouldBe 1
    }
  }

  private fun generation9() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks(
          "Ok", // keep all offered cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Greens>")
      doTasks(
          *playCardTasks("$ProtectedValley"),
          "6 Pay<Class<Steel>> FROM Steel",
          "11 Pay<> FROM MC",
          "$ProtectedValley FROM ProjectCard",
          "PROD[2 MC]",
          "4 MC",
          "Science<$Spire>",
          "2 MC",
          "GreeneryTile<Hellas_2_1>",
          "OxygenStep",
          "2 Plant",
          "0 MC",
          "TerraformRating",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$Plantation"),
          "15 Pay<> FROM MC",
          "$Plantation FROM ProjectCard",
          "GreeneryTile<Hellas_1_4>",
          "OxygenStep",
          "Plant",
          "Steel",
          "0 MC",
          "TerraformRating",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$Algae"),
          "10 Pay<> FROM MC",
          "$Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_3>",
          "OxygenStep",
          "Plant",
          "0 MC",
          "TerraformRating",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$Heather"),
          "6 Pay<> FROM MC",
          "$Heather FROM ProjectCard",
          "PROD[Plant]",
          "Plant",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$ArtificialLake"),
          "1 Pay<Class<Steel>> FROM Steel",
          "13 Pay<> FROM MC",
          "$ArtificialLake FROM ProjectCard",
          "OceanTile<Hellas_2_5>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "Plant",
          "0 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "6 PayFromCard<$Spire> FROM Science<$Spire>",
          "6 Pay<> FROM MC",
          "3 MC",
          "OceanTile<Hellas_1_1>",
          "TerraformRating",
          "PROD[MC]",
          "2 Plant",
          "2 Plant",
          "0 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_5>",
          "OxygenStep",
          "Plant",
          "1 MC",
          "2 MC",
          "TerraformRating",
          "2 MC",
      )
      doTask("Pass")
      doTasks("54 MC", "4 Heat", "3 Plant")
      // Turmoil: Solarnet Global Event.
      doTasks("10 MC", "2 MC")
      admin.count("PartyLeader<Scientists, Neutral>") shouldBe 1
      admin.count("Distant<RedResistance>") shouldBe 1
      (admin.count("PartyLeader<Reds, Neutral>") > 0) shouldBe true
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
      doTasks(*playCardTasks("$MeatIndustry"), "5 Pay<> FROM MC", "$MeatIndustry FROM ProjectCard")
      doTasks(
          *playCardTasks("$SpecialDesign"),
          "4 Pay<> FROM MC",
          "$SpecialDesign FROM ProjectCard",
          "SpecialDesign_NextCardEffect",
          "Science<$Spire>",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$Herbivores"),
          "12 Pay<> FROM MC",
          "$Herbivores FROM ProjectCard",
          "Animal<$Herbivores>",
          "2 MC",
          "PROD[-Plant<SoloOpponent>]",
      )
      doTasks(
          *playCardTasks("$RestrictedArea"),
          "11 Pay<> FROM MC",
          "$RestrictedArea FROM ProjectCard",
          "RestrictedArea_SpecialTile<Hellas_2_6>",
          "Plant",
          "1 MC",
          "2 MC",
      )
    }
  }
}
