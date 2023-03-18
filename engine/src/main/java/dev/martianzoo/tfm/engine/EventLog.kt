package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskReplacedEvent
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId

class EventLog(val events: MutableList<GameEvent> = mutableListOf()) {
  val size: Int by events::size

  fun addEntry(entry: GameEvent) {
    require(entry.ordinal == size)
    events += entry
  }

  fun addChangeEvent(change: StateChange, actor: Actor, cause: Cause?) =
      ChangeEvent(size, actor, change, cause).also { addEntry(it) }

  fun taskAdded(task: Task) = addEntry(TaskAddedEvent(size, task))

  fun taskRemoved(task: Task) = addEntry(TaskRemovedEvent(size, task))

  fun taskReplaced(oldTask: Task, newTask: Task) {
    require(oldTask.id == newTask.id)
    addEntry(TaskReplacedEvent(size, oldTask = oldTask, task = newTask))
  }

  data class Checkpoint(val ordinal: Int) {
    init {
      require(ordinal >= 0)
    }
  }

  fun checkpoint() = Checkpoint(size)

  fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  fun newTasksSince(checkpoint: Checkpoint): Set<TaskId> {
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

  fun entriesSince(checkpoint: Checkpoint): List<GameEvent> =
      events.subList(checkpoint.ordinal, size).toList()

  fun resultsSince(checkpoint: Checkpoint) =
      Result(changesSince(checkpoint), newTasksSince(checkpoint))
}
