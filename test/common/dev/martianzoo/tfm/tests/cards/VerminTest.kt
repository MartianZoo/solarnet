package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VerminTest : TfmSandboxTest() {
  @Test
  internal fun `Another player's city placement adds an animal`() {
    newTestGame()
    kim.exMachina("$Vermin")

    stan
        .inTurn {
          doTasks(
              *standardProjectTasks("CityProject"),
              "25 Pay<> FROM MC",
              "PROD[MC]",
              "CityTile<Tharsis_2_1>",
          )
          // The active player chooses when to hand this task to its owner.
          giveTask(kim, "Animal<$Vermin<Kim>>")
        }
        .expect("Animal<$Vermin<Kim>>")
  }

  @Test
  internal fun `Its action can add a microbe to another card`() {
    newTestGame()
    kim.exMachina("$Vermin, $Decomposers")

    kim.inTurn {
          doTasks(*cardActionTasks("$Vermin"), "Microbe<$Decomposers>*")
        }
        .expect("Microbe<$Decomposers>")
  }

  @Test
  internal fun `Ten animals cost every player a point per owned city`() {
    newTestGame()
    kim.exMachina(
        "$Vermin, 10 Animal<$Vermin>, NormalCityTile<Tharsis_2_1>, NormalCityTile<Tharsis_2_3>"
    )
    stan.exMachina("NormalCityTile<Tharsis_3_2>")
    rob.exMachina("NormalCityTile<Tharsis_3_3>")

    victoryPoints {
      // Expand the per-player deductions; Admin applies the scoring changes.
      kim.doTask("EACH")
    } shouldBe listOf(18, 19, 19)
  }
}
