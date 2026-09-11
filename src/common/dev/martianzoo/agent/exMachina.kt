package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.engine.World
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.TaskEditedEvent
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId

/**
 * Applies an evidenced replay adjustment, attributed to [adjustingActor], without leaving a
 * selected task resolved against stale state.
 */
public fun Agents.exMachina(adjustingActor: Actor, adjustment: String) {
  val selectedId = world.tasks.selectedTask()
  if (selectedId == null) {
    this[adjustingActor].sneak(adjustment)
    return
  }

  val selectedAgent = this[world.tasks.getTaskData(selectedId).assignee]
  val previousAutoExecPolicy = selectedAgent.autoExecPolicy
  selectedAgent.autoExecPolicy = NONE
  try {
    world.actorEngine(selectedAgent.actor).restoreTask(world.taskBeforeSelection(selectedId))
    this[adjustingActor].sneak(adjustment)
    selectedAgent.selectTask(selectedId)
  } finally {
    selectedAgent.autoExecPolicy = previousAutoExecPolicy
  }
  if (previousAutoExecPolicy == NONE) selectedAgent.autoExecNow()
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
