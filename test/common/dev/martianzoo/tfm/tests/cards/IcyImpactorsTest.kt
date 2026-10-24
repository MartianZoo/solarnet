package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
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
          doTasks(*cardActionTasks("IcyImpactors", "Action2"))
          doTask("-Asteroid<IcyImpactors>")
          // Expand the start-player choice; its System signal is executed by Admin.
          selectOnlyTask()
          kim.selectTask("OceanTile<Tharsis_2_6> BY Stan")
          doTask("OceanTile<Tharsis_2_6>")
          doTask("TerraformRating")
          doTask("2 ProjectCard")
        }
        .expect("0 TerraformRating<Kim>")
  }
}
