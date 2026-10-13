package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class InterplanetaryTradeTest : TfmSandboxTest() {
  @Test
  internal fun `Ignores the tags of a previously played event`() {
    newTestGame()
    kim.exMachina("$Ecoline, $Mine, $SearchForLife")
    kim.setToExMachina(43, "MC")
    kim.inTurn {
      doTasks("PlayProject<Class<ImportedHydrogen>>", "-16 MC")
      selectTask("-X? Titanium")
      doTasks(
          "Ok",
          "ImportedHydrogen FROM ProjectCard",
          "3 Plant",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
      doTask("PlayedEvent<Class<ImportedHydrogen>> FROM ImportedHydrogen")
    }

    kim.inTurn {
          doTasks("PlayProject<Class<InterplanetaryTrade>>", "-27 MC")
          selectTask("-X? Titanium")
          doTasks("Ok", "InterplanetaryTrade FROM ProjectCard")
          selectOnlyTask()
        }
        .expect("PROD[4 MC]")
  }
}
