package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PoliticianTest : TfmSandboxTest() {
  @Test
  internal fun `two player games award no second place points even to the funder`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner", playerCount = 2)
    kim.exMachina("PartyLeader<Scientists>, 1 PartyFollower<Scientists>")
    stan.exMachina("PartyLeader<Unity>")

    // Kim scores 3, Stan 1. Both also receive their ordinary party-leader VP.
    stan.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC")
      doTask("Politician")
    }
    victoryPoints() shouldBe listOf(26, 21)
  }

  @Test
  internal fun `Event Analysts breaks a tie even outside the dominant party`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("PartyLeader<Scientists>, 1 PartyFollower<Scientists>")
    stan.exMachina(
        "PartyLeader<Unity>, PartyLeader<Kelvinists>, PartyLeader<Greens>, EventAnalysts"
    )

    // Kim scores 3 (leader + dominant leader + delegate); Stan scores 4 (three leaders + Analysts).
    // Before funding, only their ordinary party-leader VP count.
    victoryPoints() shouldBe listOf(21, 23, 20)
    kim.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC")
      doTask("Politician")
    }
    victoryPoints() shouldBe listOf(23, 28, 20)
  }

  @Test
  internal fun `chairman contributes influence but is not another party leader`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    kim.exMachina("PartyLeader<Scientists, Neutral>, 2 PartyFollower<Scientists, Neutral>")
    stan.exMachina("PartyLeader<Unity>")

    // Kim's chairman influence ties Stan's party leadership at 1.
    stan.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC")
      doTask("Politician")
    }
    victoryPoints() shouldBe listOf(26, 26, 20)
  }

  @Test
  internal fun `several dominant party delegates give a single influence and second place can tie`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("PartyLeader<Scientists>, 3 PartyFollower<Scientists>")
    stan.exMachina("3 PartyFollower<Scientists>")
    rob.exMachina("PartyLeader<Unity>")

    // Kim scores 3; Stan's three delegates score 1, tying Rob's single party leadership.
    rob.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC")
      doTask("Politician")
    }
    victoryPoints() shouldBe listOf(26, 22, 23)
  }

  @Test
  internal fun `delegates under a neutral dominant leader can tie for first without a second prize`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("PartyLeader<Scientists, Neutral>, 2 PartyFollower<Scientists, Neutral>")
    kim.exMachina("PartyFollower<Scientists>")
    stan.exMachina("PartyLeader<Unity>")

    // Kim's delegate influence and Stan's leadership each score 1. Rob scores 0.
    rob.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC")
      doTask("Politician")
    }
    victoryPoints() shouldBe listOf(25, 26, 20)
  }

  @Test
  internal fun `delegates outside the dominant party add no influence`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("PartyLeader<Scientists, Neutral>, 5 PartyFollower<Scientists, Neutral>")
    kim.exMachina("PartyLeader<Unity>, 2 PartyFollower<Unity>")
    stan.exMachina("2 PartyFollower<Unity>")
    rob.exMachina("Chairman<Rob FROM Neutral>")

    // Kim's leadership ties Rob's chairman influence at 1; Stan's delegates score 0.
    stan.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC")
      doTask("Politician")
    }
    victoryPoints() shouldBe listOf(26, 20, 26)
  }

  @Test
  internal fun `lobbying after funding changes the winner at final scoring`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("PartyLeader<Scientists>, 1 PartyFollower<Scientists>")
    stan.exMachina("PartyLeader<Unity>, PartyFollower<Unity>")
    kim.inTurn {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC")
      doTask("Politician")
    }

    // Unity overtakes Scientists: Kim drops from 3 to 1, Stan rises from 1 to 3.
    stan
        .inTurn {
          doTask("UseAction<LobbyAction, Action1>")
          doTask("PartyFollower<Unity> FROM LobbyDelegate")
        }
        .expect("Dominant<Unity>, -Dominant<Scientists>")
    victoryPoints() shouldBe listOf(23, 26, 20)
  }
}
