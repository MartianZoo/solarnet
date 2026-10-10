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
    kim.exMachina("Ruling<Unity> FROM Ruling<Greens>")

    shouldThrow<DeadEndException> {
      kim.playProject(CulturalMetropolis, 20) {
        placeTile(4, 2)
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
    kim.exMachina("Ruling<Unity> FROM Ruling<Greens>")

    kim.playProject(CulturalMetropolis, 20) {
          placeTile(4, 2)
          doTask("2 PartyDelegate<Scientists>")
        }
        .expect("2 PartyDelegate<Scientists>, -LobbyActionAvailable")
  }
}
