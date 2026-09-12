package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilRulesTest : CardTest() {
  @Test
  internal fun `setup creates one lobby delegate per player and the initial government`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()

    p1.count("TurmoilPlayer") shouldBe 1
    p2.count("TurmoilPlayer") shouldBe 1
    p1.count("LobbyDelegate") shouldBe 1
    p2.count("LobbyDelegate") shouldBe 1
    p1.count("ReserveDelegate") shouldBe 6
    p2.count("ReserveDelegate") shouldBe 6
    admin.count("Neutral") shouldBe 1
    admin.count("ReserveDelegate<Neutral>") shouldBe 11
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
      doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
    }

    p1.count("LobbyDelegate") shouldBe 0
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
        doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst> FROM ReserveDelegate")
      }
    }
    p1.count("PartyDelegate<MarsFirst>") shouldBe 2
    admin.count("Dominant<MarsFirst>") shouldBe 1
    p2.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Scientists> FROM LobbyDelegate")
      }
      p2.count("PartyDelegate<Scientists>") shouldBe 1
      admin.count("Dominant<Scientists>") shouldBe 0
      admin.count("Dominant<MarsFirst>") shouldBe 1
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      }
    }
    admin.count("Dominant<MarsFirst>") shouldBe 1
    p1.pass()
    p2.turn {
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      }
    }

    p1.count("MC") shouldBe 5
    p2.count("MC") shouldBe 0
    p1.count("ReserveDelegate") shouldBe 5
    p2.count("ReserveDelegate") shouldBe 3
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
        doTask("PartyDelegate<Scientists> FROM LobbyDelegate")
      }
    }
    p2.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Scientists> FROM LobbyDelegate")
      }
      p1.count("PartyLeader<Scientists>") shouldBe 1
      p2.count("PartyLeader<Scientists>") shouldBe 0
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      }
    }

    p1.count("PartyLeader<Scientists>") shouldBe 0
    p2.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun `all seven delegates are finite and paid lobbying uses only the reserve`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p1.runOperation("35 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst> FROM ReserveDelegate")
      }
    }
    p2.pass()
    p1.turn {
      repeat(5) {
        stdAction("LobbyAction", 2) {
          doTask("PartyDelegate<MarsFirst> FROM ReserveDelegate")
        }
      }
    }

    p1.count("PartyDelegate") shouldBe 7
    p1.count("LobbyDelegate") shouldBe 0
    p1.count("ReserveDelegate") shouldBe 0
    p1.count("MC") shouldBe 5
    shouldThrow<NotNowException> {
      p1.stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst> FROM ReserveDelegate")
      }
    }
    p1.count("MC") shouldBe 5
    admin.runOperation("RefillLobby")
    p1.count("LobbyDelegate") shouldBe 0
  }

  @Test
  internal fun `lobby refill moves one reserve delegate only into a vacant lobby`() {
    newGame(TurmoilExpansion)
    admin.phase("Action")
    p1.stdAction("LobbyAction", 1) {
      doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
    }

    admin.runOperation("RefillLobby")

    p1.count("LobbyDelegate") shouldBe 1
    p1.count("ReserveDelegate") shouldBe 5
    requireP2().count("LobbyDelegate") shouldBe 1
    requireP2().count("ReserveDelegate") shouldBe 6
  }

  @Test
  internal fun `influence snapshot counts chairman leader and delegate presence once each`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p1.runOperation("5 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst> FROM ReserveDelegate")
      }
    }
    p2.turn {
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
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
      admin.runOperation("ReserveDelegate<Neutral> FROM PartyDelegate<$party, Neutral>")
      admin.runOperation("-PartyLeader<$party, Neutral>!")
    }
    admin.runOperation("-Dominant!")
  }
}
