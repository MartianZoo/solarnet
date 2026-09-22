package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GenerationScopeTest {
  @Test
  internal fun generationReplacesItsScopeAndDropsScopedComponents() {
    val game = setUpGame(TurmoilExpansion)
    val admin = game.testAgent(ADMIN)
    val player = game.testAgent(PLAYER1)

    player.runOperation("ChairmanInfluence")
    val beforeGeneration = game.timeline.checkpoint()

    admin.runOperation("Generation")

    admin.count("GenerationScope") shouldBe 1
    player.count("ChairmanInfluence") shouldBe 0

    game.timeline.rollBack(beforeGeneration)

    admin.count("GenerationScope") shouldBe 1
    player.count("ChairmanInfluence") shouldBe 1
  }
}
