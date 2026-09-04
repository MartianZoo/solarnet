package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class PristarTest : CardTest() {
  @Test
  internal fun `Pays its production bonus when TR did not increase`() {
    newGame(TurmoilCardPack)
    p1.manual("$Pristar")
    engine.phase("Production")
    p1.assertCounts(1 to "Preservation")
  }

  @Test
  internal fun `An opponent's TR increase does not suppress the bonus`() {
    newGame(TurmoilCardPack, players = 2)
    p1.manual("$Pristar")
    requireP2().manual("TerraformRating")
    engine.phase("Production")
    p1.assertCounts(1 to "Preservation")
  }

  @Test
  internal fun `Does not pay its production bonus after a TR increase`() {
    newGame(TurmoilCardPack)
    p1.manual("$Pristar, TerraformRating")
    engine.phase("Production")
    p1.assertCounts(0 to "Preservation")
  }
}
