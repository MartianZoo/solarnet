package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import kotlin.test.Test

class HeadStartTest : CardTest() {
  @Test
  fun `with Head Start, declines both actions`() {
    newGame("PreludeExpansion,PromoCardPack")
    engine.phase("Prelude")
    p1.manual("4, 10 ProjectCard, PreludeCard")
    p1.playPrelude("HeadStart") {
      p1.assertCounts(2 to "Steel", 24 to "Megacredit")

      doFirstTask("Ok")
      doFirstTask("Ok")
    }
  }
}
