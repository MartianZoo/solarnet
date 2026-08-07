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
import dev.martianzoo.engine.Timeline.Checkpoint

/**
 * A complete record of everything that happened in a particular game (in progress or finished),
 * optionally continuing a captured prefix held by [startingSequence]. A complete world could be
 * reconstructed by replaying these events.
 *
 * Events appended later to [startingSequence] are not part of this log. The captured events must
 * not be rolled back while this log exists.
 */
public class EventLog internal constructor(private val startingSequence: EventLog? = null) {
  private val startingSize = startingSequence?.entriesSince(Checkpoint(0))?.size ?: 0
  private val startingSetupSize = startingSequence?.entriesSinceSetup()?.size ?: 0
  private val events: MutableList<GameEvent> = mutableListOf()

  internal val size: Int
    get() = startingSize + events.size

  internal val firstLocalOrdinal: Int = startingSize

  private var start: Checkpoint? = startingSequence?.let {
    Checkpoint(startingSize - startingSetupSize)
  }

  internal fun eventsToRollBack(ordinal: Int): List<GameEvent> {
    require(ordinal >= firstLocalOrdinal)
    return events.subList(ordinal - startingSize, events.size).toList()
  }

  internal fun removeEventsFrom(ordinal: Int) {
    require(ordinal >= firstLocalOrdinal)
    events.subList(ordinal - startingSize, events.size).clear()
  }

  /** Returns all change events since engine initialization concluded, including game setup. */
  public fun changesSinceSetup(): List<ChangeEvent> =
      entriesSinceSetup().filterIsInstance<ChangeEvent>()

  internal fun entriesSinceSetup(): List<GameEvent> = entriesSince(checkNotNull(start))

  /** Returns all change events since [checkpoint]. */
  internal fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  /** Returns the ids of all tasks created since [checkpoint] that still exist. */
  internal fun newTasksSince(checkpoint: Checkpoint): Set<TaskId> = buildSet {
    entriesSince(checkpoint).forEach {
      when (it) {
        is TaskAddedEvent -> add(it.task.id)
        is TaskRemovedEvent -> remove(it.task.id)
        else -> {}
      }
    }
  }

  public fun entriesSince(checkpoint: Checkpoint): List<GameEvent> {
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

  internal fun activitySince(checkpoint: Checkpoint): TaskResult =
      TaskResult(changesSince(checkpoint), newTasksSince(checkpoint))

  private fun <E : GameEvent> addEntry(entry: E): E {
    require(entry.ordinal == size)
    events += entry
    return entry
  }

  internal fun addChangeEvent(change: StateChange, actor: Actor, cause: Cause?): ChangeEvent =
      addEntry(ChangeEvent(size, actor, change, cause))

  internal fun taskAdded(task: Task): TaskAddedEvent = addEntry(TaskAddedEvent(size, task))

  internal fun taskRemoved(task: Task): TaskRemovedEvent = addEntry(TaskRemovedEvent(size, task))

  internal fun taskReplaced(oldTask: Task, newTask: Task): TaskEditedEvent {
    require(oldTask.id == newTask.id)
    return addEntry(TaskEditedEvent(size, oldTask = oldTask, task = newTask))
  }

  internal fun setStartPoint() {
    start = Checkpoint(size)
  }
}
