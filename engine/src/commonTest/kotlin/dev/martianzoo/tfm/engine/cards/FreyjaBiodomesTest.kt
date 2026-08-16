package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import kotlin.test.Test

class FreyjaBiodomesTest : CardTest() {
  // FAQ: "you can still choose to take microbes"
  @Test
  fun `without an eligible Venus card, resolves Freyja Biodomes`() {
    newGame(VenusNextExpansion)
    p1.manual("PROD[Energy]")
    p1.manual("VenusianAnimals")
    p1.assertCounts(1 to "Animal<VenusianAnimals>")
    p1.manual("FreyjaBiodomes") { doTask("Ok") }
        .expect("PROD[-Energy, 2 Megacredit], 0 Animal<VenusianAnimals>")
  }
}
