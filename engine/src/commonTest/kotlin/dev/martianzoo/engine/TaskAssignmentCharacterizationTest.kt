package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.tfm.engine.canonicalPremise
import dev.martianzoo.types.te
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TaskAssignmentCharacterizationTest {
  @Test
  fun ordinaryActorCanOnlySeeAndExecuteTasksAssignedToIt() {
    val game = Engine.newGame(canonicalPremise())
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).also { it.autoExecMode = NONE }

    p2.godMode().addTasks("Plant")

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    shouldThrow<TaskException> { p1.doTask("Plant<Player2>") }

    p2.doTask("Plant")
    p2.count("Plant") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun wholeGameAutoExecutionPreservesAnotherAssigneesActor() {
    val game = Engine.newGame(canonicalPremise())
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).also { it.autoExecMode = NONE }
    val checkpoint = game.timeline.checkpoint()

    p2.godMode().addTasks("Plant")
    p1.autoExecMode = FIRST

    game.tasks.isEmpty() shouldBe true
    p2.count("Plant") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe PLAYER2
  }

  @Test
  fun assignedPlayerCanCompleteATaskPerformedByEngine() {
    val game = Engine.newGame(canonicalPremise())
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val checkpoint = game.timeline.checkpoint()

    p1.godMode().addTasks("Plant<Player1> BY Engine")
    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER1)

    p1.doTask("Plant<Player1> BY Engine")

    p1.count("Plant") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe Actor.ENGINE
  }

  @Test
  fun performerOverridePreservesThenTaskSequencing() {
    val game = Engine.newGame(canonicalPremise())
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val checkpoint = game.timeline.checkpoint()

    p1.godMode().addTasks("(Plant<Player1> THEN Steel<Player1>) BY Engine")
    game.tasks.extract { it.then != null }.shouldContainExactly(true)

    p1.doTask("Plant<Player1> BY Engine")

    p1.count("Plant") shouldBe 1
    p1.count("Steel") shouldBe 0
    game.tasks
        .extract { it.instruction.toString() }
        .shouldContainExactly("Steel<Player1>! BY Engine")

    p1.doTask("Steel<Player1> BY Engine")

    game.events
        .changesSince(checkpoint)
        .map { it.actor }
        .shouldContainExactly(Actor.ENGINE, Actor.ENGINE)
  }

  @Test
  fun pendingTaskReceivesItsAddEventOrdinalWhenInsertedIntoItsAssigneesQueue() {
    val events = EventLog()
    val queues = TaskQueues(events)
    val cause = Cause(te("TerraformingMars"), triggerEvent = 0)
    val pending =
        PendingTask(
            assignee = PLAYER2,
            instruction = InstructionGroup(listOf(parse<Instruction>("Plant<Player2>!"))),
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
  fun copiedQueuesRetainTasksAndThenDiverge() {
    val events = EventLog()
    val queues = TaskQueues(events)
    val pending =
        PendingTask(
            assignee = PLAYER2,
            instruction = InstructionGroup(listOf(parse<Instruction>("Plant<Player2>!"))),
            cause = Cause(te("TerraformingMars"), triggerEvent = 0),
        )
    queues[PLAYER2].addTasks(pending)

    events.markSetupStart()
    val copied = queues.copy(EventLog(events))
    copied[PLAYER2].addTasks(pending).single().task.id shouldBe TaskId(1)
    queues[PLAYER2].ids().shouldContainExactly(TaskId(0))
    copied[PLAYER2].ids().shouldContainExactly(TaskId(0), TaskId(1))
  }
}
