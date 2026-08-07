package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Instruction
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
  fun wholeGameAutoExecutionUsesTheCallingActorToPerformAnotherAssigneesTask() {
    val game = Engine.newGame(canonicalPremise())
    val p1 = game.gameplay(PLAYER1).also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).also { it.autoExecMode = NONE }
    val checkpoint = game.timeline.checkpoint()

    p2.godMode().addTasks("Plant")
    p1.autoExecMode = FIRST

    game.tasks.isEmpty() shouldBe true
    p2.count("Plant") shouldBe 1
    game.events.changesSince(checkpoint).single().actor shouldBe PLAYER1
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
  fun unidentifiedTaskReceivesAnIdWhenInsertedIntoItsAssigneesQueue() {
    val events = EventLog()
    val queues = TaskQueues(events)
    val cause = Cause(te("TerraformingMars"), triggerEvent = 0)
    val unidentified =
        Task.noid(
            assignee = PLAYER2,
            automatic = false,
            hit = parse<Instruction>("Plant<Player2>!"),
            cause = cause,
        )

    unidentified.id shouldBe TaskId("ZZ")

    val added = queues[PLAYER2].addTasks(unidentified).single().task

    added.id shouldBe TaskId("A")
    added.assignee shouldBe PLAYER2
    added.instruction shouldBe unidentified.instruction
    added.cause shouldBe cause
    queues[PLAYER2].ids().shouldContainExactly(TaskId("A"))
  }

  @Test
  fun copiedQueuesRetainTasksAndThenDiverge() {
    val queues = TaskQueues(EventLog())
    val task =
        Task.noid(
            assignee = PLAYER2,
            automatic = false,
            hit = parse<Instruction>("Plant<Player2>!"),
            cause = Cause(te("TerraformingMars"), triggerEvent = 0),
        )
    queues[PLAYER2].addTasks(task)

    val copied = queues.copy(EventLog())
    copied[PLAYER2].addTasks(task).single().task.id shouldBe TaskId("B")
    queues[PLAYER2].ids().shouldContainExactly(TaskId("A"))
    copied[PLAYER2].ids().shouldContainExactly(TaskId("A"), TaskId("B"))
  }
}
