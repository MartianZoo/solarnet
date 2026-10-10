package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TerraLabsTest : TfmGameplayTest() {
  @Test
  internal fun `Buys all four research cards for four MC`() {
    newTestGame(playerCount = 2, kimCorporation = TerraLabsResearch)
    kim.pass()
    stan.pass()
    kim.wgt("VenusStep")

    kim.buyCards(4).expect("4 ProjectCard, -4 MC")
  }

  @Test
  internal fun `Acquiring Polyphemos cancels the card-purchase discount`() {
    newTestGame(
        addOptions = "PreludeExpansion",
        playerCount = 2,
        kimCorporation = TerraLabsResearch,
    )
    kim.turn {
      playPrelude(Merger) { playCorp(Polyphemos) }
      playPrelude(Donation)
    }
    stan.turn {
      playPrelude(SupplyDrop)
      playPrelude(PowerGeneration)
    }
    kim.pass()
    stan.pass()
    kim.wgt("VenusStep")

    kim.buyCards(4).expect("4 ProjectCard, -12 MC")
  }
}
