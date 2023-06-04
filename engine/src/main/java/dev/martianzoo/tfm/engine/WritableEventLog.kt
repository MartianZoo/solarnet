package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEditedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Engine.ChangeLogger
import dev.martianzoo.tfm.engine.Engine.TaskListener
import dev.martianzoo.tfm.engine.Timeline.Checkpoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WritableEventLog @Inject constructor() : EventLog, TaskListener, ChangeLogger {
  val events: MutableList<GameEvent> = mutableListOf() // TODO only used by Timeline
  override val size: Int by events::size

  override fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  override fun changesSinceSetup() = changesSince(start)

  override fun entriesSinceSetup() = entriesSince(start)

  // we don't treat a replacement task as new...
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
      TaskResult(changesSince(checkpoint), newTasksSince(checkpoint))

  fun <E : GameEvent> addEntry(entry: E): E {
    require(entry.ordinal == size)
    events += entry
    return entry
  }

  override fun addChangeEvent(change: StateChange, player: Player, cause: Cause?): ChangeEvent =
      addEntry(ChangeEvent(size, player, change, cause))

  override fun taskAdded(task: Task) = addEntry(TaskAddedEvent(size, task))

  override fun taskRemoved(task: Task) = addEntry(TaskRemovedEvent(size, task))

  override fun taskReplaced(oldTask: Task, newTask: Task): TaskEditedEvent {
    require(oldTask.id == newTask.id)
    return addEntry(TaskEditedEvent(size, oldTask = oldTask, task = newTask))
  }

  override fun checkpoint() = Checkpoint(size)

  private lateinit var start: Checkpoint

  fun setStartPoint() {
    start = checkpoint()
  }
}
