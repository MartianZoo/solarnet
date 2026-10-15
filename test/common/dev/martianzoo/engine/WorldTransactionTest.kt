package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldTransactionTest {
  @Test
  internal fun nestedOperationsAcrossActorsReportOnlyTheOutermostCompletion() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.testAgent(PLAYER1)
    val player2 = game.testAgent(PLAYER2)
    var completions = 0
    game.onTransactionComplete = { completions++ }

    player1.runOperation("Ok") { player2.runOperation("Ok") }

    completions shouldBe 1

    player1.runOperation("Ok")

    completions shouldBe 2
  }

  @Test
  internal fun outerTransactionSettlesEveryAgentsPolicyAfterNestedCalls() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val player2 = game.testAgent(PLAYER2)

    player2.addTasks("Token")
    player1.runOperation("Ok") { player2.autoExecNow() }

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun manualTaskEditsReportAtomicCompletion() {
    val game = Engine.newGame(testGamePremise())
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    agent.sneak("Token")
    var completions = 0
    game.onTransactionComplete = { completions++ }

    val taskId = agent.addTasks("-Token?").single()
    agent.selectTask(taskId)
    agent.narrowTask("-Token")
    agent.dropTask(agent.addTasks("Token?").single())

    completions shouldBe 5
    agent.count("Token") shouldBe 0
    agent.tasks.isEmpty() shouldBe true
  }
}
