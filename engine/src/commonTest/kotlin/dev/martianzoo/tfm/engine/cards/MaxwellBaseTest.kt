package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class MaxwellBaseTest : CardTest() {
  @Test
  fun `Can add a floater to another Venus card`() {
    newGame(VenusNextExpansion)
    engine.phase("Action")
    p1.manual("PROD[Energy], $ForcedPrecipitation")
    p1.manual("$MaxwellBase").expect("CityTile<MaxwellBase_RemoteArea>, PROD[-Energy]")
    p1.cardAction1(MaxwellBase) { addCardResources(ForcedPrecipitation) }.expect("Floater")
  }
}
