package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import kotlin.test.Test

class ImmigrantCityTest : CardTest() {
  // FAQ: "place the city, gain 1 M€ production, then lose 2 M€ production thereafter"
  @Test
  fun `at the production floor, adds Immigrant City to Manutech`() {
    newGame("BMV")
    p1.manual("Manutech, PROD[-4, Energy]")
    p1.manual("ImmigrantCity") { doTask("CityTile<Tharsis_7_4>") }
        .expect("PROD[-Megacredit, -Energy], Megacredit, CityTile<Tharsis_7_4>")
    p1.assertProds(-5 to "Megacredit", 0 to "Energy")
  }
}
