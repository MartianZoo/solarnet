package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
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

  @Test
  internal fun doTaskCanDisambiguateByContextClass() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Choice {
                  CLASS Left
                  CLASS Right
                }
                CLASS SourceA
                CLASS SourceB
                """
                    .trimIndent()
            )
        )
    val agent = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val sourceA = cn("SourceA")
    val sourceB = cn("SourceB")
    agent.addTasks("Choice", Cause(sourceA.expression, triggerEvent = 0))
    agent.addTasks("Choice", Cause(sourceB.expression, triggerEvent = 0))

    agent.doTask("Left", sourceB)

    agent.count("Left") shouldBe 1
    agent.tasks.extract { it.cause?.context?.className } shouldBe listOf(sourceA)
  }

  @Test
  internal fun tryDoesNotHideInvalidNarrowingsOrDeadEnds() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Choice {
                  CLASS Left
                  CLASS Right
                }
                CLASS Other
                ABSTRACT CLASS Empty
                """
                    .trimIndent()
            )
        )
    val agent = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val choice = agent.addTasks("Choice").single()
    val empty = agent.addTasks("Empty").single()

    shouldThrow<NarrowingException> { agent.tryTask("Other", choice) }
    val deadEnd = shouldThrow<DeadEndException> { agent.tryTask(empty) }

    deadEnd.message.orEmpty().contains("uninhabited type") shouldBe true
    agent.tasks.ids() shouldBe setOf(choice, empty)
  }
}
