package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.BannedDelegate
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class BannedDelegateTest : TfmSandboxTest() {
  @Test
  internal fun `Requires the player to be Chairman`() {
    newTestGame(addOptions = "TurmoilExpansion")

    shouldThrow<NotNowException> {
      kim.inTurn {
        doTasks(*playCardTasks("BannedDelegate"))
      }
    }
  }

  @Test
  internal fun `Updates party leadership and dominance after removal`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    stan.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")
    kim.exMachina("2 PartyFollower<MarsFirst>, PartyLeader<Unity>, 3 PartyFollower<Unity>")
    kim.exMachina("PartyFollower<Unity, Neutral>")

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Stan, MarsFirst>")
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
    stan.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")
    kim.exMachina(
        "PartyLeader<Kelvinists, Neutral>, 2 PartyFollower<Kelvinists, Neutral>, 2 PartyFollower<Reds, Neutral>"
    )

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Stan, MarsFirst>")
        }
        .expect("-Dominant<MarsFirst>, Dominant<Kelvinists>, 0 Dominant<Reds>")
  }

  @Test
  internal fun `Breaks a dominance tie clockwise from the removed party`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    stan.exMachina("PartyLeader<Greens>, PartyFollower<Greens>")
    kim.exMachina("PartyFollower<Greens>")
    kim.exMachina(
        "2 PartyFollower<Reds, Neutral>, PartyLeader<Unity, Neutral>, 2 PartyFollower<Unity, Neutral>"
    )

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Stan, Greens>")
        }
        .expect("-Dominant<Greens>, Dominant<Unity>, 0 Dominant<Reds>")
  }

  @Test
  internal fun `Breaks a leader tie from the active player's seat`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
    stan.exMachina("PartyLeader<Scientists>, PartyFollower<Scientists>")
    kim.exMachina("2 PartyFollower<Scientists>")
    kim.exMachina("2 PartyFollower<Scientists, Neutral>")

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Stan, Scientists>")
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
    kim.exMachina("PartyLeader<Scientists, Rob>, 2 PartyFollower<Scientists, Rob>")
    kim.exMachina("2 PartyFollower<Scientists>")
    stan.exMachina("2 PartyFollower<Scientists>")

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Rob, Scientists>")
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
    kim.exMachina("PartyLeader<Scientists, Maya>, 3 PartyFollower<Scientists, Maya>")
    kim.exMachina("4 PartyFollower<Scientists>")
    kim.exMachina("4 PartyFollower<Scientists, Rob>")

    stan
        .inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Maya, Scientists>")
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
    stan.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")

    kim.inTurn {
          doTasks(
              *playCardTasks("$BannedDelegate"),
              "$BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<MarsFirst, Stan>")
        }
        .expect("0 PartyLeader<MarsFirst, Stan>")
  }

  @Test
  internal fun `Can remove an owned non-leader`() {
    arrangeChairman()
    kim.exMachina("PartyLeader<Scientists>, PartyFollower<Scientists>")

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Scientists>")
        }
        .expect("-PartyFollower<Scientists>, 0 PartyLeader<Scientists>")
  }

  @Test
  internal fun `Can remove a neutral non-leader`() {
    arrangeChairman()
    kim.exMachina("PartyLeader<Scientists, Neutral>, PartyFollower<Scientists, Neutral>")

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Neutral, Scientists>")
        }
        .expect("-PartyFollower<Scientists, Neutral>, 0 PartyLeader<Scientists, Neutral>")
  }

  @Test
  internal fun `Can play without removing a delegate when only leaders remain`() {
    arrangeChairman()

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
              "Ok",
          )
        }
        .expect("0 PartyDelegate<Anyone>")
  }

  @Test
  internal fun `Must remove a delegate when a non-leader exists`() {
    arrangeChairman()
    stan.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("BannedDelegate"),
            "BannedDelegate FROM ProjectCard",
        )
        doTask("Ok")
      }
    }
  }

  @Test
  internal fun `An unrelated non-leader does not permit selecting a party leader`() {
    arrangeChairman()
    stan.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>, PartyLeader<Scientists>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("BannedDelegate"),
            "BannedDelegate FROM ProjectCard",
        )
        doTask("-PartyLeader<Scientists, Stan>")
      }
    }
  }

  @Test
  internal fun `Another owner's non-leader does not permit selecting the sole leader`() {
    arrangeChairman()
    kim.exMachina("PartyLeader<Scientists>")
    stan.exMachina("PartyFollower<Scientists>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("BannedDelegate"),
            "BannedDelegate FROM ProjectCard",
        )
        doTask("-PartyLeader<Scientists, Kim>")
      }
    }
  }

  @Test
  internal fun `Removal affects only the selected party`() {
    arrangeChairman()
    stan.exMachina("PartyFollower<MarsFirst>, PartyLeader<MarsFirst>, PartyLeader<Scientists>")

    kim.inTurn {
          doTasks(
              *playCardTasks("BannedDelegate"),
              "BannedDelegate FROM ProjectCard",
          )
          doTask("-PartyFollower<Stan, MarsFirst>")
        }
        .expect("-PartyFollower<MarsFirst, Stan>, 0 PartyDelegate<Scientists, Stan>")
  }

  @Test
  internal fun `Cannot evade removal by selecting an empty party while followers exist elsewhere`() {
    arrangeChairman()
    stan.exMachina("PartyLeader<Scientists>, PartyFollower<Scientists>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("BannedDelegate"),
            "BannedDelegate FROM ProjectCard",
            "-PartyFollower<Greens, Stan>",
        )
      }
    }
    stan.count("PartyFollower<Scientists>") shouldBe 1
    kim.count("PlayedEvent<Class<BannedDelegate>>") shouldBe 0
  }

  @Test
  internal fun `Cannot evade removal by selecting an owner with no follower in the chosen party`() {
    arrangeChairman()
    stan.exMachina("PartyLeader<Scientists>, PartyFollower<Scientists>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("BannedDelegate"),
            "BannedDelegate FROM ProjectCard",
            "-PartyFollower<Scientists, Rob>",
        )
      }
    }
    stan.count("PartyFollower<Scientists>") shouldBe 1
    kim.count("PlayedEvent<Class<BannedDelegate>>") shouldBe 0
  }

  private fun arrangeChairman() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Chairman<Kim FROM Neutral>")
  }
}
