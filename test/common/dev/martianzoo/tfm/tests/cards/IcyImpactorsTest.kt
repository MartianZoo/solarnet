package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.IcyImpactors
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class IcyImpactorsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `First player chooses an ocean placed by the card owner`() {
    stan.exMachina("$IcyImpactors, Asteroid<$IcyImpactors>")

    stan
        .inTurn {
          doTask("UseCardAction<IcyImpactors, Action2>")
          selectOnlyTask()
          selectOnlyTask()
          selectOnlyTask()
          kim.doTask("OceanTile<Tharsis_2_6> BY Stan")
          selectOnlyTask()
          doTask("TerraformRating")
          selectOnlyTask()
        }
        .expect("0 TerraformRating<Kim>")
  }
}
