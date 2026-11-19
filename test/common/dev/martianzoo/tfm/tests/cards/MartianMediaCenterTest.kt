package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MartianMediaCenterTest : TfmSandboxTest() {
  @Test
  internal fun `Martian Media Center action requires and places an available owned delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")

    kim.inTurn {
          doTasks(
              *cardActionTasks("$MartianMediaCenter"),
              "3 Pay<> FROM MC",
              "PartyLeader<Greens>",
          )
        }
        .expect("0 LobbyDelegate")
  }

  @Test
  internal fun `Martian Media Center may spend the last unplaced Lobby delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")
    kim.exMachina("PartyLeader<Unity>, 5 PartyFollower<Unity>")

    kim.inTurn {
          doTasks(
              *cardActionTasks("$MartianMediaCenter"),
              "3 Pay<> FROM MC",
              "PartyLeader<Greens>",
          )
        }
        .expect("-LobbyDelegate")
  }

  @Test
  internal fun `Martian Media Center action cannot be used without an available delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")
    kim.exMachina("PartyLeader<Unity>, 6 PartyFollower<Unity>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks(
            *cardActionTasks("$MartianMediaCenter"),
            "3 Pay<> FROM MC",
            "PartyLeader<Greens>",
        )
      }
    }

    kim.count("MC") shouldBe 42
    kim.count("PartyDelegate<Greens>") shouldBe 0
  }
}
