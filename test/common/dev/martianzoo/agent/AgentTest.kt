package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecMode.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AgentTest {
  @Test
  internal fun worldReturnsOneStableAgentWithActorScopedViewsAndTaskCommands() {
    val game = Engine.newGame(testGamePremise())
    val agent = game.agent(PLAYER1).also { it.autoExecMode = NONE }

    (agent === game.agent(PLAYER1)) shouldBe true
    (agent.reader === game.reader) shouldBe true

    val taskId = agent.addTasks("Token").single()
    agent.tasks.ids() shouldBe setOf(taskId)
    agent.canExecuteTask(taskId) shouldBe true

    agent.tryTask(taskId)

    agent.count("Token") shouldBe 1
    agent.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun executionProbeAndTryLeaveAnAbstractTaskUnchanged() {
    val game = Engine.newGame(testGamePremise())
    val agent = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    val taskId = agent.addTasks("Token?").single()
    val taskBefore = agent.tasks.getTaskData(taskId)

    agent.canExecuteTask(taskId) shouldBe false
    agent.tryTask(taskId)

    agent.tasks.getTaskData(taskId) shouldBe taskBefore
  }
}
