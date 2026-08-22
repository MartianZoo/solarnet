package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.cardnames.ForcedPrecipitation
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class TfmTaskHelpersTest : CardTest() {
  @Test
  fun `Tile placement rejects multiple pending placements`() {
    newGame()

    p1.godMode().addTasks("OceanTile<WaterArea>, OceanTile<WaterArea>")

    shouldThrow<IllegalArgumentException> { p1.placeTile(1, 2) }
  }

  @Test
  fun `Declining rejects multiple declinable tasks`() {
    newGame()

    p1.godMode().addTasks("Plant?, Steel?")

    shouldThrow<IllegalArgumentException> { p1.declineTask() }
  }

  @Test
  fun `Card resources reject multiple pending placements`() {
    newGame(VenusNextExpansion)
    p1.manual("$ForcedPrecipitation")

    p1.godMode().addTasks("Floater?, Floater?")

    shouldThrow<IllegalArgumentException> { p1.addCardResources(ForcedPrecipitation) }
  }
}
