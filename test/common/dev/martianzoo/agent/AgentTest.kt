package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AgentTest {
  @Test
  internal fun oneStableAgentPerActorWithActorScopedViewsAndTaskCommands() {
    val game = Engine.newGame(testGamePremise())
    val agents = Agents(game)
    (agents.world === game) shouldBe true
    game.actors.map { agents[it].actor } shouldBe game.actors
    game.actors.map { agents[it].autoExecPolicy }.toSet() shouldBe setOf(EAGER)
    (game.actorEngine(PLAYER1) === game.actorEngine(PLAYER1)) shouldBe true
    val agent = agents[PLAYER1].also { it.autoExecPolicy = NONE }

    (agent === agents[PLAYER1]) shouldBe true
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
    val agent = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val taskId = agent.addTasks("Token?").single()
    val taskBefore = agent.tasks.getTaskData(taskId)

    agent.canExecuteTask(taskId) shouldBe false
    agent.tryTask(taskId)

    agent.tasks.getTaskData(taskId) shouldBe taskBefore
  }
}
