package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
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
  internal fun `Does not pay its production bonus after a TR increase`() {
    newGame(TurmoilCardPack)
    p1.manual("$Pristar, TerraformRating")
    engine.phase("Production")
    p1.assertCounts(0 to "Preservation")
  }
}
