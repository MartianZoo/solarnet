package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class MaxwellBaseTest : CardTest() {
  @Test
  fun `with another Venus card, uses Maxwell Base`() {
    newGame(VenusNextExpansion)
    engine.phase("Action")
    p1.manual("PROD[Energy], $ForcedPrecipitation")
    p1.manual("$MaxwellBase").expect("CityTile<Card238_RemoteArea>, PROD[-Energy]")
    p1.cardAction1(MaxwellBase) { doTask("Floater<$ForcedPrecipitation>") }.expect("Floater")
  }
}
