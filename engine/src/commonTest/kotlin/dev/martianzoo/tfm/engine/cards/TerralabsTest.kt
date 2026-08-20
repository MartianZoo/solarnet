package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class TerralabsTest : CardTest() {

  @Test
  fun `with Terralabs, buys cards`() {
    newGame(TurmoilCardPack)
    p1.playCorp(TerraLabsResearch, 10)
    p1.manual("4 BuyCard").expect("4 ProjectCard, -4")
  }

  @Test
  fun `Terralabs and Polyphemos cancel each other's card-purchase modifiers`() {
    newGame(
        TurmoilCardPack,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("$TerraLabsResearch, $Polyphemos")

    p1.manual("BuyCard").expect("ProjectCard, -3 Megacredit")
  }
}
