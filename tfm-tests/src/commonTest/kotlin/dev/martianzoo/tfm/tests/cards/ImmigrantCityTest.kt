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
    p1.manual("$Manutech, PROD[-4 MC, Energy]")
    p1.manual("$ImmigrantCity") { placeTile(7, 4) }
        .expect("PROD[-1 MC, -Energy], 1 MC, CityTile<Tharsis_7_4>")
    p1.assertProds(-5 to "MC", 0 to "Energy")
  }
}
