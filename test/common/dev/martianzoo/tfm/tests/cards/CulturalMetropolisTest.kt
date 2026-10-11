package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CulturalMetropolisTest : TfmSandboxTest() {
  @Test
  internal fun `Cultural Metropolis cannot be played with only one delegate available`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("6 PartyDelegate<MarsFirst>")
    kim.exMachina("Ruling<Unity FROM Greens>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<CulturalMetropolis>>", "-20 MC")
        selectTask("-X Steel?")
        selectTask("Ok")
        doTask("CulturalMetropolis FROM ProjectCard")
        selectTask("PROD[-Energy]")
        selectTask("PROD[3 MC]")
        doTask("CityTile<Tharsis_4_2>")
        selectTask("Plant!")
        selectOnlyTask()
        doTask("2 PartyDelegate<Scientists>")
      }
    }

    kim.count("Delegate") shouldBe 6
    kim.count("CityTile") shouldBe 0
  }

  @Test
  internal fun `Cultural Metropolis places two of the seven delegates`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("5 PartyDelegate<MarsFirst>")
    kim.exMachina("Ruling<Unity FROM Greens>")

    kim.inTurn {
          doTasks("PlayProject<Class<CulturalMetropolis>>", "-20 MC")
          selectTask("-X Steel?")
          doTasks(
              "Ok",
              "CulturalMetropolis FROM ProjectCard",
              "PROD[-Energy]",
              "PROD[3 MC]",
              "CityTile<Tharsis_4_2>",
              "Plant",
          )
          selectOnlyTask()
          doTask("2 PartyDelegate<Scientists>")
        }
        .expect("-LobbyActionAvailable")
  }
}
