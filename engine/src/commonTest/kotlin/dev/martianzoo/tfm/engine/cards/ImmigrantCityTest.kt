package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class ImmigrantCityTest : CardTest() {
  // At -4 M€ production, the reduction is unavailable until the city trigger raises production.
  @Test
  fun `Can be played at the production floor when Manutech offsets its loss`() {
    newGame(VenusNextExpansion)
    p1.manual("$Manutech, PROD[-4, Energy]")
    p1.manual("$ImmigrantCity") { doTask("CityTile<Tharsis_7_4>") }
        .expect("PROD[-Megacredit, -Energy], Megacredit, CityTile<Tharsis_7_4>")
    p1.assertProds(-5 to "Megacredit", 0 to "Energy")
  }
}
