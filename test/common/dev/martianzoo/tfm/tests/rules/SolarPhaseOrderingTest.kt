package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.count
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
  private val options = "TurmoilExpansion, -QuickStartVariant, -ColoniesExpansion"

  @Test
  internal fun `Dry Deserts can remove the ocean just placed by the outgoing first player`() {
    newTestGame(
        options,
        playerCount = 2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$FreeAcademiaTreaty", "$ExploreFirstDirective"),
    )
    // Prepare an already-current event and its pending effect; earlier event reveals are
    // incidental.
    agents.correct(
        "OceanTile<Tharsis_1_2>, $MinimalImpactPolicy, Current<$MinimalImpactPolicy>, " +
            "PendingGlobalEffect<Current<$MinimalImpactPolicy>, Class<DryDeserts>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }

    expectChanges("0 OceanTile<Tharsis_1_5>") {
      kim.doTasks(
          "OceanTile<Tharsis_1_5>! BY Admin",
          "-OceanTile<Tharsis_1_5>",
      )
      players.forEach { it.doTask("EACH") } // No influence to exchange for resources.
    }
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  internal fun `Red Influence charges the lower bracket after TR revision`() {
    newTestGame(options, playerCount = 2)
    agents.correct(
        "$AntarcticaMelts, Current<$AntarcticaMelts>, " +
            "PendingGlobalEffect<Current<$AntarcticaMelts>, Class<RedInfluence>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }

    // At TR 19 Red Influence costs 3 MC; at TR 20 it would cost 6 MC.
    expectChanges("-3 MC<Kim>, -TerraformRating<Kim>") {
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.doTasks("-3 MC", "PROD[0 MC]") }
    }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  internal fun `Kelvinists pays for heat production received from the current global event`() {
    newTestGame(options, playerCount = 2)
    agents.correct(
        "$PateraBoring, Current<$PateraBoring>, " +
            "PendingGlobalEffect<Current<$PateraBoring>, Class<VolcanicEruptions>>"
    )
    kim.exMachina("PROD[Heat], PartyLeader<Kelvinists>, PartyFollower<Kelvinists>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("20 MC", "Heat")
    stan.doTask("20 MC")

    // Leader plus supporting delegate give two influence; the new total of three heat production
    // pays.
    expectChanges("PROD[2 Heat<Kim>], 3 MC<Kim>") {
      kim.doTasks(
          "VenusStep! BY Admin",
          "PROD[2 Heat]",
      )
      stan.doTask("PROD[0 Heat]")
    }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  internal fun `The outgoing chairman and incoming delegation receive their old influence rewards`() {
    newTestGame(
        options,
        playerCount = 2,
        firstGlobalEvents =
            listOf("$FreeAcademiaTreaty", "$ExploreFirstDirective", "$MoralMovement"),
    )
    agents.correct(
        "$DemocraticReform, Current<$DemocraticReform>, " +
            "PendingGlobalEffect<Current<$DemocraticReform>, Class<AquiferReleasedByPublicCouncil>>"
    )
    kim.exMachina("Chairman<Kim> FROM Chairman<Neutral>")
    stan.exMachina("PartyLeader<Reds> FROM PartyLeader<Reds, Neutral>, 2 PartyFollower<Reds>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }

    kim.doTask("VenusStep! BY Admin")
    // Influence belongs to the outgoing committee, even though Stan will replace Kim as chairman.
    kim.count("Influence") shouldBe 1
    stan.count("Influence") shouldBe 2
    kim.doTasks(
        "OceanTile<Tharsis_1_2> BY Admin",
        "Plant",
        "Steel",
    )
    stan.doTasks("2 Plant", "2 Steel")
    stan.count("Chairman") shouldBe 1
    kim.count("Chairman") shouldBe 0
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  internal fun `Reds rewards the tied lowest player who is about to become chairman`() {
    newTestGame(options, playerCount = 2)
    kim.exMachina("PartyLeader<Reds> FROM PartyLeader<Reds, Neutral>, 2 PartyFollower<Reds>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    // Both start at TR 20. Revision and the Reds bonus cancel, then Kim gets chairman TR.
    kim.doTask("VenusStep! BY Admin").expect("TerraformRating<Kim>, 0 TerraformRating<Stan>")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `Delegates released by the election supply the next free lobby action`() {
    newTestGame(
        options,
        playerCount = 2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$FreeAcademiaTreaty"),
    )
    // All seven delegates are deployed; the election must return enough to refill the lobby.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 6 PartyFollower<MarsFirst>"
    )
    kim.count("Delegate") shouldBe 7
    kim.count("LobbyDelegate") shouldBe 0
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")

    kim.turn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Unity> FROM LobbyDelegate")
          .expect("0 MC, 0 Delegate")
    }
  }

  @Test
  internal fun `The coming event wins a dominance tie before the newly revealed distant event`() {
    newTestGame(
        options,
        playerCount = 2,
        firstGlobalEvents = listOf("$DemocraticReform", "$MinimalImpactPolicy", "$RedResistance"),
    )
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, PartyFollower<MarsFirst>"
    )
    stan.exMachina("PartyLeader<Greens>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

    // Clearing Mars First leaves Reds and Greens at one each. Democratic Reform adds to Greens;
    // Red Resistance then adds to Reds. Both finish at two, but Greens keeps dominance.
    admin.count("PartyDelegate<Greens, Anyone>") shouldBe 2
    admin.count("PartyDelegate<Reds, Anyone>") shouldBe 2
    admin.count("Dominant<Greens>") shouldBe 1
    admin.count("PartyDelegate<MarsFirst, Anyone>") shouldBe 0
  }

  @Test
  internal fun `The next dominant party earns influence after the elected party is emptied`() {
    newTestGame(
        options,
        playerCount = 2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$FreeAcademiaTreaty"),
    )
    kim.exMachina("PartyFollower<MarsFirst>")
    stan.exMachina("PartyLeader<Kelvinists>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

    // Kelvinists wins the clockwise tie after Mars First empties; the later additions only tie it.
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("19 MC") }
    stan.doTask("VenusStep! BY Admin")
    stan.count("Influence") shouldBe 1
    kim.count("Influence") shouldBe 0
    stan.doTasks(
        "OceanTile<Tharsis_1_2> BY Admin",
        "Plant",
        "Steel",
    )
    kim.doTasks("0 Plant", "0 Steel")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  internal fun `New neutral delegates do not change which party pays this election's bonus`() {
    newTestGame(
        options,
        playerCount = 2,
        firstGlobalEvents = listOf("$DemocraticReform", "$MinimalImpactPolicy", "$RedResistance"),
    )
    kim.turn {
      doTasks(*playCardTasks("$Mine"), "4 Pay<> FROM MC", "$Mine FROM ProjectCard", "PROD[Steel]")
    }
    stan.doTask("Pass")
    kim.doTask("Pass")
    kim.doTasks("20 MC", "Steel")
    stan.doTask("20 MC")

    // Red Resistance makes Reds larger after the election has paid Mars First's building-tag bonus.
    kim.doTask("VenusStep! BY Admin")
        .expect("MC<Kim>, -TerraformRating<Kim>, -TerraformRating<Stan>")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("Ruling<MarsFirst>") shouldBe 1
    admin.count("Dominant<Reds>") shouldBe 1
  }

  @Test
  internal fun `UNMI cannot use an Election global event's TR in the following generation`() {
    newTestGame(options, playerCount = 2, kimCorporation = UnitedNationsMarsInitiative)
    kim.exMachina("$Mine") // Only the building tag matters to Election's ranking.
    agents.correct(
        "$SelfSufficiencyProgram, Current<$SelfSufficiencyProgram>, " +
            "PendingGlobalEffect<Current<$SelfSufficiencyProgram>, Class<Election>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    expectChanges("TerraformRating<Kim>") {
      // Revision costs one; Kim's building tag wins Election's two TR.
      kim.doTasks(
          "VenusStep! BY Admin",
          "2 TerraformRating",
      )
      stan.doTask("TerraformRating")
    }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")

    shouldThrow<RequirementException> {
      kim.completeOperation {
        doTasks(
            *cardActionTasks("$UnitedNationsMarsInitiative"),
            "3 Pay<> FROM MC",
            "TerraformRating",
        )
      }
    }
    kim.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
      )
    }
  }

  @Test
  internal fun `UNMI cannot use last generation's Reds bonus or chairman TR in the new action phase`() {
    newTestGame(options, playerCount = 2, kimCorporation = UnitedNationsMarsInitiative)
    kim.exMachina("PartyLeader<Reds> FROM PartyLeader<Reds, Neutral>, 2 PartyFollower<Reds>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin").expect("TerraformRating<Kim>")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")

    shouldThrow<RequirementException> {
      kim.completeOperation {
        doTasks(
            *cardActionTasks("$UnitedNationsMarsInitiative"),
            "3 Pay<> FROM MC",
            "TerraformRating",
        )
      }
    }
    // A real gain this generation qualifies; rejection did not consume the action.
    kim.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "-3 MC",
      )
      doTasks(
          *cardActionTasks("$UnitedNationsMarsInitiative"),
          "3 Pay<> FROM MC",
          "TerraformRating",
          "-3 MC",
      )
    }
  }

  @Test
  internal fun `Pristar still preserves after receiving chairman TR in the previous solar phase`() {
    newTestGame(options, playerCount = 2, kimCorporation = Pristar)
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 2 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("24 MC", "Preservation<$Pristar>")
    stan.doTask("20 MC")
    kim.doTask("VenusStep! BY Admin").expect("0 TerraformRating<Kim>")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.doTask("Pass")

    kim.doTask("Pass")
    kim.doTasks("24 MC", "Preservation<$Pristar>")
    stan.doTask("19 MC")
    kim.count("Preservation<$Pristar>") shouldBe 2
    admin.count("SolarPhase") shouldBe 1
  }

  @Test
  internal fun `The former chairman returns the only delegate available for a free lobby action`() {
    newTestGame(
        options,
        playerCount = 2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$FreeAcademiaTreaty"),
    )
    // Six party delegates and the chairman use Kim's whole reserve. None belongs to the elected
    // Reds.
    kim.exMachina(
        "Chairman<Kim> FROM Chairman<Neutral>, " +
            "PartyLeader<Kelvinists>, PartyFollower<Kelvinists>, PartyLeader<Greens>, " +
            "PartyFollower<Greens>, PartyLeader<Unity>, PartyFollower<Unity>"
    )
    stan.exMachina("PartyLeader<Reds> FROM PartyLeader<Reds, Neutral>, 2 PartyFollower<Reds>")
    kim.count("Delegate") shouldBe 7
    kim.count("LobbyDelegate") shouldBe 0
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

    kim.count("PartyDelegate") shouldBe 6
    kim.count("Chairman") shouldBe 0
    stan.doTask("Pass")
    kim.turn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
          .expect("0 MC, 0 Delegate")
    }
  }

  @Test
  internal fun `Solo Reds rewards TR twenty reached by this solar phase's revision`() {
    newTestGame(options, playerCount = 1)
    // Prepare the rating and delegation; the revision, ruling bonus, and chairman gain remain real.
    kim.exMachina(
        "7 TerraformRating, PartyLeader<Reds> FROM PartyLeader<Reds, Neutral>, 2 PartyFollower<Reds>"
    )
    kim.doTask("Pass")
    kim.doTask("21 MC")
    kim.doTask("VenusStep! BY Admin").expect("TerraformRating")
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    kim.count("TerraformRating") shouldBe 22
  }

  @Test
  internal fun `WGT's ocean cannot be financed with the upcoming Unity ruling bonus`() {
    prepareCashPoorElection("Unity")
    kim.doTask("OceanTile<Tharsis_1_5>! BY Admin")
    shouldThrow<TaskException> {
      kim.completeOperation {
        doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
        kim.count("Owed") shouldBe 5
        doTask("5 Pay<> FROM MC") // Only three MC are available before the election.
      }
    }
    kim.count("MC") shouldBe 3
    kim.count("Hydroelectric") shouldBe 0
    expectChanges("5 MC<Kim>, 0 Hydroelectric<Kim>") {
      kim.doTask("0 UseAction<NeptunianOption<$NeptunianPowerConsultants>>")
      kim.doTask("2 MC") // Terraforming Deal rewards the chairman's TR after the ruling bonus.
    }
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
  }

  @Test
  internal fun `WGT's ocean cannot be financed with the upcoming chairman's Terraforming Deal reward`() {
    prepareCashPoorElection("Scientists")
    kim.doTask("OceanTile<Tharsis_1_5>! BY Admin")
    shouldThrow<TaskException> {
      kim.completeOperation {
        doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
        kim.count("Owed") shouldBe 5
        doTask("5 Pay<> FROM MC") // Only three MC are available before the election.
      }
    }
    kim.count("MC") shouldBe 3
    kim.count("Hydroelectric") shouldBe 0
    // The Scientists' one MC alone cannot finance it; chairman TR supplies another two MC.
    expectChanges("3 MC<Kim>, 0 Hydroelectric<Kim>") {
      kim.doTasks(
          "0 UseAction<NeptunianOption<$NeptunianPowerConsultants>>",
          "2 MC",
      )
    }
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
  }

  private fun prepareCashPoorElection(party: String) {
    newTestGame("$options, $TerraformingDeal", playerCount = 1, kimCorporation = TerraLabsResearch)
    // Earlier purchases and five generations of income are incidental. Prepare zero cash,
    // TR six, production minus three, and the cards that make the election rewards relevant.
    kim.exMachina(
        "-9 MC, -7 TerraformRating, PROD[-3 MC], $NeptunianPowerConsultants, " +
            "$TerraformingDeal, $OrbitalCleanup, PartyLeader<$party>, 2 PartyFollower<$party>"
    )
    kim.doTask("Pass")
    kim.doTask("3 MC")
    kim.count("MC") shouldBe 3
    kim.count("Steel") shouldBe 0
  }
}
