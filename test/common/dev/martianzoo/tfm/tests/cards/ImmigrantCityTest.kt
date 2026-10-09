package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ImmigrantCityTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame(kimCorporation = Manutech)

  // At -4 M€ production, the reduction is unavailable until the city trigger raises production.
  @Test
  internal fun `Can be played at the production floor when Manutech offsets its loss`() {
    kim.setToExMachina(0, "PROD[MC]")
    kim.exMachina("PROD[-4 MC]")
    kim.setToExMachina(13, "MC")

    kim.playProject(ImmigrantCity, 13) { placeTile(7, 4) }
        .expect("PROD[-MC, -Energy], -12 MC, CityTile<Tharsis_7_4>")
  }
}
