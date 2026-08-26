package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class FreyjaBiodomesTest : CardTest() {
  // FAQ: "you can still choose to take microbes"
  @Test
  internal fun `Can be played without another eligible Venus card`() {
    newGame(VenusNextExpansion)
    p1.manual("PROD[Energy]")
    p1.manual("$VenusianAnimals")
    p1.assertCounts(1 to "Animal<$VenusianAnimals>")
    p1.manual("$FreyjaBiodomes") {
          // Decline adding animals to Venusian Animals by choosing the unavailable microbe gain.
          declineTask()
        }
        .expect("PROD[-Energy, 2 MC], 0 Animal<$VenusianAnimals>")
  }
}
