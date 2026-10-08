package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MaxwellBaseTest : ProjectCardTest() {
  @Test
  internal fun `Can add a floater to another Venus card`() {
    kim.setToExMachina(6, "VenusStep")
    kim.exMachina("$ForcedPrecipitation")

    kim.playProject(MaxwellBase, 18).expect("CityTile<MaxwellBase_RemoteArea>, PROD[-Energy]")
    kim.cardAction1(MaxwellBase) { addCardResources(ForcedPrecipitation) }.expect("Floater")
  }
}
