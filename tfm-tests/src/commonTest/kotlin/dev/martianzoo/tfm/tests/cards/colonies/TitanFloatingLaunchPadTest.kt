package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TitanFloatingLaunchPadTest : ColoniesCardTest() {
  @Test
  internal fun `Can fund a trade with two floaters`() {
    p1.manual("$TitanFloatingLaunchPad") { addCardResources(TitanFloatingLaunchPad) }
    p1.cardAction2(TitanFloatingLaunchPad) { doTask("Trade<Io>") }.expect("-Floater, 3 Heat")
  }
}
