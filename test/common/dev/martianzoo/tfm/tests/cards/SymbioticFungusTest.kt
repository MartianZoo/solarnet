package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SymbioticFungusTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  // FAQ: "the microbe obtained would be discarded"
  @Test
  internal fun `Can use its action without an eligible target`() {
    kim.exMachina("$SymbioticFungus")

    kim.inTurn {
          doTasks(*cardActionTasks("SymbioticFungus"))
          // Resolve the microbe offer: no eligible card can hold it.
          selectTask("Microbe")
        }
        .expect("0 Microbe")
  }
}
