package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SpinOffDepartmentTest : CardTest() {
  @Test
  fun `Triggers on a 20-cost card but not cheaper cards`() {
    newGame(
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.manual("$SpinOffDepartment")
    p1.manual("$Mine")
    p1.count("ProjectCard") shouldBe 0
    p1.manual("$EarthCatapult").expect("ProjectCard")
  }
}
