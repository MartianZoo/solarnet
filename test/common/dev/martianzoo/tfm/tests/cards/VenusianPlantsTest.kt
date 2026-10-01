package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.VenusianAnimals
import dev.martianzoo.tfm.tests.cards.cardnames.VenusianPlants
import kotlin.test.Test

internal class VenusianPlantsTest : CardTest() {
  @Test
  internal fun `Can choose unavailable microbes instead of available animals`() {
    newGame(VenusNextExpansion)
    p1.runOperation("$VenusianAnimals")

    // Unlike Corroder Suits, the microbe arm can resolve to nothing despite the animal holder.
    p1.runOperation("$VenusianPlants") { declineTask() }
        .expect("VenusStep, 0 Animal<$VenusianAnimals>")
  }
}
