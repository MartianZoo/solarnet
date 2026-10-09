package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
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
              CLASS AutomaticBy { This:: Token<Player1> BY Admin }
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
  internal fun concreteByWorkIsHandedToItsPerformingActor() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val checkpoint = game.timeline.checkpoint()

    val task = p1.addTasks("Token<Player1> BY Admin").single()
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER1)

    shouldThrow<TaskException> { p1.doTask("Token<Player1> BY Admin") }
    game.tasks.getTaskData(task).selected shouldBe false
    p1.selectTask(task)

    game.tasks.getTaskData(task).let {
      it.assignee shouldBe ADMIN
      it.selected shouldBe true
      it.instruction shouldBe parse<Instruction>("Token<Player1>!")
    }
    p1.count("Token") shouldBe 0

    admin.selectTask(task)

    p1.count("Token") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe Actor.ADMIN
  }

  @Test
  internal fun automaticByCannotInventAnExecutingActorWithoutATaskHandoff() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    shouldThrow<ExpressionException> { p1.runOperation("AutomaticBy") }

    p1.count("AutomaticBy") shouldBe 0
    p1.count("Token") shouldBe 0
  }

  @Test
  internal fun aFormCanCompleteConcreteByWorkThatRemainsWithItsAssignee() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val task = p1.addTasks("Token<Player1> BY Player1").single()

    p1.fillInTask(task).commit()

    p1.count("Token") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun performerOverridePreservesThenTaskSequencing() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val checkpoint = game.timeline.checkpoint()

    val token = p1.addTasks("(Token<Player1> THEN Marker<Player1>) BY Admin").single()
    game.tasks.extract { it.then != null }.shouldContainExactly(true)

    p1.selectTask(token)
    admin.selectTask(token)

    p1.count("Token") shouldBe 1
    p1.count("Marker") shouldBe 0
    game.tasks
        .extract { it.instruction.toString() }
        .shouldContainExactly("Marker<Player1>! BY Admin")

    val marker = game.tasks.ids().single()
    p1.selectTask(marker)
    admin.selectTask(marker)

    game.events
        .changesSince(checkpoint)
        .map { it.actor }
        .shouldContainExactly(Actor.ADMIN, Actor.ADMIN)
  }

  @Test
  internal fun resolvedByIsRejectedWhenSharedChoicesKeepThenInOneTask() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    val task =
        p1.addTasks("((X Token<Player1>? THEN X Marker<Player1>?) OR -Marker<Player1>) BY Player2")
            .single()
    val beforeSelection = game.tasks.getTaskData(task)

    shouldThrow<TaskException> { p1.selectTask(task) }

    game.tasks.getTaskData(task) shouldBe beforeSelection
    p1.count("Token<Player1>") shouldBe 0
    p1.count("Marker<Player1>") shouldBe 0
  }

  @Test
  internal fun resolvedOuterByRemainsOnEverySeparableThenStage() {
    val game = game()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val task =
        p1.addTasks("((Token<Player1> THEN Marker<Player1>) OR -AdminToken) BY Player2").single()

    p1.selectTask(task)

    game.tasks.getTaskData(task).let {
      it.assignee shouldBe PLAYER2
      it.instruction shouldBe parse<Instruction>("Token<Player1>!")
      it.then shouldBe InstructionGroup(listOf(parse<Instruction>("Marker<Player1>! BY Player2")))
    }
    p2.selectTask(task)

    val marker = game.tasks.ids().single()
    p1.selectTask(marker)
    p2.selectTask(marker)
    p1.count("Token<Player1>") shouldBe 1
    p1.count("Marker<Player1>") shouldBe 1
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
    added.selectionAssignee shouldBe PLAYER2
    added.instruction shouldBe pending.instruction.instructions.single()
    added.cause shouldBe cause
    world.tasksFor(PLAYER2).ids().shouldContainExactly(TaskId(event.ordinal))
  }
}
