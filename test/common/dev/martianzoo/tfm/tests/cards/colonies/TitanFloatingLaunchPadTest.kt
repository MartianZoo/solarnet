package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.TitanFloatingLaunchPad
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TitanFloatingLaunchPadTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Can fund a trade with a floater`() {
    kim.exMachina("$TitanFloatingLaunchPad, Floater<$TitanFloatingLaunchPad>")

    kim.cardAction2(TitanFloatingLaunchPad) { doTask("Trade<Io>") }.expect("-Floater, 3 Heat")
  }
}
