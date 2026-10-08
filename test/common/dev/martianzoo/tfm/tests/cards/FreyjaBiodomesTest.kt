package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class FreyjaBiodomesTest : ProjectCardTest() {
  // FAQ: "you can still choose to take microbes"
  @Test
  internal fun `Can be played without another eligible Venus card`() {
    kim.setToExMachina(5, "VenusStep")
    kim.exMachina("$VenusianAnimals, Animal<$VenusianAnimals>")
    kim.assertCounts(1 to "Animal<$VenusianAnimals>")

    kim.playProject(FreyjaBiodomes, 14) {
          // Decline adding animals to Venusian Animals by choosing the unavailable microbe gain.
          declineTask()
        }
        .expect("PROD[-Energy, 2 MC], 0 Animal<$VenusianAnimals>")
  }
}
