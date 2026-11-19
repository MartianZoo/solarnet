package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ProductionPhaseTest {

  // Unresolved adjudication (question 19): this exercises real production from a prepared position.
  // https://boardgamegeek.com/thread/1705209/article/44044598#44044598
  @Test
  internal fun `Negative combined income currently removes money when cash covers it`() {
    val game = setUpGame()
    val admin = game.testAgent(ADMIN)
    val kim = game.testAgent(PLAYER1)
    kim.runOperation("3 MC, PROD[-3 MC], -19 TerraformRating")

    TestHelpers.assertNetChanges(
        admin.beginOperation("ProductionPhase FROM Phase"),
        game,
        "-2 MC<Player1>",
    )
  }

  // Unresolved adjudication (question 19): current production cannot finish with insufficient cash.
  // https://boardgamegeek.com/thread/1705209/article/44044598#44044598
  @Test
  internal fun `Negative combined income currently fails instead of stopping at zero cash`() {
    val game = setUpGame()
    val admin = game.testAgent(ADMIN)
    val kim = game.testAgent(PLAYER1)
    kim.runOperation("MC, PROD[-3 MC], -19 TerraformRating")

    shouldThrow<LimitsException> { admin.beginOperation("ProductionPhase FROM Phase") }
    kim.count("MC") shouldBe 1
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Production converts existing energy before producing new resources`() {
      newTestGame(playerCount = 2, addOptions = "-VenusNextExpansion")
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat") }
      players.forEach { it.doTask("4 ProjectCard<Selecting>") }
      kim.doTask("-4 ProjectCard<Selecting>")
      // The beginner's free starting cards do not make later Research purchases free.
      stan.doTasks(
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      stan.assertCounts(12 to "ProjectCard<Hand>", 0 to "ProjectCard<Selecting>", 57 to "MC")
      kim.count("Energy") shouldBe 1
      kim.count("Heat") shouldBe 1

      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.doTask("Heat FROM Energy") }
      players.forEach { it.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat") }
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

      kim.count("Energy") shouldBe 1
      kim.count("Heat") shouldBe 3
      kim.count("Steel") shouldBe 2
    }
  }
}
