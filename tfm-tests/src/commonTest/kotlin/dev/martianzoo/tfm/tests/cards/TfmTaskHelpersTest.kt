package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.ForcedPrecipitation
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class TfmTaskHelpersTest : CardTest() {
  @Test
  internal fun `Tile placement rejects multiple pending placements`() {
    newGame()

    p1.godMode().addTasks("OceanTile<WaterArea>, OceanTile<WaterArea>")

    shouldThrow<IllegalArgumentException> { p1.placeTile(1, 2) }
  }

  @Test
  internal fun `Declining rejects multiple declinable tasks`() {
    newGame()

    p1.godMode().addTasks("Plant?, Steel?")

    shouldThrow<IllegalArgumentException> { p1.declineTask() }
  }

  @Test
  internal fun `Card resources reject multiple pending placements`() {
    newGame(VenusNextExpansion)
    p1.manual("$ForcedPrecipitation")

    p1.godMode().addTasks("Floater?, Floater?")

    shouldThrow<IllegalArgumentException> { p1.addCardResources(ForcedPrecipitation) }
  }
}
