package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.MartianMediaCenter
import dev.martianzoo.tfm.tests.cards.cardnames.VoteOfNoConfidence
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilRulesTest : TfmSandboxTest() {
  @Test
  internal fun `Each player has a free lobby action followed by paid lobbying`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.inTurn {
          doTask("LobbyAction")
          doTask("PartyLeader<Scientists> FROM LobbyDelegate")
        }
        .expect("PartyLeader<Scientists>, -LobbyDelegate, 0 MC")
    stan
        .inTurn {
          doTask("LobbyAction")
          doTask("PartyLeader<Unity> FROM LobbyDelegate")
        }
        .expect("-LobbyDelegate, 0 MC")
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks("LobbyAction", "PartyFollower<Scientists> FROM LobbyDelegate")
      }
    }
    kim.inTurn {
      doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyFollower<Scientists>")
    }
  }

  // Unresolved adjudication (question 20, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3775830
  @Test
  internal fun `Paid lobbying reserves the last delegate for the free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyLeader<Unity>, 4 PartyFollower<Unity>")
    kim.inTurn {
          doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyLeader<Scientists>")
        }
        .expect("-5 MC, Delegate, 0 LobbyDelegate")
    kim.setToExMachina(10, "MC")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PaidLobbyAction")
        doTask("5 Pay<> FROM MC")
        // Resolve the delegate offer so the reserve requirement is checked.
        selectTask("PartyDelegate")
      }
    }
    kim.count("MC") shouldBe 10
    kim.count("Delegate") shouldBe 7
    kim.inTurn {
          doTask("LobbyAction")
          doTask("PartyFollower<Scientists> FROM LobbyDelegate")
        }
        .expect("0 Delegate, -LobbyDelegate, 0 MC")
    kim.count("Delegate") shouldBe 7
  }

  @Test
  internal fun `Dominance preserves a tied incumbent until another party has more delegates`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.inTurn {
          doTask("LobbyAction")
          doTask("PartyLeader<Scientists> FROM LobbyDelegate")
        }
        .expect("0 Dominant<Scientists>")
    kim.inTurn {
          doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyFollower<Scientists>")
        }
        .expect("Dominant<Scientists>, -Dominant<MarsFirst>")
    stan.inTurn {
      doTask("LobbyAction")
      doTask("PartyLeader<Unity> FROM LobbyDelegate")
    }
    stan
        .inTurn {
          doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyFollower<Unity>")
        }
        .expect("0 Dominant<Unity>, 0 Dominant<Scientists>")
    stan
        .inTurn {
          doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyFollower<Unity>")
        }
        .expect("Dominant<Unity>, -Dominant<Scientists>")
  }

  @Test
  internal fun `Party leadership preserves a tie and transfers for a strict lead`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.inTurn {
      doTask("LobbyAction")
      doTask("PartyLeader<Scientists> FROM LobbyDelegate")
    }
    stan
        .inTurn {
          doTask("LobbyAction")
          doTask("PartyFollower<Scientists> FROM LobbyDelegate")
        }
        .expect("0 PartyLeader<Scientists, Stan>, 0 PartyLeader<Scientists, Kim>")
    stan
        .inTurn {
          doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyLeader<Scientists>")
        }
        .expect("PartyLeader<Scientists, Stan>, -PartyLeader<Scientists, Kim>")
  }

  private fun lobbyScientists() {
    kim.inTurn {
      doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyFollower<Scientists>")
    }
  }

  @Test
  internal fun `Seven placed delegates prevent further lobbying without charging the player`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.inTurn {
      doTask("LobbyAction")
      doTask("PartyLeader<Scientists> FROM LobbyDelegate")
    }
    lobbyScientists()
    lobbyScientists()
    lobbyScientists()
    lobbyScientists()
    lobbyScientists()
    lobbyScientists()
    kim.count("Delegate") shouldBe 7
    kim.setToExMachina(10, "MC")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("PaidLobbyAction")
        doTask("5 Pay<> FROM MC")
        // Resolve the delegate offer so the reserve requirement is checked.
        selectTask("PartyDelegate")
      }
    }
    kim.count("MC") shouldBe 10
    kim.count("Delegate") shouldBe 7

    // Kim's full reserve does not consume Stan's delegates.
    stan
        .inTurn {
          doTask("LobbyAction")
          doTask("PartyLeader<Unity> FROM LobbyDelegate")
        }
        .expect("0 Delegate, 0 MC")
  }

  @Test
  internal fun `A card places a delegate without using the free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")
    kim.inTurn {
          doTasks("UseCardAction<MartianMediaCenter, Action1>", "3 Pay<> FROM MC")
          doTask("PartyLeader<Scientists>")
        }
        .expect("-3 MC, Delegate, 0 LobbyDelegate")
    kim.inTurn {
          doTask("LobbyAction")
          doTask("PartyLeader<Unity> FROM LobbyDelegate")
        }
        .expect("0 Delegate, -LobbyDelegate, 0 MC")
  }

  @Test
  internal fun `Returning a delegate does not refill a lobby emptied by the seventh placement`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyLeader<Scientists>, 6 PartyFollower<Scientists>")
    stan.exMachina("Chairman<Stan FROM Neutral>")
    stan
        .inTurn {
          doTasks(
              "PlayProject<Class<BannedDelegate>>",
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Scientists, Kim>")
        }
        .expect("-Delegate<Kim>, 0 LobbyDelegate<Kim>")
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks("LobbyAction", "PartyFollower<Scientists> FROM LobbyDelegate")
      }
    }
    kim.inTurn {
          doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyLeader<Unity>")
        }
        .expect("-5 MC, Delegate")
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `A new government cannot refill a lobby while all seven delegates remain placed`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("PartyLeader<Unity> FROM LobbyDelegate")
      }
      kim.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Unity>")
      }
      stan.completeOperation {
        doTask("LobbyAction")
        doTask("PartyLeader<Scientists> FROM LobbyDelegate")
      }
      stan.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Scientists>")
      }
      kim.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Unity>")
      }
      kim.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyLeader<Greens>")
      }
      stan.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Scientists>")
      }
      stan.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Scientists>")
      }
      kim.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Greens>")
      }
      kim.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyLeader<Kelvinists>")
      }
      stan.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Scientists>")
      }
      stan.selectTask("PassBaton") // Decline the second action.
      kim.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Kelvinists>")
      }
      kim.selectTask("PassBaton") // Decline the second action.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
      admin.doTask("PartyFollower<Greens, Neutral>")

      admin.doTasks("ExploreFirstDirective", "PartyFollower<Reds, Neutral>")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      admin.count("Ruling<Scientists>") shouldBe 1
      kim.count("Delegate") shouldBe 7
      kim.count("LobbyDelegate") shouldBe 0
      stan
          .completeOperation {
            doTask("LobbyAction")
            doTask("PartyLeader<Scientists> FROM LobbyDelegate")
          }
          .expect("0 Delegate, 0 MC")

      stan.selectTask("PassBaton") // Decline the second action.
      shouldThrow<LimitsException> {
        kim.completeOperation { doTasks("LobbyAction", "PartyFollower<Unity> FROM LobbyDelegate") }
      }
    }

    @Test
    internal fun `A party leader's sole delegate does not also count as an ordinary delegate`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("PartyLeader<Scientists> FROM LobbyDelegate")
      }
      kim.selectTask("PassBaton") // Decline the second action.
      stan.completeOperation {
        doTask("LobbyAction")
        doTask("PartyFollower<Scientists> FROM LobbyDelegate")
      }
      stan.selectTask("PassBaton") // Decline the second action.
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
      admin.doTask("PartyLeader<Greens, Neutral>")

      kim.count("Influence") shouldBe 1
      stan.count("Influence") shouldBe 1
    }

    @Test
    internal fun `Influence counts chairman leader and ordinary delegates then expires at Research`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("PartyLeader<Scientists> FROM LobbyDelegate")
      }
      kim.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Scientists>")
      }
      stan.completeOperation {
        doTask("LobbyAction")
        doTask("PartyFollower<Scientists> FROM LobbyDelegate")
      }
      stan.selectTask("PassBaton") // Decline the second action.
      kim.completeOperation {
        doTasks("PlayProject<Class<$VoteOfNoConfidence>>")
        autoExecNow(EAGER)
      }
      kim.selectTask("PassBaton") // Decline the second action.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
      admin.doTask("PartyLeader<Greens, Neutral>")

      kim.count("Influence") shouldBe 3
      stan.count("Influence") shouldBe 1
      admin.doTasks("ExploreFirstDirective", "PartyFollower<Reds, Neutral>")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
      kim.count("Influence") shouldBe 0
      stan.count("Influence") shouldBe 0
    }

    @Test
    internal fun `A new government refills the lobby without returning other parties' delegates`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("PartyLeader<Unity> FROM LobbyDelegate")
      }
      kim.selectTask("PassBaton") // Decline the second action.
      stan.completeOperation {
        doTask("LobbyAction")
        doTask("PartyLeader<Scientists> FROM LobbyDelegate")
      }
      stan.completeOperation {
        doTasks("PaidLobbyAction")
        doTask("PartyFollower<Scientists>")
      }
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
      admin.doTask("PartyLeader<Greens, Neutral>")

      admin.doTasks("ExploreFirstDirective", "PartyFollower<Reds, Neutral>")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
      stan.doTask("Pass")

      kim.count("PartyDelegate<Unity>") shouldBe 1
      kim.completeOperation {
            doTask("LobbyAction")
            doTask("PartyFollower<Unity> FROM LobbyDelegate")
          }
          .expect("PartyDelegate<Unity>, 0 MC")
    }
  }
}
