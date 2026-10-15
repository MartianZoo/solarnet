package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Solar ordering is observed through game consequences, never through an execution trace. */
internal class SolarPhaseOrderingTest : TfmGameplayTest() {
  @Test
  internal fun `Dry Deserts can remove the ocean just placed by the outgoing first player`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    finishFirstGeneration("ExploreFirstDirective")
    finishSecondGeneration("FreeAcademiaTreaty")

    kim.doTask("Pass")
    stan.doTask("Pass")
    settlePlayers()
    expectChanges("0 OceanTile<Tharsis_3_6>") {
      kim.doTask("OceanTile<Tharsis_3_6>! BY Admin")
      settlePlayers()
      kim.doTask("-OceanTile<Tharsis_3_6>")
      settlePlayers()
    }
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
  }

  @Test
  internal fun `Red Influence charges the lower bracket after TR revision`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    reachFourthGeneration("AntarcticaMelts")
    stan.doTask("Pass")
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.count("TerraformRating") shouldBe 20
    kim.doTask("Pass")
    settlePlayers()

    // At TR 19 Red Influence costs 3 MC; at TR 20 it would cost 6 MC.
    expectChanges("-3 MC<Kim>, -TerraformRating<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      settlePlayers()
    }
  }

  @Test
  internal fun `Kelvinists pays for heat production received from the current global event`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    reachFourthGeneration("PateraBoring")
    stan.doTask("Pass")
    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Kelvinists>")
        }
        .expect("0 MC, Delegate")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Kelvinists>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Kelvinists>")
    }
    kim.doTask("Pass")
    settlePlayers()

    // Leader plus supporting delegates give 2 influence. Heat production rises from 1 to 3.
    expectChanges("PROD[2 Heat<Kim>], 3 MC<Kim>") {
      stan.doTask("VenusStep! BY Admin")
      settlePlayers()
    }
  }

  @Test
  internal fun `The outgoing chairman and incoming delegation receive their old influence rewards`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    electKimInFirstGeneration("MarsFirst")
    finishResearch("FreeAcademiaTreaty")

    stan
        .completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Reds>")
        }
        .expect("0 MC, Delegate")
    stan.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Reds>")
    }
    kim.doTask("Pass")
    stan.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Reds>")
    }
    stan.doTask("Pass")
    settlePlayers()

    expectChanges("Plant<Kim>, Steel<Kim>, 2 Plant<Stan>, 2 Steel<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      settlePlayers()
      // Stan is still first player when Democratic Reform asks for its ocean.
      stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
      settlePlayers()
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
          doTasks("LobbyAction", "PartyDelegate<MarsFirst>")
        }
        .expect("0 MC, Delegate")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<MarsFirst>")
    }
    stan.doTask("Pass")
    // All seven delegates are deployed: there is nothing in reserve to refill the lobby yet.
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<MarsFirst>")
    }
    kim.count("Delegate") shouldBe 7
    kim.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()
    finishResearch("FreeAcademiaTreaty")
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Unity>")
        }
        .expect("0 MC, Delegate")
  }

  @Test
  internal fun `The coming event wins a dominance tie before the newly revealed distant event`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<MarsFirst>")
        }
        .expect("0 MC, Delegate")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<MarsFirst>")
    }
    stan
        .completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Greens>")
        }
        .expect("0 MC, Delegate")
    stan.doTask("0 StandardAction") // Decline the second action.
    kim.doTask("Pass")
    stan.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()

    // Clearing Mars First leaves Reds and Greens at 1 each. Democratic Reform adds to Greens;
    // Red Resistance then adds to Reds. Both finish at 2, but Greens keeps dominance.
    admin.doTask("RedResistance")
    admin.count("PartyDelegate<Greens, Anyone>") shouldBe 2
    admin.count("PartyDelegate<Reds, Anyone>") shouldBe 2
    admin.count("Dominant<Greens>") shouldBe 1
    admin.count("PartyDelegate<MarsFirst, Anyone>") shouldBe 0
  }

  @Test
  internal fun `The next dominant party earns influence after the elected party is emptied`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<MarsFirst>")
        }
        .expect("0 MC, Delegate")
    kim.doTask("0 StandardAction") // Decline the second action.
    stan
        .completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Kelvinists>")
        }
        .expect("0 MC, Delegate")
    stan.doTask("0 StandardAction") // Decline the second action.
    kim.doTask("Pass")
    stan.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()
    finishResearch("FreeAcademiaTreaty")

    // Kelvinists wins the clockwise tie after both the neutral and Kim leave Mars First.
    // The later neutral additions only tie it. Stan's leadership must therefore earn influence.
    stan.doTask("Pass")
    kim.doTask("Pass")
    settlePlayers()
    expectChanges("Plant<Stan>, Steel<Stan>") {
      stan.doTask("VenusStep! BY Admin")
      settlePlayers()
      stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
      settlePlayers()
    }
  }

  @Test
  internal fun `New neutral delegates do not change which party pays this election's bonus`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.doTasks("PlayProject<Class<$Mine>>", "-4 MC").expect("PROD[Steel]")
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")
    kim.doTask("Pass")
    settlePlayers()

    // Mars First is tied with Reds, but incumbent. Red Resistance will make Reds larger;
    // this election still pays Mars First's building-tag bonus, not the Reds' tied-lowest TR.
    expectChanges("MC<Kim>, -TerraformRating<Kim>, -TerraformRating<Stan>") {
      kim.doTask("VenusStep! BY Admin")
      settlePlayers()
      admin.doTask("RedResistance")
    }
  }

  @Test
  internal fun `UNMI cannot use an Election global event's TR in the following generation`() {
    newTestGame(
        addOptions = "TurmoilExpansion",
        playerCount = 2,
        kimCorporation = UnitedNationsMarsInitiative,
    )
    reachFourthGeneration("SelfSufficiencyProgram")
    stan.doTask("Pass")
    kim.completeOperation {
          doTasks("PlayProject<Class<$Mine>>", "-2 Steel")
          doTask("Ok") // Decline MC payment; the two steel paid the full cost.
          autoExecNow(EAGER)
        }
        .expect("PROD[Steel]")
    kim.doTask("Pass")
    settlePlayers()
    expectChanges("TerraformRating<Kim>") {
      // Revision costs 1; Kim's building tag wins Election's 2 TR. Kim holds no delegates.
      stan.doTask("VenusStep! BY Admin")
      settlePlayers()
    }
    finishResearch("ExploreFirstDirective")

    shouldThrow<RequirementException> {
      kim.completeOperation {
        doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>", "-3 MC")
        autoExecNow(EAGER)
      }
    }
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>", "-3 MC")
        .expect("TerraformRating")
  }

  @Test
  internal fun `UNMI cannot use last generation's Reds bonus or chairman TR in the new action phase`() {
    newTestGame(
        addOptions = "TurmoilExpansion",
        playerCount = 2,
        kimCorporation = UnitedNationsMarsInitiative,
    )
    electKimInFirstGeneration("Reds")
    finishResearch("FreeAcademiaTreaty")
    stan.doTask("Pass")

    shouldThrow<RequirementException> {
      kim.completeOperation {
        doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>", "-3 MC")
        autoExecNow(EAGER)
      }
    }
    // A real TR gain in this generation does qualify; rejection did not consume the action.
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
          autoExecNow(EAGER)
        }
        .expect("TerraformRating")
    kim.doTasks("UseCardAction<$UnitedNationsMarsInitiative, Action1>", "-3 MC")
        .expect("TerraformRating")
  }

  @Test
  internal fun `Pristar still preserves after receiving chairman TR in the previous solar phase`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2, kimCorporation = Pristar)
    electKimInFirstGeneration("MarsFirst")
    finishResearch("FreeAcademiaTreaty")
    stan.doTask("Pass")

    expectChanges("Preservation<Pristar<Kim>>") {
      kim.doTask("Pass")
      settlePlayers()
    }
  }

  @Test
  internal fun `The former chairman returns the only delegate available for a free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    electKimInFirstGeneration("MarsFirst")
    finishResearch("FreeAcademiaTreaty")
    stan
        .completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Reds>")
        }
        .expect("0 MC, Delegate")
    stan.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Reds>")
    }
    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Kelvinists>")
        }
        .expect("0 MC, Delegate")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Kelvinists>")
    }
    stan.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Reds>")
    }
    stan.doTask("0 StandardAction") // Decline the second action.
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Greens>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Greens>")
    }
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Unity>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Unity>")
    }
    kim.count("Delegate") shouldBe 7
    kim.doTask("Pass")
    settlePlayers()
    stan.doTask("VenusStep! BY Admin")
    settlePlayers()
    stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
    settlePlayers()
    finishResearch("RedResistance")

    // None of Kim's six party delegates belonged to the elected Reds.
    kim.count("PartyDelegate") shouldBe 6
    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Scientists>")
        }
        .expect("0 MC, Delegate")
  }

  @Test
  internal fun `Solo Reds rewards TR twenty reached by this solar phase's revision`() {
    newTestGame(addOptions = "TurmoilExpansion, PreludeExpansion", playerCount = 1)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$HugeAsteroid>>")
    settlePlayers()
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SmeltingPlant>>")
    settlePlayers()
    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<Reds>")
        }
        .expect("0 MC, Delegate")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Reds>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Reds>")
    }
    kim.doTasks("PlayProject<Class<$BribedCommittee>>", "-7 MC").expect("2 TerraformRating")
    kim.count("TerraformRating") shouldBe 21
    kim.doTask("Pass")
    settlePlayers()

    expectChanges("TerraformRating<Kim>") {
      kim.doTask("VenusStep! BY Admin")
      settlePlayers()
    }
  }

  @Test
  internal fun `WGT's ocean cannot be financed with the upcoming Unity ruling bonus`() {
    prepareCashPoorElection("Unity")
    kim.doTask("OceanTile<Tharsis_3_6>! BY Admin")
    shouldThrow<LimitsException> {
      kim.completeOperation {
        doTasks("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>", "-5 MC")
      }
    }
    kim.count("MC") shouldBe 3
    kim.count("Hydroelectric") shouldBe 0
    expectChanges("5 MC<Kim>, 0 Hydroelectric<Kim>") {
      kim.doTask("0 UseAction<NeptunianOption<NeptunianPowerConsultants>>")
      settlePlayers()
    }
  }

  @Test
  internal fun `WGT's ocean cannot be financed with the upcoming chairman's Terraforming Deal reward`() {
    prepareCashPoorElection("Scientists")
    kim.doTask("OceanTile<Tharsis_3_6>! BY Admin")
    shouldThrow<LimitsException> {
      kim.completeOperation {
        doTasks("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>", "-5 MC")
      }
    }
    kim.count("MC") shouldBe 3
    kim.count("Hydroelectric") shouldBe 0
    // The Scientists' 1 MC alone would not finance it; chairman TR supplies another 2 MC.
    expectChanges("3 MC<Kim>, 0 Hydroelectric<Kim>") {
      kim.doTask("0 UseAction<NeptunianOption<NeptunianPowerConsultants>>")
      settlePlayers()
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
    settlePlayers()
    kim.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()
    finishResearch("SeparatistMovement")

    kim.doTasks("PlayProject<Class<$NeptunianPowerConsultants>>", "-14 MC")
    kim.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()
    kim.doTask("OceanTile<Tharsis_1_2> BY Admin")
    // Save the cash instead of buying a hydroelectric resource from Democratic Reform's ocean.
    kim.doTask("0 UseAction<NeptunianOption<NeptunianPowerConsultants>>")
    settlePlayers()
    finishResearch("HeatFirstPolicy")

    kim.doTasks("PlayProject<Class<$OrbitalCleanup>>", "-14 MC")
    kim.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()
    finishResearch("FreeAcademiaTreaty")

    kim.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()
    admin.doTask("ExploreFirstDirective")
    kim.doTasks("Ok", "-4 MC") // Buy all four research cards at TerraLabs' price.
    kim.count("MC") shouldBe 20
    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<$party>")
        }
        .expect("0 MC, Delegate")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<$party>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<$party>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<$party>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<$party>")
    }
    kim.count("MC") shouldBe 0
    kim.doTask("Pass")
    settlePlayers()
    kim.count("MC") shouldBe 3
    kim.count("Steel") shouldBe 0
  }

  private fun settlePlayers() {
    players.forEach { it.autoExecNow(EAGER) }
  }

  private fun finishResearch(distantEvent: String) {
    admin.doTask(distantEvent)
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  private fun finishFirstGeneration(distantEvent: String) {
    kim.doTask("Pass")
    stan.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()
    finishResearch(distantEvent)
  }

  private fun finishSecondGeneration(distantEvent: String) {
    stan.doTask("Pass")
    kim.doTask("Pass")
    settlePlayers()
    stan.doTask("VenusStep! BY Admin")
    settlePlayers()
    stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
    settlePlayers()
    finishResearch(distantEvent)
  }

  /** The fixture's first two events resolve normally; the chosen event is due in generation 4. */
  private fun reachFourthGeneration(event: String) {
    finishFirstGeneration(event)
    finishSecondGeneration("FreeAcademiaTreaty")
    kim.doTask("Pass")
    stan.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    // Dry Deserts removes the sole ocean without a location choice.
    settlePlayers()
    finishResearch("MoralMovement")
  }

  private fun electKimInFirstGeneration(party: String) {
    kim.completeOperation {
          doTasks("LobbyAction", "PartyDelegate<$party>")
        }
        .expect("0 MC, Delegate")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<$party>")
    }
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<$party>")
    }
    kim.doTask("Pass")
    settlePlayers()
    kim.doTask("VenusStep! BY Admin")
    settlePlayers()
  }
}
