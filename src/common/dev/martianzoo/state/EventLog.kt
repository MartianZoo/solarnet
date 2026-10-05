package dev.martianzoo.state

import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.Task.TaskId

/** The event history of one game world. */
public class EventLog internal constructor() {
  private val events: MutableList<GameEvent> = mutableListOf()

  internal val size: Int
    get() = events.size

  internal val nextOrdinal: Int
    get() = size

  private var setupStart: Checkpoint? = null

  internal fun requireNext(entry: GameEvent) {
    require(entry.ordinal == nextOrdinal) {
      "expected event ordinal $nextOrdinal, got ${entry.ordinal}"
    }
  }

  internal fun append(entry: GameEvent) {
    requireNext(entry)
    events += entry
  }

  internal fun last(): GameEvent = events.last()

  internal fun removeLast() {
    events.removeLast()
  }

  /** Returns all change events since engine initialization concluded, including game setup. */
  public fun changesSinceSetup(): List<ChangeEvent> =
      entriesSinceSetup().filterIsInstance<ChangeEvent>()

  public fun entriesSinceSetup(): List<GameEvent> = entriesSince(checkNotNull(setupStart))

  /**
   * Returns the component change at [ordinal], or null when that ordinal belongs to a task event.
   */
  public fun changeAt(ordinal: Int): ChangeEvent? {
    require(ordinal in events.indices)
    return events[ordinal] as? ChangeEvent
  }

  /** Returns all change events at or after [checkpoint]. */
  public fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  /** Returns all events at or after [checkpoint]. */
  public fun entriesSince(checkpoint: Checkpoint): List<GameEvent> {
    val ordinal = checkpoint.ordinal
    require(ordinal in 0..events.size)
    return events.subList(ordinal, events.size).toList()
  }

  internal fun activitySince(checkpoint: Checkpoint): TaskResult {
    val changes = mutableListOf<ChangeEvent>()
    val newTasks = mutableSetOf<TaskId>()
    for (entry in entriesSince(checkpoint)) {
      when (entry) {
        is ChangeEvent -> changes += entry
        is TaskAddedEvent -> newTasks += entry.task.id
        is TaskRemovedEvent -> newTasks -= entry.task.id
        else -> {}
      }
    }
    return TaskResult(changes, newTasks)
  }

  internal fun markSetupStart(checkpoint: Checkpoint) {
    require(checkpoint.ordinal in 0..size)
    check(setupStart == null) { "setup start is already marked" }
    setupStart = checkpoint
  }
}
