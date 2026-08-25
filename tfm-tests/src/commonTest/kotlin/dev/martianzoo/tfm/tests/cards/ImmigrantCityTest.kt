package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ImmigrantCityTest : CardTest() {
  // At -4 M€ production, the reduction is unavailable until the city trigger raises production.
  @Test
  internal fun `Can be played at the production floor when Manutech offsets its loss`() {
    newGame(VenusNextExpansion)
    p1.manual("$Manutech, PROD[-4, Energy]")
    p1.manual("$ImmigrantCity") { placeTile(7, 4) }
        .expect("PROD[-Megacredit, -Energy], Megacredit, CityTile<Tharsis_7_4>")
    p1.assertProds(-5 to "Megacredit", 0 to "Energy")
  }
}
