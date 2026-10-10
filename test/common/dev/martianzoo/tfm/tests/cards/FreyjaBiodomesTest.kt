package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class FreyjaBiodomesTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "you can still choose to take microbes"
  @Test
  internal fun `Can choose unavailable microbes instead of available animals`() {
    kim.setToExMachina(5, "VenusStep")
    kim.exMachina("$VenusianAnimals, Animal<$VenusianAnimals>")

    kim.playProject(FreyjaBiodomes, 14) {
          // Decline adding animals to Venusian Animals by choosing the unavailable microbe gain.
          declineTask()
        }
        .expect("PROD[-Energy, 2 MC], 0 Animal<$VenusianAnimals>")
  }
}
