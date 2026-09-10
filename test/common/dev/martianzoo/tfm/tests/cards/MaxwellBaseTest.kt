package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MaxwellBaseTest : CardTest() {
  @Test
  internal fun `Can add a floater to another Venus card`() {
    newGame(VenusNextExpansion)
    admin.phase("Action")
    p1.runOperation("PROD[Energy], $ForcedPrecipitation")
    p1.runOperation("$MaxwellBase").expect("CityTile<MaxwellBase_RemoteArea>, PROD[-Energy]")
    p1.cardAction1(MaxwellBase) { addCardResources(ForcedPrecipitation) }.expect("Floater")
  }
}
