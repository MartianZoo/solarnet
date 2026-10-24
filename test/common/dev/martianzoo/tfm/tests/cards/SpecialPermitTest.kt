package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class SpecialPermitTest : TfmSandboxTest() {
  @Test
  internal fun `Takes all four plants from the chosen opponent`() {
    newTestGame("SpecialPermit, TurmoilExpansion")
    stan.exMachina("4 Plant")
    rob.exMachina("4 Plant")

    kim.inTurn {
          doTasks(
              *playCardTasks("SpecialPermit"),
              "5 Pay<> FROM MC",
              "SpecialPermit FROM ProjectCard",
              "4 Plant<Kim FROM Stan>",
          )
        }
        .expect("0 Plant<Rob>")
  }
}
