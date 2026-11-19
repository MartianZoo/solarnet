package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ProductionPhaseTest {

  // Unresolved adjudication (question 19): this exercises real production from a prepared position.
  // https://boardgamegeek.com/thread/1705209/article/44044598#44044598
  @Test
  internal fun `Negative combined income currently removes money when cash covers it`() {
    val game = setUpGame()
    val agents = Agents(game) { NONE }
    val admin = agents[ADMIN]
    val kim = agents[PLAYER1]
    kim.exMachina("3 MC, PROD[-3 MC], -19 TerraformRating")

    // Start the phase directly; earlier turns are irrelevant to this income calculation.
    admin.beginOperation("ProductionPhase FROM Phase")
    kim.doTask("-2 MC")
    agents[PLAYER2].doTask("20 MC")
    admin.count("ProductionPhase") shouldBe 0
  }

  // Unresolved adjudication (question 19): current production cannot finish with insufficient cash.
  // https://boardgamegeek.com/thread/1705209/article/44044598#44044598
  @Test
  internal fun `Negative combined income currently fails instead of stopping at zero cash`() {
    val game = setUpGame()
    val agents = Agents(game) { NONE }
    val admin = agents[ADMIN]
    val kim = agents[PLAYER1]
    kim.exMachina("MC, PROD[-3 MC], -19 TerraformRating")

    admin.beginOperation("ProductionPhase FROM Phase")
    shouldThrow<LimitsException> { kim.doTask("-2 MC") }
    kim.count("MC") shouldBe 1
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Production converts existing energy before producing new resources`() {
      newTestGame(playerCount = 2, addOptions = "-VenusNextExpansion")
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat") }
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
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
