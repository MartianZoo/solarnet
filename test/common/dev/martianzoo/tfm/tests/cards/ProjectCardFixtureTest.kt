package dev.martianzoo.tfm.tests.cards

import io.kotest.assertions.throwables.shouldThrow
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

  @Test
  internal fun `Two-player games fail clearly only when Rob is used`() {
    newTestGame(playerCount = 2)

    players.size shouldBe 2
    kim.actor.toString() shouldBe "Kim"
    stan.actor.toString() shouldBe "Stan"
    shouldThrow<IllegalStateException> { rob }.message shouldBe "Rob is sitting this game out"
  }

  @Test
  internal fun `Four-player games prepare a fourth beginner corporation`() {
    newTestGame(playerCount = 4)

    players.size shouldBe 4
    players[3].actor.toString() shouldBe "Maya"
    players[3].count("BeginnerCorporation4") shouldBe 1
  }

  @Test
  internal fun `Five-player games prepare a fifth beginner corporation`() {
    newTestGame(playerCount = 5)

    players.size shouldBe 5
    players[4].actor.toString() shouldBe "Nadia"
    players[4].count("BeginnerCorporation5") shouldBe 1
  }
}
