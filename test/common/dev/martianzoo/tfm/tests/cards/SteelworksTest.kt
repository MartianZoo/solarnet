package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SteelworksTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "used even after the oxygen has been maxed out"
  @Test
  internal fun `Can be used after oxygen is maxed`() {
    kim.exMachina("$Steelworks")
    kim.setToExMachina(13, "OxygenStep")
    kim.setToExMachina(4, "Energy")
    kim.inTurn {
      doTasks("UseStandardProject<GreeneryProject>", "-23 MC", "GreeneryTile<Tharsis_5_2>")
      selectTask("OxygenStep.")
      doTasks("2 Plant", "TerraformRating")
    }

    kim.inTurn {
          doTasks("UseCardAction<Steelworks, Action1>", "-Energy / Owed<Class<Energy>>", "2 Steel")
          selectTask("OxygenStep.")
        }
        .expect("-4 Energy, 0 OxygenStep, 0 TerraformRating")
  }
}
