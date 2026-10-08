package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.HomeostasisBureau
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class HomeostasisBureauTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Pays when its owner raises temperature`() {
    kim.exMachina("$HomeostasisBureau")

    kim.stdProject("AsteroidProject").expect("TemperatureStep, TerraformRating, -11 MC")
  }

  @Test
  internal fun `Does not pay when an opponent raises temperature`() {
    kim.exMachina("$HomeostasisBureau")

    stan.stdProject("AsteroidProject").expect("0 MC<Kim>")
  }
}
