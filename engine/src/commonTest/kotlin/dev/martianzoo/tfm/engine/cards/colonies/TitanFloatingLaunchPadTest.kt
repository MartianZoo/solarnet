package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class TitanFloatingLaunchPadTest : ColoniesCardTest() {
  @Test
  fun `Can fund a trade with two floaters`() {
    p1.manual("$TitanFloatingLaunchPad") { addCardResources(TitanFloatingLaunchPad) }
    p1.cardAction2(TitanFloatingLaunchPad) {
          doTask("Trade<Io>")
        }
        .expect("-Floater, 3 Heat")
  }
}
