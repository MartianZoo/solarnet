package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class PolyphemosTest : CardTest() {
  @Test
  internal fun `Applies its card-purchase surcharge to Inventors Guild`() {
    newGame(
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.playCorp(Polyphemos, 7)
    engine.phase("Action")
    p1.playProject(InventorsGuild, 9)
    p1.cardAction1(InventorsGuild) { p1.buyCards(1) }.expect("ProjectCard, -5 MC")
  }
}
