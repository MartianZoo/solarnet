package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class InterplanetaryTradeTest : CardTest() {
  @Test
  fun `Counts three existing tag types and adds four production`() {
    newGame(PromoCardPack)
    // These have to be played: tags depend on their cards.
    p1.manual("$Ecoline, $Mine, $SearchForLife, 8 Plant, 6 Steel, 4 Heat, 3 ProjectCard")
    p1.manual("$InterplanetaryTrade").expect("PROD[4]")
  }

  @Test
  fun `Does not count a tag from a played event`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("100, 2 ProjectCard, $Ecoline, $Mine, $SearchForLife")
    p1.playProject(ImportedHydrogen, 16) {
      doTask("3 Plant")
      placeTile(1, 2)
    }

    p1.playProject(InterplanetaryTrade, 27).expect("PROD[4]")
  }
}
