package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
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
  internal fun executionProbeAndTryLeaveAnAbstractTaskPending() {
    val game = Engine.newGame(testGamePremise())
    val agent = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    val taskId = agent.addTasks("Token?").single()

    agent.canExecuteTask(taskId) shouldBe false
    agent.tryTask(taskId)

    val task = agent.tasks.getTaskData(taskId)
    task.selected shouldBe false
    task.whyPending shouldBe "abstract"
  }
}
