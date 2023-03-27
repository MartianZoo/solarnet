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

public class EventLog(internal val events: MutableList<GameEvent> = mutableListOf()) {
  public val size: Int by events::size

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

  public data class Checkpoint(internal val ordinal: Int) {
    init {
      require(ordinal >= 0)
    }
  }

  internal fun checkpoint() = Checkpoint(size)

  public fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  public fun newTasksSince(checkpoint: Checkpoint): Set<TaskId> {
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

  public fun entriesSince(checkpoint: Checkpoint): List<GameEvent> =
      events.subList(checkpoint.ordinal, size).toList()

  public fun activitySince(checkpoint: Checkpoint) =
      Result(changesSince(checkpoint), newTasksSince(checkpoint))
}
