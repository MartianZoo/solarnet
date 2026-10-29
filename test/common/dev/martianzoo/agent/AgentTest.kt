package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
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
  internal fun incompleteExecutionAnalysisIsReportedByQueriesAndEveryTryOverload() {
    val game = Engine.newGame(testGamePremise())
    val agent = Agents(game) { NONE }[PLAYER1]
    // Unbound numeric domains are currently outside the engine's finite proof.
    val task = agent.addTasks("X Token").single()
    val pending = agent.tasks.getTaskData(task)
    val checkpoint = game.timeline.checkpoint()
    val events = game.events.nextOrdinal

    val attempts: List<() -> Unit> =
        listOf(
            { agent.canExecuteTask(task) },
            { agent.tryTask(task) },
            { agent.tryTask("X Token") },
            { agent.tryTask("X Token", task) },
        )
    for (attempt in attempts) {
      shouldThrow<IncompleteAnalysisException> { attempt() }
      agent.tasks.getTaskData(task) shouldBe pending
      game.timeline.checkpoint() shouldBe checkpoint
      game.events.nextOrdinal shouldBe events
      agent.count("Token") shouldBe 0
    }

    agent.doTask("2 Token", task)
    agent.count("Token") shouldBe 2
    agent.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun failedTryRestoresImmediateEffectsAndTheOriginalTask() {
    for (byId in listOf(false, true)) {
      val game =
          Engine.newGame(
              testGamePremise(
                  """
                  CLASS Offered { This:: Notice!, -Missing! }
                  CLASS Notice
                  CLASS Missing
                  """
              )
          )
      val agent = Agents(game) { NONE }[PLAYER1]
      val task = agent.addTasks("Offered!").single()
      val pending = agent.tasks.getTaskData(task)
      val before = game.events.nextOrdinal

      if (byId) agent.tryTask(task) else agent.tryTask("Offered!", task)

      game.events.nextOrdinal shouldBe before
      agent.tasks.getTaskData(task) shouldBe pending
      agent.count("Offered") shouldBe 0
      agent.count("Notice") shouldBe 0
    }
  }

  @Test
  internal fun tryRestoresAnUnfinishedHandoffBeforeAdminRuns() {
    val game = Engine.newGame(testGamePremise("CLASS SystemToken : System\nCLASS Token"))
    val agent = Agents(game) { NONE }[PLAYER1]
    val task = agent.addTasks("SystemToken OR Token").single()
    val pending = agent.tasks.getTaskData(task)
    val before = game.events.nextOrdinal

    agent.tryTask("SystemToken", task)

    game.events.nextOrdinal shouldBe before
    agent.tasks.getTaskData(task) shouldBe pending
    agent.count("SystemToken") shouldBe 0
    game.tasks.selectedTask() shouldBe null
  }

  @Test
  internal fun unavailableMetricRequestLeavesItsTaskPending() {
    val game = Engine.newGame(testGamePremise("CLASS Token\nCLASS Unit"))
    val agent = Agents(game) { NONE }[PLAYER1]
    agent.exMachina("Unit")
    val task = agent.addTasks("-Token! / Unit").single()
    val pending = agent.tasks.getTaskData(task)
    val before = game.events.nextOrdinal

    agent.tryTask("-Token! / Unit")

    game.events.nextOrdinal shouldBe before
    agent.tasks.getTaskData(task) shouldBe pending
  }

  @Test
  internal fun aFormCannotExecuteADifferentAlternativeAfterSelectionResolvesTheTask() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Token
                CLASS Marker
                """
                    .trimIndent()
            )
        )
    val agent = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val taskId = agent.addTasks("Token OR -Marker").single()
    val taskBefore = agent.tasks.getTaskData(taskId)
    val form = agent.fillInTask(taskId)
    form.narrow("-Marker")

    shouldThrow<NarrowingException> { form.commit() }

    agent.tasks.getTaskData(taskId) shouldBe taskBefore
    agent.count("Token") shouldBe 0
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
