package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ProductionPhaseTest {
  @Test
  internal fun playerConvertsExistingEnergyBeforeNewEnergyProduction() {
    val game = setUpGame()
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    p1.runOperation("2 Energy, PROD[Energy]")
    val manual = admin
    p1.autoExecPolicy = NONE

    manual.beginOperation("ProductionPhase FROM Phase") {
      p1.count("Energy") shouldBe 2
      p1.count("Heat") shouldBe 0
      p1.doTasks("2 Heat FROM Energy", "Energy")
    }

    p1.count("Energy") shouldBe 1
    p1.count("Heat") shouldBe 2
  }

  // Unresolved adjudication (question 19): this exercises real production from a prepared position.
  // https://boardgamegeek.com/thread/1705209/article/44044598#44044598
  @Test
  internal fun `Negative combined income currently removes money when cash covers it`() {
    val game = setUpGame()
    val admin = game.testTfm(ADMIN)
    val kim = game.testTfm(PLAYER1)
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
    val admin = game.testTfm(ADMIN)
    val kim = game.testTfm(PLAYER1)
    kim.runOperation("MC, PROD[-3 MC], -19 TerraformRating")

    shouldThrow<LimitsException> { admin.beginOperation("ProductionPhase FROM Phase") }
    kim.count("MC") shouldBe 1
  }
}
