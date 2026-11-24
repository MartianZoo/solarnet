package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.Player
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

// Complete archive replay: Massive Gamma Zone (gf165e91f6977), solo win in generation 12.
// Sources: all database saves for gf165e91f6977 in
// _local/terraforming-mars-solarnet/db/game.db,
// _local/replays/Game20260911/full-log-p662f1cec186f.txt,
// _local/replays/Game20260911/player-p662f1cec186f.json, and
// _local/replays/Game20260911/end-page-p662f1cec186f.html
// http://newazure.local:8080/the-end?id=p662f1cec186f
internal class MassiveGammaZoneTest : FullGameTest() {
  override val globalEventDeck =
      listOf(
          ScienceSummit,
          AiResearch,
          RedResistance,
          LocalTerraformingSupport,
          MinimalImpactPolicy,
          DemocraticReform,
          MoholeLakeGlobalEvent,
          TerraformingLobbying,
          ThawMining,
          PateraBoring,
          ExperimentalLifeforms,
          CorporateAlliance,
          HeatFirstPolicy,
      )
  override val config =
      GameConfig(
          """
          TharsisMap
          PreludeExpansion, prelude2:cards, promos:cards, TurmoilExpansion
          Tr63SoloObjective
          $FakePolderTechDutch
          """,
          "EK",
      )

  override val soloTileLocations =
      listOf("Tharsis_2_2", "Tharsis_3_3", "Tharsis_5_8", "Tharsis_5_9")

  private val me
    get() = p1

  @Test
  internal fun massiveGammaZone() {
    startGame()
    admin.count("Coming<$ScienceSummit>") shouldBe 1
    (admin.count("PartyLeader<Scientists, Neutral>") > 0) shouldBe true
    admin.count("Distant<$AiResearch>") shouldBe 1
    (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
    with(me) {
      doTasks(
          "14 TerraformRating",
          "2 CorporationCard",
          "-CorporationCard",
          "10 ProjectCard<Selecting>",
          "4 PreludeCard",
          "-2 PreludeCard",
          "-5 ProjectCard<Selecting>",
      )
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

    // _local/replays/Game20260911/player-p662f1cec186f.json; final Mars map.
    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -              LSS   WSS    L    [O]   [O]
        |
        | 2 -           [S1]  [C]   [G1]  [G1]  [C1]  [O]
        |
        | 3 -        [C1]  [G1]  [G]   [C1]  [G1]  [G1]  [G1]
        |
        | 4 -     [G1]  [G1]  [C1]  [G1]  [G1]  [C1]  [G1]  [O]
        |
        | 5 -  [C1]  [G1]  [C1]  [O]   [O]   [O]   [G1]  [C]   [G]
        |
        | 6 -     [G1]  [C1]  [G1]  [G1]  [S1]  [O]    WP    WP
        |
        | 7 -        [G1]  [G1]  [C1]  [G1]   L     LP    L
        |
        | 8 -           [C1]  [G1]  [G1]  [C1]   L     LT
        |
        | 9 -              [G1]  [C1]  [G1]   L    [O]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }

  private fun generation1() {
    with(me) {
      // The source selected PolderTECH Dutch, which Canon does not treat as a published promo.
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$FakePolderTechDutch>>",
          "35 MC",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "4 Director<$BoardOfDirectors>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ProjectEden>>",
          "-3 ProjectCard",
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
          "Energy",
          "2 ProjectCard",
          "CityTile<Tharsis_4_6>",
          "Plant",
          "GreeneryTile<Tharsis_3_6>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
      me.turn {
        doTasks(
            "DoRequiredActions",
            "OceanTile<Tharsis_4_8>",
            "TerraformRating",
            "Energy",
            "2 Plant",
            "GreeneryTile<Tharsis_4_7>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "Plant",
            "2 MC",
            "TerraformRating",
        )
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
            "PlayCard<Class<PreludeCard>, Class<$AppliedScience>>",
            "6 Science<$AppliedScience>",
        )
        doTasks(
            *playCardTasks("$BribedCommittee"),
            "7 Pay<> FROM MC",
            "2 TerraformRating",
        )
        doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Plant")
        doTasks(*playCardTasks("$Mine"), "4 Pay<> FROM MC", "PROD[Steel]")
        doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
      }
      doTask("Pass")
      doTasks("20 MC", "2 Heat FROM Energy", "Steel")
          .expect("$RedResistance, Distant<$RedResistance>, PartyLeader<Reds, Neutral>")
      (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
      admin.count("Distant<$RedResistance>") shouldBe 1
    }
  }

  private fun generation2() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-4 ProjectCard<Selecting>",
      )
      me.turn {
        doTasks("UseAction<UsePolicyAction, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
            "PlayCard<Class<PreludeCard>, Class<$CorporateArchives>>",
            "13 MC",
            "7 ProjectCard<Selecting>",
            "2 ProjectCard<Hand FROM Selecting>",
        )
        doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Steel")
        doTasks(
            *playCardTasks("$TitaniumMine"),
            "2 Pay<Class<Steel>> FROM Steel",
            "3 Pay<> FROM MC",
            "PROD[Titanium]",
        )
        doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
      }
      doTask("Pass")
      doTasks("19 MC", "Steel", "Titanium")
      // Turmoil: Sponsored Projects.
      doTasks(
              "EACH",
              "ProjectCard",
          )
          .expect(
              "$LocalTerraformingSupport, Distant<$LocalTerraformingSupport>, PartyFollower<MarsFirst, Neutral>"
          )
      admin.count("PartyLeader<MarsFirst, Neutral>") shouldBe 1
      admin.count("Distant<$LocalTerraformingSupport>") shouldBe 1
      (admin.count("PartyFollower<MarsFirst, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation3() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      me.turn {
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
            "PlayCard<Class<PreludeCard>, Class<$RiseToPower>>",
            "PROD[3 MC]",
            "PartyLeader<Reds>",
            "PartyLeader<Unity>",
            "PartyLeader<Scientists>",
        )
        doTasks(
            *playCardTasks("$ArcticAlgae"),
            "12 Pay<> FROM MC",
            "Plant",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_3_7>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "Steel",
            "4 MC",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Steel")
        doTasks(
            *playCardTasks("$UndergroundDetonations"),
            "3 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
        )
        doTasks(*cardActionTasks("$UndergroundDetonations"), "10 Pay<> FROM MC", "PROD[2 Heat]")
        doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Unity> FROM LobbyDelegate")
      }
      doTask("Pass")
      doTasks("23 MC", "Steel", "Titanium", "2 Heat")
      // Turmoil: Solarnet Shutdown.
      doTask("-0 MC")
          .expect("$MinimalImpactPolicy, Distant<$MinimalImpactPolicy>, PartyLeader<Reds, Neutral>")
      (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
      admin.count("Distant<$MinimalImpactPolicy>") shouldBe 1
      (admin.count("PartyLeader<Reds, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation4() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      me.turn {
        doTasks(*playCardTasks("$MediaGroup"), "6 Pay<> FROM MC")
        doTasks(
            *playCardTasks("$InventionContest"),
            "2 Pay<> FROM MC",
            "3 ProjectCard<Selecting>",
            "ProjectCard<Hand FROM Selecting>",
            "3 MC",
        )
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
            "PlayCard<Class<PreludeCard>, Class<$AntiDesertificationTechniques>>",
            "3 MC",
            "PROD[Plant]",
            "PROD[Steel]",
        )
        doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
        doTasks(
            *cardActionTasks("$AppliedScience"),
            "-Science<$AppliedScience>",
            "Director<$BoardOfDirectors>",
        )
      }
      doTask("Pass")
      doTasks("24 MC", "2 Steel", "Titanium", "2 Heat", "Plant")
      // Turmoil: Homeworld Support.
      doTask("12 MC")
          .expect(
              "$DemocraticReform, Distant<$DemocraticReform>, PartyFollower<MarsFirst, Neutral>"
          )
      (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
      admin.count("Distant<$DemocraticReform>") shouldBe 1
      (admin.count("PartyFollower<MarsFirst, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation5() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
        // The Applied Science wild tag supplies the second science tag required by Mercurian
        // Alloys.
        doTasks(
            *playCardTasks("$MercurianAlloys"),
            "Ok", // no titanium paid
            "3 Pay<> FROM MC",
        )
        doTasks(
            *playCardTasks("$AsteroidCard"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "4 Pay<> FROM MC",
            "TemperatureStep",
            "2 Titanium",
            "3 MC",
            "TerraformRating",
            "Ok", // decline the optional plant removal
        )
        doTasks(
            *playCardTasks("$IceAsteroid"),
            "4 Pay<Class<Titanium>> FROM Titanium",
            "3 Pay<> FROM MC",
            "3 MC",
            "OceanTile<Tharsis_5_6>",
            "TerraformRating",
            "Energy",
            "4 Plant",
            "OceanTile<Tharsis_5_5>",
            "TerraformRating",
            "Energy",
            "4 Plant",
            "2 MC",
        )
        doTasks(
            *playCardTasks("$PoliticalAlliance"),
            "4 Pay<> FROM MC",
            "TerraformRating",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$SpecialDesign"),
            "4 Pay<> FROM MC",
            "SpecialDesign_NextCardEffect",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$Algae"),
            "10 Pay<> FROM MC",
            "Plant",
            "PROD[2 Plant]",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_4_5>",
            "OxygenStep",
            "3 Plant",
            "4 MC",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$BoardOfDirectors"), "PreludeCard", "-PreludeCard")
        // Applied Science's wild tag is the fifth Plant tag counted by Robot Pollinators.
        doTasks(
            *playCardTasks("$RobotPollinators"),
            "9 Pay<> FROM MC",
            "PROD[Plant]",
            "5 Plant",
        )
        doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Unity> FROM LobbyDelegate")
        doTasks(
            *playCardTasks("$AstraMechanica"),
            "7 Pay<> FROM MC",
            "ProjectCard FROM PlayedEvent<Class<$BribedCommittee>>",
            "ProjectCard FROM PlayedEvent<Class<$PoliticalAlliance>>",
        )
        doTasks(
            *playCardTasks("$PoliticalAlliance"),
            "4 Pay<> FROM MC",
            "TerraformRating",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$BribedCommittee"),
            "7 Pay<> FROM MC",
            "2 TerraformRating",
            "3 MC",
        )
      }
      doTask("Pass")
      doTasks("32 MC", "2 Steel", "Titanium", "2 Heat", "4 Plant", "2 Heat FROM Energy")
      // Turmoil: Successful Organisms.
      doTasks("5 Plant")
          .expect(
              "$MoholeLakeGlobalEvent, Distant<$MoholeLakeGlobalEvent>, PartyLeader<Kelvinists, Neutral>"
          )
      (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
      admin.count("Distant<$MoholeLakeGlobalEvent>") shouldBe 1
      (admin.count("PartyLeader<Kelvinists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation6() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      me.turn {
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
            "PlayCard<Class<PreludeCard>, Class<$Merger>>",
            "4 CorporationCard<Selecting>",
            "PlayCard<Class<CorporationCard>, Class<$LakefrontResorts>>",
            "54 MC",
            "-42 MC",
        )
        doTasks(
            *cardActionTasks("$AppliedScience"),
            "-Science<$AppliedScience>",
            "Director<$BoardOfDirectors>",
        )
        doTasks(
            *standardProjectTasks("AquiferProject"),
            "18 Pay<> FROM MC",
            "OceanTile<Tharsis_6_7>",
            "TerraformRating",
            "Steel",
            "Energy",
            "2 Plant",
            "PROD[MC]",
            "Plant",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$RestrictedArea"),
            "11 Pay<> FROM MC",
            "RestrictedArea_SpecialTile<Tharsis_6_6>",
            "Steel",
            "Plant",
            "9 MC",
        )
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
        )
        doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
        doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Greens>")
        doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
        doTasks(*cardActionTasks("$UndergroundDetonations"), "10 Pay<> FROM MC", "PROD[2 Heat]")
      }
      doTask("Pass")
      doTasks("34 MC", "2 Steel", "Titanium", "4 Heat", "4 Plant", "Heat FROM Energy")
      // Turmoil: Dry Deserts.
      doTask("EACH")
      // Dry Deserts removes the ocean placed by Project Eden.
      doTasks(
          "-OceanTile<Tharsis_2_6>",
          "Titanium",
      )
      doTask("Heat")
          .expect(
              "$TerraformingLobbying, Distant<$TerraformingLobbying>, PartyFollower<Kelvinists, Neutral>"
          )
      admin.count("PartyLeader<Greens, Neutral>") shouldBe 1
      admin.count("Distant<$TerraformingLobbying>") shouldBe 1
      (admin.count("PartyFollower<Kelvinists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation7() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
      me.turn {
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "PROD[Heat]",
        )
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
            "PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_3_5>",
            "OxygenStep",
            "Plant",
            "4 MC", // Includes the zero ocean bonus.
            "TerraformRating",
            "2 MC",
        )
        doTasks(
            *standardProjectTasks("AquiferProject"),
            "18 Pay<> FROM MC",
            "OceanTile<Tharsis_5_4>",
            "TerraformRating",
            "Energy",
            "2 Plant",
            "PROD[MC]",
            "2 Plant",
            "5 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_4_4>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "Plant",
            "6 MC",
            "TerraformRating",
            "2 MC",
        )
        // Payment reconstruction: database saves 74–75 preserve all 9 steel and spend 10 M€.
        doTasks(
            *playCardTasks("$RegoPlastics"),
            "Ok", // no steel paid
            "10 Pay<> FROM MC",
        )
        doTasks(
            *playCardTasks("$BiomassCombustors"),
            "1 Pay<Class<Steel>> FROM Steel",
            "1 Pay<> FROM MC",
            "PROD[-Plant<SoloOpponent>]",
            "PROD[2 Energy]",
        )

        doTasks(
            *playCardTasks("$GhgFactories"),
            "4 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
            "PROD[-Energy]",
            "PROD[4 Heat]",
        )
        doTasks(
            *playCardTasks("$HeatTrappers"),
            "2 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
            "PROD[-2 Heat<SoloOpponent>]",
            "PROD[Energy]",
        )

        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_6_5>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "Plant",
            "6 MC",
            "TerraformRating",
            "2 MC",
        )
        doTasks(
            *playCardTasks("$StJosephOfCupertinoMission"),
            "7 Pay<> FROM MC",
        )
        doTasks(
            *cardActionTasks("$StJosephOfCupertinoMission"),
            "1 Pay<Class<Steel>> FROM Steel",
            "2 Pay<> FROM MC",
            "Cathedral<CityTile<Tharsis_4_6>>",
            "CathedralOffer",
            "UseAction<CathedralOption, Action1>",
            "2 Pay<> FROM MC",
            "ProjectCard",
        )
        doTasks(*cardActionTasks("$UndergroundDetonations"), "10 Pay<> FROM MC", "PROD[2 Heat]")
        doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_5_7>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "2 Plant",
            "6 MC",
            "TerraformRating",
            "TemperatureStep",
            "2 MC",
            "TerraformRating",
            "2 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_6_4>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "Plant",
            "3 MC",
            "TerraformRating",
            "2 MC",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "PROD[Heat]",
            "2 MC",
        )
        doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Unity>")
      }
      doTask("Pass")
      doTasks("44 MC", "2 Steel", "Titanium", "12 Heat", "4 Plant", "Heat FROM Energy", "2 Energy")
      // Turmoil: Aquifer Released by Public Council.
      doTasks("3 Plant", "3 Steel")
      doTasks("OceanTile<Tharsis_1_5> BY Admin", "2 Plant", "PROD[MC]", "2 MC")
          .expect("$ThawMining, Distant<$ThawMining>, 2 PartyFollower<Kelvinists, Neutral>")
      (admin.count("PartyFollower<Kelvinists, Neutral>") > 0) shouldBe true
      admin.count("Distant<$ThawMining>") shouldBe 1
      (admin.count("PartyFollower<Kelvinists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation8() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      me.turn {
        doTasks("UseAction<UsePolicyAction, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *playCardTasks("$MineralDeposit"),
            "5 Pay<> FROM MC",
            "5 Steel",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$CulturalMetropolis"),
            "6 Pay<Class<Steel>> FROM Steel",
            "2 Pay<> FROM MC",
            "PROD[-Energy]",
            "PROD[3 MC]",
            "CityTile<Tharsis_7_5>",
            "0 MC",
            "PartyFollower<Greens>",
            "PartyLeader<Greens>",
        )
        doTasks(
            *cardActionTasks("$StJosephOfCupertinoMission"),
            "1 Pay<Class<Steel>> FROM Steel",
            "2 Pay<> FROM MC",
            "Cathedral<CityTile<Tharsis_7_5>>",
            "CathedralOffer",
            "UseAction<CathedralOption, Action1>",
            "2 Pay<> FROM MC",
            "ProjectCard",
        )
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
            "PlayCard<Class<PreludeCard>, Class<$FocusedOrganization>>",
            "ProjectCard",
            "Titanium",
        )
        doTasks(
            *cardActionTasks("$FocusedOrganization"),
            "-ProjectCard",
            "-MC",
            "ProjectCard",
            "Titanium",
        )
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "2 MC",
        )
        doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Kelvinists> FROM LobbyDelegate")
        doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
        // Payment reconstruction: database saves 100–101 spend 18 M€ and 3 of 4 steel.
        doTasks(
            *playCardTasks("$SpaceElevator"),
            "3 Pay<Class<Steel>> FROM Steel",
            "Ok", // no titanium paid
            "18 Pay<> FROM MC",
            "PROD[Titanium]",
        )
        doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
        doTasks(
            *playCardTasks("$Archaebacteria"),
            "6 Pay<> FROM MC",
            "PROD[Plant]",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_8_6>",
            "OxygenStep",
            "Plant",
            "ProjectCard",
            "0 MC",
            "TerraformRating",
            "2 MC",
        )
      }
      doTask("Pass")
      doTasks(
          "50 MC",
          "2 Steel",
          "2 Titanium",
          "12 Heat",
          "5 Plant",
          "Energy",
          "2 Heat FROM Energy",
      )
      // Turmoil: Snow Cover.
      doTasks("-2 TemperatureStep BY Admin", "2 ProjectCard")
          .expect("$PateraBoring, Distant<$PateraBoring>, PartyLeader<Scientists, Neutral>")
      (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
      admin.count("Distant<$PateraBoring>") shouldBe 1
      (admin.count("PartyLeader<Scientists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation9() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *playCardTasks("$HomeostasisBureau"),
            "1 Pay<Class<Steel>> FROM Steel",
            "13 Pay<> FROM MC",
            "PROD[2 Heat]",
        )
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "PROD[Heat]",
            "5 MC",
        )
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(*playCardTasks("$EarthOffice"), "1 Pay<> FROM MC")
        // Payment reconstruction: database saves 113–114 spend 5 M€ and 3 titanium.
        doTasks(
            *playCardTasks("$SolarLogistics"),
            "3 Pay<Class<Titanium>> FROM Titanium",
            "5 Pay<> FROM MC",
            "2 Titanium",
        )
        doTasks(
            *playCardTasks("$GhgShipment"),
            "Ok", // no titanium paid
            "3 Pay<> FROM MC",
            "PROD[Heat]",
            "0 Heat",
            "3 MC",
            "ProjectCard",
        )
        doTasks(
            *playCardTasks("$KelpFarming"),
            "17 Pay<> FROM MC",
            "PROD[2 MC]",
            "PROD[3 Plant]",
            "2 Plant",
        )
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "CityTile<Tharsis_4_3>",
            "Plant",
            "3 MC",
        )
        doTasks(
            *cardActionTasks("$StJosephOfCupertinoMission"),
            "5 Pay<> FROM MC",
            "Cathedral<CityTile<Tharsis_4_3>>",
            "CathedralOffer",
            "UseAction<CathedralOption, Action1>",
            "2 Pay<> FROM MC",
            "ProjectCard",
        )
        doTasks(
            *cardActionTasks("$FocusedOrganization"),
            "-ProjectCard",
            "-MC",
            "ProjectCard",
            "Plant",
        )
        doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Kelvinists> FROM LobbyDelegate")
        doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<MarsFirst>")
      }
      doTask("Pass")
      doTasks("54 MC", "2 Steel", "2 Titanium", "16 Heat", "8 Plant", "Energy", "Heat FROM Energy")
      // Turmoil: Generous Funding.
      // The capped TR payout and the influence payout require separate submissions.
      doTasks("14 MC", "2 MC")
          .expect(
              "$ExperimentalLifeforms, Distant<$ExperimentalLifeforms>, PartyLeader<Greens, Neutral>"
          )
      (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
      admin.count("Distant<$ExperimentalLifeforms>") shouldBe 1
      (admin.count("PartyLeader<Greens, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation10() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(
            *playCardTasks("$TechnologyDemonstration"),
            "1 Pay<Class<Titanium>> FROM Titanium",
            "0 Pay<> FROM MC", // no MC remainder
            "2 ProjectCard",
            "3 MC",
            "ProjectCard",
        )
        doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *playCardTasks("$GiantIceAsteroid"),
            "7 Pay<Class<Titanium>> FROM Titanium",
            "1 Pay<> FROM MC",
            "TemperatureStep",
            "TemperatureStep",
            "3 MC",
            "ProjectCard",
            "TerraformRating",
            "3 MC",
            "TerraformRating",
            "7 MC",
            "OceanTile<Tharsis_2_6>",
            "TerraformRating",
            "Energy",
            "2 Plant",
            "PROD[MC]",
            "2 ProjectCard",
            "5 MC",
            "OceanTile<Tharsis_1_4>",
            "TerraformRating",
            "Energy",
            "2 Plant",
            "PROD[MC]",
            "ProjectCard",
            "5 MC",
            "Ok", // decline the optional plant removal
        )
        doTasks(
            *playCardTasks("$SterlingVents"),
            "1 Pay<Class<Steel>> FROM Steel",
            "2 Pay<> FROM MC",
            "PROD[-2 Heat]",
            "PROD[2 Energy]",
        )
        doTasks(
            *cardActionTasks("$FocusedOrganization"),
            "-ProjectCard",
            "-MC",
            "ProjectCard",
            "Titanium",
        )
        doTasks(
            *playCardTasks("$SmallAsteroid"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "0 Pay<> FROM MC", // no MC remainder
            "TemperatureStep",
            "3 MC",
            "ProjectCard",
            "TerraformRating",
            "5 MC",
            "Ok", // decline the optional plant removal
        )
        // Applied Science's wild tag is the eighth Earth tag counted by Saturn Surfing.
        doTasks(
            *playCardTasks("$SaturnSurfing"),
            "8 Pay<> FROM MC",
            "8 Floater<$SaturnSurfing>",
        )
        doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "5 MC")
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(
            *playCardTasks("$Capital"),
            "26 Pay<> FROM MC",
            "PROD[-2 Energy]",
            "PROD[5 MC]",
            "CapitalTile<Tharsis_2_5>",
            "9 MC",
        )
        doTasks(
            *cardActionTasks("$StJosephOfCupertinoMission"),
            "5 Pay<> FROM MC",
            "Cathedral<CityTile<Tharsis_2_5>>",
            "CathedralOffer",
            "UseAction<CathedralOption, Action1>",
            "2 Pay<> FROM MC",
            "ProjectCard",
        )
        doTasks(*playCardTasks("$RedShips"), "2 Pay<> FROM MC")
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "NormalCityTile<Tharsis_6_3>",
            "2 Plant",
            "0 MC",
        )
        doTasks(
            *playCardTasks("$NoctisCity"),
            "18 Pay<> FROM MC",
            "PROD[-Energy]",
            "PROD[3 MC]",
            "NormalCityTile<NoctisArea>",
            "2 Plant",
            "3 MC",
        )
        // Applied Science's wild tag supplies the second Jovian tag for this play.
        doTasks(
            *playCardTasks("$JovianEnvoys"),
            "2 Pay<> FROM MC",
            "3 MC",
            "PartyFollower<Greens>",
            "PartyLeader<Greens>",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
        doTasks(*cardActionTasks("$RedShips"), "6 MC")
        doTasks(
            *playCardTasks("$CuttingEdgeTechnology"),
            "12 Pay<> FROM MC",
        )
        doTasks(
            *playCardTasks("$CloudSeeding"),
            "9 Pay<> FROM MC",
            "PROD[-MC]",
            "PROD[-Heat<SoloOpponent>]",
            "PROD[2 Plant]",
        )

        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(
            *playCardTasks("$InvestmentLoan"),
            "PROD[-MC]",
            "13 MC",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
            "OceanTile<Tharsis_9_9>",
            "TerraformRating",
            "Energy",
            "2 Plant",
            "PROD[MC]",
            "2 Titanium",
            "0 MC",
            "2 MC",
        )
        doTasks(
            *playCardTasks("$ImportedNitrogen"),
            "3 Pay<Class<Titanium>> FROM Titanium",
            "3 Pay<> FROM MC",
            "TerraformRating",
            "4 Plant",
            "3 Microbe",
            "2 Animal",
            "3 MC",
            "ProjectCard",
            "2 MC",
        )
      }
      doTask("Pass")
      doTasks("77 MC", "2 Steel", "2 Titanium", "14 Heat", "10 Plant", "4 Heat FROM Energy")
      // Turmoil: Mud Slides.
      doTasks("-8 MC", "2 MC")
          .expect("$CorporateAlliance, Distant<$CorporateAlliance>, PartyLeader<Unity, Neutral>")
      (admin.count("PartyFollower<Kelvinists, Neutral>") > 0) shouldBe true
      admin.count("Distant<$CorporateAlliance>") shouldBe 1
      (admin.count("PartyLeader<Unity, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation11() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
        doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "5 MC")
        doTasks(
            *cardActionTasks("$StJosephOfCupertinoMission"),
            "1 Pay<Class<Steel>> FROM Steel",
            "2 Pay<> FROM MC",
            "Cathedral<CityTile<Tharsis_5_3>>",
            "CathedralOffer",
            "UseAction<CathedralOption, Action1>",
            "2 Pay<> FROM MC",
            "ProjectCard",
        )
        doTasks(
            *playCardTasks("$EarthCatapult"),
            "18 Pay<> FROM MC",
        )
        doTasks(
            *playCardTasks("$BusinessContacts"),
            "4 ProjectCard<Selecting>",
            "2 ProjectCard<Hand FROM Selecting>",
            "3 MC",
        )
        doTasks(
            *cardActionTasks("$FocusedOrganization"),
            "-ProjectCard",
            "-Heat",
            "ProjectCard",
            "Steel",
        )
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(
            *playCardTasks("$Meltworks"),
            "Ok", // no steel paid
            "2 Pay<> FROM MC",
        )
        doTasks(*cardActionTasks("$Meltworks"), "5 Pay<Class<Heat>> FROM Heat", "3 Steel")
        doTasks(
            *playCardTasks("$TectonicStressPower"),
            "4 Pay<Class<Steel>> FROM Steel",
            "2 Pay<> FROM MC",
            "PROD[3 Energy]",
        )
        doTasks(
            *playCardTasks("$LavaTubeSettlement"),
            "13 Pay<> FROM MC",
            "PROD[-Energy]",
            "PROD[2 MC]",
            "NormalCityTile<Tharsis_3_1>",
            "ProjectCard",
            "0 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_5_2>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "2 Plant",
            "TerraformRating",
            "2 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_4_2>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "Plant",
            "TerraformRating",
            "2 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_3_2>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "TerraformRating",
            "2 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_4_1>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "Plant",
            "Titanium",
            "TerraformRating",
            "2 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_6_2>",
            "OxygenStep",
            "Plant",
            "4 MC",
            "Plant",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_7_4>",
            "OxygenStep",
            "Plant",
            "4 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_2_4>",
            "OxygenStep",
            "Plant",
            "7 MC",
        )
        doTasks(
            *playCardTasks("$OpenCity"),
            "19 Pay<> FROM MC",
            "PROD[-Energy]",
            "PROD[4 MC]",
            "2 Plant",
            "NormalCityTile<Tharsis_5_1>",
            "2 Plant",
            "0 MC",
        )
        doTasks(
            *playCardTasks("$ViralEnhancers"),
            "7 Pay<> FROM MC",
            "Plant",
        )
        doTasks(
            *playCardTasks("$Farming"),
            "12 Pay<> FROM MC",
            "PROD[2 MC]",
            "PROD[2 Plant]",
            "2 Plant",
            "Plant", // choose Viral Enhancers' plant
        )
        doTasks(
            *playCardTasks("$TundraFarming"),
            "12 Pay<> FROM MC",
            "PROD[Plant]",
            "PROD[2 MC]",
        )
        doTask("Plant", TundraFarming) // Tundra Farming
        doTask("Plant") // Viral Enhancers
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_7_3>",
            "OxygenStep",
            "Plant",
            "4 MC",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "5 MC",
        )
        doTasks(*cardActionTasks("$RedShips"), "6 MC")
        doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "NormalCityTile<Tharsis_3_4>",
            "0 MC",
        )
      }
      doTask("Pass")
      doTasks("96 MC", "2 Steel", "2 Titanium", "14 Heat", "13 Plant", "Energy")
      // Turmoil: Volcanic Eruptions.
      doTasks("PROD[2 Heat]", "2 MC")
          .expect("$HeatFirstPolicy, Distant<$HeatFirstPolicy>, PartyLeader<Kelvinists, Neutral>")
      (admin.count("PartyFollower<Reds, Neutral>") > 0) shouldBe true
      admin.count("Distant<$HeatFirstPolicy>") shouldBe 1
      (admin.count("PartyLeader<Kelvinists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation12() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
        doTasks(*cardActionTasks("$Meltworks"), "5 Pay<Class<Heat>> FROM Heat", "3 Steel")
        doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "5 MC")
        doTasks(
            *cardActionTasks("$StJosephOfCupertinoMission"),
            "1 Pay<Class<Steel>> FROM Steel",
            "2 Pay<> FROM MC",
            "Cathedral<CityTile<Tharsis_6_3>>",
            "CathedralOffer",
            "UseAction<CathedralOption, Action1>",
            "2 Pay<> FROM MC",
            "ProjectCard",
        )
        doTasks(
            *playCardTasks("$Recruitment"),
            "3 MC",
            "PartyFollower<Reds> FROM PartyFollower<Reds, Neutral>",
        )
        doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Reds> FROM LobbyDelegate")
        doTasks(
            "UseAction<PaidLobbyAction, Action1>",
            "5 Pay<> FROM MC",
            "PartyFollower<Scientists>",
        )
        doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Scientists>")
        doTasks(
            *playCardTasks("$EventAnalysts"),
            "1 Pay<> FROM MC",
        )
        // Applied Science's wild tag supplies the seventh science tag.
        doTasks(
            *playCardTasks("$AntiGravityTechnology"),
            "10 Pay<> FROM MC",
        )
        doTasks(
            *cardActionTasks("$FocusedOrganization"),
            "-ProjectCard",
            "-Heat",
            "ProjectCard",
            "Steel",
        )
        doTasks(
            *playCardTasks("$CommercialDistrict"),
            "4 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
            "PROD[-Energy]",
            "PROD[4 MC]",
            "CommercialDistrict_SpecialTile<Tharsis_2_1>",
            "0 MC",
        )
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "NormalCityTile<Tharsis_8_4>",
            "2 Steel",
            "0 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_9_5>",
            "OxygenStep",
            "Plant",
            "Steel",
            "0 MC",
        )
        doTasks(
            *playCardTasks("$MedicalLab"),
            "3 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
            "PROD[10 MC]",
        )
        doTasks(
            *playCardTasks("$MethaneFromTitan"),
            "5 Pay<Class<Titanium>> FROM Titanium",
            "2 Pay<> FROM MC",
            "PROD[2 Heat]",
            "PROD[2 Plant]",
        )
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "NormalCityTile<Tharsis_8_7>",
            "ProjectCard",
            "0 MC",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "7 MC FROM ProjectCard")
        doTasks(*cardActionTasks("$RedShips"), "6 MC")
        doTasks(
            *playCardTasks("$StanfordTorus"),
            "8 Pay<> FROM MC",
            "NormalCityTile<StanfordTorus_RemoteArea>",
        )
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "NormalCityTile<Tharsis_9_6>",
            "2 Steel",
            "0 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Tharsis_8_5>",
            "OxygenStep",
            "Plant",
            "0 MC",
        )
        doTasks(
            *standardProjectTasks("GreeneryProject"),
            "23 Pay<> FROM MC",
            "GreeneryTile<Tharsis_7_6>",
            "OxygenStep",
            "Plant",
            "0 MC",
        )
      }
      doTask("Pass")
      doTasks("113 MC", "2 Steel", "2 Titanium", "18 Heat", "15 Plant", "Heat FROM Energy")

      // Player-record evidence: production completed and final greenery placement is pending.
      assertResources(m = 124, s = 4, t = 2, p = 19, e = 0, h = 27)
      assertProduction(m = 50, s = 2, t = 2, p = 15, e = 0, h = 18)
      assertCounts(63 to "TerraformRating")
      assertSidebar(gen = 12, temp = 8, oxygen = 14, oceans = 9)
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_9_7>",
          "Plant",
          "0 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_2_3>",
          "Plant",
          "0 MC",
      )
      // Decline another final greenery with only 5 plants remaining.
      doTask("Ok") // decline final greenery
      assertCounts(0 to "ProjectCard")
      doTask("171 VictoryPoint")

      // Player-record evidence: complete final dashboard and score table.
      assertResources(m = 124, s = 4, t = 2, p = 5, e = 0, h = 27)
      assertProduction(m = 50, s = 2, t = 2, p = 15, e = 0, h = 18)
      assertCounts(0 to "ProjectCard", 70 to "CardFront OR PlayedEvent")
      assertDashRight(events = 16, tagless = 8)
      assertTags(
          but = 20,
          spt = 5,
          sct = 8,
          pot = 4,
          eat = 8,
          jot = 2,
          plt = 7,
          mit = 3,
          ant = 0,
          cit = 7,
      )
      assertCardResources(5 to SaturnSurfing)

      admin.assertCounts(
          1 to "Current<$ExperimentalLifeforms>",
          1 to "Coming<$CorporateAlliance>",
          1 to "Distant<$HeatFirstPolicy>",
          1 to "Ruling<Kelvinists>",
          1 to "Dominant<Reds>",
          1 to "Chairman<EK>",
      )
      assertCounts(
          1 to "PartyDelegate<MarsFirst>",
          2 to "PartyDelegate<Scientists>",
          0 to "PartyDelegate<Unity>",
          1 to "PartyDelegate<Greens>",
          2 to "PartyDelegate<Reds>",
          0 to "PartyDelegate<Kelvinists>",
          7 to "Delegate",
          0 to "LobbyDelegate",
      )

      assertCounts(63 to "TerraformRating", 171 to "VictoryPoint", 1 to "Victory")
    }
  }
}
