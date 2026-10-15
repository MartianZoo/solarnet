package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class CulturalMetropolisTest : TfmSandboxTest() {
  @Test
  internal fun `Cultural Metropolis can take both delegates from reserve and leave the lobby available`() {
    playWithFullReserve(useLobby = false).expect("0 LobbyActionAvailable")
  }

  @Ignore // Known defect: card placements cannot choose the lobby before the reserve is empty.
  @Test
  internal fun `Cultural Metropolis can use the lobby while five delegates remain in reserve`() {
    playWithFullReserve(useLobby = true).expect("-LobbyActionAvailable")
  }

  @Test
  internal fun `BUG - Cultural Metropolis cannot choose the lobby while delegates remain in reserve`() {
    shouldThrow<TaskException> { playWithFullReserve(useLobby = true) }
    kim.count("LobbyActionAvailable") shouldBe 1
    kim.count("Delegate") shouldBe 0
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
          "2 PartyDelegate<Scientists>",
      )
      // Choosing the lobby for a card placement must consume its free action too.
      if (useLobby) doTask("-LobbyActionAvailable")
    }
  }

  @Test
  internal fun `Cultural Metropolis cannot be played with only one delegate available`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("6 PartyDelegate<MarsFirst>")
    kim.exMachina("Ruling<Unity FROM Greens>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks("PlayProject<Class<CulturalMetropolis>>", "20 Pay<> FROM MC")
        doTask("CulturalMetropolis FROM ProjectCard")
        doTask("PROD[-Energy]")
        doTask("PROD[3 MC]")
        doTask("CityTile<Tharsis_4_2>")
        doTask("Plant")
        doTask("2 PartyDelegate<Scientists>")
      }
    }

    kim.count("Delegate") shouldBe 6
    kim.count("CityTile") shouldBe 0
  }

  @Test
  internal fun `Cultural Metropolis uses the last reserve delegate and the lobby delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("5 PartyDelegate<MarsFirst>")
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
          doTask("2 PartyDelegate<Scientists>")
        }
        .expect("-LobbyActionAvailable")
  }
}
