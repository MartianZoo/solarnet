package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.FakeBannedDelegate
import dev.martianzoo.tfm.tests.cards.cardnames.Recruitment
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior in Terraforming Mars rules. */
internal class BugsTest : CardTest() {
  @Test
  internal fun `Fake Banned Delegate incorrectly leaves party leadership and dominance unchanged`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    val p2 = requireP2()
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman")
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")
    p1.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>, 4 PartyDelegate<Unity>")
    admin.runOperation("PartyDelegate<Unity, Neutral>")
    p1.runOperation("ProjectCard")
    admin.phase("Action")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
      doTask("-PartyDelegate<MarsFirst, Player2>")
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
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, ProjectCard")
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")
    admin.runOperation("3 PartyDelegate<Kelvinists, Neutral>, 2 PartyDelegate<Reds, Neutral>")
    admin.phase("Action")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
      doTask("-PartyDelegate<MarsFirst, Player2>")
    }

    admin.count("Dominant<MarsFirst>") shouldBe 1
    admin.count("Dominant<Kelvinists>") shouldBe 0
    admin.count("Dominant<Reds>") shouldBe 0
  }

  @Test
  internal fun `Fake Banned Delegate incorrectly preserves the former leader after a tied challenge`() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    val p2 = requireP2()
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, ProjectCard")
    p2.runOperation("PartyDelegate<Scientists>, PartyDelegate<Scientists>")
    p1.runOperation("PartyDelegate<Scientists>, PartyDelegate<Scientists>")
    admin.runOperation("PartyDelegate<Scientists, Neutral>, PartyDelegate<Scientists, Neutral>")
    admin.phase("Action")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, Scientists, Player2>")
      doTask("-PartyDelegate<Scientists, Player2>")
    }

    p1.count("PartyLeader<Scientists>") shouldBe 0
    p2.count("PartyLeader<Scientists>") shouldBe 1
    admin.count("PartyLeader<Scientists, Neutral>") shouldBe 0
  }

  @Test
  internal fun `Recruitment incorrectly leaves two leaders after a tied challenge`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    admin.runOperation("PartyDelegate<MarsFirst, Neutral>")
    p1.runOperation("PartyDelegate<MarsFirst>, 2 MC, ProjectCard")
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")
    admin.phase("Action")

    p1.playProject(Recruitment, 2) {
      doTask("RecruitmentExchange<MarsFirst>")
    }

    p1.count("PartyLeader<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
    admin.count("PartyLeader<MarsFirst, Neutral>") shouldBe 0
  }

  @Test
  internal fun `paid lobbying incorrectly uses the Lobby delegate while free lobbying is available`() {
    newGame(TurmoilExpansion)
    repeat(6) { p1.runOperation("PartyDelegate<Unity>") }
    p1.runOperation("5 MC")
    admin.phase("Action")

    p1.count("LobbyActionAvailable") shouldBe 1
    p1.count("PartyDelegate OR Chairman") shouldBe 6

    p1.stdAction("LobbyAction", 2) {
      doTask("PartyDelegate<Scientists>")
    }

    p1.count("MC") shouldBe 0
    p1.count("LobbyActionAvailable") shouldBe 0
    p1.count("PartyDelegate OR Chairman") shouldBe 7
    p1.count("PartyDelegate") shouldBe 7
  }

  @Test
  internal fun `SecondPlace incorrectly remains active with only two players`() {
    val twoPlayers = Engine.newGame(Canon.gamePremise(GameConfig("", "Player1", "Player2")))
    val threePlayers =
        Engine.newGame(Canon.gamePremise(GameConfig("", "Player1", "Player2", "Player3")))

    twoPlayers.classTable.allClassNames.shouldContain(cn("SecondPlace"))
    threePlayers.classTable.allClassNames.shouldContain(cn("SecondPlace"))
  }
}
