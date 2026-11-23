package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldTransactionTest {
  @Test
  internal fun outerCallHandlesEveryAgentsPolicyAfterNestedCalls() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val agents = Agents(game) { if (it == PLAYER2) EAGER else NONE }
    val player1 = agents[PLAYER1]
    val player2 = agents[PLAYER2]

    player2.addTasks("Token")
    player1.runOperation("Ok") { player2.autoExecNow() }

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun manualTaskEditsPreserveSelectionAndDoNotExecutePlayerWork() {
    val game = Engine.newGame(testGamePremise())
    val agent = Agents(game)[PLAYER1]
    agent.exMachina("Token")

    val taskId = agent.addTasks("-Token?").single()
    agent.selectTask(taskId)
    agent.narrowTask("-Token")
    agent.count("Token") shouldBe 1
    agent.doTask(taskId)
    agent.dropTask(agent.addTasks("Token?").single())

    agent.count("Token") shouldBe 0
    agent.tasks.isEmpty() shouldBe true
  }
}
