package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class FocusedOrganizationTest : TfmSandboxTest() {
  @Test
  internal fun `May gain a different resource than it spends`() {
    newTestGame("FocusedOrganization")
    kim.exMachina("$FocusedOrganization, Steel")

    kim.inTurn {
          doTask("UseCardAction<FocusedOrganization, Action1>")
          selectTask("-ProjectCard<Kim, Hand>!")
          selectOnlyTask()
          doTask("-Steel")
          selectTask("ProjectCard<Kim, Hand>!")
          selectTask("StandardResource!")
          doTask("Plant")
        }
        .expect("0 ProjectCard")
  }
}
