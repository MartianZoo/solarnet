package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.VenusianAnimals
import dev.martianzoo.tfm.tests.cards.cardnames.VenusianPlants
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class VenusianPlantsTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can choose unavailable microbes instead of available animals`() {
    kim.setToExMachina(8, "VenusStep")
    kim.exMachina("$VenusianAnimals")

    // Unlike Corroder Suits, the microbe arm can resolve to nothing despite the animal holder.
    kim.playProject(VenusianPlants, 13) { declineTask() }
        .expect("VenusStep, 0 Animal<$VenusianAnimals>")
  }
}
