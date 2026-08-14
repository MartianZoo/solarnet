package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.*
import kotlin.test.Test

class TerralabsTest : CardTest() {

  @Test
  fun `with Terralabs, buys cards`() {
    newGame(TurmoilCardPack)
    p1.playCorp("TerraLabsResearch", 10)
    p1.manual("4 BuyCard").expect("4 ProjectCard, -4")
  }
}
