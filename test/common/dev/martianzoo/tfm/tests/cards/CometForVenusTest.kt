package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.CometForVenus
import dev.martianzoo.tfm.tests.cards.cardnames.Dirigibles
import kotlin.test.Test

internal class CometForVenusTest : CardTest() {
  @Test
  internal fun `Can remove MC from another Venus card owner`() {
    newGame(VenusNextExpansion)
    requireP2().runOperation("4 MC, $Dirigibles")

    p1.runOperation("$CometForVenus") { doTask("-4 MC<Player2>") }.expect("-4 MC<Player2>")
  }
}
