package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class FloatingRefineryTest : TfmSandboxTest() {
  @Test
  internal fun `Counts its own Venus tag when gaining starting floaters`() {
    newTestGame("FloatingRefinery")
    kim.exMachina("$ForcedPrecipitation")

    kim.playProject(FloatingRefinery, 7).expect("2 Floater<$FloatingRefinery>")
  }
}
