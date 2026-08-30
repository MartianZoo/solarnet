package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.pets.data.Task.TaskId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskAssignmentCharacterizationTest {
  private fun game() =
      Engine.newGame(
          testGamePremise("CLASS Token<Owner>\nCLASS Marker<Owner>\nCLASS EngineToken", players = 2)
      )

  @Test
  internal fun ordinaryActorCanOnlySeeAndExecuteTasksAssignedToIt() {
    val game = game()
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).also { it.autoExecMode = NONE }

    p2.godMode().addTasks("Token<Player2>")

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    shouldThrow<TaskException> { p1.doTask("Token<Player2>") }

    p2.doTask("Token<Player2>")
    p2.count("Token<Player2>") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun wholeGameAutoExecutionPreservesAnotherAssigneesActor() {
    val game = game()
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).also { it.autoExecMode = NONE }
    val checkpoint = game.timeline.checkpoint()

    p2.godMode().addTasks("Token<Player2>")
    p1.autoExecMode = FIRST

    game.tasks.isEmpty() shouldBe true
    p2.count("Token<Player2>") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe PLAYER2
  }

  @Test
  internal fun playerNoneDrainsOnlyEngineWork() {
    val game = game()
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).also { it.autoExecMode = NONE }
    val engine = game.gameplay(ENGINE).also { it.autoExecMode = NONE }

    p2.godMode().addTasks("Token<Player2>")
    engine.godMode().addTasks("EngineToken")
    p1.autoExecNow()

    engine.count("EngineToken") shouldBe 1
    p2.count("Token<Player2>") shouldBe 0
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
  }

  @Test
  internal fun assignedPlayerCanCompleteATaskPerformedByEngine() {
    val game = game()
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val checkpoint = game.timeline.checkpoint()

    p1.godMode().addTasks("Token<Player1> BY Engine")
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER1)

    p1.doTask("Token<Player1> BY Engine")

    p1.count("Token") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe Actor.ENGINE
  }

  @Test
  internal fun performerOverridePreservesThenTaskSequencing() {
    val game = game()
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val checkpoint = game.timeline.checkpoint()

    p1.godMode().addTasks("(Token<Player1> THEN Marker<Player1>) BY Engine")
    game.tasks.extract { it.then != null }.shouldContainExactly(true)

    p1.doTask("Token<Player1> BY Engine")

    p1.count("Token") shouldBe 1
    p1.count("Marker") shouldBe 0
    game.tasks
        .extract { it.instruction.toString() }
        .shouldContainExactly("Marker<Player1>! BY Engine")

    p1.doTask("Marker<Player1> BY Engine")

    game.events
        .changesSince(checkpoint)
        .map { it.actor }
        .shouldContainExactly(Actor.ENGINE, Actor.ENGINE)
  }

  @Test
  internal fun pendingTaskReceivesItsAddEventOrdinalWhenInsertedIntoItsAssigneesQueue() {
    val events = EventLog()
    val queues = TaskQueues(events)
    val cause = Cause(parse<Expression>("Token"), triggerEvent = 0)
    val pending =
        PendingTask(
            assignee = PLAYER2,
            instruction = InstructionGroup(listOf(parse<Instruction>("Token<Player2>!"))),
            cause = cause,
        )

    val event = queues[PLAYER2].addTasks(pending).single()
    val added = event.task

    added.id.ordinal shouldBe event.ordinal
    added.assignee shouldBe PLAYER2
    added.actor shouldBe PLAYER2
    added.instruction shouldBe pending.instruction.instructions.single()
    added.cause shouldBe cause
    queues[PLAYER2].ids().shouldContainExactly(TaskId(event.ordinal))
  }

  @Test
  internal fun copiedQueuesRetainTasksAndThenDiverge() {
    val events = EventLog()
    val queues = TaskQueues(events)
    val pending =
        PendingTask(
            assignee = PLAYER2,
            instruction = InstructionGroup(listOf(parse<Instruction>("Token<Player2>!"))),
            cause = Cause(parse<Expression>("Token"), triggerEvent = 0),
        )
    queues[PLAYER2].addTasks(pending)

    events.markSetupStart()
    val copied = queues.copy(EventLog(events))
    copied[PLAYER2].addTasks(pending).single().task.id shouldBe TaskId(1)
    queues[PLAYER2].ids().shouldContainExactly(TaskId(0))
    copied[PLAYER2].ids().shouldContainExactly(TaskId(0), TaskId(1))
  }
}
