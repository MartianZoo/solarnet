package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Current callback behavior; failure recovery remains an open design question. */
internal class AgentCallCharacterizationTest {
  @Test
  fun failingCompletionCallbackLeavesEarlierTransactionsAndExternalEffects() {
    val world = Engine.newGame(testGamePremise("CLASS Foo"))
    val agent = Agents(world)[PLAYER1]
    var notifications = 0
    world.onTransactionComplete = {
      notifications++
      error("application failure after the initial work")
    }

    shouldThrow<IllegalStateException> { agent.runOperation("Foo!") }

    agent.count("Foo") shouldBe 1
    world.tasks.isEmpty() shouldBe true
    notifications shouldBe 1
  }

  @Test
  fun resultIncludesAdminHandlingAndNestedCallbackCommands() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        CLASS Foo { This: Reward BY Admin }
        CLASS Reward
        CLASS Bar
        """
            )
        )
    val agent = Agents(world)[PLAYER1]
    var notifications = 0
    world.onTransactionComplete = {
      notifications++
      agent.runOperation("Bar!")
    }

    val result = agent.runOperation("Foo!")

    agent.count("Foo") shouldBe 1
    agent.count("Reward") shouldBe 1
    agent.count("Bar") shouldBe 1
    result.changes.size shouldBe 3
    notifications shouldBe 1
  }
}
