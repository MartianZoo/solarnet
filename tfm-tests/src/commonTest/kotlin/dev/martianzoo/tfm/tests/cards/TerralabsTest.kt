package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TerralabsTest : CardTest() {

  @Test
  internal fun `Buys project cards for one mc each`() {
    newGame(TurmoilCardPack)
    p1.playCorp(TerraLabsResearch, 10)
    p1.manual("Selecting THEN 4 ProjectCard<Selecting> THEN BuySelectedCards") {
          p1.pay(mc = 4)
        }
        .expect("4 ProjectCard, -4 MC")
  }

  @Test
  internal fun `Terralabs and Polyphemos cancel each other's card-purchase modifiers`() {
    newGame(
        TurmoilCardPack,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("$TerraLabsResearch, $Polyphemos")

    p1.manual("Selecting THEN ProjectCard<Selecting> THEN BuySelectedCards") {
          p1.pay(mc = 3)
        }
        .expect("ProjectCard, -3 MC")
  }
}
