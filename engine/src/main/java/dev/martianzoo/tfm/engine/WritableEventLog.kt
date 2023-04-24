package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskReplacedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.EventLog.Checkpoint

internal class WritableEventLog : EventLog {
  override val events: MutableList<GameEvent> = mutableListOf()
  override val size: Int by events::size

  override fun checkpoint() = Checkpoint(size)

  override fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  override fun newTasksSince(checkpoint: Checkpoint): Set<TaskId> {
    val ids = mutableSetOf<TaskId>()
    entriesSince(checkpoint).forEach {
      when (it) {
        is TaskAddedEvent -> ids += it.task.id
        is TaskRemovedEvent -> ids -= it.task.id
        else -> {}
      }
    }
    return ids
  }

  override fun entriesSince(checkpoint: Checkpoint): List<GameEvent> =
      events.subList(checkpoint.ordinal, size).toList()

  override fun activitySince(checkpoint: Checkpoint) =
      Result(changesSince(checkpoint), newTasksSince(checkpoint))

  internal fun addEntry(entry: GameEvent) {
    require(entry.ordinal == size)
    events += entry
  }

  internal fun addChangeEvent(change: StateChange?, player: Player, cause: Cause?): ChangeEvent? {
    if (change == null) return null
    val event = ChangeEvent(size, player, change, cause)
    addEntry(event)
    return event
  }

  internal fun taskAdded(task: Task) = addEntry(TaskAddedEvent(size, task))

  internal fun taskRemoved(task: Task) = addEntry(TaskRemovedEvent(size, task))

  internal fun taskReplaced(oldTask: Task, newTask: Task) {
    require(oldTask.id == newTask.id)
    addEntry(TaskReplacedEvent(size, oldTask = oldTask, task = newTask))
  }
}
