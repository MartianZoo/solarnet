package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SpinOffDepartmentTest : CardTest() {
  @Test
  internal fun `Triggers on a 20-cost card but not cheaper cards`() {
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
