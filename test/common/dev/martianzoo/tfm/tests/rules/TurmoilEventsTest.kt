package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilEventsTest : TfmGameplayTest() {
  @Test
  internal fun `Democratic Reform cannot place its ocean on land`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    admin.doTask("PartyLeader<Greens, Neutral>")
    admin.doTasks("ExperimentalLifeforms", "PartyFollower<Greens, Neutral>")
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

    val first = players.single { it.count("StartToken") == 1 }
    first.doTask("Pass")
    players.single { it != first }.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    first.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    shouldThrow<NarrowingException> { first.doTask("OceanTile<Tharsis_3_3> BY Admin") }
    admin.count("OceanTile") shouldBe 0
    first.doTask("OceanTile<Tharsis_1_2> BY Admin").expect("OceanTile, 0 TerraformRating")
  }

  @Test
  internal fun `An event stays dormant until Current and is discarded after resolving`() {
    prepareEvent(
        "ExperimentalLifeforms",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    ) {
      kim.doTasks("PlayProject<Class<$AdaptedLichen>>")
      kim.selectTask("PassBaton") // Decline the second action.
      stan.doTasks("PlayProject<Class<$Archaebacteria>>")
      stan.selectTask("PassBaton") // Decline the second action.
    }
    admin.count("Current<ExperimentalLifeforms>") shouldBe 1
    admin.count("Coming<ExploreFirstDirective>") shouldBe 1
    admin.count("Distant<MoralMovement>") shouldBe 1
    // Both players already exceed the three plants protected by the event.
    kim.count("Plant") shouldBe 6
    stan.count("Plant") shouldBe 6
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.count("Plant") shouldBe 8
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    kim.count("Plant") shouldBe 3
    stan.count("Plant") shouldBe 3
    admin.count("ExperimentalLifeforms") shouldBe 0
    admin.count("Current<ExploreFirstDirective>") shouldBe 1
    admin.count("Coming<MoralMovement>") shouldBe 1
    admin.count("Distant") shouldBe 0
    admin.doTasks(
        "PartyLeader<Scientists, Neutral>",
        "ScientificProgress",
        "PartyFollower<Scientists, Neutral>",
    )
    admin.count("Distant<ScientificProgress>") shouldBe 1
  }

  @Test
  internal fun `Asteroid Mining measures this generation's influence before paying titanium`() {
    prepareEvent(
        "MiningRestrictions",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$VestaShipyard>>", "Ok" /* No Titanium payment. */)
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("3 Titanium<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("Influence") shouldBe 2
      // A Jovian tag and two influence.
    }
  }

  @Test
  internal fun `Homeworld Support caps Earth tags at five before adding influence`() {
    prepareEvent(
        "RedResistance",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$EarthOffice>>")
    kim.doTasks("PlayProject<Class<$Sponsors>>")
    kim.doTasks("PlayProject<Class<$AcquiredCompany>>")
    kim.doTask("PlayProject<Class<$LunaGovernor>>")
    kim.doTasks("PlayProject<Class<$Cartel>>")
    kim.count("EarthTag") shouldBe 6
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    // Six Earth tags capped at five, plus two influence, at 2 MC each.
    expectChanges("14 MC<Kim>, 0 MC<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Productivity pays for steel production plus influence`() {
    prepareEvent(
        "ScientificProgress",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$Mine>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$IndustrialMicrobes>>", "Ok" /* No Steel payment. */)
    kim.autoExecNow(EAGER)
    lobbyForInfluence("PartyFollower", "PartyLeader", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    // Three production and two influence.
    expectChanges("5 Steel<Kim>, 1 Steel<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Successful Organisms pays for plant production plus influence`() {
    prepareEvent(
        "LocalTerraformingSupport",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$AdaptedLichen>>")
    lobbyForInfluence("PartyFollower", "PartyLeader", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("1 Plant<Stan>, 4 Plant<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Scientific Community pays for the current hand and influence`() {
    prepareEvent(
        "ExploreFirstDirective",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyFollower<Scientists, Neutral>",
            "PartyLeader<Reds, Neutral>",
        ),
    )
    kim.doTasks("UseStandardProject<SellPatentsProject>", "7 MC FROM ProjectCard")
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("10 MC<Stan>, 5 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Celebrity Leaders pays the owner of played events`() {
    prepareEvent(
        "SolarnetGlobalEvent",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$InvestmentLoan>>")
    kim.autoExecNow(EAGER)
    kim.doTasks("PlayProject<Class<$BribedCommittee>>")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("0 MC<Stan>, 4 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Strong Society pays only for owned cities`() {
    prepareEvent(
        "MoralMovement",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Greens, Neutral>",
        ),
    )
    kim.completeOperation {
      doTasks("UseStandardProject<CityProject>")
      doTask("CityTile<Tharsis_3_3>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<CityProject>")
      doTask("CityTile<Tharsis_8_8>")
      autoExecNow(EAGER)
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("0 MC<Stan>, 4 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Global Dust Storm consumes heat and influence reduces the building penalty`() {
    prepareEvent(
        "HeatFirstPolicy",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$Mine>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$PowerPlant>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$BuildingIndustries>>", "Ok" /* No Steel payment. */)
    kim.autoExecNow(EAGER)
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("1 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("Heat") shouldBe 0
      stan.count("Heat") shouldBe 0
      // Three building tags minus two influence costs 2 MC; Mars First then pays 3 MC.
    }
  }

  @Test
  internal fun `Pandemic caps six building tags before subtracting influence`() {
    prepareEvent(
        "ViralModificationsApproved",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$Mine>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$PowerPlant>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$IndustrialMicrobes>>", "Ok" /* No Steel payment. */)
    kim.autoExecNow(EAGER)
    kim.doTasks("PlayProject<Class<$BuildingIndustries>>", "Ok" /* No Steel payment. */)
    kim.autoExecNow(EAGER)
    kim.doTasks("PlayProject<Class<$TitaniumMine>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$FuelFactory>>", "Ok" /* No Steel payment. */)
    kim.autoExecNow(EAGER)
    lobbyForInfluence("PartyFollower", "PartyLeader", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("-3 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // (Five counted buildings - two influence) costs 9; Mars First pays for all six buildings.
    }
  }

  @Test
  internal fun `Solarnet Shutdown counts active cards but excludes automated cards and played events`() {
    prepareEvent(
        "AiResearch",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$Pets>>")
    kim.doTasks("PlayProject<Class<$SpaceMirrors>>", "Ok" /* No Titanium payment. */)
    kim.doTasks("PlayProject<Class<$Tardigrades>>")
    kim.doTasks("PlayProject<Class<$StandardTechnology>>")
    kim.doTasks("PlayProject<Class<$Mine>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$BribedCommittee>>")
    lobbyForInfluence("PartyFollower", "PartyLeader", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("-5 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // Four active cards minus two influence costs 6; Mars First pays 1 for Mine.
    }
  }

  @Test
  internal fun `War on Earth reduces rating loss for influence while still charging other players`() {
    prepareEvent(
        "SeparatistMovement",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    lobbyForInfluence("PartyFollower", "PartyLeader", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    // Kim loses two plus revision, then becomes chairman; Stan loses four plus revision.
    expectChanges("-2 TerraformRating<Kim>, -5 TerraformRating<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Paradigm Breakdown discards the last card without demanding a second`() {
    prepareEvent(
        "ScientificConsensus",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("UseStandardProject<SellPatentsProject>", "9 MC FROM ProjectCard")
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("4 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("ProjectCard") shouldBe 0
      stan.count("ProjectCard") shouldBe 8
    }
  }

  @Test
  internal fun `Sabotage reduces available production without taking energy production below zero`() {
    prepareEvent(
        "CorporateAlliance",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$BuildingIndustries>>", "Ok" /* No Steel payment. */)
    kim.autoExecNow(EAGER)
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("2 Steel<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("PROD[Steel]") shouldBe 2
      kim.count("PROD[Energy]") shouldBe 0
      stan.count("PROD[Steel]") shouldBe 0
      stan.count("PROD[Energy]") shouldBe 0
    }
  }

  @Test
  internal fun `Miners on Strike takes only the titanium available`() {
    prepareEvent(
        "RisingAlloyDemand",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$VestaShipyard>>", "3 Pay<Class<Titanium>> FROM Titanium")
    kim.doTasks("PlayProject<Class<$MirandaResort>>")
    kim.doTasks("PlayProject<Class<$JovianEmbassy>>", "Ok" /* No Steel payment. */)
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.count("Titanium") shouldBe 2
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    kim.count("Titanium") shouldBe 0
    stan.count("Titanium") shouldBe 4
  }

  @Test
  internal fun `Mud Slides counts each coastal tile once even beside two oceans`() {
    prepareEvent(
        "ThawMining",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.completeOperation {
      doTasks("UseStandardProject<CityProject>")
      doTask("CityTile<Tharsis_4_4>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<GreeneryProject>")
      doTask("GreeneryTile<Tharsis_4_5>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<AquiferProject>")
      doTask("OceanTile<Tharsis_5_4>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<AquiferProject>")
      doTask("OceanTile<Tharsis_5_5>")
      autoExecNow(EAGER)
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("-8 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Diversity does not count duplicate science tags as distinct tags`() {
    prepareDiversity()
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("1 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // Six distinct tag kinds plus two influence is short; Mars First pays for Power Plant.
    }
  }

  @Test
  internal fun `Diversity combines distinct tags with influence to reach nine`() {
    prepareDiversity()
    kim.doTasks("PlayProject<Class<$AdaptedLichen>>")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("11 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Improved Energy Templates combines power tags and influence before rounding`() {
    prepareEvent(
        "SecondEnergyCrisis",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$PowerPlant>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$SpaceMirrors>>", "Ok" /* No Titanium payment. */)
    kim.completeOperation {
      doTasks("PlayProject<Class<$SolarWindPower>>", "Ok" /* No Titanium payment. */)
      autoExecNow(EAGER)
    }
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("PROD[2 Energy<Kim>]") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // Floor of (three tags + two influence)/2.
    }
  }

  @Test
  internal fun `Sponsored Projects grows occupied cards for both players but skips empty cards`() {
    prepareEvent(
        "ScienceSummit",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    ) {
      kim.completeOperation {
        doTasks("PlayProject<Class<$Research>>")
        autoExecNow(EAGER)
      }
      kim.selectTask("PassBaton") // Decline the second action.
      stan.doTasks("PlayProject<Class<$TitanShuttles>>")
      stan.completeOperation {
        doTask("UseCardAction<$TitanShuttles, Action1>")
        doTask("2 Floater<$TitanShuttles>")
      }
    }
    kim.doTasks("PlayProject<Class<$AerialMappers>>")
    kim.doTasks("PlayProject<Class<$FloatingHabs>>")
    kim.doTasks("PlayProject<Class<$Tardigrades>>")
    kim.completeOperation {
      doTask("UseCardAction<$AerialMappers, Action1>")
      doTask("1 Floater<$AerialMappers>")
    }
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("2 ProjectCard<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("Floater<$AerialMappers>") shouldBe 2
      kim.count("Floater<$FloatingHabs>") shouldBe 0
      kim.count("Microbe<$Tardigrades>") shouldBe 0
      stan.count("Floater<$TitanShuttles>") shouldBe 3
    }
  }

  @Test
  internal fun `Election awards tied first places without a second prize`() {
    prepareEvent(
        "SelfSufficiencyProgram",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Reds, Neutral>",
        ),
    )
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    // Revision -1, tied first +2.
    expectChanges("1 TerraformRating<Kim>, 1 TerraformRating<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Election awards distinct first and second places`() {
    prepareEvent(
        "SelfSufficiencyProgram",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$Mine>>", "Ok" /* No Steel payment. */)
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("1 TerraformRating<Kim>, 0 TerraformRating<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Revolution leaves a zero score alone`() {
    prepareEvent(
        "WorldGovernmentDirectives",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$EarthOffice>>")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    // Revision -1, Revolution -2, Reds +1.
    expectChanges("-2 TerraformRating<Kim>, -1 TerraformRating<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Revolution applies the first place penalty to both tied players`() {
    prepareEvent(
        "WorldGovernmentDirectives",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    ) {
      kim.doTasks("PlayProject<Class<$EarthOffice>>")
      kim.selectTask("PassBaton") // Decline the second action.
      stan.doTasks("PlayProject<Class<$Sponsors>>")
      stan.selectTask("PassBaton") // Decline the second action.
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    // Revision -1, Revolution -2, Reds +1.
    expectChanges("-2 TerraformRating<Kim>, -2 TerraformRating<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Cloud Societies gives everyone a floater then groups each player's influence on a card`() {
    prepareEvent(
        "TitanVenusAlliance",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    ) {
      kim.completeOperation {
        doTasks("PlayProject<Class<$Research>>")
        autoExecNow(EAGER)
      }
      kim.selectTask("PassBaton") // Decline the second action.
      stan.doTasks("PlayProject<Class<$TitanShuttles>>")
      stan.selectTask("PassBaton") // Decline the second action.
    }
    kim.doTasks("PlayProject<Class<$FloatingHabs>>")
    kim.doTasks("PlayProject<Class<$AerialMappers>>")
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    shouldThrow<NarrowingException> { kim.doTask("2 Floater<$TitanShuttles<Stan>>") }
    kim.doTask("2 Floater<$FloatingHabs>")
    kim.count("Floater<$FloatingHabs>") shouldBe 3
    kim.count("Floater<$AerialMappers>") shouldBe 1
    stan.count("Floater<$TitanShuttles>") shouldBe 1
  }

  @Test
  internal fun `Corrosive Rain consumes two floaters from the same card before drawing for influence`() {
    prepareEvent(
        "ImmigrationToVenus",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$TitanShuttles>>", "Ok" /* No Titanium payment. */)
    kim.completeOperation {
      doTask("UseCardAction<$TitanShuttles, Action1>")
      doTask("2 Floater<$TitanShuttles>")
    }
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("0 ProjectCard<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
    expectChanges("0 MC<Kim>, 2 ProjectCard<Kim>") {
      kim.doTask("-2 Floater<$TitanShuttles>")
    }
    kim.count("Floater<$TitanShuttles>") shouldBe 0
  }

  @Test
  internal fun `Corrosive Rain charges money when floaters are split across cards`() {
    prepareEvent(
        "ImmigrationToVenus",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.completeOperation {
      doTasks("PlayProject<Class<$Research>>")
      autoExecNow(EAGER)
    }
    kim.doTasks("PlayProject<Class<$FloatingHabs>>")
    kim.doTasks("PlayProject<Class<$AerialMappers>>")
    kim.completeOperation {
      doTasks("UseCardAction<$FloatingHabs, Action1>")
      doTask("1 Floater<$FloatingHabs>")
    }
    kim.completeOperation {
      doTask("UseCardAction<$AerialMappers, Action1>")
      doTask("1 Floater<$AerialMappers>")
    }
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("-10 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
    kim.count("Floater<$FloatingHabs>") shouldBe 1
    kim.count("Floater<$AerialMappers>") shouldBe 1
  }

  @Test
  internal fun `Jovian Tax Rights pays production for owned colonies and titanium for influence`() {
    prepareEvent(
        "JovianColonyBoom",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.completeOperation {
      doTasks("UseStandardProject<BuildColonyProject>", "Colony<Luna>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<BuildColonyProject>", "Colony<Ceres>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<BuildColonyProject>", "Colony<Triton>")
      autoExecNow(EAGER)
    }
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("PROD[0 MC<Stan>], PROD[3 MC<Kim>], 2 Titanium<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Microgravity Health Problems subtracts influence from owned colonies`() {
    prepareEvent(
        "Diaspora",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.completeOperation {
      doTasks("UseStandardProject<BuildColonyProject>", "Colony<Luna>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<BuildColonyProject>", "Colony<Ceres>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<BuildColonyProject>", "Colony<Triton>")
      autoExecNow(EAGER)
    }
    lobbyForInfluence("PartyFollower", "PartyLeader", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("0 MC<Stan>, -3 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Venus Infrastructure pays for Venus tags and influence`() {
    prepareEvent(
        "MartianProtectionism",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$AerialMappers>>")
    kim.doTasks("PlayProject<Class<$Dirigibles>>")
    lobbyForInfluence("PartyFollower", "PartyLeader", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("8 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Snow Cover cannot lower a completed temperature track`() {
    prepareTemperatureEvent(
        "MoholeLakeGlobalEvent",
        19,
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Scientists, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyFollower<Kelvinists, Neutral>",
            "PartyLeader<Greens, Neutral>",
        ),
    )
    val first = players.single { it.count("StartToken") == 1 }
    first.doTask("Pass")
    // Generation 20 starts with Stan; Kim can still lobby before passing.
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("2 ProjectCard<Kim>") {
      first.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.count("TemperatureStep") shouldBe 19
    }
  }

  @Test
  internal fun `Volcanic Eruptions lets the first player place the threshold ocean for Admin`() {
    prepareTemperatureEvent(
        "PateraBoring",
        13,
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Scientists, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    val first = players.single { it.count("StartToken") == 1 }
    first.doTask("Pass")
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("-1 TerraformRating<${first.actor}>, PROD[2 Heat<Kim>]") {
      first.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      shouldThrow<NarrowingException> { first.doTask("OceanTile<Tharsis_3_3> BY Admin") }
      first.doTask("OceanTile<Tharsis_1_4> BY Admin").expect("OceanTile")
      players.forEach { it.autoExecNow(EAGER) }

      admin.count("TemperatureStep") shouldBe 15
      admin.count("Ruling<MarsFirst>") shouldBe 1
      // Only revision; the ocean grants no TR.
    }
  }

  @Test
  internal fun `Generous Funding combines the post-revision rating bracket with influence`() {
    prepareEvent(
        "TerraformingLobbying",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
    kim.count("TerraformRating") shouldBe 20
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("4 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // Revision drops TR below the next payout bracket.
    }
  }

  @Test
  internal fun `Dry Deserts grants independent resource choices for each influence without taking TR`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.completeOperation {
      doTask("LobbyAction")
      doTask("PartyLeader<Scientists> FROM LobbyDelegate")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyFollower<Scientists>")
    }
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    admin.doTasks("PartyLeader<Greens, Neutral>", "RedResistance", "PartyFollower<Reds, Neutral>")
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

    // Kim is chairman and takes Unity's leadership and an ordinary delegate's influence.
    stan.doTask("Pass")
    kim.completeOperation {
      doTask("LobbyAction")
      doTask("PartyLeader<Unity> FROM LobbyDelegate")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyFollower<Unity>")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyFollower<Unity>")
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("3 Plant<Kim>, 3 Steel<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
    admin.doTasks(
        "PartyLeader<Unity, Neutral>",
        "CharismaticWgPresident",
        "PartyFollower<Unity, Neutral>",
    )
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

    kim.completeOperation {
      doTask("LobbyAction")
      doTask("PartyFollower<MarsFirst> FROM LobbyDelegate")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyLeader<MarsFirst>")
    }
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyFollower<MarsFirst>")
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    expectChanges("0 TerraformRating<Kim>") {
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // These indistinguishable choices may be answered in any order, using their public task IDs.
      val choices = kim.tasks.extract { it.id }
      kim.doTask("Heat", choices[0])
      kim.doTask("Plant", choices[1])
      kim.doTask("Steel", choices[2])
      admin.count("OceanTile") shouldBe 0
      // Revision and chairman TR cancel.
    }
  }

  @Test
  internal fun `Revealing events still works when all neutral delegates are on the board`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    val reveals =
        listOf(
            "CharismaticWgPresident",
            "RedResistance",
            "TerraformingLobbying",
            "SolarnetGlobalEvent",
        )
    val neutralPlacements =
        listOf(
            listOf("PartyLeader<Greens, Neutral>", "PartyLeader<Unity, Neutral>"),
            listOf("PartyFollower<Unity, Neutral>", "PartyFollower<Reds, Neutral>"),
            listOf("PartyFollower<Unity, Neutral>", "PartyLeader<Kelvinists, Neutral>"),
            listOf("PartyFollower<Unity, Neutral>", "PartyFollower<Unity, Neutral>"),
            listOf("PartyFollower<Unity, Neutral>", "PartyFollower<Unity, Neutral>"),
            listOf("PartyFollower<Greens, Neutral>", "PartyFollower<Reds, Neutral>"),
            listOf(null, null),
        )
    repeat(7) { generation ->
      val first = players.single { it.count("StartToken") == 1 }
      val other = players.single { it != first }
      repeat(2) { round ->
        listOf(first, other).forEachIndexed { seat, player ->
          val lobbyAction = if (round == 0) "LobbyAction" else "PaidLobbyAction"
          player.completeOperation {
            doTask(lobbyAction)

            doTask(
                if (round == 0) {
                  listOf(
                      "PartyLeader<Scientists> FROM LobbyDelegate",
                      "PartyFollower<Scientists> FROM LobbyDelegate",
                  )[seat]
                } else "PartyFollower<Scientists>"
            )
          }
          player.completeOperation {
            doTasks("PaidLobbyAction")
            doTask("PartyFollower<Scientists>")
          }
        }
      }
      listOf(first, other).forEach { player ->
        player.completeOperation {
          doTasks("PaidLobbyAction")
          doTask("PartyFollower<Scientists>")
        }
        player.selectTask("PassBaton") // Decline the second action.
      }
      first.doTask("Pass")
      other.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      if (generation == 6) admin.count("Delegate<Neutral>") shouldBe 14
      first.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      if (generation == 1) {
        first.doTask("OceanTile<Tharsis_1_2> BY Admin")
        players.forEach { it.autoExecNow(EAGER) }
      }
      if (generation == 2)
          players.forEach { player ->
            player.tasks.extract { it.id }.forEach { player.doTask("Heat", it) }
          }
      neutralPlacements[generation][0]?.let { admin.doTask(it) }
      admin.doTask(reveals[generation % reveals.size])
      neutralPlacements[generation][1]?.let { admin.doTask(it) }
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
    }
    admin.count("Delegate<Neutral>") shouldBe 14
    admin.count("Distant<TerraformingLobbying>") shouldBe 1
  }

  @Test
  internal fun `Solo Election requires five points rather than awarding an automatic first place`() {
    prepareSoloRankingEvent(
        "SelfSufficiencyProgram",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Reds, Neutral>",
        ),
    )
    expectChanges("-1 TerraformRating<Kim>") {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  @Test
  internal fun `Solo Election combines buildings and city tiles to reach five`() {
    prepareSoloRankingEvent(
        "SelfSufficiencyProgram",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$Mine>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$PowerPlant>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$IndustrialMicrobes>>", "Ok" /* No Steel payment. */)
    kim.autoExecNow(EAGER)
    kim.doTasks("PlayProject<Class<$BuildingIndustries>>", "Ok" /* No Steel payment. */)
    kim.autoExecNow(EAGER)
    kim.completeOperation {
      doTasks("UseStandardProject<CityProject>")
      doTask("CityTile<Tharsis_8_8>")
      autoExecNow(EAGER)
    }
    expectChanges("0 TerraformRating<Kim>") {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // Revision -1 and Election +1.
    }
  }

  @Test
  internal fun `Solo Revolution does not penalize fewer than four Earth tags and influence`() {
    prepareSoloRankingEvent(
        "WorldGovernmentDirectives",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$EarthOffice>>")
    kim.doTasks("PlayProject<Class<$Sponsors>>")
    kim.doTasks("PlayProject<Class<$AcquiredCompany>>")
    expectChanges("0 TerraformRating<Kim>") {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // Revision and Reds bonus cancel.
    }
  }

  @Test
  internal fun `Solo Revolution penalizes reaching four Earth tags`() {
    prepareSoloRankingEvent(
        "WorldGovernmentDirectives",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.doTasks("PlayProject<Class<$EarthOffice>>")
    kim.doTasks("PlayProject<Class<$Sponsors>>")
    kim.doTasks("PlayProject<Class<$AcquiredCompany>>")
    kim.doTasks("PlayProject<Class<$Cartel>>")
    expectChanges("-2 TerraformRating<Kim>") {
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // Revision and Reds cancel; Revolution costs two.
    }
  }

  private fun prepareSoloRankingEvent(event: String, neutralPlacements: List<String>) {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 1)
    listOf(event, "ExploreFirstDirective", "MoralMovement").forEachIndexed { index, reveal ->
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      if (index == 1) {
        kim.doTask("OceanTile<Tharsis_1_2> BY Admin")
        players.forEach { it.autoExecNow(EAGER) }
      }
      admin.doTask(neutralPlacements[index * 2])
      admin.doTasks(reveal, neutralPlacements[index * 2 + 1])
      kim.doTask("-4 ProjectCard<Selecting>")
    }
  }

  private fun prepareDiversity() {
    prepareEvent(
        "FreeAcademiaTreaty",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Scientists, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    kim.completeOperation {
      doTasks("PlayProject<Class<$Research>>")
      autoExecNow(EAGER)
    }
    kim.doTasks("PlayProject<Class<$PowerPlant>>", "Ok" /* No Steel payment. */)
    kim.doTasks("PlayProject<Class<$VestaShipyard>>", "Ok" /* No Titanium payment. */)
    kim.doTasks("PlayProject<Class<$EarthOffice>>")
    lobbyForInfluence("PartyLeader", "PartyFollower", "PartyFollower")
  }

  private fun lobbyForInfluence(
      firstRole: String = "PartyLeader",
      secondRole: String = "PartyFollower",
      thirdRole: String = "PartyFollower",
  ) {
    kim.completeOperation {
      doTask("LobbyAction")
      doTask("$firstRole<MarsFirst> FROM LobbyDelegate")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("$secondRole<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("$thirdRole<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyFollower<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyFollower<MarsFirst>")
    }
  }

  private fun prepareEvent(event: String, neutralPlacements: List<String>, setup: () -> Unit = {}) {
    newTestGame(
        addOptions = "TurmoilExpansion, Luna, Ceres, Triton, Ganymede, Callisto",
        playerCount = 2,
    )
    setup()
    // Reveal the subject in generation 1; it moves through Coming and Current before generation 4.
    listOf(
            event,
            if (event == "ExploreFirstDirective") "ScientificProgress" else "ExploreFirstDirective",
            if (event == "MoralMovement") "BioengineeringBoom" else "MoralMovement",
        )
        .forEachIndexed { index, reveal ->
          val first = players.single { it.count("StartToken") == 1 }
          first.doTask("Pass")
          players.single { it != first }.doTask("Pass")
          players.forEach { it.autoExecNow(EAGER) }
          first.doTask("VenusStep! BY Admin")
          players.forEach { it.autoExecNow(EAGER) }

          if (index == 1) {
            first.doTask("OceanTile<Tharsis_1_2> BY Admin")
            players.forEach { it.autoExecNow(EAGER) }
          } // Democratic Reform.
          // Minimal Impact Policy removes that sole ocean automatically in generation 3.
          admin.doTask(neutralPlacements[index * 2])
          admin.doTasks(reveal, neutralPlacements[index * 2 + 1])
          players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
        }
    stan.doTask("Pass") // Kim may now prepare the subject's relevant position in generation 4.
  }

  private fun prepareTemperatureEvent(
      event: String,
      temperature: Int,
      neutralPlacements: List<String>,
  ) {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    val laterEvents =
        listOf(
            "RedResistance",
            "CharismaticWgPresident",
            "BioengineeringBoom",
            "MiningRestrictions",
        )
    repeat(temperature) { index ->
      val first = players.single { it.count("StartToken") == 1 }
      first.doTask("Pass")
      players.single { it != first }.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      first.doTask("TemperatureStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      if (index == 1) {
        first.doTask("OceanTile<Tharsis_1_2> BY Admin")
        players.forEach { it.autoExecNow(EAGER) }
      } // Democratic Reform.
      if (index == 14) {
        first.doTask("OceanTile<Tharsis_1_2> BY Admin")
        players.forEach { it.autoExecNow(EAGER) }
      } // Temperature threshold.
      admin.doTask(neutralPlacements[index * 2])
      admin.doTask(if (index == temperature - 3) event else laterEvents[index % laterEvents.size])
      admin.doTask(neutralPlacements[index * 2 + 1])
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
    }
    admin.count("Current<$event>") shouldBe 1
    admin.count("TemperatureStep") shouldBe temperature
  }
}
