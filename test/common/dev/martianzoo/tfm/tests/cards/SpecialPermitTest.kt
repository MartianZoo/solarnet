package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class SpecialPermitTest : TfmSandboxTest() {
  @Test
  internal fun `Takes all four plants from the chosen opponent`() {
    newTestGame("SpecialPermit, TurmoilExpansion")
    stan.exMachina("4 Plant")
    rob.exMachina("4 Plant")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<SpecialPermit>>",
              "-5 MC",
              "SpecialPermit FROM ProjectCard",
              "4 Plant<Kim FROM Stan>",
          )
          doTask("PlayedEvent<Class<SpecialPermit>> FROM SpecialPermit")
        }
        .expect("0 Plant<Rob>")
  }
}
