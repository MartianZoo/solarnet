package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.cards.cardnames.BannedDelegate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class BannedDelegateTest : ProjectCardTest() {
  @Test
  internal fun `Requires the player to be Chairman`() {
    newTestGame(addOptions = "TurmoilExpansion")

    shouldThrow<NotNowException> { kim.playProject(BannedDelegate, 0) }
  }

  @Test
  internal fun `Updates party leadership and dominance after removal`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman FROM Chairman<Neutral>")
    stan.exMachina("2 PartyDelegate<MarsFirst>")
    kim.exMachina("2 PartyDelegate<MarsFirst>, 4 PartyDelegate<Unity>")
    kim.exMachina("PartyDelegate<Unity, Neutral>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, MarsFirst, Stan>")
        }
        .expect(
            "-Dominant<MarsFirst>, Dominant<Unity>, " +
                "PartyLeader<MarsFirst, Kim>, -PartyLeader<MarsFirst, Stan>"
        )
  }

  @Test
  internal fun `Replaces an incumbent below a dominance tie`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman FROM Chairman<Neutral>")
    stan.exMachina("2 PartyDelegate<MarsFirst>")
    kim.exMachina("3 PartyDelegate<Kelvinists, Neutral>, 2 PartyDelegate<Reds, Neutral>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, MarsFirst, Stan>")
        }
        .expect("-Dominant<MarsFirst>, Dominant<Kelvinists>, 0 Dominant<Reds>")
  }

  @Test
  internal fun `Breaks a dominance tie clockwise from the removed party`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman FROM Chairman<Neutral>")
    stan.exMachina("2 PartyDelegate<Greens>")
    kim.exMachina("PartyDelegate<Greens>")
    kim.exMachina("2 PartyDelegate<Reds, Neutral>, 3 PartyDelegate<Unity, Neutral>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, Greens, Stan>")
        }
        .expect("-Dominant<Greens>, Dominant<Unity>, 0 Dominant<Reds>")
  }

  @Test
  internal fun `Breaks a leader tie from the active player's seat`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman FROM Chairman<Neutral>")
    stan.exMachina("2 PartyDelegate<Scientists>")
    kim.exMachina("2 PartyDelegate<Scientists>")
    kim.exMachina("2 PartyDelegate<Scientists, Neutral>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, Scientists, Stan>")
        }
        .expect(
            "PartyLeader<Scientists, Kim>, -PartyLeader<Scientists, Stan>, " +
                "0 PartyLeader<Scientists, Neutral>"
        )
  }

  @Test
  internal fun `Preserves the former leader after a removal tie`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman FROM Chairman<Neutral>")
    kim.exMachina("3 PartyDelegate<Scientists, Rob>")
    kim.exMachina("2 PartyDelegate<Scientists>")
    stan.exMachina("2 PartyDelegate<Scientists>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, Scientists, Rob>")
        }
        .expect(
            "0 PartyLeader<Scientists, Kim>, 0 PartyLeader<Scientists, Stan>, " +
                "0 PartyLeader<Scientists, Rob>"
        )
    rob.count("PartyLeader<Scientists>") shouldBe 1
  }

  @Test
  internal fun `Measures a challenger tie from the acting seat`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 4)
    stan.exMachina("Chairman FROM Chairman<Neutral>")
    kim.exMachina("4 PartyDelegate<Scientists, Maya>")
    kim.exMachina("4 PartyDelegate<Scientists>")
    kim.exMachina("4 PartyDelegate<Scientists, Rob>")

    stan
        .playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Stan, Scientists, Maya>")
        }
        .expect(
            "0 PartyLeader<Scientists, Kim>, PartyLeader<Scientists, Rob>, " +
                "-PartyLeader<Scientists, Maya>"
        )
  }

  @Test
  internal fun `Removes a selected non-leader delegate`() {
    arrangeChairman()
    stan.exMachina("2 PartyDelegate<MarsFirst>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, MarsFirst, Stan>")
        }
        .expect("-PartyDelegate<MarsFirst, Stan>, 0 PartyLeader<MarsFirst, Stan>")
  }

  @Test
  internal fun `Can remove an owned non-leader`() {
    arrangeChairman()
    kim.exMachina("2 PartyDelegate<Scientists>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, Scientists, Kim>")
        }
        .expect("-PartyDelegate<Scientists>, 0 PartyLeader<Scientists>")
  }

  @Test
  internal fun `Can remove a neutral non-leader`() {
    arrangeChairman()
    kim.exMachina("2 PartyDelegate<Scientists, Neutral>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, Scientists, Neutral>")
        }
        .expect("-PartyDelegate<Scientists, Neutral>, 0 PartyLeader<Scientists, Neutral>")
  }

  @Test
  internal fun `Can play without removing a delegate when only leaders remain`() {
    arrangeChairman()

    kim.playProject(BannedDelegate, 0).expect("0 PartyDelegate<Anyone>")
  }

  @Test
  internal fun `Must remove a delegate when a non-leader exists`() {
    arrangeChairman()
    stan.exMachina("2 PartyDelegate<MarsFirst>")

    shouldThrow<NarrowingException> {
      kim.playProject(BannedDelegate, 0) { declineTask() }
    }
  }

  @Test
  internal fun `An unrelated non-leader does not permit selecting a party leader`() {
    arrangeChairman()
    stan.exMachina("2 PartyDelegate<MarsFirst>, PartyDelegate<Scientists>")

    shouldThrow<DeadEndException> {
      kim.playProject(BannedDelegate, 0) {
        doTask("BannedDelegateRemoval<Kim, Scientists, Stan>")
      }
    }
  }

  @Test
  internal fun `Another owner's non-leader does not permit selecting the sole leader`() {
    arrangeChairman()
    kim.exMachina("PartyDelegate<Scientists>")
    stan.exMachina("PartyDelegate<Scientists>")

    shouldThrow<DeadEndException> {
      kim.playProject(BannedDelegate, 0) {
        doTask("BannedDelegateRemoval<Kim, Scientists, Kim>")
      }
    }
  }

  @Test
  internal fun `Removal affects only the selected party`() {
    arrangeChairman()
    stan.exMachina("2 PartyDelegate<MarsFirst>, PartyDelegate<Scientists>")

    kim.playProject(BannedDelegate, 0) {
          doTask("BannedDelegateRemoval<Kim, MarsFirst, Stan>")
        }
        .expect("-PartyDelegate<MarsFirst, Stan>, 0 PartyDelegate<Scientists, Stan>")
  }

  private fun arrangeChairman() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman FROM Chairman<Neutral>")
  }
}
