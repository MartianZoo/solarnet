package dev.martianzoo.engine

import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Timeline.Checkpoint

/**
 * A complete record of everything that happened in a particular game (in progress or finished),
 * optionally continuing a captured prefix held by [prefixSource]. A complete world could be
 * reconstructed by replaying these events.
 *
 * Events appended later to [prefixSource] are not part of this log. The captured events must not be
 * rolled back while this log exists.
 *
 * [record] and [rollBackTo] are the single boundary between live state and its history. Callers
 * supply the corresponding state mutation, which succeeds before the history and [revision] advance
 * together.
 */
public class EventLog internal constructor(private val prefixSource: EventLog? = null) {
  private val prefixSize: Int = prefixSource?.size ?: 0
  private val events: MutableList<GameEvent> = mutableListOf()

  internal val size: Int
    get() = prefixSize + events.size

  /** The ordinal required for the next event recorded in this history. */
  internal val nextOrdinal: Int
    get() = size

  internal val firstLocalOrdinal: Int = prefixSize

  internal var revision: WorldRevision = WorldRevision.INITIAL
    private set

  private var setupStart: Checkpoint? = prefixSource?.let { checkNotNull(it.setupStart) }

  /**
   * Applies [applyToState], then appends [entry] and advances the world's revision. If the state
   * update fails, history and revision remain unchanged.
   */
  internal fun <E : GameEvent> record(entry: E, applyToState: () -> Unit): E {
    require(entry.ordinal == nextOrdinal) {
      "expected event ordinal $nextOrdinal, got ${entry.ordinal}"
    }
    applyToState()
    events += entry
    revision = revision.next()
    return entry
  }

  /** Reverses local events back to [ordinal], advancing the revision after each reversal. */
  internal fun rollBackTo(ordinal: Int, reverseInState: (GameEvent) -> Unit) {
    require(ordinal >= firstLocalOrdinal) { "can't roll back into captured history at $ordinal" }
    require(ordinal <= size) { "can't roll back past the end of history at $ordinal" }
    while (size > ordinal) {
      val entry = events.last()
      reverseInState(entry)
      events.removeAt(events.lastIndex)
      revision = revision.next()
    }
  }

  /** Returns all change events since engine initialization concluded, including game setup. */
  public fun changesSinceSetup(): List<ChangeEvent> =
      entriesSinceSetup().filterIsInstance<ChangeEvent>()

  internal fun entriesSinceSetup(): List<GameEvent> = entriesSince(checkNotNull(setupStart))

  /** Returns all change events since [checkpoint]. */
  internal fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  public fun entriesSince(checkpoint: Checkpoint): List<GameEvent> {
    require(checkpoint.ordinal <= size)
    if (checkpoint.ordinal >= prefixSize) {
      return events.subList(checkpoint.ordinal - prefixSize, events.size).toList()
    }

    val startingEntries =
        checkNotNull(prefixSource).entriesSince(checkpoint).take(prefixSize - checkpoint.ordinal)
    return startingEntries + events
  }

  /** Replaces the free-form comment on event [ordinal], without changing replayable game state. */
  public fun reviseComment(ordinal: Int, comment: String?): GameEvent {
    require(ordinal in 0 until size) { "no event with ordinal $ordinal" }
    if (ordinal < prefixSize) {
      return checkNotNull(prefixSource).reviseComment(ordinal, comment)
    }

    val index = ordinal - prefixSize
    val entry = events[index]
    val revised =
        when (entry) {
          is ChangeEvent -> entry.copy(comment = comment)
          is TaskAddedEvent -> entry.copy(comment = comment)
          is TaskRemovedEvent -> entry.copy(comment = comment)
          is GameEvent.TaskEditedEvent -> entry.copy(comment = comment)
        }
    events[index] = revised
    return revised
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

  internal fun markSetupStart() {
    setupStart = Checkpoint(size)
  }
}
