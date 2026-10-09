package dev.martianzoo.tfm.tests.cards

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ProjectCardFixtureTest : ProjectCardTest() {
  @Test
  internal fun `Forked games remain isolated`() {
    newTestGame()
    val firstKim = kim
    firstKim.exMachina("Plant")

    newTestGame()
    kim.count("Plant") shouldBe 0
    kim.exMachina("2 Plant")

    firstKim.count("Plant") shouldBe 1
    kim.count("Plant") shouldBe 2
  }
}
