package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Solar ordering is observed through game consequences, never through an execution trace. */
internal class SolarPhaseOrderingTest : TfmGameplayTest() {
  @Test
  internal fun `Dry Deserts can remove the ocean just placed by the outgoing first player`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    finishFirstGeneration(
        "ExploreFirstDirective",
        "PartyLeader<Greens, Neutral>",
        "PartyFollower<Reds, Neutral>",
    )
    finishSecondGeneration(
        "FreeAcademiaTreaty",
        "PartyLeader<Unity, Neutral>",
        "PartyLeader<Scientists, Neutral>",
    )

    kim.doTask("Pass")
    stan.doTask("Pass")
    autoExecutePlayers()
    expectChanges("0 OceanTile<Tharsis_1_5>") {
      kim.doTask("OceanTile<Tharsis_1_5>! BY Admin")
      autoExecutePlayers()
      kim.doTask("-OceanTile<Tharsis_1_5>")
      autoExecutePlayers()
    }
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
  }

  @Test
  internal fun `Red Influence charges the lower bracket after TR revision`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    reachFourthGeneration(
        "AntarcticaMelts",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Reds, Neutral>",
            "PartyFollower<Reds, Neutral>",
        ),
    )
    stan.doTask("Pass")
    kim.completeOperation {
          doTasks(*standardProjectTasks("AsteroidProject"))
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.completeOperation {
          doTasks(*standardProjectTasks("AsteroidProject"))
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.count("TerraformRating") shouldBe 20
    kim.doTask("Pass")
    autoExecutePlayers()

    // At TR 19 Red Influence costs 3 MC; at TR 20 it would cost 6 MC.
    expectChanges("-3 MC<Kim>, -TerraformRating<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      autoExecutePlayers()
    }
  }

  @Test
  internal fun `Kelvinists pays for heat production received from the current global event`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    reachFourthGeneration(
        "PateraBoring",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyFollower<Scientists, Neutral>",
            "PartyLeader<Kelvinists, Neutral>",
            "PartyLeader<Reds, Neutral>",
        ),
    )
    stan.doTask("Pass")
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Kelvinists> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<Kelvinists>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<Kelvinists>")
    }
    kim.doTask("Pass")
    autoExecutePlayers()

    // PartyLeader plus supporting delegates give 2 influence. Heat production rises from 1 to 3.
    expectChanges("PROD[2 Heat<Kim>], 3 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      autoExecutePlayers()
    }
  }

  @Test
  internal fun `The outgoing chairman and incoming delegation receive their old influence rewards`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    electKimInFirstGeneration("MarsFirst")
    finishResearch(
        "FreeAcademiaTreaty",
        "PartyLeader<Greens, Neutral>",
        "PartyLeader<Scientists, Neutral>",
    )

    stan
        .completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    stan.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<Reds>")
    }
    kim.doTask("Pass")
    stan.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<Reds>")
    }
    stan.doTask("Pass")
    autoExecutePlayers()

    expectChanges("Plant<Kim>, Steel<Kim>, 2 Plant<Stan>, 2 Steel<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      autoExecutePlayers()
      // Stan is still first player when Democratic Reform asks for its ocean.
      stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
      autoExecutePlayers()
    }
    stan.count("Chairman") shouldBe 1
  }

  @Test
  internal fun `Reds rewards the tied lowest player who is about to become chairman`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    // Both start at TR 20. Revision and the Reds bonus cancel, then Kim gets chairman TR.
    electKimInFirstGeneration("Reds")
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `Delegates released by the election supply the next free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<MarsFirst>")
    }
    stan.doTask("Pass")
    // All seven delegates are deployed: there is nothing in reserve to refill the lobby yet.
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<MarsFirst>")
    }
    kim.count("Delegate") shouldBe 7
    kim.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    finishResearch(
        "FreeAcademiaTreaty",
        "PartyLeader<Greens, Neutral>",
        "PartyLeader<Scientists, Neutral>",
    )
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Unity> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
  }

  @Test
  internal fun `The coming event wins a dominance tie before the newly revealed distant event`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<MarsFirst>")
    }
    stan
        .completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    stan.selectTask("PassBaton") // Decline the second action.
    kim.doTask("Pass")
    stan.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()

    // Clearing Mars First leaves Reds and Greens at 1 each. Democratic Reform adds to Greens;
    // Red Resistance then adds to Reds. Both finish at 2, but Greens keeps dominance.
    admin.doTasks("PartyFollower<Greens, Neutral>", "RedResistance", "PartyFollower<Reds, Neutral>")
    admin.count("PartyDelegate<Greens, Anyone>") shouldBe 2
    admin.count("PartyDelegate<Reds, Anyone>") shouldBe 2
    admin.count("Dominant<Greens>") shouldBe 1
    admin.count("PartyDelegate<MarsFirst, Anyone>") shouldBe 0
  }

  @Test
  internal fun `The next dominant party earns influence after the elected party is emptied`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    kim.selectTask("PassBaton") // Decline the second action.
    stan
        .completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Kelvinists> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    stan.selectTask("PassBaton") // Decline the second action.
    kim.doTask("Pass")
    stan.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    finishResearch(
        "FreeAcademiaTreaty",
        "PartyLeader<Greens, Neutral>",
        "PartyLeader<Scientists, Neutral>",
    )

    // Kelvinists wins the clockwise tie after both the neutral and Kim leave Mars First.
    // The later neutral additions only tie it. Stan's leadership must therefore earn influence.
    stan.doTask("Pass")
    kim.doTask("Pass")
    autoExecutePlayers()
    expectChanges("Plant<Stan>, Steel<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      autoExecutePlayers()
      stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
      autoExecutePlayers()
    }
  }

  @Test
  internal fun `New neutral delegates do not change which party pays this election's bonus`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.doTasks(*playCardTasks("$Mine")).expect("PROD[Steel]")
    kim.selectTask("PassBaton") // Decline the second action.
    stan.doTask("Pass")
    kim.doTask("Pass")
    autoExecutePlayers()

    // Mars First is tied with Reds, but incumbent. Red Resistance will make Reds larger;
    // this election still pays Mars First's building-tag bonus, not the Reds' tied-lowest TR.
    expectChanges("MC<Kim>, -TerraformRating<Kim>, -TerraformRating<Stan>") {
      kim.doTask("VenusStep! BY Admin")
      autoExecutePlayers()
      admin.doTasks("PartyLeader<Greens, Neutral>", "RedResistance", "PartyFollower<Reds, Neutral>")
    }
  }

  @Test
  internal fun `UNMI cannot use an Election global event's TR in the following generation`() {
    newTestGame(
        addOptions = "TurmoilExpansion",
        playerCount = 2,
        kimCorporation = UnitedNationsMarsInitiative,
    )
    reachFourthGeneration(
        "SelfSufficiencyProgram",
        listOf(
            "PartyLeader<Greens, Neutral>",
            "PartyFollower<Greens, Neutral>",
            "PartyLeader<Unity, Neutral>",
            "PartyLeader<Scientists, Neutral>",
            "PartyLeader<MarsFirst, Neutral>",
            "PartyLeader<Reds, Neutral>",
        ),
    )
    stan.doTask("Pass")
    kim.completeOperation {
          doTasks(*playCardTasks("$Mine"), "2 Pay<Class<Steel>> FROM Steel")
          autoExecNow(EAGER)
        }
        .expect("PROD[Steel]")
    kim.doTask("Pass")
    autoExecutePlayers()
    expectChanges("TerraformRating<Kim>") {
      // Revision costs 1; Kim's building tag wins Election's 2 TR. Kim holds no delegates.
      stan.doTask("VenusStep! BY Admin")
      autoExecutePlayers()
    }
    finishResearch(
        "ExploreFirstDirective",
        "PartyFollower<Scientists, Neutral>",
        "PartyFollower<Reds, Neutral>",
    )

    shouldThrow<RequirementException> {
      kim.completeOperation {
        doTasks(*cardActionTasks("$UnitedNationsMarsInitiative"))
        autoExecNow(EAGER)
      }
    }
    kim.completeOperation {
          doTasks(*standardProjectTasks("AsteroidProject"))
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.doTasks(*cardActionTasks("$UnitedNationsMarsInitiative")).expect("TerraformRating")
  }

  @Test
  internal fun `UNMI cannot use last generation's Reds bonus or chairman TR in the new action phase`() {
    newTestGame(
        addOptions = "TurmoilExpansion",
        playerCount = 2,
        kimCorporation = UnitedNationsMarsInitiative,
    )
    electKimInFirstGeneration("Reds")
    finishResearch(
        "FreeAcademiaTreaty",
        "PartyLeader<Greens, Neutral>",
        "PartyLeader<Scientists, Neutral>",
    )
    stan.doTask("Pass")

    shouldThrow<RequirementException> {
      kim.completeOperation {
        doTasks(*cardActionTasks("$UnitedNationsMarsInitiative"))
        autoExecNow(EAGER)
      }
    }
    // A real TR gain in this generation does qualify; rejection did not consume the action.
    kim.completeOperation {
          doTasks(*standardProjectTasks("AsteroidProject"))
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.doTasks(*cardActionTasks("$UnitedNationsMarsInitiative")).expect("TerraformRating")
  }

  @Test
  internal fun `Pristar still preserves after receiving chairman TR in the previous solar phase`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2, kimCorporation = Pristar)
    electKimInFirstGeneration("MarsFirst")
    finishResearch(
        "FreeAcademiaTreaty",
        "PartyLeader<Greens, Neutral>",
        "PartyLeader<Scientists, Neutral>",
    )
    stan.doTask("Pass")

    expectChanges("Preservation<Pristar<Kim>>") {
      kim.doTask("Pass")
      autoExecutePlayers()
    }
  }

  @Test
  internal fun `The former chairman returns the only delegate available for a free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    electKimInFirstGeneration("MarsFirst")
    finishResearch(
        "FreeAcademiaTreaty",
        "PartyLeader<Greens, Neutral>",
        "PartyLeader<Scientists, Neutral>",
    )
    stan
        .completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    stan.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<Reds>")
    }
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Kelvinists> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<Kelvinists>")
    }
    stan.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<Reds>")
    }
    stan.selectTask("PassBaton") // Decline the second action.
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<Greens>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<Greens>")
    }
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<Unity>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<Unity>")
    }
    kim.count("Delegate") shouldBe 7
    kim.doTask("Pass")
    autoExecutePlayers()
    stan.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
    autoExecutePlayers()
    finishResearch("RedResistance", "PartyFollower<Unity, Neutral>", "PartyLeader<Reds, Neutral>")

    // None of Kim's six party delegates belonged to the elected Reds.
    kim.count("PartyDelegate") shouldBe 6
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
  }

  @Test
  internal fun `Solo Reds rewards TR twenty reached by this solar phase's revision`() {
    newTestGame(addOptions = "TurmoilExpansion, PreludeExpansion", playerCount = 1)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$HugeAsteroid>>")
    autoExecutePlayers()
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SmeltingPlant>>")
    autoExecutePlayers()
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<Reds>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<Reds>")
    }
    kim.doTasks(*playCardTasks("$BribedCommittee")).expect("2 TerraformRating")
    kim.count("TerraformRating") shouldBe 21
    kim.doTask("Pass")
    autoExecutePlayers()

    expectChanges("TerraformRating<Kim>") {
      kim.doTask("VenusStep! BY Admin")
      autoExecutePlayers()
    }
  }

  @Test
  internal fun `WGT's ocean cannot be financed with the upcoming Unity ruling bonus`() {
    prepareCashPoorElection("Unity")
    kim.doTask("OceanTile<Tharsis_1_5>! BY Admin")
    shouldThrow<LimitsException> {
      kim.completeOperation {
        doTasks("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>", "5 Pay<> FROM MC")
      }
    }
    kim.count("MC") shouldBe 3
    kim.count("Hydroelectric") shouldBe 0
    expectChanges("5 MC<Kim>, 0 Hydroelectric<Kim>") {
      kim.doTask("0 UseAction<NeptunianOption<NeptunianPowerConsultants>>")
      autoExecutePlayers()
    }
  }

  @Test
  internal fun `WGT's ocean cannot be financed with the upcoming chairman's Terraforming Deal reward`() {
    prepareCashPoorElection("Scientists")
    kim.doTask("OceanTile<Tharsis_1_5>! BY Admin")
    shouldThrow<LimitsException> {
      kim.completeOperation {
        doTasks("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>", "5 Pay<> FROM MC")
      }
    }
    kim.count("MC") shouldBe 3
    kim.count("Hydroelectric") shouldBe 0
    // The Scientists' 1 MC alone would not finance it; chairman TR supplies another 2 MC.
    expectChanges("3 MC<Kim>, 0 Hydroelectric<Kim>") {
      kim.doTask("0 UseAction<NeptunianOption<NeptunianPowerConsultants>>")
      autoExecutePlayers()
    }
  }

  /** Five real generations leave TerraLabs with TR 6, MC production -3, and no steel. */
  private fun prepareCashPoorElection(party: String) {
    newTestGame(
        addOptions =
            "TurmoilExpansion, PreludeExpansion, TerraformingDeal, -QuickStartVariant, -ColoniesExpansion",
        playerCount = 1,
        kimCorporation = TerraLabsResearch,
    )
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SocietySupport>>")
    autoExecutePlayers()
    kim.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    finishResearch(
        "SeparatistMovement",
        "PartyLeader<Greens, Neutral>",
        "PartyLeader<MarsFirst, Neutral>",
    )

    kim.doTasks(*playCardTasks("$NeptunianPowerConsultants"))
    kim.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    kim.doTask("OceanTile<Tharsis_1_2> BY Admin")
    // Save the cash instead of buying a hydroelectric resource from Democratic Reform's ocean.
    kim.doTask("0 UseAction<NeptunianOption<NeptunianPowerConsultants>>")
    autoExecutePlayers()
    finishResearch(
        "HeatFirstPolicy",
        "PartyLeader<Unity, Neutral>",
        "PartyLeader<Kelvinists, Neutral>",
    )

    kim.doTasks(*playCardTasks("$OrbitalCleanup"))
    kim.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    finishResearch(
        "FreeAcademiaTreaty",
        "PartyFollower<Kelvinists, Neutral>",
        "PartyLeader<Scientists, Neutral>",
    )

    kim.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    admin.doTasks(
        "PartyLeader<Greens, Neutral>",
        "ExploreFirstDirective",
        "PartyLeader<Reds, Neutral>",
    )
    kim.doTasks("Ok") // Buy all four research cards at TerraLabs' price.
    kim.count("MC") shouldBe 20
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<$party> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<$party>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<$party>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<$party>")
    }
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<$party>")
    }
    kim.count("MC") shouldBe 0
    kim.doTask("Pass")
    autoExecutePlayers()
    kim.count("MC") shouldBe 3
    kim.count("Steel") shouldBe 0
  }

  private fun autoExecutePlayers() {
    players.forEach { it.autoExecNow(EAGER) }
  }

  private fun finishResearch(
      distantEvent: String,
      currentPlacement: String,
      distantPlacement: String,
  ) {
    admin.doTasks(currentPlacement, distantEvent, distantPlacement)
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  private fun finishFirstGeneration(
      distantEvent: String,
      currentPlacement: String,
      distantPlacement: String,
  ) {
    kim.doTask("Pass")
    stan.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    finishResearch(distantEvent, currentPlacement, distantPlacement)
  }

  private fun finishSecondGeneration(
      distantEvent: String,
      currentPlacement: String,
      distantPlacement: String,
  ) {
    stan.doTask("Pass")
    kim.doTask("Pass")
    autoExecutePlayers()
    stan.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
    stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
    autoExecutePlayers()
    finishResearch(distantEvent, currentPlacement, distantPlacement)
  }

  /** The fixture's first two events resolve normally; the chosen event is due in generation 4. */
  private fun reachFourthGeneration(event: String, neutralPlacements: List<String>) {
    finishFirstGeneration(event, neutralPlacements[0], neutralPlacements[1])
    finishSecondGeneration("FreeAcademiaTreaty", neutralPlacements[2], neutralPlacements[3])
    kim.doTask("Pass")
    stan.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    // Dry Deserts removes the sole ocean without a location choice.
    autoExecutePlayers()
    finishResearch("MoralMovement", neutralPlacements[4], neutralPlacements[5])
  }

  private fun electKimInFirstGeneration(party: String) {
    kim.completeOperation {
          doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<$party> FROM LobbyDelegate")
        }
        .expect("0 MC, 0 Delegate")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyLeader<$party>")
    }
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("UseAction<PaidLobbyAction, Action1>", "PartyFollower<$party>")
    }
    kim.doTask("Pass")
    autoExecutePlayers()
    kim.doTask("VenusStep! BY Admin")
    autoExecutePlayers()
  }
}
