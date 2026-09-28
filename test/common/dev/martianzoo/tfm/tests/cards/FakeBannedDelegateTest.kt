package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeBannedDelegate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FakeBannedDelegateTest : CardTest() {
  @Test
  internal fun `removes a selected non-leader delegate`() {
    arrangeChairman()
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
    }

    p2.count("PartyDelegate<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
  }

  @Test
  internal fun `can remove an own non-leader`() {
    removeNonLeader("Player1")
  }

  @Test
  internal fun `can remove a neutral non-leader`() {
    removeNonLeader("Neutral")
  }

  @Test
  internal fun `can play without removing a delegate when only leaders remain`() {
    arrangeChairman()
    admin.count("PartyDelegate<Anyone>") shouldBe admin.count("PartyLeader<Anyone>")

    p1.playProject(FakeBannedDelegate, 0).expect("0 PartyDelegate<Anyone>")

    p1.count("PlayedEvent<Class<FakeBannedDelegate>>") shouldBe 1
  }

  @Test
  internal fun `must remove a delegate when a non-leader exists`() {
    arrangeChairman()
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")

    p1.playProject(FakeBannedDelegate, 0) {
      shouldThrow<NarrowingException> { declineTask() }
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
    }

    p2.count("PartyDelegate<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
  }

  @Test
  internal fun `an unrelated non-leader does not permit selecting a party leader`() {
    arrangeChairman()
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>, PartyDelegate<Scientists>")
    p2.count("PartyLeader<Scientists>") shouldBe 1

    p1.playProject(FakeBannedDelegate, 0) {
      shouldThrow<DeadEndException> {
        doTask("FakeBannedDelegateRemoval<Player1, Scientists, Player2>")
      }
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
    }

    p2.count("PartyDelegate<MarsFirst>") shouldBe 1
    p2.count("PartyDelegate<Scientists>") shouldBe 1
    p2.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun `another owner's non-leader does not permit selecting the sole leader`() {
    arrangeChairman()
    val p2 = requireP2()
    p1.runOperation("PartyDelegate<Scientists>")
    p2.runOperation("PartyDelegate<Scientists>")
    p1.count("PartyLeader<Scientists>") shouldBe 1

    p1.playProject(FakeBannedDelegate, 0) {
      shouldThrow<DeadEndException> {
        doTask("FakeBannedDelegateRemoval<Player1, Scientists, Player1>")
      }
      doTask("FakeBannedDelegateRemoval<Player1, Scientists, Player2>")
    }

    p1.count("PartyDelegate<Scientists>") shouldBe 1
    p1.count("PartyLeader<Scientists>") shouldBe 1
    p2.count("PartyDelegate<Scientists>") shouldBe 0
  }

  @Test
  internal fun `removal affects only the selected party`() {
    arrangeChairman()
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>, PartyDelegate<Scientists>")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, MarsFirst, Player2>")
    }

    p2.count("PartyDelegate<MarsFirst>") shouldBe 1
    p2.count("PartyDelegate<Scientists>") shouldBe 1
  }

  private fun removeNonLeader(owner: String) {
    arrangeChairman()
    admin.runOperation("PartyDelegate<Scientists, $owner>, PartyDelegate<Scientists, $owner>")

    p1.playProject(FakeBannedDelegate, 0) {
      doTask("FakeBannedDelegateRemoval<Player1, Scientists, $owner>")
    }

    admin.count("PartyDelegate<Scientists, $owner>") shouldBe 1
    admin.count("PartyLeader<Scientists, $owner>") shouldBe 1
  }

  private fun arrangeChairman() {
    newGame(TurmoilExpansion, FakeStuffBundle)
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, ProjectCard")
    admin.phase("Action")
  }
}
