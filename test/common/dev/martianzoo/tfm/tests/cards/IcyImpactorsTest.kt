package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.IcyImpactors
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class IcyImpactorsTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `First player chooses an ocean placed by the card owner`() {
    stan.exMachina("$IcyImpactors, Asteroid<$IcyImpactors>")

    stan
        .cardAction2(IcyImpactors) { kim.doTask("OceanTile<Tharsis_2_6> BY Stan") }
        .expect("TerraformRating<Stan>, 0 TerraformRating<Kim>")
  }
}
