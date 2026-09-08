package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
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
    admin.count("Neutral") shouldBe 1
    admin.count("Party") shouldBe 6
    admin.count("Chairman<Neutral>") shouldBe 1
    admin.count("Ruling<Greens>") shouldBe 1
    admin.count("Ruling") shouldBe 1
  }

  @Test
  internal fun `lobbying moves the lobby delegate for free and appoints the first leader`() {
    newGame(TurmoilExpansion)
    admin.phase("Action")

    p1.stdAction("SendDelegateSA", 1) {
      doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
    }

    p1.count("LobbyDelegate") shouldBe 0
    p1.count("PartyDelegate<MarsFirst>") shouldBe 1
    p1.count("PartyLeader<MarsFirst>") shouldBe 1
    admin.count("Dominant<MarsFirst>") shouldBe 1
  }

  @Test
  internal fun `paid lobbying preserves a tied incumbent and transfers a strict lead`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p1.manual("10 MC")
    p2.manual("10 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
      }
      stdAction("SendDelegateSA", 2) { doTask("PartyDelegate<MarsFirst>") }
    }
    p1.count("PartyDelegate<MarsFirst>") shouldBe 2
    admin.count("Dominant<MarsFirst>") shouldBe 1
    p2.turn {
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Scientists> FROM LobbyDelegate")
      }
      p2.count("PartyDelegate<Scientists>") shouldBe 1
      admin.count("Dominant<Scientists>") shouldBe 0
      admin.count("Dominant<MarsFirst>") shouldBe 1
      stdAction("SendDelegateSA", 2) { doTask("PartyDelegate<Scientists>") }
    }
    admin.count("Dominant<MarsFirst>") shouldBe 1
    p1.pass()
    p2.turn { stdAction("SendDelegateSA", 2) { doTask("PartyDelegate<Scientists>") } }

    p1.count("MC") shouldBe 5
    p2.count("MC") shouldBe 0
    p1.count("PartyLeader<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<Scientists>") shouldBe 1
    admin.count("Dominant<Scientists>") shouldBe 1
  }

  @Test
  internal fun `party leadership needs a strict delegate lead`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p2.manual("5 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
      }
    }
    p2.turn {
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
      }
      p1.count("PartyLeader<MarsFirst>") shouldBe 1
      p2.count("PartyLeader<MarsFirst>") shouldBe 0
      stdAction("SendDelegateSA", 2) { doTask("PartyDelegate<MarsFirst>") }
    }

    p1.count("PartyLeader<MarsFirst>") shouldBe 0
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
  }
}
