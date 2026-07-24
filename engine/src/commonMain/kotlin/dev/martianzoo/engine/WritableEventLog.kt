package dev.martianzoo.engine

import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine.ChangeLogger
import dev.martianzoo.engine.Engine.TaskListener
import dev.martianzoo.engine.Timeline.Checkpoint

/**
 * Writable event history, optionally continuing a captured prefix held by [startingSequence].
 * Events appended later to [startingSequence] are not part of this log. The captured events must
 * not be rolled back while this log exists.
 */
internal class WritableEventLog(private val startingSequence: EventLog? = null) :
    EventLog, TaskListener, ChangeLogger {
  private val startingSize = startingSequence?.entriesSince(Checkpoint(0))?.size ?: 0
  private val startingSetupSize = startingSequence?.entriesSinceSetup()?.size ?: 0
  private val events: MutableList<GameEvent> = mutableListOf()

  internal val size: Int
    get() = startingSize + events.size

  internal val firstWritableOrdinal = startingSize

  private var start: Checkpoint? = startingSequence?.let {
    Checkpoint(startingSize - startingSetupSize)
  }

  internal fun eventsToRollBack(ordinal: Int): List<GameEvent> {
    require(ordinal >= firstWritableOrdinal)
    return events.subList(ordinal - startingSize, events.size).toList()
  }

  internal fun removeEventsFrom(ordinal: Int) {
    require(ordinal >= firstWritableOrdinal)
    events.subList(ordinal - startingSize, events.size).clear()
  }

  override fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  override fun changesSinceSetup() = entriesSinceSetup().filterIsInstance<ChangeEvent>()

  override fun entriesSinceSetup() = entriesSince(checkNotNull(start))

  // we don't treat a replacement task as new...
  override fun newTasksSince(checkpoint: Checkpoint): Set<TaskId> = buildSet {
    entriesSince(checkpoint).forEach {
      when (it) {
        is TaskAddedEvent -> add(it.task.id)
        is TaskRemovedEvent -> remove(it.task.id)
        else -> {}
      }
    }
  }

  override fun entriesSince(checkpoint: Checkpoint): List<GameEvent> {
    require(checkpoint.ordinal <= size)
    if (checkpoint.ordinal >= startingSize) {
      return events.subList(checkpoint.ordinal - startingSize, events.size).toList()
    }

    val startingEntries =
        checkNotNull(startingSequence)
            .entriesSince(checkpoint)
            .take(startingSize - checkpoint.ordinal)
    return startingEntries + events
  }

  override fun activitySince(checkpoint: Checkpoint) =
      TaskResult(changesSince(checkpoint), newTasksSince(checkpoint))

  private fun <E : GameEvent> addEntry(entry: E): E {
    require(entry.ordinal == size)
    events += entry
    return entry
  }

  override fun addChangeEvent(change: StateChange, actor: Actor, cause: Cause?): ChangeEvent =
      addEntry(ChangeEvent(size, actor, change, cause))

  override fun taskAdded(task: Task) = addEntry(TaskAddedEvent(size, task))

  override fun taskRemoved(task: Task) = addEntry(TaskRemovedEvent(size, task))

  override fun taskReplaced(oldTask: Task, newTask: Task): TaskEditedEvent {
    require(oldTask.id == newTask.id)
    return addEntry(TaskEditedEvent(size, oldTask = oldTask, task = newTask))
  }

  internal fun setStartPoint() {
    start = Checkpoint(size)
  }
}
