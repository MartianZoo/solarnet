package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilDelegatesTest : TfmSandboxTest() {
  @Test
  internal fun `An empty party cannot receive a follower`() {
    newTestGame("TurmoilExpansion")
    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
      }
    }
    kim.count("PartyDelegate<Scientists>") shouldBe 0
    kim.count("LobbyDelegate") shouldBe 1
  }

  @Test
  internal fun `Leaders and chairman score while followers and lobby delegates do not`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina("PartyLeader<Scientists>, PartyFollower<Scientists>")
    stan.exMachina("Chairman<Stan FROM Neutral>, PartyLeader<Unity>")
    victoryPoints() shouldBe listOf(21, 22, 20)
  }

  @Test
  internal fun `Choosing leader on a tie cannot complete the turn`() {
    newTestGame("TurmoilExpansion")
    kim.inTurn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Scientists> FROM LobbyDelegate")
    }
    shouldThrow<RequirementException> {
      stan.inTurn {
        doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Scientists> FROM LobbyDelegate")
      }
    }
    stan.count("LobbyDelegate") shouldBe 1
    stan.count("PartyDelegate") shouldBe 0
    kim.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun `Choosing follower when overtaking the incumbent cannot complete the turn`() {
    newTestGame("TurmoilExpansion")
    kim.inTurn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Scientists> FROM LobbyDelegate")
    }
    stan.inTurn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
    }
    shouldThrow<DeadEndException> {
      stan.inTurn {
        doTasks(
            "UseAction<PaidLobbyAction, Action1>",
            "5 Pay<> FROM MC",
            "PartyFollower<Scientists>",
        )
      }
    }
    stan.count("MC") shouldBe 42
    stan.count("PartyDelegate<Scientists>") shouldBe 1
    kim.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun `Recruitment can promote another player while the recruiter remains a follower`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina("3 PartyFollower<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>")
    rob.exMachina("4 PartyFollower<MarsFirst>")
    stan
        .inTurn {
          doTasks(
              *playCardTasks("$Recruitment"),
              "2 Pay<> FROM MC",
              "PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
          )
        }
        .expect(
            "-PartyLeader<MarsFirst, Neutral>, PartyLeader<MarsFirst, Rob>, 0 PartyLeader<MarsFirst, Kim>"
        )
  }

  @Test
  internal fun `The last neutral reserve delegate can take over a player led party`() {
    newTestGame(
        "TurmoilExpansion",
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExperimentalLifeforms"),
    )
    kim.exMachina(
        "-$MinimalImpactPolicy, 9 PartyFollower<MarsFirst, Neutral>, " +
            "PartyLeader<Greens>, PartyFollower<Greens, Neutral>"
    )
    admin.count("Delegate<Neutral>") shouldBe 13

    agents
        .runSystem("RevealDistantEvent")
        .expect("Delegate<Neutral>, -PartyLeader<Greens, Kim>, PartyFollower<Greens, Kim>")
    admin.count("Delegate<Neutral>") shouldBe 14
  }

  @Test
  internal fun `Changing times skips both neutral placements when the reserve is empty`() {
    newTestGame(
        "TurmoilExpansion",
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExperimentalLifeforms"),
    )
    kim.exMachina("11 PartyFollower<MarsFirst, Neutral>")
    admin.count("Delegate<Neutral>") shouldBe 14

    agents.runSystem("ChangingTimes").expect("0 Delegate<Neutral>")
    admin.count("Current<$DemocraticReform>") shouldBe 1
    admin.count("Coming<$MinimalImpactPolicy>") shouldBe 1
    admin.count("Distant<$ExperimentalLifeforms>") shouldBe 1
  }

  @Test
  internal fun `Government returns more than seven neutral followers`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina("8 PartyFollower<MarsFirst, Neutral>, PartyFollower<MarsFirst>")

    agents
        .runSystem("FormGovernment")
        .expect(
            "-9 PartyDelegate<MarsFirst, Neutral>, -PartyFollower<MarsFirst, Kim>, 0 PartyDelegate<Reds, Neutral>"
        )
    admin.count("PartyDelegate<MarsFirst, Anyone>") shouldBe 0
    admin.count("Chairman<Neutral>") shouldBe 1
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Returning followers does not replace Kim with a tied challenger`() =
        electionReturningFollowers(0)

    @Test
    internal fun `Returning followers does not replace Stan with a tied challenger`() =
        electionReturningFollowers(1)

    private fun electionReturningFollowers(incumbentSeat: Int) {
      newTestGame(
          "TurmoilExpansion, -QuickStartVariant",
          playerCount = 2,
          firstGlobalEvents =
              listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExploreFirstDirective"),
      )
      val incumbent = players[incumbentSeat]
      val challenger = players[1 - incumbentSeat]
      // Both players already placed three delegates; election cleanup is the behavior under test.
      incumbent.exMachina("-LobbyDelegate, PartyLeader<Scientists>, 2 PartyFollower<Scientists>")
      challenger.exMachina("-LobbyDelegate, 3 PartyFollower<Scientists>")
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.doTask("20 MC") }

      val winner = listOf("Kim", "Stan")[incumbentSeat]
      val loser = listOf("Stan", "Kim")[incumbentSeat]
      expectChanges("Chairman<$winner>, 0 Chairman<$loser>, -6 PartyDelegate<Scientists, Anyone>") {
        kim.doTask("VenusStep! BY Admin")
      }
      admin.count("Ruling<Scientists>") shouldBe 1
      incumbent.count("Delegate") shouldBe 2 // Chairman and the refilled lobby.
      challenger.count("Delegate") shouldBe 1 // Refilled lobby only.
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
      admin.count("ActionPhase") shouldBe 1
    }

    @Test
    internal fun `A neutral event placement can overtake a player leader`() {
      newTestGame(
          "TurmoilExpansion, -QuickStartVariant",
          playerCount = 2,
          firstGlobalEvents =
              listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExperimentalLifeforms"),
      )
      kim.exMachina("PartyLeader<Greens>")
      stan.exMachina("PartyLeader<Scientists>, PartyFollower<Scientists>")
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.doTask("20 MC") }
      kim.doTask("VenusStep! BY Admin")

      // Democratic Reform becoming current ties Kim's delegate in Greens.
      // Revealing Experimental Lifeforms adds the second neutral delegate and takes the lead.
      (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
      admin.count("Distant<$ExperimentalLifeforms>") shouldBe 1
      admin.count("PartyLeader<Greens, Neutral>") shouldBe 1
      kim.count("PartyFollower<Greens>") shouldBe 1
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
      admin.count("ActionPhase") shouldBe 1
    }

    @Test
    internal fun `Government returns followers makes the leader chairman and refills the lobby`() {
      newTestGame("TurmoilExpansion, -QuickStartVariant", playerCount = 2)
      kim.exMachina("-LobbyDelegate, PartyLeader<Scientists>, PartyFollower<Scientists>")
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.doTask("20 MC") }
      kim.doTask("VenusStep! BY Admin")

      kim.count("Chairman") shouldBe 1
      kim.count("PartyDelegate<Scientists>") shouldBe 0
      kim.count("LobbyDelegate") shouldBe 1
      kim.count("Delegate") shouldBe 2
      admin.count("Ruling<Scientists>") shouldBe 1
      admin.count("Chairman<Neutral>") shouldBe 0
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
      admin.count("ActionPhase") shouldBe 1
    }
  }
}
