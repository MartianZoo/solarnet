package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EventAnalystsTest : TfmGameplayTest() {
  @Test
  internal fun `Influence stacks with High Circles and Colonial Representation beyond four`() {
    newTestGame(
        "PreludeExpansion, HighCircles, TurmoilExpansion, ColonialRepresentation, Luna",
        playerCount = 2,
    )
    kim.turn {
      playPrelude(HighCircles) { doTask("2 PartyDelegate<Scientists>") }
      playPrelude(Donation)
    }
    stan.turn {
      playPrelude(Supplier)
      playPrelude(MetalsCompany)
    }
    kim.turn {
      playProject(EventAnalysts, 5)
      stdProject("BuildColonyProject") { doTask("Colony<Luna>") }
    }
    stan.pass()
    kim.turn {
      playProject(ColonialRepresentation, 10)
      pass()
    }
    kim.wgt("VenusStep")
    admin.doTask("ExploreFirstDirective")
    kim.buyCards(0)
    stan.buyCards(0)
    stan.pass()
    kim.turn {
      // The chairman retains its influence; the dominant party supplies leader and delegate
      // influence.
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") }
      repeat(3) {
        stdAction("LobbyAction", 2, payment = { kim.pay(5) }) {
          doTask("PartyDelegate<Scientists>")
        }
      }
      pass()
    }
    val plantsAfterProduction = kim.count("Plant")
    val steelAfterProduction = kim.count("Steel")
    stan.wgt("VenusStep")
    stan.doTask("OceanTile<Tharsis_1_5> BY Admin")

    kim.count("Plant") shouldBe plantsAfterProduction + 6
    kim.count("Steel") shouldBe steelAfterProduction + 6
  }
}
