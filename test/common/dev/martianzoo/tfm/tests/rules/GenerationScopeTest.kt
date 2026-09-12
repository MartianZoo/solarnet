package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GenerationScopeTest {
  @Test
  internal fun generationReplacesItsScopeAndDropsScopedComponents() {
    val game = setUpGame()
    val admin = game.testAgent(ADMIN)
    val player = game.testAgent(PLAYER1)

    player.runOperation("Pass")
    val beforeGeneration = game.timeline.checkpoint()

    admin.runOperation("Generation")

    admin.count("GenerationScope") shouldBe 1
    player.count("Pass") shouldBe 0

    game.timeline.rollBack(beforeGeneration)

    admin.count("GenerationScope") shouldBe 1
    player.count("Pass") shouldBe 1
  }
}
