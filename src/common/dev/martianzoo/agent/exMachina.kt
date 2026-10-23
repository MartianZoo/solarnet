package dev.martianzoo.agent

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.SystemClasses.MUST_CLEAN_UP
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.RecordedInput.Kind
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/**
 * Applies an evidenced replay adjustment, attributed to [adjustingActor], without leaving a
 * selected task resolved against stale state. Explicit System and MustCleanUp targets are
 * forbidden; required parts may still change them, but must not leave new unfinished work.
 */
// TODO(ENGINE_CONTRACT): decide whether an explicit correction is allowed while a task is selected.
// This restores and reselects it around a mutation that the proposed ordinary-client rules exclude.
public fun Agents.exMachina(adjustingActor: Actor, adjustment: String) {
  val agent = this[adjustingActor]
  val restricted = listOf(SYSTEM, MUST_CLEAN_UP).map(world.classTable::getClass)
  for (change in agent.parse<InstructionTree>(adjustment).descendantsOfType<Change>()) {
    for (target in listOfNotNull(change.gaining, change.removing)) {
      val type = agent.reader.resolve(target)
      if (restricted.any(type.rootClass::isSubtypeOf)) {
        throw ExpressionException(
            "exMachina cannot explicitly target System or MustCleanUp type `$target`"
        )
      }
    }
  }
  val auditedAdjustment = "Audit<$adjustingActor>, $adjustment"
  taskLog.capture(adjustingActor, adjustment, kind = Kind.EX_MACHINA) {
    world.timeline.atomic {
      val unfinished = agent.list("MustCleanUp").entries.associate { it.key to it.value }
      val selected =
          world.tasks.selectedTask()?.let { id ->
            val currentAssignee = world.tasks.getTaskData(id).assignee
            world.taskBeforeSelection(id) to this[currentAssignee]
          }
      selected?.let { (beforeSelection, currentAssignee) ->
        world.actorEngine(currentAssignee.actor).restoreTask(beforeSelection)
      }
      world
          .actorEngine(adjustingActor)
          .sneak(InstructionGroup.of(agent.parse<InstructionTree>(auditedAdjustment)))
      world.agentTransactionGuard?.validate()
      if (
          agent.list("MustCleanUp").entries.any { (type, count) -> count > (unfinished[type] ?: 0) }
      ) {
        throw NotNowException("exMachina cannot leave new unfinished work")
      }
      selected?.let { (beforeSelection, currentAssignee) ->
        world.actorEngine(beforeSelection.assignee).selectTask(beforeSelection.id)
        currentAssignee.autoExecNow()
      }
    }
  }
}

private fun World.taskBeforeSelection(selectedId: TaskId): Task {
  val expectedTask = tasks.getTaskData(selectedId)
  return events
      .entriesSince(0)
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
