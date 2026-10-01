package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.BannedDelegate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class BannedDelegateTest : CardTest() {
  @Test
  internal fun `requires the player to be Chairman`() {
    newGame(TurmoilExpansion)
    p1.runOperation("ProjectCard")
    admin.phase("Action")

    shouldThrow<NotNowException> { p1.playProject(BannedDelegate, 0) }
  }

  @Test
  internal fun `Banned Delegate updates party leadership and dominance after removal`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman")
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")
    p1.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>, 4 PartyDelegate<Unity>")
    admin.runOperation("PartyDelegate<Unity, Neutral>")
    p1.runOperation("ProjectCard")
    admin.phase("Action")

    p1.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player1, MarsFirst, Player2>")
        }
        .expect(
            "-Dominant<MarsFirst>, Dominant<Unity>, " +
                "PartyLeader<MarsFirst, Player1>, -PartyLeader<MarsFirst, Player2>"
        )
  }

  @Test
  internal fun `Banned Delegate replaces an incumbent below a dominance tie`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, ProjectCard")
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")
    admin.runOperation("3 PartyDelegate<Kelvinists, Neutral>, 2 PartyDelegate<Reds, Neutral>")
    admin.phase("Action")

    p1.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player1, MarsFirst, Player2>")
        }
        .expect("-Dominant<MarsFirst>, Dominant<Kelvinists>, 0 Dominant<Reds>")
  }

  @Test
  internal fun `Banned Delegate breaks a dominance tie clockwise from the removed party`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, ProjectCard")
    p2.runOperation("2 PartyDelegate<Greens>")
    p1.runOperation("PartyDelegate<Greens>")
    admin.runOperation("2 PartyDelegate<Reds, Neutral>, 3 PartyDelegate<Unity, Neutral>")
    admin.phase("Action")
    admin.count("PartyDelegate<Greens, Anyone>") shouldBe 3
    admin.count("PartyDelegate<Reds, Anyone>") shouldBe 3
    admin.count("PartyDelegate<Unity, Anyone>") shouldBe 3
    admin.count("Dominant<Greens>") shouldBe 1

    p1.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player1, Greens, Player2>")
        }
        .expect("-Dominant<Greens>, Dominant<Unity>, 0 Dominant<Reds>")
  }

  @Test
  internal fun `Banned Delegate breaks a leader tie from the active player's seat`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, ProjectCard")
    p2.runOperation("PartyDelegate<Scientists>, PartyDelegate<Scientists>")
    p1.runOperation("PartyDelegate<Scientists>, PartyDelegate<Scientists>")
    admin.runOperation("PartyDelegate<Scientists, Neutral>, PartyDelegate<Scientists, Neutral>")
    admin.phase("Action")

    p1.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player1, Scientists, Player2>")
        }
        .expect(
            "PartyLeader<Scientists, Player1>, -PartyLeader<Scientists, Player2>, " +
                "0 PartyLeader<Scientists, Neutral>"
        )
  }

  @Test
  internal fun `Banned Delegate preserves the former leader after a removal tie`() {
    newGame(TurmoilExpansion, players = 3)
    val p2 = requireP2()
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, ProjectCard")
    admin.runOperation("3 PartyDelegate<Scientists, Player3>")
    p1.runOperation("2 PartyDelegate<Scientists>")
    p2.runOperation("2 PartyDelegate<Scientists>")
    admin.phase("Action")
    admin.count("PartyLeader<Scientists, Player3>") shouldBe 1

    p1.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player1, Scientists, Player3>")
        }
        .expect(
            "0 PartyLeader<Scientists, Player1>, 0 PartyLeader<Scientists, Player2>, " +
                "0 PartyLeader<Scientists, Player3>"
        )
  }

  @Test
  internal fun `Banned Delegate measures a challenger tie from the acting seat`() {
    newGame(TurmoilExpansion, players = 4)
    val p2 = requireP2()
    admin.runOperation("-Chairman<Neutral>")
    p2.runOperation("Chairman, ProjectCard")
    admin.runOperation("4 PartyDelegate<Scientists, Player4>")
    p1.runOperation("4 PartyDelegate<Scientists>")
    admin.runOperation("4 PartyDelegate<Scientists, Player3>")
    admin.phase("Action")

    p2.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player2, Scientists, Player4>")
        }
        .expect(
            "0 PartyLeader<Scientists, Player1>, PartyLeader<Scientists, Player3>, " +
                "-PartyLeader<Scientists, Player4>"
        )
  }

  @Test
  internal fun `removes a selected non-leader delegate`() {
    arrangeChairman()
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")

    p1.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player1, MarsFirst, Player2>")
        }
        .expect("-PartyDelegate<MarsFirst, Player2>, 0 PartyLeader<MarsFirst, Player2>")
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

    p1.playProject(BannedDelegate, 0).expect("0 PartyDelegate<Anyone>")
  }

  @Test
  internal fun `must remove a delegate when a non-leader exists`() {
    arrangeChairman()
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")

    p1.playProject(BannedDelegate, 0) {
          shouldThrow<NarrowingException> { declineTask() }
          doTask("BannedDelegateRemoval<Player1, MarsFirst, Player2>")
        }
        .expect("-PartyDelegate<MarsFirst, Player2>, 0 PartyLeader<MarsFirst, Player2>")
  }

  @Test
  internal fun `an unrelated non-leader does not permit selecting a party leader`() {
    arrangeChairman()
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>, PartyDelegate<Scientists>")
    p2.count("PartyLeader<Scientists>") shouldBe 1

    p1.playProject(BannedDelegate, 0) {
          shouldThrow<DeadEndException> {
            doTask("BannedDelegateRemoval<Player1, Scientists, Player2>")
          }
          doTask("BannedDelegateRemoval<Player1, MarsFirst, Player2>")
        }
        .expect(
            "-PartyDelegate<MarsFirst, Player2>, 0 PartyDelegate<Scientists, Player2>, " +
                "0 PartyLeader<Scientists, Player2>"
        )
  }

  @Test
  internal fun `another owner's non-leader does not permit selecting the sole leader`() {
    arrangeChairman()
    val p2 = requireP2()
    p1.runOperation("PartyDelegate<Scientists>")
    p2.runOperation("PartyDelegate<Scientists>")
    p1.count("PartyLeader<Scientists>") shouldBe 1

    p1.playProject(BannedDelegate, 0) {
          shouldThrow<DeadEndException> {
            doTask("BannedDelegateRemoval<Player1, Scientists, Player1>")
          }
          doTask("BannedDelegateRemoval<Player1, Scientists, Player2>")
        }
        .expect(
            "0 PartyDelegate<Scientists, Player1>, 0 PartyLeader<Scientists, Player1>, " +
                "-PartyDelegate<Scientists, Player2>"
        )
  }

  @Test
  internal fun `removal affects only the selected party`() {
    arrangeChairman()
    val p2 = requireP2()
    p2.runOperation("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>, PartyDelegate<Scientists>")

    p1.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player1, MarsFirst, Player2>")
        }
        .expect("-PartyDelegate<MarsFirst, Player2>, 0 PartyDelegate<Scientists, Player2>")
  }

  private fun removeNonLeader(owner: String) {
    arrangeChairman()
    admin.runOperation("PartyDelegate<Scientists, $owner>, PartyDelegate<Scientists, $owner>")

    p1.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Player1, Scientists, $owner>")
        }
        .expect("-PartyDelegate<Scientists, $owner>, 0 PartyLeader<Scientists, $owner>")
  }

  private fun arrangeChairman() {
    newGame(TurmoilExpansion)
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, ProjectCard")
    admin.phase("Action")
  }
}
