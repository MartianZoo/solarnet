package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.FakeBannedDelegate
import dev.martianzoo.tfm.tests.cards.cardnames.Recruitment
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior in Terraforming Mars rules. */
internal class BugsTest : CardTest() {
  @Test
  internal fun `Fake Banned Delegate incorrectly leaves party leadership and dominance unchanged`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    val p2 = requireP2()
    admin.runOperation("ReserveDelegate<Neutral> FROM Chairman<Neutral>")
    p1.runOperation("Chairman FROM ReserveDelegate")
    p2.runOperation(
        "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<MarsFirst> FROM ReserveDelegate"
    )
    p1.runOperation(
        "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "4 PartyDelegate<Unity> FROM ReserveDelegate"
    )
    admin.runOperation("PartyDelegate<Unity, Neutral> FROM ReserveDelegate<Neutral>")
    p1.runOperation("ProjectCard")
    admin.phase("Action")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
      doTask("ReserveDelegate<Player2> FROM PartyDelegate<MarsFirst, Player2>")
    }

    admin.count("Dominant<MarsFirst>") shouldBe 1
    admin.count("Dominant<Unity>") shouldBe 0
    p1.count("PartyLeader<MarsFirst>") shouldBe 0
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
  }

  @Test
  internal fun `Fake Banned Delegate incorrectly preserves an incumbent below a dominance tie`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    val p2 = requireP2()
    admin.runOperation("ReserveDelegate<Neutral> FROM Chairman<Neutral>")
    p1.runOperation("Chairman FROM ReserveDelegate, ProjectCard")
    p2.runOperation(
        "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<MarsFirst> FROM ReserveDelegate"
    )
    admin.runOperation(
        "3 PartyDelegate<Kelvinists, Neutral> FROM ReserveDelegate<Neutral>, " +
            "2 PartyDelegate<Reds, Neutral> FROM ReserveDelegate<Neutral>"
    )
    admin.phase("Action")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
      doTask("ReserveDelegate<Player2> FROM PartyDelegate<MarsFirst, Player2>")
    }

    admin.count("Dominant<MarsFirst>") shouldBe 1
    admin.count("Dominant<Kelvinists>") shouldBe 0
    admin.count("Dominant<Reds>") shouldBe 0
  }

  @Test
  internal fun `Fake Banned Delegate incorrectly preserves the former leader after a tied challenge`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    val p2 = requireP2()
    admin.runOperation("ReserveDelegate<Neutral> FROM Chairman<Neutral>")
    p1.runOperation("Chairman FROM ReserveDelegate, ProjectCard")
    p2.runOperation(
        "PartyDelegate<Scientists> FROM ReserveDelegate, " +
            "PartyDelegate<Scientists> FROM ReserveDelegate"
    )
    p1.runOperation(
        "PartyDelegate<Scientists> FROM ReserveDelegate, " +
            "PartyDelegate<Scientists> FROM ReserveDelegate"
    )
    admin.runOperation(
        "PartyDelegate<Scientists, Neutral> FROM ReserveDelegate<Neutral>, " +
            "PartyDelegate<Scientists, Neutral> FROM ReserveDelegate<Neutral>"
    )
    admin.phase("Action")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, Scientists, Player2>")
      doTask("ReserveDelegate<Player2> FROM PartyDelegate<Scientists, Player2>")
    }

    p1.count("PartyLeader<Scientists>") shouldBe 0
    p2.count("PartyLeader<Scientists>") shouldBe 1
    admin.count("PartyLeader<Scientists, Neutral>") shouldBe 0
  }

  @Test
  internal fun `Recruitment incorrectly leaves two leaders after a tied challenge`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    admin.runOperation("PartyDelegate<MarsFirst, Neutral> FROM ReserveDelegate<Neutral>")
    p1.runOperation("PartyDelegate<MarsFirst> FROM ReserveDelegate, 2 MC, ProjectCard")
    p2.runOperation(
        "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<MarsFirst> FROM ReserveDelegate"
    )
    admin.phase("Action")

    p1.playProject(Recruitment, 2) {
      doTask("RecruitmentExchange<MarsFirst>")
      doTask("ReserveDelegate<Neutral> FROM PartyDelegate<MarsFirst, Neutral>")
    }

    p1.count("PartyLeader<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
    admin.count("PartyLeader<MarsFirst, Neutral>") shouldBe 0
  }

  @Test
  internal fun `paid lobbying incorrectly uses the Lobby delegate while free lobbying is available`() {
    newGame(TurmoilExpansion)
    repeat(6) { p1.runOperation("PartyDelegate<Unity> FROM ReserveDelegate") }
    p1.runOperation("5 MC")
    admin.phase("Action")

    p1.count("LobbyActionAvailable") shouldBe 1
    p1.count("ReserveDelegate") shouldBe 1

    p1.stdAction("LobbyAction", 2) {
      doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
    }

    p1.count("MC") shouldBe 0
    p1.count("LobbyActionAvailable") shouldBe 0
    p1.count("ReserveDelegate") shouldBe 0
    p1.count("PartyDelegate") shouldBe 7
  }
}
