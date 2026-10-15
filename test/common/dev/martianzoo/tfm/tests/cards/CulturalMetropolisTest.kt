package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CulturalMetropolisTest : TfmSandboxTest() {
  @Test
  internal fun `Cultural Metropolis cannot split its placements between parties`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Ruling<Unity FROM Greens>")
    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<CulturalMetropolis>>",
            "20 Pay<> FROM MC",
            "CulturalMetropolis FROM ProjectCard",
            "PROD[-Energy]",
            "PROD[3 MC]",
            "CityTile<Tharsis_4_2>",
            "Plant",
            "PartyLeader<Scientists>",
            "PartyLeader<Greens>",
        )
      }
    }
    kim.count("PartyDelegate") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  @Test
  internal fun `Cultural Metropolis can take both delegates from reserve and leave the lobby available`() {
    playWithFullReserve(useLobby = false).expect("0 LobbyDelegate")
  }

  @Test
  internal fun `Forced-lobby variant does not let Cultural Metropolis choose the lobby while reserve delegates remain`() {
    shouldThrow<TaskException> { playWithFullReserve(useLobby = true) }
    kim.count("LobbyDelegate") shouldBe 1
    kim.count("Delegate") shouldBe 1
    kim.count("CulturalMetropolis") shouldBe 0
    kim.count("CityTile") shouldBe 0
    kim.count("MC") shouldBe 42
  }

  private fun playWithFullReserve(useLobby: Boolean): TaskResult {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Ruling<Unity FROM Greens>")

    // Kim starts with six reserve delegates and the unused lobby delegate.
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<CulturalMetropolis>>",
          "20 Pay<> FROM MC",
          "CulturalMetropolis FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Tharsis_4_2>",
          "Plant",
          "PartyLeader<Scientists>",
          "PartyFollower<Scientists>",
      )
      // The selected variant does not offer this voluntary removal.
      if (useLobby) doTask("-LobbyDelegate")
    }
  }

  @Test
  internal fun `Cultural Metropolis can tie with its first delegate and take leadership with its second`() {
    newTestGame("TurmoilExpansion")
    kim.exMachina("Ruling<Unity FROM Greens>, PartyLeader<Scientists, Neutral>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<CulturalMetropolis>>",
              "20 Pay<> FROM MC",
              "CulturalMetropolis FROM ProjectCard",
              "PROD[-Energy]",
              "PROD[3 MC]",
              "CityTile<Tharsis_4_2>",
              "Plant",
              "PartyFollower<Scientists>",
              "PartyLeader<Scientists>",
          )
        }
        .expect(
            "-PartyLeader<Scientists, Neutral>, PartyFollower<Scientists, Neutral>, 0 LobbyDelegate"
        )
  }

  @Test
  internal fun `Cultural Metropolis cannot be played with only one delegate available`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyLeader<Unity>, 5 PartyFollower<Unity>")
    kim.exMachina("Ruling<Unity FROM Greens>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks("PlayProject<Class<CulturalMetropolis>>", "20 Pay<> FROM MC")
        doTask("CulturalMetropolis FROM ProjectCard")
        doTask("PROD[-Energy]")
        doTask("PROD[3 MC]")
        doTask("CityTile<Tharsis_4_2>")
        doTask("Plant")
        doTasks("PartyLeader<Scientists>", "PartyFollower<Scientists>")
      }
    }

    kim.count("Delegate") shouldBe 7
    kim.count("CityTile") shouldBe 0
  }

  @Test
  internal fun `Cultural Metropolis uses the last reserve delegate and the lobby delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyLeader<Unity>, 4 PartyFollower<Unity>")
    kim.exMachina("Ruling<Unity FROM Greens>")

    kim.inTurn {
          doTasks("PlayProject<Class<CulturalMetropolis>>", "20 Pay<> FROM MC")
          doTasks(
              "CulturalMetropolis FROM ProjectCard",
              "PROD[-Energy]",
              "PROD[3 MC]",
              "CityTile<Tharsis_4_2>",
              "Plant",
          )
          doTasks("PartyLeader<Scientists>", "PartyFollower<Scientists>")
        }
        .expect("-LobbyDelegate")
  }
}
