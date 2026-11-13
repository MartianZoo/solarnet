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

// Complete archive replay: Synthetic Plasma Current (g5625e0252c7c), solo win in generation 12.
// Source: _local/replays/Game20260908/game-g5625e0252c7c.sqlite and full-log.txt
// http://newazure.local:8080/the-end?id=p145be84b9959
internal class SyntheticPlasmaCurrentTest : FullGameTest() {
  override val firstGlobalEvents =
      listOf(
          "IndependenceMovement",
          "WorldGovernmentDirectives",
          "ScienceSummit",
          "MoralMovement",
          "MoholeLakeGlobalEvent",
          "ExploreFirstDirective",
          "RedResistance",
          "ViralModificationsApproved",
          "SolarnetGlobalEvent",
          "CharismaticWgPresident",
          "BioengineeringBoom",
          "LocalTerraformingSupport",
          "PateraBoring",
      )
  override val config =
      GameConfig(
          """
          TharsisMap
          VenusNextExpansion, PreludeExpansion, prelude2:cards, promos:cards, TurmoilExpansion
          Tr63SoloObjective
          """,
          "Bloo",
      )

  override val soloTileLocations =
      listOf("Tharsis_2_4", "Tharsis_3_5", "Tharsis_8_9", "Tharsis_7_9")

  private val me
    get() = p1

  @Test
  internal fun syntheticPlasmaCurrent() {
    startGame()
    admin.count("Coming<IndependenceMovement>") shouldBe 1
    (admin.count("PartyLeader<MarsFirst, Neutral>") > 0) shouldBe true
    admin.count("Distant<WorldGovernmentDirectives>") shouldBe 1
    (admin.count("PartyLeader<Unity, Neutral>") > 0) shouldBe true
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
          "PlayCard<Class<CorporationCard>, Class<$PhoboLog>>",
          "$PhoboLog FROM CorporationCard",
          "23 MC",
          "10 Titanium",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AcquiredSpaceAgency>>",
          "$AcquiredSpaceAgency FROM PreludeCard",
          "6 Titanium",
          "2 SearchForCard<TagFilter<Class<SpaceTag>>>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$CorridorsOfPower>>",
          "$CorridorsOfPower FROM PreludeCard",
          "TerraformRating",
          "4 MC",
      )
      doTasks(
          *playCardTasks("$AsteroidMining"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$AsteroidMining FROM ProjectCard",
          "PROD[2 Titanium]",
      )
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Scientists> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTasks(
          "UseAction<PaidLobbyAction, Action1>",
          "5 Pay<> FROM MC",
          "PartyLeader<Reds>",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$TowingAComet"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$TowingAComet FROM ProjectCard",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
          "2 ProjectCard",
      )
      doTask("Pass")
      doTasks("17 MC", "2 Titanium")
      // The following Turmoil phase forms government and advances the two visible events;
      // generation 1 has no Current event to resolve yet.
      doTask("OceanTile<Tharsis_6_7>! BY Admin")
          .expect("ScienceSummit, Distant<ScienceSummit>, PartyFollower<Scientists, Neutral>")
      (admin.count("PartyFollower<Reds, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "MarsFirst",
          dominant = "Reds",
          current = "IndependenceMovement",
          coming = "WorldGovernmentDirectives",
      )
      admin.count("Distant<ScienceSummit>") shouldBe 1
      (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation2() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      // Save 10: immediately after Research.
      assertResources(m = 13, s = 0, t = 6, p = 2, e = 0, h = 0)
      assertProduction(m = 0, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertCounts(16 to "TerraformRating")
      assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 2, venus = 0)
      doTasks(
          *playCardTasks("$TitaniumMine"),
          "7 Pay<> FROM MC",
          "$TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTasks(*playCardTasks("$LocalShading"), "4 Pay<> FROM MC", "$LocalShading FROM ProjectCard")
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Greens> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTask("Pass")
      doTasks("16 MC", "3 Titanium")
      // Riots resolves before the Reds government forms, then the visible events advance.
      val trBeforeWgt = count("TerraformRating")
      doTask("OceanTile<Tharsis_5_5>! BY Admin")
      doTask("-0 MC").expect("MoralMovement, Distant<MoralMovement>, PartyLeader<Reds, Neutral>")
      count("TerraformRating") shouldBe trBeforeWgt + 1
      admin.count("PartyLeader<MarsFirst, Neutral>") shouldBe 1
      assertTurmoilState(
          ruling = "Reds",
          chairman = "Bloo",
          dominant = "Scientists",
          removed = "IndependenceMovement",
          current = "WorldGovernmentDirectives",
          coming = "ScienceSummit",
      )
      admin.count("Distant<MoralMovement>") shouldBe 1
      (admin.count("PartyLeader<Reds, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation3() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTask("-4 ProjectCard<Selecting>")

      // Save 17: immediately after Research.
      assertResources(m = 18, s = 0, t = 9, p = 2, e = 0, h = 0)
      assertProduction(m = 0, s = 0, t = 3, p = 0, e = 0, h = 0)
      assertCounts(17 to "TerraformRating")
      assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 3, venus = 0)
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
      )
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Kelvinists> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$GiantSpaceMirror"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$GiantSpaceMirror FROM ProjectCard",
          "PROD[3 Energy]",
      )
      doTasks(
          *playCardTasks("$Moss"),
          "4 Pay<> FROM MC",
          "$Moss FROM ProjectCard",
          "PROD[Plant]",
          "-Plant",
      )
      doTask("Pass")
      doTasks("18 MC", "3 Titanium", "3 Energy", "Plant")
      // Revolution is harmless at this influence, Scientists take power, and the visible events
      // advance.
      val mcBeforeWgt = count("MC")
      val trBeforeWgt = count("TerraformRating")
      val scienceTagsBeforeWgt = count("ScienceTag")
      val scientistDelegatesBeforeWgt = count("PartyDelegate<Scientists>")
      doTask("OceanTile<Tharsis_6_9>! BY Admin")
          .expect(
              "MoholeLakeGlobalEvent, Distant<MoholeLakeGlobalEvent>, PartyFollower<Kelvinists, Neutral>"
          )
      count("MC") shouldBe mcBeforeWgt + scienceTagsBeforeWgt
      count("TerraformRating") shouldBe trBeforeWgt
      count("PartyDelegate<Scientists>") shouldBe scientistDelegatesBeforeWgt - 1
      (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "Scientists",
          dominant = "Greens",
          removed = "WorldGovernmentDirectives",
          current = "ScienceSummit",
          coming = "MoralMovement",
      )
      admin.count("Distant<MoholeLakeGlobalEvent>") shouldBe 1
      (admin.count("PartyFollower<Kelvinists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation4() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")

      // Save 25: immediately after Research.
      assertResources(m = 13, s = 0, t = 9, p = 2, e = 3, h = 0)
      assertProduction(m = 1, s = 0, t = 3, p = 1, e = 3, h = 0)
      assertCounts(17 to "TerraformRating")
      assertSidebar(gen = 4, temp = -30, oxygen = 1, oceans = 4, venus = 0)
      admin.assertCounts(0 to "PartyDelegate<Scientists>")
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$Comet"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "ProjectCard",
          "TerraformRating",
          "OceanTile<Tharsis_5_6>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "2 MC",
          "Ok", // decline the optional plant removal
      )
      doTasks("UseAction<ScientistsPolicy, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "5 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
      )
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Scientists> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTask("Pass")
      doTasks("22 MC", "4 Titanium", "3 Energy", "Plant", "3 Heat FROM Energy")
      // Sponsored Projects draws through its influence effect before Greens form the government;
      // the visible events then advance.
      val mcBeforeWgt = count("MC")
      val trBeforeWgt = count("TerraformRating")
      val bioTagsBeforeWgt = count("BioTag")
      doTask("TemperatureStep! BY Admin")
      doTasks(
              "EACH Card@ResourceHolder<Player>(HAS CardResource) { CardResource<Card@ResourceHolder>* BY Admin }",
              "2 ProjectCard",
          )
          .expect(
              "ExploreFirstDirective, Distant<ExploreFirstDirective>, PartyFollower<Reds, Neutral>"
          )
      count("MC") shouldBe mcBeforeWgt + bioTagsBeforeWgt
      count("TerraformRating") shouldBe trBeforeWgt
      (admin.count("PartyFollower<MarsFirst, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "Greens",
          dominant = "Kelvinists",
          removed = "ScienceSummit",
          current = "MoralMovement",
          coming = "MoholeLakeGlobalEvent",
      )
      admin.count("Distant<ExploreFirstDirective>") shouldBe 1
      (admin.count("PartyFollower<Reds, Neutral>") > 0) shouldBe true
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

      // Save 35: immediately after Research.
      assertResources(m = 12, s = 0, t = 4, p = 5, e = 3, h = 3)
      assertProduction(m = 1, s = 0, t = 4, p = 1, e = 3, h = 0)
      assertCounts(21 to "TerraformRating")
      assertSidebar(gen = 5, temp = -26, oxygen = 1, oceans = 5, venus = 0)
      admin.assertCounts(1 to "PartyDelegate<Scientists>")
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      // Source log: 17 cards revealed and 17 M€ gained.
      doTasks(
          *playCardTasks("$PublicPlans"),
          "7 Pay<> FROM MC",
          "$PublicPlans FROM ProjectCard",
          "17 ProjectCard<Revealed FROM Hand>",
          "17 ProjectCard<Hand FROM Revealed>",
          "17 MC",
      )
      doTasks(
          *playCardTasks("$Satellites"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$Satellites FROM ProjectCard",
          "PROD[6 MC]",
      )
      doTasks(
          *playCardTasks("$WavePower"),
          "8 Pay<> FROM MC",
          "$WavePower FROM ProjectCard",
          "PROD[Energy]",
      )
      doTasks(
          *playCardTasks("$Algae"),
          "10 Pay<> FROM MC",
          "$Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
      )
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Greens> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTask("Pass")
      doTasks("29 MC", "4 Titanium", "4 Energy", "3 Plant", "3 Heat FROM Energy")
      // Strong Society pays for influence, Kelvinists form the government, and the visible events
      // advance.
      val trBeforeWgt = count("TerraformRating")
      doTask("VenusStep! BY Admin")
      doTasks("0 MC", "4 MC")
          .expect("RedResistance, Distant<RedResistance>, PartyFollower<Reds, Neutral>")
      count("TerraformRating") shouldBe trBeforeWgt
      admin.count("PartyLeader<Kelvinists, Neutral>") shouldBe 1
      assertTurmoilState(
          ruling = "Kelvinists",
          dominant = "Reds",
          removed = "MoralMovement",
          current = "MoholeLakeGlobalEvent",
          coming = "ExploreFirstDirective",
      )
      admin.count("Distant<RedResistance>") shouldBe 1
      (admin.count("PartyFollower<Reds, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation6() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")

      // Save 44: immediately after Research.
      assertResources(m = 34, s = 0, t = 6, p = 9, e = 4, h = 6)
      assertProduction(m = 8, s = 0, t = 4, p = 3, e = 4, h = 0)
      assertCounts(21 to "TerraformRating")
      assertSidebar(gen = 6, temp = -26, oxygen = 1, oceans = 5, venus = 2)
      admin.assertCounts(1 to "PartyDelegate<Scientists>")
      doTasks(
          "UseAction<KelvinistsPolicy, Action1>",
          "10 Pay<> FROM MC",
          "PROD[Heat]",
          "PROD[Energy]",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_6>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "8 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "18 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Tharsis_8_6>",
          "ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "2 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<SoloOpponent>]",
      )
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
      doTask("Pass")
      doTasks("31 MC", "4 Titanium", "6 Energy", "5 Plant", "4 Heat FROM Energy", "3 Heat")
      // Snow Cover resolves first, Reds form the government, and the visible events advance.
      doTask("VenusStep! BY Admin")
      doTasks("-2 TemperatureStep BY Admin", "ProjectCard")
          .expect(
              "ViralModificationsApproved, Distant<ViralModificationsApproved>, PartyFollower<Greens, Neutral>"
          )
      (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "Reds",
          chairman = "Neutral",
          dominant = "Scientists",
          removed = "MoholeLakeGlobalEvent",
          current = "ExploreFirstDirective",
          coming = "RedResistance",
      )
      admin.count("Distant<ViralModificationsApproved>") shouldBe 1
      (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
      // FAQ v1.8 p.100 awards the solo Reds bonus only at 20 TR or below. The archived server
      // nevertheless awarded it at 21 after annual revision; retain that source result explicitly.
      exMachina("TerraformRating")
    }
  }

  private fun generation7() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      // Save 55: immediately after Research.
      assertResources(m = 25, s = 0, t = 5, p = 7, e = 6, h = 13)
      assertProduction(m = 9, s = 0, t = 4, p = 5, e = 6, h = 3)
      assertCounts(22 to "TerraformRating")
      assertSidebar(gen = 7, temp = -30, oxygen = 2, oceans = 5, venus = 4)
      admin.assertCounts(3 to "PartyDelegate<Scientists>")
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Reds> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$AtalantaPlanitiaLab"),
          "9 Pay<> FROM MC",
          "$AtalantaPlanitiaLab FROM ProjectCard",
          "2 ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(*playCardTasks("$RedTourismWave"), "$RedTourismWave FROM ProjectCard", "8 MC")
      doTasks(
          *playCardTasks("$ExtractorBalloons"),
          "20 Pay<> FROM MC",
          "$ExtractorBalloons FROM ProjectCard",
          "3 Floater<$ExtractorBalloons>",
      )
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTask("Pass")
      doTasks("31 MC", "4 Titanium", "6 Energy", "5 Plant", "6 Heat FROM Energy", "3 Heat")
      // Scientific Community pays before its discard, Scientists form the government, and the
      // visible events advance.
      val mcBeforeWgt = count("MC")
      val trBeforeWgt = count("TerraformRating")
      doTask("VenusStep! BY Admin")
      doTask("18 MC")
          .expect(
              "SolarnetGlobalEvent, Distant<SolarnetGlobalEvent>, 2 PartyFollower<Unity, Neutral>"
          )
      // Scientific Community pays 18 M€ for cards and influence; the Scientists bonus pays 4 M€.
      count("MC") shouldBe mcBeforeWgt + 22
      count("TerraformRating") shouldBe trBeforeWgt
      (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "Scientists",
          chairman = "Bloo",
          removed = "ExploreFirstDirective",
          current = "RedResistance",
          coming = "ViralModificationsApproved",
      )
      admin.count("Distant<SolarnetGlobalEvent>") shouldBe 1
      (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation8() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      // Save 64: immediately after Research.
      assertResources(m = 51, s = 0, t = 9, p = 12, e = 6, h = 22)
      assertProduction(m = 9, s = 0, t = 4, p = 5, e = 6, h = 3)
      assertCounts(22 to "TerraformRating")
      assertSidebar(gen = 8, temp = -30, oxygen = 2, oceans = 5, venus = 6)
      doTasks("UseAction<ScientistsPolicy, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
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
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTasks(
          *playCardTasks("$CometForVenus"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$CometForVenus FROM ProjectCard",
          "VenusStep",
          "ProjectCard",
          "TerraformRating",
          "Ok", // no MC remainder
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_7>",
          "OxygenStep",
          "ProjectCard",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$CallistoPenalMines"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$CallistoPenalMines FROM ProjectCard",
          "PROD[3 MC]",
      )
      doTasks(
          *playCardTasks("$EnvoysFromVenus"),
          "$EnvoysFromVenus FROM ProjectCard",
          "PartyFollower<Kelvinists>",
          "PartyLeader<Kelvinists>",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$Sabotage"),
          "$Sabotage FROM ProjectCard",
          "Ok", // no MC remainder
      )
      doTasks(*playCardTasks("$Steelworks"), "14 Pay<> FROM MC", "$Steelworks FROM ProjectCard")
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
      )
      // The archive used only two of the available steel cubes for Strip Mine.
      doTasks(
          *playCardTasks("$StripMine"),
          "2 Pay<Class<Steel>> FROM Steel",
          "18 Pay<> FROM MC",
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
          *playCardTasks("$SulphurExports"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$SulphurExports FROM ProjectCard",
          "VenusStep",
          "PROD[4 MC]",
          "TerraformRating",
      )
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Scientists> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTask("Pass")
      doTasks(
          "47 MC",
          "5 Titanium",
          "5 Energy",
          "5 Plant",
          "2 Heat FROM Energy",
          "3 Heat",
          "2 Steel",
      )
      // Homeworld Support pays, Unity forms the government, and the visible events advance.
      val mcBeforeWgt = count("MC")
      doTask("VenusStep! BY Admin")
      doTask("6 MC").expect("CharismaticWgPresident, Distant<CharismaticWgPresident>")
      // Homeworld Support pays 6 M€; the Unity bonus pays 10 M€ for planetary tags.
      count("MC") shouldBe mcBeforeWgt + 16
      (admin.count("PartyFollower<MarsFirst, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "Unity",
          chairman = "Neutral",
          dominant = "Kelvinists",
          removed = "RedResistance",
          current = "ViralModificationsApproved",
          coming = "SolarnetGlobalEvent",
      )
      admin.count("Distant<CharismaticWgPresident>") shouldBe 1
      admin.count("PartyLeader<Unity, Neutral>") shouldBe 1
    }
  }

  private fun generation9() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      // Save 83: immediately after Research.
      assertResources(m = 58, s = 2, t = 5, p = 9, e = 5, h = 11)
      assertProduction(m = 16, s = 2, t = 5, p = 5, e = 5, h = 3)
      assertCounts(30 to "TerraformRating")
      assertSidebar(gen = 9, temp = -26, oxygen = 6, oceans = 5, venus = 14)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*playCardTasks("$Tardigrades"), "3 Pay<> FROM MC", "$Tardigrades FROM ProjectCard")
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$InterplanetaryTrade FROM ProjectCard",
          "EACH @WildTagElection<Bloo> { Tag<@WildTagElection>? }",
          "PROD[10 MC]",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_6>",
          "OxygenStep",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$IshtarExpedition"),
          "5 Pay<> FROM MC",
          "$IshtarExpedition FROM ProjectCard",
          "3 Titanium",
          "2 SearchForCard<TagFilter<Class<VenusTag>>>",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Unity>")
      doTasks(
          *playCardTasks("$GanymedeColony"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$GanymedeColony FROM ProjectCard",
          "NormalCityTile<GanymedeColony_RemoteArea>",
      )
      doTasks(
          *playCardTasks("$SubterraneanReservoir"),
          "10 Pay<> FROM MC",
          "$SubterraneanReservoir FROM ProjectCard",
          "OceanTile<Tharsis_6_8>",
          "TerraformRating",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$AirScrappingExpedition"),
          "12 Pay<> FROM MC",
          "$AirScrappingExpedition FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 Floater<$ExtractorBalloons>",
      )
      doTasks(
          *playCardTasks("$AstraMechanica"),
          "6 Pay<> FROM MC",
          "$AstraMechanica FROM ProjectCard",
          "ProjectCard FROM PlayedEvent<Class<$BribedCommittee>>",
          "ProjectCard FROM PlayedEvent<Class<$Comet>>",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "$CeosFavoriteProject FROM ProjectCard",
          "Floater<$ExtractorBalloons>",
      )
      doTasks(
          *playCardTasks("$SmallAnimals"),
          "5 Pay<> FROM MC",
          "$SmallAnimals FROM ProjectCard",
          "PROD[-Plant<SoloOpponent>]",
      )
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "4 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
      )
      doTasks(
          *playCardTasks("$SnowAlgae"),
          "11 Pay<> FROM MC",
          "$SnowAlgae FROM ProjectCard",
          "PROD[Plant]",
          "PROD[Heat]",
      )
      doTask("Pass")
      doTasks("66 MC", "5 Titanium", "5 Energy", "6 Plant", "Heat FROM Energy", "5 Heat", "2 Steel")
      // Pandemic resolves before Kelvinists form the government, then the visible events advance.
      val mcBeforeWgt = count("MC")
      val trBeforeWgt = count("TerraformRating")
      doTask("OxygenStep! BY Admin")
      doTask("-9 MC")
          .expect("BioengineeringBoom, Distant<BioengineeringBoom>, PartyLeader<Greens, Neutral>")
      // Pandemic costs 9 M€; the Kelvinists bonus pays 5 M€ for heat production.
      count("MC") shouldBe mcBeforeWgt - 4
      count("TerraformRating") shouldBe trBeforeWgt
      (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "Kelvinists",
          chairman = "Bloo",
          dominant = "Greens",
          removed = "ViralModificationsApproved",
          current = "SolarnetGlobalEvent",
          coming = "CharismaticWgPresident",
      )
      admin.count("Distant<BioengineeringBoom>") shouldBe 1
      (admin.count("PartyLeader<Greens, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation10() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")

      // Save 107: immediately after Research.
      assertResources(m = 54, s = 6, t = 6, p = 8, e = 5, h = 9)
      assertProduction(m = 26, s = 2, t = 5, p = 6, e = 5, h = 5)
      assertCounts(40 to "TerraformRating")
      assertSidebar(gen = 10, temp = -22, oxygen = 9, oceans = 6, venus = 18)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(
          "UseAction<KelvinistsPolicy, Action1>",
          "10 Pay<> FROM MC",
          "PROD[Heat]",
          "PROD[Energy]",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
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
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      // The archive paid cash despite holding steel that could cover Rego Plastics.
      doTasks(
          *playCardTasks("$RegoPlastics"),
          "Ok", // no steel paid
          "9 Pay<> FROM MC",
          "$RegoPlastics FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$Omnicourt"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$Omnicourt FROM ProjectCard",
          "2 TerraformRating",
      )
      doTasks(
          *playCardTasks("$GhgFactories"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$GhgFactories FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 Heat]",
      )
      doTasks(
          *playCardTasks("$AiCentral"),
          "4 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$AiCentral FROM ProjectCard",
          "PROD[-Energy]",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTasks(
          *playCardTasks("$OlympusConference"),
          "7 Pay<> FROM MC",
          "$OlympusConference FROM ProjectCard",
          "Science<$OlympusConference>",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Kelvinists> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$NitriteReducingBacteria"),
          "10 Pay<> FROM MC",
          "$NitriteReducingBacteria FROM ProjectCard",
          "3 Microbe<$NitriteReducingBacteria>",
      )
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$Comet"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "ProjectCard",
          "TerraformRating",
          "OceanTile<Tharsis_9_9>",
          "TerraformRating",
          "2 Titanium",
          "Ok", // decline the optional plant removal
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_8>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Extremophiles"),
          "2 Pay<> FROM MC",
          "$Extremophiles FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$NitriteReducingBacteria>")
      doTasks(
          *playCardTasks("$MiningArea"),
          "3 Pay<> FROM MC",
          "$MiningArea FROM ProjectCard",
          "MiningArea_SpecialTile<Tharsis_9_6>",
          "Production<Class<Metal>(HAS PlacementBonus<LandArea(HAS MiningArea_SpecialTile)>)>",
          "2 Steel",
      )
      doTasks(*playCardTasks("$FloatingHabs"), "4 Pay<> FROM MC", "$FloatingHabs FROM ProjectCard")
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$ExtractorBalloons>")
      doTask("Pass")
      doTasks(
          "76 MC",
          "5 Titanium",
          "4 Energy",
          "6 Plant",
          "Heat FROM Energy",
          "11 Heat",
          "3 Steel",
      )
      // Celebrity Leaders pays before Greens take power, then the visible events advance.
      val mcBeforeWgt = count("MC")
      doTask("TemperatureStep! BY Admin")
      doTask("14 MC")
          .expect(
              "LocalTerraformingSupport, Distant<LocalTerraformingSupport>, PartyFollower<MarsFirst, Neutral>"
          )
      // Celebrity Leaders pays 14 M€; the Greens bonus pays 7 M€ for bio tags.
      count("MC") shouldBe mcBeforeWgt + 21
      (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "Greens",
          chairman = "Neutral",
          dominant = "MarsFirst",
          removed = "SolarnetGlobalEvent",
          current = "CharismaticWgPresident",
          coming = "BioengineeringBoom",
      )
      admin.count("Distant<LocalTerraformingSupport>") shouldBe 1
      (admin.count("PartyFollower<MarsFirst, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation11() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")

      // Save 133: immediately after Research.
      assertResources(m = 89, s = 5, t = 9, p = 6, e = 4, h = 13)
      assertProduction(m = 27, s = 3, t = 5, p = 6, e = 4, h = 11)
      assertCounts(48 to "TerraformRating")
      assertSidebar(gen = 11, temp = -16, oxygen = 11, oceans = 7, venus = 20)
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$AntiGravityTechnology"),
          "13 Pay<> FROM MC",
          "$AntiGravityTechnology FROM ProjectCard",
          "ProjectCard FROM Science",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$SpecialPermit"),
          "2 Pay<> FROM MC",
          "$SpecialPermit FROM ProjectCard",
          "4 Plant FROM Plant<SoloOpponent>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_8>",
          "OxygenStep",
          "4 MC",
          "Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$OpenCity"),
          "5 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$OpenCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
          "2 Plant",
          "CityTile<Tharsis_7_7>",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$AqueductSystems"),
          "1 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$AqueductSystems FROM ProjectCard",
          "3 SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTasks(
          *playCardTasks("$RoverConstruction"),
          "1 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$RoverConstruction FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$BactoviralResearch"),
          "7 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
          "Science<$OlympusConference>",
          "-ProjectCard",
          "ProjectCard",
          "9 Microbe<$NitriteReducingBacteria>",
      )
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$ExtractorBalloons>")
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$Extremophiles>")
      doTasks(
          *playCardTasks("$Heather"),
          "3 Pay<> FROM MC",
          "$Heather FROM ProjectCard",
          "PROD[Plant]",
          "Plant",
      )
      doTasks(
          *playCardTasks("$LargeConvoy"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$LargeConvoy FROM ProjectCard",
          "2 ProjectCard",
          "ProjectCard",
          "OceanTile<Tharsis_5_4>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "5 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_5>",
          "OxygenStep",
          "4 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$KelpFarming"),
          "14 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
      )
      doTasks(
          *playCardTasks("$AsteroidRights"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$AsteroidRights FROM ProjectCard",
          "2 Asteroid<$AsteroidRights>",
      )
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$PeroxidePower"),
          "4 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
      )
      doTasks(
          *playCardTasks("$VenusianPlants"),
          "10 Pay<> FROM MC",
          "$VenusianPlants FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "Microbe<$Extremophiles>",
      )
      doTasks(
          *playCardTasks("$NoctisFarming"),
          "7 Pay<> FROM MC",
          "$NoctisFarming FROM ProjectCard",
          "PROD[MC]",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_4>",
          "OxygenStep",
          "4 MC",
          "Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Greens> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$MagneticFieldGeneratorsPromo"),
          "19 Pay<> FROM MC",
          "$MagneticFieldGeneratorsPromo FROM ProjectCard",
          "PROD[-4 Energy]",
          "PROD[2 Plant]",
          "3 TerraformRating",
          "MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_5_7>",
          "2 Plant",
          "2 MC",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$Plantation"),
          "12 Pay<> FROM MC",
          "$Plantation FROM ProjectCard",
          "GreeneryTile<Tharsis_8_5>",
          "OxygenStep",
          "4 MC",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Greens>")
      doTask("Pass")
      doTasks("93 MC", "5 Titanium", "Energy", "12 Plant", "11 Heat", "3 Steel")
      // Interplanetary Trade pays before Mars First takes power, then the visible events advance.
      val mcBeforeWgt = count("MC")
      doTask("TemperatureStep! BY Admin")
      doTasks("10 MC", "0 MC")
          .expect("PateraBoring, Distant<PateraBoring>, PartyLeader<Scientists, Neutral>")
      // Interplanetary Trade pays 10 M€; the Mars First bonus pays 18 M€ for building tags.
      count("MC") shouldBe mcBeforeWgt + 28
      (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
      assertTurmoilState(
          ruling = "MarsFirst",
          dominant = "Unity",
          removed = "CharismaticWgPresident",
          current = "BioengineeringBoom",
          coming = "LocalTerraformingSupport",
      )
      admin.count("Distant<PateraBoring>") shouldBe 1
      (admin.count("PartyLeader<Scientists, Neutral>") > 0) shouldBe true
    }
  }

  private fun generation12() {
    with(me) {
      doTask("4 ProjectCard<Selecting>")
      doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

      // Save 168: immediately after Research.
      assertResources(m = 120, s = 3, t = 9, p = 16, e = 1, h = 16)
      assertProduction(m = 33, s = 3, t = 5, p = 12, e = 1, h = 11)
      assertCounts(59 to "TerraformRating")
      assertSidebar(gen = 12, temp = -10, oxygen = 14, oceans = 8, venus = 24)
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
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_3>",
          "OxygenStep",
          "Steel",
          "2 Plant",
      )
      doTasks(
          *playCardTasks("$FrontierTown"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$FrontierTown FROM ProjectCard",
          "FrontierTownBonus",
          "PROD[-Energy]",
          "CityTile<Tharsis_6_5>",
          "Steel",
          "2 MC",
          "-FrontierTownBonus",
          "Plant",
          "2 MC",
          "2 MC",
          "RepeatPlacementBonus<Tharsis_6_5>",
          "Plant",
          "Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_4>",
          "OxygenStep",
          "Steel",
          "2 Steel",
      )
      doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$Extremophiles>")
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$BigAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 Titanium",
          "ProjectCard",
          "TerraformRating",
          "TerraformRating",
          "Ok", // decline the optional plant removal
      )
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remainder
          "$GiantIceAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "ProjectCard",
          "TerraformRating",
          "TerraformRating",
          "OceanTile<Tharsis_1_4>",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>",
          "TerraformRating",
          "Steel",
          "ProjectCard",
          "Ok", // decline the optional plant removal
      )
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<MarsFirst> FROM LobbyDelegate",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$Bushes"),
          "7 Pay<> FROM MC",
          "$Bushes FROM ProjectCard",
          "PROD[2 Plant]",
          "2 Plant",
      )
      // The archive recorded no payment for the one M€ remaining after card discounts.
      doTasks(*playCardTasks("$BusinessNetwork"), "$BusinessNetwork FROM ProjectCard", "PROD[-MC]")
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *playCardTasks("$Fish"),
          "6 Pay<> FROM MC",
          "$Fish FROM ProjectCard",
          "PROD[-Plant<SoloOpponent>]",
      )
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
      doTasks(
          *playCardTasks("$MartianRails"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remainder
          "$MartianRails FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$MartianRails"), "Pay<Class<Energy>> FROM Energy", "5 MC")
      doTasks(*standardProjectTasks("SellPatentsProject"), "7 MC FROM ProjectCard")
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_7_4>",
          "Steel",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_9_8>",
          "Steel",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_5_2>",
          "OxygenStep",
          "Steel",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_9_7>",
          "OxygenStep",
          "Steel",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_6_2>",
          "Steel",
          "2 MC",
          "Plant",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("Pass")
      doTasks("106 MC", "5 Titanium", "14 Plant", "11 Heat", "3 Steel")

      // Save 200: production completed and final greenery placement is pending.
      assertResources(m = 113, s = 12, t = 8, p = 16, e = 0, h = 11)
      assertProduction(m = 36, s = 3, t = 5, p = 14, e = 0, h = 11)
      assertCounts(70 to "TerraformRating")
      assertSidebar(gen = 12, temp = 6, oxygen = 14, oceans = 9, venus = 26)
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_7_3>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_5_1>",
          "2 Plant",
      )
      doTask("PassBaton")
      assertCounts(0 to "ProjectCard")
      admin.assertCounts(1 to "End", 1 to "Phase", 0 to "GameScope")

      assertResources(m = 113, s = 12, t = 8, p = 2, e = 0, h = 11)
      assertProduction(m = 36, s = 3, t = 5, p = 14, e = 0, h = 11)
      assertCounts(
          0 to "ProjectCard",
          70 to "TerraformRating",
          74 to "CardFront OR PlayedEvent",
          150 to "VictoryPoint",
          1 to "Victory",
      )
      assertDashRight(events = 16, tagless = 1, cities = 7)
      assertTags(
          but = 20,
          spt = 12,
          sct = 9,
          pot = 5,
          eat = 5,
          jot = 5,
          vet = 7,
          plt = 9,
          mit = 4,
          ant = 2,
          cit = 4,
      )
      assertCardResources(
          4 to Tardigrades,
          4 to SmallAnimals,
          1 to OlympusConference,
          4 to NitriteReducingBacteria,
          3 to Extremophiles,
          1 to Fish,
      )

      admin.assertCounts(
          1 to "Current<$BioengineeringBoom>",
          1 to "Coming<$LocalTerraformingSupport>",
          1 to "Distant<$PateraBoring>",
          1 to "Ruling<MarsFirst>",
          1 to "Dominant<Unity>",
          1 to "Chairman<Neutral>",
      )
      assertCounts(
          1 to "PartyDelegate<MarsFirst>",
          1 to "PartyDelegate<Scientists>",
          1 to "PartyDelegate<Unity>",
          2 to "PartyDelegate<Greens>",
          1 to "PartyDelegate<Reds>",
          1 to "PartyDelegate<Kelvinists>",
      )

      val score = Summarizer(game)
      score.net("TerraformRating", "VictoryPoint<Bloo>") shouldBe 70
      score.net("GreeneryTile", "VictoryPoint<Bloo>") shouldBe 14
      score.net("CityTile", "VictoryPoint<Bloo>") shouldBe 27
      score.net("Card", "VictoryPoint<Bloo>") shouldBe 35
      score.net("PartyLeader", "VictoryPoint<Bloo>") shouldBe 4
    }
  }

  private fun assertTurmoilState(
      ruling: String,
      dominant: String? = null,
      chairman: String? = null,
      removed: String? = null,
      current: String,
      coming: String,
  ) {
    val expected =
        mutableListOf(
            1 to "Ruling<$ruling>",
            1 to "Current<$current>",
            1 to "Coming<$coming>",
        )
    dominant?.let { expected += 1 to "Dominant<$it>" }
    chairman?.let { expected += 1 to "Chairman<$it>" }
    removed?.let { expected += 0 to it }
    admin.assertCounts(*expected.toTypedArray())
  }

  private companion object {
    val BioengineeringBoom = cn("BioengineeringBoom")
    val LocalTerraformingSupport = cn("LocalTerraformingSupport")
    val PateraBoring = cn("PateraBoring")
  }
}
