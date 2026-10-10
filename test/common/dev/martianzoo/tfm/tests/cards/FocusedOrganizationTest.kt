package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class FocusedOrganizationTest : TfmSandboxTest() {
  @Test
  internal fun `May gain a different resource than it spends`() {
    newTestGame("FocusedOrganization")
    kim.exMachina("$FocusedOrganization, Steel")

    kim.cardAction1(FocusedOrganization) {
          doTask("-Steel")
          doTask("Plant")
        }
        .expect("-Steel, Plant, 0 ProjectCard")
  }
}
