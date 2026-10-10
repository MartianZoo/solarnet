package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.BannedDelegate
import dev.martianzoo.tfm.tests.cards.cardnames.MartianMediaCenter
import dev.martianzoo.tfm.tests.cards.cardnames.VoteOfNoConfidence
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilRulesTest : TfmSandboxTest() {
  @Test
  internal fun `Each player has a free lobby action followed by paid lobbying`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
        .expect("PartyDelegate<Scientists>, PartyLeader<Scientists>, -LobbyActionAvailable, 0 MC")
    stan
        .stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }
        .expect("PartyDelegate<Unity>, -LobbyActionAvailable, 0 MC")
    shouldThrow<NotNowException> { kim.stdAction("LobbyAction") }
    kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
        .expect("-5 MC, PartyDelegate<Scientists>")
  }

  @Test
  internal fun `Paid lobbying reserves the last delegate for the free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("5 PartyDelegate<Unity>")
    kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
        .expect("-5 MC, Delegate, 0 LobbyActionAvailable")
    val money = kim.count("MC")
    shouldThrow<RequirementException> {
      kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
    }
    kim.count("MC") shouldBe money
    kim.count("Delegate") shouldBe 6
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
        .expect("Delegate, -LobbyActionAvailable, 0 MC")
    kim.count("Delegate") shouldBe 7
  }

  @Test
  internal fun `Dominance preserves a tied incumbent until another party has more delegates`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
        .expect("0 Dominant<Scientists>")
    kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
        .expect("Dominant<Scientists>, -Dominant<MarsFirst>")
    stan.stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }
    stan
        .stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }
        .expect("0 Dominant<Unity>, 0 Dominant<Scientists>")
    stan
        .stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }
        .expect("Dominant<Unity>, -Dominant<Scientists>")
  }

  @Test
  internal fun `Party leadership preserves a tie and transfers for a strict lead`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
    stan
        .stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
        .expect("0 PartyLeader<Scientists, Stan>, 0 PartyLeader<Scientists, Kim>")
    stan
        .stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
        .expect("PartyLeader<Scientists, Stan>, -PartyLeader<Scientists, Kim>")
  }

  @Test
  internal fun `Seven placed delegates prevent further lobbying without charging the player`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
    repeat(6) { kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") } }
    kim.count("Delegate") shouldBe 7
    val money = kim.count("MC")
    shouldThrow<RequirementException> { kim.stdAction("LobbyAction", 2) }
    kim.count("MC") shouldBe money
    kim.count("Delegate") shouldBe 7

    // Kim's full reserve does not consume Stan's delegates.
    stan.stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }.expect("Delegate, 0 MC")
  }

  @Test
  internal fun `A card places a delegate without using the free lobby action`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")
    kim.cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Scientists>") }
        .expect("-3 MC, Delegate, 0 LobbyActionAvailable")
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }
        .expect("Delegate, -LobbyActionAvailable, 0 MC")
  }

  @Test
  internal fun `Returning a delegate does not refill a lobby emptied by the seventh placement`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("7 PartyDelegate<Scientists>")
    stan.exMachina("Chairman FROM Chairman<Neutral>")
    stan
        .playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Stan, Scientists, Kim>")
        }
        .expect("-Delegate<Kim>, 0 LobbyActionAvailable<Kim>")
    shouldThrow<NotNowException> { kim.stdAction("LobbyAction") }
    kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }.expect("-5 MC, Delegate")
  }

  internal class Gameplay : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `A new government cannot refill a lobby while all seven delegates remain placed`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.turn {
        stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }
        stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }
      }
      stan.turn {
        stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
        stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
      }
      kim.turn {
        stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }
        stdAction("LobbyAction", 2) { doTask("PartyDelegate<Greens>") }
      }
      stan.turn {
        repeat(2) { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") } }
      }
      kim.turn {
        stdAction("LobbyAction", 2) { doTask("PartyDelegate<Greens>") }
        stdAction("LobbyAction", 2) { doTask("PartyDelegate<Kelvinists>") }
      }
      stan.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") } }
      kim.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Kelvinists>") } }
      stan.pass()
      kim.pass()
      kim.wgt("VenusStep")
      admin.doTask("ExploreFirstDirective")
      players.forEach { it.buyCards(0) }

      admin.count("Ruling<Scientists>") shouldBe 1
      kim.count("Delegate") shouldBe 7
      kim.count("LobbyActionAvailable") shouldBe 0
      stan.turn {
        stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }.expect("Delegate, 0 MC")
      }
      shouldThrow<NotNowException> { kim.stdAction("LobbyAction") }
    }

    @Test
    internal fun `A party leader's sole delegate does not also count as an ordinary delegate`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.turn { stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") } }
      stan.turn { stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") } }
      kim.pass()
      stan.pass()
      kim.wgt("VenusStep")
      kim.count("Influence") shouldBe 1
      stan.count("Influence") shouldBe 1
    }

    @Test
    internal fun `Influence counts chairman leader and ordinary delegates then expires at Research`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.turn {
        stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
        stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
      }
      stan.turn { stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") } }
      kim.turn { playProject(VoteOfNoConfidence, 5) }
      stan.pass()
      kim.pass()
      kim.wgt("VenusStep")

      kim.count("Influence") shouldBe 3
      stan.count("Influence") shouldBe 1
      admin.doTask("ExploreFirstDirective")
      players.forEach { it.buyCards(0) }
      kim.count("Influence") shouldBe 0
      stan.count("Influence") shouldBe 0
    }

    @Test
    internal fun `A new government refills the lobby without returning other parties' delegates`() {
      newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
      kim.turn { stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") } }
      stan.turn {
        stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
        stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
      }
      kim.pass()
      stan.pass()
      kim.wgt("VenusStep")
      admin.doTask("ExploreFirstDirective")
      players.forEach { it.buyCards(0) }
      stan.pass()

      kim.count("PartyDelegate<Unity>") shouldBe 1
      kim.stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }
          .expect("PartyDelegate<Unity>, 0 MC")
    }
  }
}
