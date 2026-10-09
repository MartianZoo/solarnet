package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.cards.cardnames.Manutech
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
  internal fun `Kim can use a live corporation while the other players remain beginners`() {
    newTestGame(kimCorporation = Manutech)

    kim.count("$Manutech") shouldBe 1
    kim.count("BeginnerCorporation") shouldBe 0
    kim.production(cn("Steel")) shouldBe 2
    kim.count("Steel") shouldBe 1
    stan.count("BeginnerCorporation2") shouldBe 1
    rob.count("BeginnerCorporation3") shouldBe 1
  }

  @Test
  internal fun `Absolute correction accepts production types`() {
    newTestGame()

    kim.setToExMachina(0, "PROD[Energy]")
    kim.setToExMachina(3, "PROD[Heat]")

    kim.production(cn("Energy")) shouldBe 0
    kim.production(cn("Heat")) shouldBe 3
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
