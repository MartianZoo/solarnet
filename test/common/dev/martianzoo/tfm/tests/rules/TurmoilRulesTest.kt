package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilRulesTest : CardTest() {
  @Test
  internal fun `setup establishes delegate capacity and one free lobbying action per player`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()

    p1.count("TurmoilPlayer") shouldBe 1
    p2.count("TurmoilPlayer") shouldBe 1
    p1.count("LobbyActionAvailable") shouldBe 1
    p2.count("LobbyActionAvailable") shouldBe 1
    p1.count("Delegate") shouldBe 0
    p2.count("Delegate") shouldBe 0
    admin.count("Neutral") shouldBe 1
    admin.count("Delegate<Neutral>") shouldBe 3
    admin.count("Party") shouldBe 6
    admin.count("Chairman<Neutral>") shouldBe 1
    admin.count("Ruling<Greens>") shouldBe 1
    admin.count("Ruling") shouldBe 1
    admin.count("Coming") shouldBe 1
    admin.count("Distant") shouldBe 1
    admin.count("Current") shouldBe 0
  }

  @Test
  internal fun `a sole lobbied delegate becomes party leader and supplies no delegate influence`() {
    newGame(TurmoilExpansion)
    clearSetupPolitics()
    admin.phase("Action")

    p1.stdAction("LobbyAction", 1) {
      doTask("PartyDelegate<MarsFirst>")
    }

    p1.count("LobbyActionAvailable") shouldBe 0
    p1.count("Delegate") shouldBe 1
    p1.count("PartyDelegate<MarsFirst>") shouldBe 1
    p1.count("PartyLeader<MarsFirst>") shouldBe 1
    admin.count("Dominant<MarsFirst>") shouldBe 1
    admin.runOperation("MeasureInfluence<Player1>")
    p1.count("PartyLeaderInfluence") shouldBe 1
    p1.count("DelegateInfluence") shouldBe 0
    p1.count("Influence") shouldBe 1
  }

  @Test
  internal fun `paid lobbying preserves a tied incumbent and transfers a strict lead`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p1.runOperation("10 MC")
    p2.runOperation("15 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<MarsFirst>")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst>")
      }
    }
    p1.count("PartyDelegate<MarsFirst>") shouldBe 2
    admin.count("Dominant<MarsFirst>") shouldBe 1
    p2.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Scientists>")
      }
      p2.count("PartyDelegate<Scientists>") shouldBe 1
      admin.count("Dominant<Scientists>") shouldBe 0
      admin.count("Dominant<MarsFirst>") shouldBe 1
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists>")
      }
    }
    admin.count("Dominant<MarsFirst>") shouldBe 1
    p1.pass()
    p2.turn {
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists>")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists>")
      }
    }

    p1.count("MC") shouldBe 5
    p2.count("MC") shouldBe 0
    p1.count("Delegate") shouldBe 2
    p2.count("Delegate") shouldBe 4
    p1.count("PartyLeader<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<Scientists>") shouldBe 1
    admin.count("Dominant<Scientists>") shouldBe 1
  }

  @Test
  internal fun `party leadership needs a strict delegate lead`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p2.runOperation("5 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Scientists>")
      }
    }
    p2.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Scientists>")
      }
      p1.count("PartyLeader<Scientists>") shouldBe 1
      p2.count("PartyLeader<Scientists>") shouldBe 0
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists>")
      }
    }

    p1.count("PartyLeader<Scientists>") shouldBe 0
    p2.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun `removing a sole party leader promotes the remaining delegation`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p1.runOperation("PartyDelegate<Scientists>")
    p2.runOperation("PartyDelegate<Scientists>")

    admin.runOperation("-PartyDelegate<Scientists, Player1>")

    p1.count("PartyLeader<Scientists>") shouldBe 0
    p2.count("PartyDelegate<Scientists>") shouldBe 1
    p2.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun `free then paid lobbying cannot place more than seven delegates`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p1.runOperation("35 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<MarsFirst>")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst>")
      }
    }
    p2.pass()
    p1.turn {
      repeat(5) {
        stdAction("LobbyAction", 2) {
          doTask("PartyDelegate<MarsFirst>")
        }
      }
    }

    p1.count("PartyDelegate") shouldBe 7
    p1.count("LobbyActionAvailable") shouldBe 0
    p1.count("MC") shouldBe 5
    shouldThrow<DeadEndException> {
      p1.stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst>")
      }
    }
    p1.count("MC") shouldBe 5
    admin.runOperation("RefillLobby")
    p1.count("LobbyActionAvailable") shouldBe 0
  }

  @Test
  internal fun `card placement uses delegate capacity without consuming free lobbying`() {
    newGame(TurmoilExpansion)

    p1.runOperation("PartyDelegate<MarsFirst>")

    p1.count("Delegate") shouldBe 1
    p1.count("LobbyActionAvailable") shouldBe 1
    p1.count("PartyDelegate<MarsFirst>") shouldBe 1
  }

  @Test
  internal fun `lobbying cannot place an eighth delegate`() {
    newGame(TurmoilExpansion)
    repeat(7) { p1.runOperation("PartyDelegate<MarsFirst>") }
    p1.runOperation("5 MC")
    admin.phase("Action")

    shouldThrow<DeadEndException> {
      p1.stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists>")
      }
    }

    p1.count("MC") shouldBe 5
    p1.count("LobbyActionAvailable") shouldBe 0
    p1.count("PartyDelegate") shouldBe 7
  }

  @Test
  internal fun `delegate limit is independent for each player`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()

    repeat(7) { p1.runOperation("PartyDelegate<MarsFirst>") }
    repeat(7) { p2.runOperation("PartyDelegate<Scientists>") }

    p1.count("Delegate") shouldBe 7
    p2.count("Delegate") shouldBe 7
  }

  @Test
  internal fun `a returned delegate does not restore a Lobby emptied with the seventh delegate`() {
    newGame(TurmoilExpansion)
    repeat(7) { p1.runOperation("PartyDelegate<MarsFirst>") }
    admin.phase("Action")

    p1.count("LobbyActionAvailable") shouldBe 0
    p1.runOperation("-PartyDelegate<MarsFirst>")
    shouldThrow<NotNowException> {
      p1.stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Scientists>")
      }
    }

    p1.count("LobbyActionAvailable") shouldBe 0
    p1.count("Delegate") shouldBe 6
  }

  @Test
  internal fun `lobby refill restores the free action without changing placed delegate count`() {
    newGame(TurmoilExpansion)
    admin.phase("Action")
    p1.stdAction("LobbyAction", 1) {
      doTask("PartyDelegate<MarsFirst>")
    }

    admin.runOperation("RefillLobby")

    p1.count("LobbyActionAvailable") shouldBe 1
    p1.count("Delegate") shouldBe 1
    requireP2().count("LobbyActionAvailable") shouldBe 1
    requireP2().count("Delegate") shouldBe 0
  }

  @Test
  internal fun `influence snapshot counts chairman leader and delegate presence once each`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p1.runOperation("5 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<MarsFirst>")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst>")
      }
    }
    p2.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<MarsFirst>")
      }
    }
    admin.runOperation("Chairman<Player1> FROM Chairman<Neutral>")

    admin.runOperation("MeasureInfluence<Player1>")
    admin.runOperation("MeasureInfluence<Player2>")

    p1.count("ChairmanInfluence") shouldBe 1
    p1.count("PartyLeaderInfluence") shouldBe 1
    p1.count("DelegateInfluence") shouldBe 1
    p1.count("Influence") shouldBe 3
    p2.count("ChairmanInfluence") shouldBe 0
    p2.count("PartyLeaderInfluence") shouldBe 0
    p2.count("DelegateInfluence") shouldBe 1
    p2.count("Influence") shouldBe 1

    admin.runOperation("End FROM Phase")
    p1.count("VictoryPoint") shouldBe 22
    p2.count("VictoryPoint") shouldBe 20
  }

  private fun clearSetupPolitics() {
    listOf("MarsFirst", "Reds").forEach { party ->
      admin.runOperation("-PartyDelegate<$party, Neutral>")
    }
    admin.runOperation("-Dominant!")
  }
}
