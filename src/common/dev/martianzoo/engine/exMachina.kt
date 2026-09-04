package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.data.GameEvent.TaskEditedEvent
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId

/**
 * Applies an evidenced replay adjustment without leaving a selected task resolved against stale
 * state.
 */
public fun World.exMachina(adjustingAgent: Agent, adjustment: String) {
  val selectedId = tasks.selectedTask()
  if (selectedId == null) {
    adjustingAgent.sneak(adjustment)
    return
  }

  val selectedAgent = agent(tasks.getTaskData(selectedId).assignee)
  val previousAutoExecMode = selectedAgent.autoExecMode
  selectedAgent.autoExecMode = NONE
  try {
    tasks.editTask(taskBeforeSelection(selectedId))
    adjustingAgent.sneak(adjustment)
    selectedAgent.selectTask(selectedId)
  } finally {
    selectedAgent.autoExecMode = previousAutoExecMode
  }
  if (previousAutoExecMode == NONE) selectedAgent.autoExecNow()
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
