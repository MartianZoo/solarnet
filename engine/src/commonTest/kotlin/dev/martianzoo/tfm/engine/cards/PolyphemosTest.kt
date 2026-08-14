package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import kotlin.test.Test

class PolyphemosTest : CardTest() {

  @Test
  fun `with Polyphemos, buys a card from Inventors Guild`() {
    newGame(
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.playCorp("Polyphemos", 7)
    engine.phase("Action")
    p1.playProject("InventorsGuild", 9)
    p1.cardAction1("InventorsGuild") { doTask("BuyCard") }.expect("ProjectCard, -5")
  }
}
