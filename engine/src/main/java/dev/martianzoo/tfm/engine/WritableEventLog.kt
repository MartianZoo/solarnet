package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskReplacedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Result
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Game.EventLog
import dev.martianzoo.tfm.engine.Game.EventLog.Checkpoint

internal class WritableEventLog(private val events: MutableList<GameEvent> = mutableListOf()) :
    EventLog {

  override val size: Int by events::size

  override fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  override fun changesSinceSetup() = changesSince(start)

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

  fun addEntry(entry: GameEvent) {
    require(entry.ordinal == size)
    events += entry
  }

  fun addChangeEvent(change: StateChange?, player: Player, cause: Cause?): ChangeEvent? {
    if (change == null) return null
    val event = ChangeEvent(size, player, change, cause)
    addEntry(event)
    return event
  }

  fun taskAdded(task: Task) = addEntry(TaskAddedEvent(size, task))

  fun taskRemoved(task: Task) = addEntry(TaskRemovedEvent(size, task))

  fun taskReplaced(oldTask: Task, newTask: Task) {
    require(oldTask.id == newTask.id)
    addEntry(TaskReplacedEvent(size, oldTask = oldTask, task = newTask))
  }

  override fun checkpoint() = Checkpoint(size)

  private lateinit var start: Checkpoint

  fun setStartPoint() {
    start = checkpoint()
  }

  fun rollBack(checkpoint: Checkpoint, reverser: (GameEvent) -> Unit) {
    val ordinal = checkpoint.ordinal
    require(ordinal <= events.size)
    if (ordinal == events.size) return

    val subList = events.subList(ordinal, events.size)
    for (entry in subList.asReversed()) {
      reverser(entry)
    }
    subList.clear()
  }

  fun clone() = WritableEventLog(events.toMutableList()).also { it.start = start }
}
