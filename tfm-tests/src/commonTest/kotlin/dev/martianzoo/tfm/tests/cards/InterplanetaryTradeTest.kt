package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class InterplanetaryTradeTest : CardTest() {
  @Test
  internal fun `Counts three existing tag types and adds four production`() {
    newGame(PromoCardPack)
    // These have to be played: tags depend on their cards.
    p1.manual("$Ecoline, $Mine, $SearchForLife, 8 Plant, 6 Steel, 4 Heat, 3 ProjectCard")
    p1.manual("$InterplanetaryTrade").expect("PROD[4 MC]")
  }

  @Test
  internal fun `Does not count a tag from a played event`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("100 MC, 2 ProjectCard, $Ecoline, $Mine, $SearchForLife")
    p1.playProject(ImportedHydrogen, 16) {
      doTask("3 Plant")
      placeTile(1, 2)
    }

    p1.playProject(InterplanetaryTrade, 27).expect("PROD[4 MC]")
  }
}
