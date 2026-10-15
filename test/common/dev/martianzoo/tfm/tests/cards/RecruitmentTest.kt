package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RecruitmentTest : TfmSandboxTest() {
  @Test
  internal fun `Recruitment cannot be played without a neutral non-leader delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<Recruitment>>")

        doTasks(
            "2 Pay<> FROM MC",
            "Recruitment FROM ProjectCard",
        )
        doTask("PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>")
      }
    }
  }

  @Test
  internal fun `Recruitment exchanges a neutral non-leader for an available owned delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")

    kim.exMachina(
        "PartyFollower<MarsFirst, Neutral>, " +
            "PartyLeader<Unity, Neutral>, PartyFollower<Unity, Neutral>"
    )
    admin.count("Dominant<MarsFirst>") shouldBe 1
    kim.inTurn {
          doTasks(
              "PlayProject<Class<Recruitment>>",
              "2 Pay<> FROM MC",
              "Recruitment FROM ProjectCard",
              "PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
          )
          doTask("PlayedEvent<Class<$Recruitment>> FROM $Recruitment")
        }
        .expect(
            "PartyDelegate<MarsFirst>, Delegate, -Delegate<Neutral>, 0 Dominant<MarsFirst>, 0 PartyLeader<MarsFirst, Neutral>"
        )
  }

  @Test
  internal fun `Recruitment makes only the recruiting player leader after a tied challenge`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyFollower<MarsFirst, Neutral>")
    kim.exMachina("PartyFollower<MarsFirst>")
    stan.exMachina("2 PartyFollower<MarsFirst>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Recruitment>>",
              "2 Pay<> FROM MC",
              "Recruitment FROM ProjectCard",
              "PartyLeader<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
          )
          doTask("PlayedEvent<Class<$Recruitment>> FROM $Recruitment")
        }
        .expect(
            "PartyLeader<MarsFirst>, -PartyLeader<MarsFirst, Neutral>, 0 PartyLeader<MarsFirst, Stan>"
        )
  }

  @Test
  internal fun `Recruitment promotes another player with the sole largest delegation`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyFollower<MarsFirst, Neutral>")
    kim.exMachina("PartyFollower<MarsFirst>")
    stan.exMachina("3 PartyFollower<MarsFirst>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Recruitment>>",
              "2 Pay<> FROM MC",
              "Recruitment FROM ProjectCard",
              "PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
          )
          doTask("PlayedEvent<Class<$Recruitment>> FROM $Recruitment")
        }
        .expect(
            "PartyLeader<MarsFirst, Stan>, -PartyLeader<MarsFirst, Neutral>, 0 PartyLeader<MarsFirst>"
        )
  }

  @Test
  internal fun `Recruitment preserves a tied player incumbent`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyFollower<MarsFirst, Neutral>")
    stan.exMachina("2 PartyFollower<MarsFirst>, PartyLeader<MarsFirst>")
    kim.exMachina("2 PartyFollower<MarsFirst>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Recruitment>>",
              "2 Pay<> FROM MC",
              "Recruitment FROM ProjectCard",
              "PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
          )
          doTask("PlayedEvent<Class<$Recruitment>> FROM $Recruitment")
        }
        .expect("0 PartyLeader<MarsFirst>, 0 PartyLeader<MarsFirst, Stan>")
  }

  @Test
  internal fun `Recruitment chooses the nearest clockwise challenger when the recruiter trails`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyFollower<MarsFirst, Neutral>")
    kim.exMachina("PartyFollower<MarsFirst>")
    stan.exMachina("3 PartyFollower<MarsFirst>")
    rob.exMachina("3 PartyFollower<MarsFirst>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<$Recruitment>>",
              "2 Pay<> FROM MC",
              "$Recruitment FROM ProjectCard",
              "PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
          )
          doTask("PlayedEvent<Class<$Recruitment>> FROM $Recruitment")
        }
        .expect(
            "PartyLeader<MarsFirst, Stan>, 0 PartyLeader<MarsFirst, Rob>, " +
                "-PartyLeader<MarsFirst, Neutral>, 0 PartyLeader<MarsFirst>"
        )
  }

  @Test
  internal fun `Recruitment measures clockwise order from the recruiting player`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyFollower<MarsFirst, Neutral>")
    stan.exMachina("PartyFollower<MarsFirst>")
    kim.exMachina("3 PartyFollower<MarsFirst>")
    kim.exMachina("3 PartyFollower<MarsFirst, Rob>")

    stan
        .inTurn {
          doTasks(
              "PlayProject<Class<Recruitment>>",
              "2 Pay<> FROM MC",
              "Recruitment FROM ProjectCard",
              "PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
          )
          doTask("PlayedEvent<Class<$Recruitment>> FROM $Recruitment")
        }
        .expect(
            "PartyLeader<MarsFirst, Rob>, 0 PartyLeader<MarsFirst, Kim>, " +
                "-PartyLeader<MarsFirst, Neutral>, 0 PartyLeader<MarsFirst, Stan>"
        )
  }

  @Test
  internal fun `Recruitment consumes the lobby when every reserve delegate is placed`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina("PartyFollower<MarsFirst, Neutral>, PartyLeader<Unity>, 5 PartyFollower<Unity>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Recruitment>>",
              "2 Pay<> FROM MC",
              "Recruitment FROM ProjectCard",
              "PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>",
              "PlayedEvent<Class<Recruitment>> FROM Recruitment",
          )
        }
        .expect("-LobbyDelegate, 0 Delegate, -Delegate<Neutral>")
  }

  @Test
  internal fun `Recruitment cannot replace the neutral leader`() {
    newTestGame("TurmoilExpansion")
    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<Recruitment>>",
            "2 Pay<> FROM MC",
            "Recruitment FROM ProjectCard",
            "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>",
        )
      }
    }
    kim.count("MC") shouldBe 42
    admin.count("PartyLeader<MarsFirst, Neutral>") shouldBe 1
  }

  @Test
  internal fun `Recruitment cannot move the replacement to another party`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina("PartyFollower<MarsFirst, Neutral>")
    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<Recruitment>>",
            "2 Pay<> FROM MC",
            "Recruitment FROM ProjectCard",
            "PartyLeader<Scientists> FROM PartyFollower<MarsFirst, Neutral>",
        )
      }
    }
    kim.count("PartyDelegate") shouldBe 0
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 2
  }

  @Test
  internal fun `Recruitment cannot be played without an available owned delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyFollower<MarsFirst, Neutral>")
    kim.exMachina("PartyLeader<Unity>, 6 PartyFollower<Unity>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTask("PlayProject<Class<Recruitment>>")

        doTasks(
            "2 Pay<> FROM MC",
            "Recruitment FROM ProjectCard",
        )
        doTask("PartyFollower<MarsFirst> FROM PartyFollower<MarsFirst, Neutral>")
      }
    }

    kim.count("Delegate") shouldBe 7
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 2
  }
}
