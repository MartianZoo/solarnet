package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilDelegatesTest : TfmSandboxTest() {
  @Test
  internal fun `An empty party cannot receive a follower`() {
    newTestGame("TurmoilExpansion")
    shouldThrow<DeadEndException> {
      kim.inTurn { doTasks("LobbyAction", "PartyFollower<Scientists> FROM LobbyDelegate") }
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
    kim.inTurn { doTasks("LobbyAction", "PartyLeader<Scientists> FROM LobbyDelegate") }
    shouldThrow<RequirementException> {
      stan.inTurn { doTasks("LobbyAction", "PartyLeader<Scientists> FROM LobbyDelegate") }
    }
    stan.count("LobbyDelegate") shouldBe 1
    stan.count("PartyDelegate") shouldBe 0
    kim.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun `Choosing follower when overtaking the incumbent cannot complete the turn`() {
    newTestGame("TurmoilExpansion")
    kim.inTurn { doTasks("LobbyAction", "PartyLeader<Scientists> FROM LobbyDelegate") }
    stan.inTurn { doTasks("LobbyAction", "PartyFollower<Scientists> FROM LobbyDelegate") }
    shouldThrow<DeadEndException> {
      stan.inTurn { doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyFollower<Scientists>") }
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
              "PlayProject<Class<Recruitment>>",
              "2 Pay<> FROM MC",
              "Recruitment FROM ProjectCard",
              "PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
          )
        }
        .expect(
            "-PartyLeader<MarsFirst, Neutral>, PartyLeader<MarsFirst, Rob>, 0 PartyLeader<MarsFirst, Kim>"
        )
  }

  @Test
  internal fun `The last neutral reserve delegate can take over a player led party`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina(
        "-MinimalImpactPolicy, 9 PartyFollower<MarsFirst, Neutral>, " +
            "PartyLeader<Greens>, PartyFollower<Greens, Neutral>"
    )
    admin.count("Delegate<Neutral>") shouldBe 13

    admin
        .runOperation("RevealDistantEvent") {
          doTasks("ExperimentalLifeforms", "PartyLeader<Greens, Neutral>")
        }
        .expect("Delegate<Neutral>, -PartyLeader<Greens, Kim>, PartyFollower<Greens, Kim>")
    admin.count("Delegate<Neutral>") shouldBe 14
  }

  @Test
  internal fun `Changing times skips both neutral placements when the reserve is empty`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina("11 PartyFollower<MarsFirst, Neutral>")
    admin.count("Delegate<Neutral>") shouldBe 14

    admin
        .runOperation("ChangingTimes") { doTask("ExperimentalLifeforms") }
        .expect("0 Delegate<Neutral>")
    admin.count("Current<DemocraticReform>") shouldBe 1
    admin.count("Coming<MinimalImpactPolicy>") shouldBe 1
    admin.count("Distant<ExperimentalLifeforms>") shouldBe 1
  }

  @Test
  internal fun `Government returns more than seven neutral followers`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina("8 PartyFollower<MarsFirst, Neutral>, PartyFollower<MarsFirst>")

    admin
        .runOperation("FormGovernment")
        .expect(
            "-9 PartyDelegate<MarsFirst, Neutral>, -PartyFollower<MarsFirst, Kim>, 0 PartyDelegate<Reds, Neutral>"
        )
    admin.count("PartyDelegate<MarsFirst, Anyone>") shouldBe 0
    admin.count("Chairman<Neutral>") shouldBe 1
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Returning followers during an election does not let a tied challenger take the chair`() {
      // Exercise both incumbents so the return order cannot conceal an unwanted promotion.
      for (incumbentSeat in 0..1) {
        newTestGame("TurmoilExpansion", playerCount = 2)
        val incumbent = players[incumbentSeat]
        val challenger = players[1 - incumbentSeat]
        if (incumbentSeat == 1) {
          kim.completeOperation { doTask("UseStandardProject<PowerPlantProject>") }
          kim.selectTask("PassBaton")
        }
        incumbent.completeOperation {
          doTasks("LobbyAction", "PartyLeader<Scientists> FROM LobbyDelegate")
        }
        incumbent.completeOperation { doTasks("PaidLobbyAction", "PartyFollower<Scientists>") }
        challenger.completeOperation {
          doTasks("LobbyAction", "PartyFollower<Scientists> FROM LobbyDelegate")
        }
        challenger.completeOperation { doTasks("PaidLobbyAction", "PartyFollower<Scientists>") }
        incumbent.completeOperation { doTasks("PaidLobbyAction", "PartyFollower<Scientists>") }
        incumbent.selectTask("PassBaton")
        challenger.completeOperation { doTasks("PaidLobbyAction", "PartyFollower<Scientists>") }
        challenger.selectTask("PassBaton")
        incumbent.doTask("Pass")
        challenger.doTask("Pass")
        players.forEach { it.autoExecNow(EAGER) }

        // Both players have three delegates. Returning followers must not change the winner.
        val winner = listOf("Kim", "Stan")[incumbentSeat]
        val loser = listOf("Stan", "Kim")[incumbentSeat]
        expectChanges(
            "Chairman<$winner>, 0 Chairman<$loser>, -6 PartyDelegate<Scientists, Anyone>"
        ) {
          kim.doTask("VenusStep! BY Admin")
          players.forEach { it.autoExecNow(EAGER) }
        }
        admin.count("Ruling<Scientists>") shouldBe 1
        incumbent.count("Delegate") shouldBe 2 // Chairman and the refilled lobby.
        challenger.count("Delegate") shouldBe 1 // Refilled lobby only.
      }
    }

    @Test
    internal fun `A neutral event placement can overtake a player leader`() {
      newTestGame("TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTasks("LobbyAction", "PartyLeader<Greens> FROM LobbyDelegate")
      }
      kim.selectTask("PassBaton")
      stan.completeOperation {
        doTasks("LobbyAction", "PartyLeader<Scientists> FROM LobbyDelegate")
      }
      stan.completeOperation {
        doTasks("PaidLobbyAction", "PartyFollower<Scientists>")
      }
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      // Democratic Reform becoming current ties Kim's delegate in Greens.
      admin.doTask("PartyFollower<Greens, Neutral>")
      admin.doTask("ExperimentalLifeforms")
      admin
          .doTask("PartyLeader<Greens, Neutral>")
          .expect(
              "PartyLeader<Greens, Neutral>, -PartyLeader<Greens, Kim>, PartyFollower<Greens, Kim>"
          )
    }

    @Test
    internal fun `Government returns followers makes the leader chairman and refills the lobby`() {
      newTestGame("TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTasks("LobbyAction", "PartyLeader<Scientists> FROM LobbyDelegate")
      }
      kim.selectTask("PassBaton")
      stan.doTask("Pass")
      kim.completeOperation {
        doTasks("PaidLobbyAction", "PartyFollower<Scientists>")
      }
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("Chairman") shouldBe 1
      kim.count("PartyDelegate<Scientists>") shouldBe 0
      kim.count("LobbyDelegate") shouldBe 1
      kim.count("Delegate") shouldBe 2
      admin.count("Ruling<Scientists>") shouldBe 1
      admin.count("Chairman<Neutral>") shouldBe 0
    }
  }
}
