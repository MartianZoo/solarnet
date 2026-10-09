package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class NoctisCityTest : CardTest() {
  @Test
  internal fun `Can be placed anywhere on Hellas`() {
    newGame(Hellas)
    p1.runOperation("PROD[Energy]")
    p1.runOperation("$NoctisCity") {
          placeTile(1, 3)
        }
        .expect("PROD[3 MC, -Energy]")
  }
}
