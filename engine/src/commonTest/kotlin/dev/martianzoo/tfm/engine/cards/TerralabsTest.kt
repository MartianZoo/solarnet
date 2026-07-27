package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class TerralabsTest : CardTest() {

  @Test
  fun `with Terralabs, buys cards`() {
    newGame("BMT")
    p1.playCorp("TerralabsResearch", 10)
    p1.manual("4 BuyCard").expect("4 ProjectCard, -4")
  }
}
