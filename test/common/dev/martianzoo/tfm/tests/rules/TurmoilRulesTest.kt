package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.NotNowException
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
          doTask("PartyDelegate<Scientists>")
        }
        .expect("PartyLeader<Scientists>, -LobbyActionAvailable, 0 MC")
    stan
        .inTurn {
          doTask("LobbyAction")
          doTask("PartyDelegate<Unity>")
        }
        .expect("-LobbyActionAvailable, 0 MC")
    shouldThrow<NotNowException> {
      kim.inTurn {
        doTask("LobbyAction")
      }
    }
    kim.inTurn {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Scientists>")
    }
  }

  // Unresolved adjudication (question 20, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3775830
  @Test
  internal fun `Paid lobbying reserves the last delegate for the free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("5 PartyDelegate<Unity>")
    kim.inTurn {
          doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Scientists>")
        }
        .expect("-5 MC, Delegate, 0 LobbyActionAvailable")
    kim.setToExMachina(10, "MC")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("LobbyAction<Action2>")
        doTask("-5 MC")
        // Resolve the delegate offer so the reserve requirement is checked.
        selectTask("PartyDelegate")
      }
    }
    kim.count("MC") shouldBe 10
    kim.count("Delegate") shouldBe 6
    kim.inTurn {
          doTask("LobbyAction")
          doTask("PartyDelegate<Scientists>")
        }
        .expect("Delegate, -LobbyActionAvailable, 0 MC")
    kim.count("Delegate") shouldBe 7
  }

  @Test
  internal fun `Dominance preserves a tied incumbent until another party has more delegates`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.inTurn {
          doTask("LobbyAction")
          doTask("PartyDelegate<Scientists>")
        }
        .expect("0 Dominant<Scientists>")
    kim.inTurn {
          doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Scientists>")
        }
        .expect("Dominant<Scientists>, -Dominant<MarsFirst>")
    stan.inTurn {
      doTask("LobbyAction")
      doTask("PartyDelegate<Unity>")
    }
    stan
        .inTurn {
          doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Unity>")
        }
        .expect("0 Dominant<Unity>, 0 Dominant<Scientists>")
    stan
        .inTurn {
          doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Unity>")
        }
        .expect("Dominant<Unity>, -Dominant<Scientists>")
  }

  @Test
  internal fun `Party leadership preserves a tie and transfers for a strict lead`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.inTurn {
      doTask("LobbyAction")
      doTask("PartyDelegate<Scientists>")
    }
    stan
        .inTurn {
          doTask("LobbyAction")
          doTask("PartyDelegate<Scientists>")
        }
        .expect("0 PartyLeader<Scientists, Stan>, 0 PartyLeader<Scientists, Kim>")
    stan
        .inTurn {
          doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Scientists>")
        }
        .expect("PartyLeader<Scientists, Stan>, -PartyLeader<Scientists, Kim>")
  }

  private fun lobbyScientists() {
    kim.inTurn {
      doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Scientists>")
    }
  }

  @Test
  internal fun `Seven placed delegates prevent further lobbying without charging the player`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.inTurn {
      doTask("LobbyAction")
      doTask("PartyDelegate<Scientists>")
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
        doTask("LobbyAction<Action2>")
        doTask("-5 MC")
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
          doTask("PartyDelegate<Unity>")
        }
        .expect("Delegate, 0 MC")
  }

  @Test
  internal fun `A card places a delegate without using the free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")
    kim.inTurn {
          doTasks("UseCardAction<MartianMediaCenter, Action1>", "-3 MC")
          doTask("PartyDelegate<Scientists>")
        }
        .expect("-3 MC, Delegate, 0 LobbyActionAvailable")
    kim.inTurn {
          doTask("LobbyAction")
          doTask("PartyDelegate<Unity>")
        }
        .expect("Delegate, -LobbyActionAvailable, 0 MC")
  }

  @Test
  internal fun `Returning a delegate does not refill a lobby emptied by the seventh placement`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("7 PartyDelegate<Scientists>")
    stan.exMachina("Chairman<Stan FROM Neutral>")
    stan
        .inTurn {
          doTasks(
              "PlayProject<Class<BannedDelegate>>",
              "BannedDelegate FROM ProjectCard",
          )
          // Hand the chosen removal to Admin, which updates the delegate reserve.
          selectTask("BannedDelegateRemoval<Stan, Scientists, Kim>*")
          doTask("-PartyDelegate<Kim, Scientists>")
          doTask("PlayedEvent<Class<BannedDelegate>> FROM BannedDelegate")
        }
        .expect("-Delegate<Kim>, 0 LobbyActionAvailable<Kim>")
    shouldThrow<NotNowException> {
      kim.inTurn {
        doTask("LobbyAction")
      }
    }
    kim.inTurn {
          doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Unity>")
        }
        .expect("-5 MC, Delegate")
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `A new government cannot refill a lobby while all seven delegates remain placed`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("PartyDelegate<Unity>")
      }
      kim.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Unity>")
      }
      stan.completeOperation {
        doTask("LobbyAction")
        doTask("PartyDelegate<Scientists>")
      }
      stan.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Scientists>")
      }
      kim.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Unity>")
      }
      kim.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Greens>")
      }
      stan.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Scientists>")
      }
      stan.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Scientists>")
      }
      kim.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Greens>")
      }
      kim.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Kelvinists>")
      }
      stan.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Scientists>")
      }
      stan.doTask("0 StandardAction") // Decline the second action.
      kim.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Kelvinists>")
      }
      kim.doTask("0 StandardAction") // Decline the second action.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("ExploreFirstDirective")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }

      admin.count("Ruling<Scientists>") shouldBe 1
      kim.count("Delegate") shouldBe 7
      kim.count("LobbyActionAvailable") shouldBe 0
      stan
          .completeOperation {
            doTask("LobbyAction")
            doTask("PartyDelegate<Scientists>")
          }
          .expect("Delegate, 0 MC")

      stan.doTask("0 StandardAction") // Decline the second action.
      shouldThrow<NotNowException> { kim.doTask("LobbyAction") }
    }

    @Test
    internal fun `A party leader's sole delegate does not also count as an ordinary delegate`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("PartyDelegate<Scientists>")
      }
      kim.doTask("0 StandardAction") // Decline the second action.
      stan.completeOperation {
        doTask("LobbyAction")
        doTask("PartyDelegate<Scientists>")
      }
      stan.doTask("0 StandardAction") // Decline the second action.
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("Influence") shouldBe 1
      stan.count("Influence") shouldBe 1
    }

    @Test
    internal fun `Influence counts chairman leader and ordinary delegates then expires at Research`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("PartyDelegate<Scientists>")
      }
      kim.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Scientists>")
      }
      stan.completeOperation {
        doTask("LobbyAction")
        doTask("PartyDelegate<Scientists>")
      }
      stan.doTask("0 StandardAction") // Decline the second action.
      kim.completeOperation {
        doTasks("PlayProject<Class<$VoteOfNoConfidence>>", "-5 MC")
        autoExecNow(EAGER)
      }
      kim.doTask("0 StandardAction") // Decline the second action.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      kim.count("Influence") shouldBe 3
      stan.count("Influence") shouldBe 1
      admin.doTask("ExploreFirstDirective")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
      kim.count("Influence") shouldBe 0
      stan.count("Influence") shouldBe 0
    }

    @Test
    internal fun `A new government refills the lobby without returning other parties' delegates`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.completeOperation {
        doTask("LobbyAction")
        doTask("PartyDelegate<Unity>")
      }
      kim.doTask("0 StandardAction") // Decline the second action.
      stan.completeOperation {
        doTask("LobbyAction")
        doTask("PartyDelegate<Scientists>")
      }
      stan.completeOperation {
        doTasks("LobbyAction<Action2>", "-5 MC")
        doTask("PartyDelegate<Scientists>")
      }
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("ExploreFirstDirective")
      players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
      stan.doTask("Pass")

      kim.count("PartyDelegate<Unity>") shouldBe 1
      kim.completeOperation {
            doTask("LobbyAction")
            doTask("PartyDelegate<Unity>")
          }
          .expect("PartyDelegate<Unity>, 0 MC")
    }
  }
}
