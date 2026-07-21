package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class ImmigrantCityTest : CardTest() {
  // FAQ: "place the city, gain 1 M€ production, then lose 2 M€ production thereafter"
  @Test
  fun `Immigrant City can rescue Manutech from the megacredit production floor`() {
    val p1 = newGame("BRMV", 2).tfm(PLAYER1)
    p1.sneak("Manutech, PROD[-4, Energy]")

    p1.manual("ImmigrantCity") { doTask("CityTile<Tharsis_7_4>") }
        .expect("PROD[-Megacredit, -Energy], Megacredit, CityTile<Tharsis_7_4>")

    p1.assertProds(-5 to "Megacredit", 0 to "Energy")
  }
}
