package dev.martianzoo.state

import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.Task.TaskId

/**
 * Append-only event history of a game world until rollback removes a suffix. List queries return
 * copies of the event sequence in ordinal order; event ordinals begin at zero. Events in those
 * lists still carry mutable [GameEvent.notes] commentary.
 *
 * Queries here take plain `Int` event ordinals, which address individual events. They are not
 * client navigation positions; see [Checkpoint].
 */
public class EventLog
internal constructor(
    private val backing: EventLog? = null,
    private val requireValid: () -> Unit = {},
) {
  private val prefixSize: Int = backing?.nextOrdinal ?: 0
  private val events: MutableList<GameEvent> = mutableListOf()
  private val prefixSnapshots = mutableMapOf<Int, GameEvent>()

  private val size: Int
    get() {
      if (backing != null) requireValid()
      return prefixSize + events.size
    }

  /** The ordinal the next appended event will have, which is also this history's event count. */
  public val nextOrdinal: Int
    get() = size

  private var setupStart: Int? = backing?.setupStart

  internal fun fork(): EventLog =
      EventLog().also { copy ->
        entriesSince(0).mapTo(copy.events, GameEvent::snapshot)
        copy.setupStart = setupStart
      }

  internal fun overlay(requireValid: () -> Unit): EventLog = EventLog(this, requireValid)

  internal fun resetOverlay() {
    check(backing != null)
    requireValid()
    events.clear()
    prefixSnapshots.clear()
    setupStart = backing.setupStart
  }

  private fun prefixEvent(ordinal: Int): GameEvent =
      prefixSnapshots.getOrPut(ordinal) {
        backing!!.entryAt(ordinal).snapshot()
      }

  private fun entryAt(ordinal: Int): GameEvent {
    require(ordinal in 0 until size)
    return if (ordinal < prefixSize) prefixEvent(ordinal) else events[ordinal - prefixSize]
  }

  internal fun requireNext(entry: GameEvent) {
    require(entry.ordinal == nextOrdinal) {
      "expected event ordinal $nextOrdinal, got ${entry.ordinal}"
    }
  }

  internal fun append(entry: GameEvent) {
    requireNext(entry)
    events += entry
  }

  internal fun last(): GameEvent {
    if (backing != null) requireValid()
    return events.last()
  }

  internal fun removeLast() {
    if (backing != null) requireValid()
    events.removeLast()
  }

  /** Returns all change events since engine initialization concluded, including game setup. */
  public fun changesSinceSetup(): List<ChangeEvent> =
      entriesSinceSetup().filterIsInstance<ChangeEvent>()

  /**
   * Returns events since [GameWorld.markSetupStart], including both changes and task events. Fails
   * if setup start has not been marked.
   */
  public fun entriesSinceSetup(): List<GameEvent> = entriesSince(checkNotNull(setupStart))

  /**
   * Returns the component change at [ordinal], or null when that ordinal belongs to a task event.
   */
  public fun changeAt(ordinal: Int): ChangeEvent? {
    return entryAt(ordinal) as? ChangeEvent
  }

  /** Returns all change events at or after event [ordinal]. */
  public fun changesSince(ordinal: Int): List<ChangeEvent> =
      entriesSince(ordinal).filterIsInstance<ChangeEvent>()

  /**
   * Returns all events at or after event [ordinal]. For example, if the history has ordinals 0, 1,
   * and 2, an ordinal of 1 returns the events at 1 and 2. [ordinal] may also be [nextOrdinal], in
   * which case the result is empty.
   */
  public fun entriesSince(ordinal: Int): List<GameEvent> {
    require(ordinal in 0..size)
    if (ordinal >= prefixSize) return events.subList(ordinal - prefixSize, events.size).toList()
    val prefix = (ordinal until prefixSize).map(::prefixEvent)
    return prefix + events
  }

  internal fun activitySince(ordinal: Int): TaskResult {
    val changes = mutableListOf<ChangeEvent>()
    val newTasks = mutableSetOf<TaskId>()
    for (entry in entriesSince(ordinal)) {
      when (entry) {
        is ChangeEvent -> changes += entry
        is TaskAddedEvent -> newTasks += entry.task.id
        is TaskRemovedEvent -> newTasks -= entry.task.id
        else -> {}
      }
    }
    return TaskResult(changes, newTasks)
  }

  internal fun markSetupStart(ordinal: Int) {
    require(ordinal in 0..size)
    check(setupStart == null) { "setup start is already marked" }
    setupStart = ordinal
  }
}
