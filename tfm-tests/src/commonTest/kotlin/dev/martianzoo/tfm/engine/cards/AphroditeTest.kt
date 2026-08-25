package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

internal class AphroditeTest : CardTest() {
  @Test
  internal fun `Triggers when an opponent raises Venus`() {
    newGame(VenusNextExpansion)
    val p2 = requireP2()
    p2.manual("$Aphrodite")
    p1.manual("VenusStep").expect("2 M<Player2>")
  }
}
