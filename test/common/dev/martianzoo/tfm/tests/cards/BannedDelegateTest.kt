package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.BannedDelegate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class BannedDelegateTest : TfmSandboxTest() {
  @Test
  internal fun `Requires the player to be Chairman`() {
    newTestGame(addOptions = "TurmoilExpansion")

    shouldThrow<NotNowException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<BannedDelegate>>")
      }
    }
  }

  @Test
  internal fun `Updates party leadership and dominance after removal`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    stan.exMachina("2 PartyDelegate<MarsFirst>")
    kim.exMachina("2 PartyDelegate<MarsFirst>, 4 PartyDelegate<Unity>")
    kim.exMachina("PartyDelegate<Unity, Neutral>")

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Kim, MarsFirst, Stan>.")
          selectOnlyTask()
        }
        .expect(
            "-Dominant<MarsFirst>, Dominant<Unity>, " +
                "PartyLeader<MarsFirst, Kim>, -PartyLeader<MarsFirst, Stan>"
        )
  }

  @Test
  internal fun `Replaces an incumbent below a dominance tie`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    stan.exMachina("2 PartyDelegate<MarsFirst>")
    kim.exMachina("3 PartyDelegate<Kelvinists, Neutral>, 2 PartyDelegate<Reds, Neutral>")

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Kim, MarsFirst, Stan>.")
          selectOnlyTask()
        }
        .expect("-Dominant<MarsFirst>, Dominant<Kelvinists>, 0 Dominant<Reds>")
  }

  @Test
  internal fun `Breaks a dominance tie clockwise from the removed party`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    stan.exMachina("2 PartyDelegate<Greens>")
    kim.exMachina("PartyDelegate<Greens>")
    kim.exMachina("2 PartyDelegate<Reds, Neutral>, 3 PartyDelegate<Unity, Neutral>")

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Kim, Greens, Stan>.")
          selectOnlyTask()
        }
        .expect("-Dominant<Greens>, Dominant<Unity>, 0 Dominant<Reds>")
  }

  @Test
  internal fun `Breaks a leader tie from the active player's seat`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    stan.exMachina("2 PartyDelegate<Scientists>")
    kim.exMachina("2 PartyDelegate<Scientists>")
    kim.exMachina("2 PartyDelegate<Scientists, Neutral>")

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Kim, Scientists, Stan>.")
          selectOnlyTask()
        }
        .expect(
            "PartyLeader<Scientists, Kim>, -PartyLeader<Scientists, Stan>, " +
                "0 PartyLeader<Scientists, Neutral>"
        )
  }

  @Test
  internal fun `Preserves the former leader after a removal tie`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    kim.exMachina("3 PartyDelegate<Scientists, Rob>")
    kim.exMachina("2 PartyDelegate<Scientists>")
    stan.exMachina("2 PartyDelegate<Scientists>")

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Kim, Scientists, Rob>.")
          selectOnlyTask()
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
    stan.exMachina("Chairman<Stan FROM Neutral>")
    kim.exMachina("4 PartyDelegate<Scientists, Maya>")
    kim.exMachina("4 PartyDelegate<Scientists>")
    kim.exMachina("4 PartyDelegate<Scientists, Rob>")

    stan
        .inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Stan, Scientists, Maya>.")
          selectOnlyTask()
        }
        .expect(
            "0 PartyLeader<Scientists, Kim>, PartyLeader<Scientists, Rob>, " +
                "-PartyLeader<Scientists, Maya>"
        )
  }

  @Test
  internal fun `Removes a selected non-leader delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    stan.exMachina("2 PartyDelegate<MarsFirst>")

    kim.inTurn {
          doTasks("PlayProject<Class<$BannedDelegate>>", "$BannedDelegate FROM ProjectCard")
          selectTask("BannedDelegateRemoval<Kim, MarsFirst, Stan>")
          doTask("-PartyDelegate<MarsFirst, Stan>")
        }
        .expect("0 PartyLeader<MarsFirst, Stan>")
  }

  @Test
  internal fun `Can remove an owned non-leader`() {
    arrangeChairman()
    kim.exMachina("2 PartyDelegate<Scientists>")

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Kim, Scientists, Kim>.")
          selectOnlyTask()
        }
        .expect("-PartyDelegate<Scientists>, 0 PartyLeader<Scientists>")
  }

  @Test
  internal fun `Can remove a neutral non-leader`() {
    arrangeChairman()
    kim.exMachina("2 PartyDelegate<Scientists, Neutral>")

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Kim, Scientists, Neutral>.")
          selectOnlyTask()
        }
        .expect("-PartyDelegate<Scientists, Neutral>, 0 PartyLeader<Scientists, Neutral>")
  }

  @Test
  internal fun `Can play without removing a delegate when only leaders remain`() {
    arrangeChairman()

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
        }
        .expect("0 PartyDelegate<Anyone>")
  }

  @Test
  internal fun `Must remove a delegate when a non-leader exists`() {
    arrangeChairman()
    stan.exMachina("2 PartyDelegate<MarsFirst>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
        selectOnlyTask()
        doTask("Ok")
      }
    }
  }

  @Test
  internal fun `An unrelated non-leader does not permit selecting a party leader`() {
    arrangeChairman()
    stan.exMachina("2 PartyDelegate<MarsFirst>, PartyDelegate<Scientists>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
        selectOnlyTask()
        narrowTask("BannedDelegateRemoval<Kim, Scientists, Stan>")
      }
    }
  }

  @Test
  internal fun `Another owner's non-leader does not permit selecting the sole leader`() {
    arrangeChairman()
    kim.exMachina("PartyDelegate<Scientists>")
    stan.exMachina("PartyDelegate<Scientists>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
        selectOnlyTask()
        narrowTask("BannedDelegateRemoval<Kim, Scientists, Kim>")
      }
    }
  }

  @Test
  internal fun `Removal affects only the selected party`() {
    arrangeChairman()
    stan.exMachina("2 PartyDelegate<MarsFirst>, PartyDelegate<Scientists>")

    kim.inTurn {
          doTasks("PlayProject<Class<BannedDelegate>>", "BannedDelegate FROM ProjectCard")
          selectOnlyTask()
          selectTask("BannedDelegateRemoval<Kim, MarsFirst, Stan>.")
          selectOnlyTask()
        }
        .expect("-PartyDelegate<MarsFirst, Stan>, 0 PartyDelegate<Scientists, Stan>")
  }

  private fun arrangeChairman() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
  }
}
