package dev.martianzoo.agent

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.SystemClasses.HIDDEN
import dev.martianzoo.pets.api.SystemClasses.MUST_CLEAN_UP
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/**
 * Applies an evidenced replay adjustment, attributed to [adjustingActor], without leaving a
 * selected task resolved against stale state. Direct System, Hidden, and MustCleanUp targets are
 * forbidden; required parts may still change them, but must not leave new unfinished work.
 */
public fun Agents.exMachina(adjustingActor: Actor, adjustment: String) {
  val agent = this[adjustingActor]
  val restricted = listOf(SYSTEM, HIDDEN, MUST_CLEAN_UP).map(world.classTable::getClass)
  for (change in agent.parse<InstructionTree>(adjustment).descendantsOfType<Change>()) {
    for (target in listOfNotNull(change.gaining, change.removing)) {
      val type = agent.reader.resolve(target)
      if (restricted.any(type.rootClass::isSubtypeOf)) {
        throw ExpressionException(
            "exMachina cannot directly change System, Hidden, or MustCleanUp type `$target`"
        )
      }
    }
  }
  val auditedAdjustment = "Audit<$adjustingActor>, $adjustment"
  world.timeline.atomic {
    val unfinished = agent.list("MustCleanUp").entries.associate { it.key to it.value }
    val selected =
        world.tasks.selectedTask()?.let { it to this[world.tasks.getTaskData(it).assignee] }
    selected?.let { (id, owner) ->
      world.actorEngine(owner.actor).restoreTask(world.taskBeforeSelection(id))
    }
    agent.sneak(auditedAdjustment)
    if (
        agent.list("MustCleanUp").entries.any { (type, count) -> count > (unfinished[type] ?: 0) }
    ) {
      throw NotNowException("exMachina cannot leave new unfinished work")
    }
    selected?.let { (id, owner) ->
      world.actorEngine(owner.actor).selectTask(id)
      owner.autoExecNow()
    }
  }
}

private fun World.taskBeforeSelection(selectedId: TaskId): Task {
  val expectedTask = tasks.getTaskData(selectedId)
  return events
      .entriesSince(Checkpoint(0))
      .asReversed()
      .asSequence()
      .filterIsInstance<TaskEditedEvent>()
      .filter { it.task.id == selectedId }
      .map { event ->
        check(event.task == expectedTask) {
          "unexpected event after selection of task $selectedId: $event"
        }
        if (!event.oldTask.selected && event.task.selected) return@map event.oldTask
        error("unexpected edit after selection of task $selectedId: $event")
      }
      .first()
}
