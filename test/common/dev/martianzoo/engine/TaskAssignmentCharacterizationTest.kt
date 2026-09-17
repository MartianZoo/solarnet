package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskAssignmentCharacterizationTest {
  private fun game() =
      Engine.newGame(
          testGamePremise(
              """
              CLASS Token<Owner>
              CLASS Marker<Owner>
              CLASS AdminToken
              CLASS Blocked<Owner> { HAS MAX 0 This }
              """,
              players = 2,
          )
      )

  @Test
  internal fun ordinaryActorCanOnlySeeAndExecuteTasksAssignedToIt() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p2.addTasks("Token<Player2>")

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    shouldThrow<TaskException> { p1.doTask("Token<Player2>") }

    p2.doTask("Token<Player2>")
    p2.count("Token<Player2>") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun oneAgentsPolicyDoesNotExecuteAnotherActorsTask() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }

    p2.addTasks("Token<Player2>")
    p1.autoExecPolicy = EAGER

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    p2.count("Token<Player2>") shouldBe 0
  }

  @Test
  internal fun sharedLoopRespectsEveryAssigneesPolicy() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    p2.addTasks("Token<Player2>")
    admin.addTasks("AdminToken")
    p1.autoExecNow()

    admin.count("AdminToken") shouldBe 0
    p2.count("Token<Player2>") shouldBe 0
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2, ADMIN)
  }

  @Test
  internal fun assignedPlayerCanCompleteATaskPerformedByAdmin() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val checkpoint = game.timeline.checkpoint()

    p1.addTasks("Token<Player1> BY Admin")
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER1)

    p1.doTask("Token<Player1> BY Admin")

    p1.count("Token") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe Actor.ADMIN
  }

  @Test
  internal fun performerOverridePreservesThenTaskSequencing() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val checkpoint = game.timeline.checkpoint()

    p1.addTasks("(Token<Player1> THEN Marker<Player1>) BY Admin")
    game.tasks.extract { it.then != null }.shouldContainExactly(true)

    p1.doTask("Token<Player1> BY Admin")

    p1.count("Token") shouldBe 1
    p1.count("Marker") shouldBe 0
    game.tasks
        .extract { it.instruction.toString() }
        .shouldContainExactly("Marker<Player1>! BY Admin")

    p1.doTask("Marker<Player1> BY Admin")

    game.events
        .changesSince(checkpoint)
        .map { it.actor }
        .shouldContainExactly(Actor.ADMIN, Actor.ADMIN)
  }

  @Test
  internal fun resolvedPerformerOverrideRenormalizesThenTaskSequencing() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    val task =
        p1.addTasks("((X Token<Player1>? THEN X Marker<Player1>?) OR Blocked<Player1>) BY Player2")
            .single()
    p1.selectTask(task)
    p1.doTask("2 Token<Player1> BY Player2")

    p1.count("Token<Player1>") shouldBe 2
    p1.count("Marker<Player1>") shouldBe 0
    game.tasks
        .extract { it.instruction.toString() }
        .shouldContainExactly("2 Marker<Player1>? BY Player2")

    p1.doTask("2 Marker<Player1> BY Player2")
    p1.count("Marker<Player1>") shouldBe 2
  }

  @Test
  internal fun pendingTaskReceivesItsAddEventOrdinalWhenInsertedIntoItsAssigneesQueue() {
    val world = GameWorld(testGamePremise("CLASS Token<Player>", players = 2))
    val queues = TaskQueues(world)
    val cause = Cause(parse<Expression>("Token"), triggerEvent = 0)
    val pending =
        PendingTask(
            controller = PLAYER2,
            instruction = InstructionGroup(listOf(parse<Instruction>("Token<Player2>!"))),
            cause = cause,
        )

    val event = queues.addTasks(pending).single()
    val added = event.task

    added.id.ordinal shouldBe event.ordinal
    added.assignee shouldBe PLAYER2
    added.actor shouldBe PLAYER2
    added.instruction shouldBe pending.instruction.instructions.single()
    added.cause shouldBe cause
    world.tasksFor(PLAYER2).ids().shouldContainExactly(TaskId(event.ordinal))
  }
}
