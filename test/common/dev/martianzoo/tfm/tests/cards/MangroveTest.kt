package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MangroveTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "placed on any ocean area even if it is not adjacent"
  @Test
  internal fun `Can be placed on an ocean area despite a distant city`() {
    kim.setToExMachina(17, "TemperatureStep")
    kim.exMachina("NormalCityTile<Kim, Tharsis_9_5>")

    kim.playProject(Mangrove, 12) { placeTile(1, 2) }
        .expect("GreeneryTile<Tharsis_1_2>, OxygenStep, TerraformRating")
  }
}
