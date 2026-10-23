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
 *
 * Each event carries the [Transaction] that produced it, so this log also answers which transaction
 * owns an event and which events a transaction owns. There is no separate list of transactions to
 * keep synchronized: removing a suffix of events removes exactly the transaction records those
 * events carried, and the serial they freed is reused by later work.
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
    requireTransactionFollows(entry)
    events += entry
  }

  /**
   * Transaction serials begin at zero, never decrease, and advance by one, and an initialization
   * prefix event cannot follow a transaction. An event continuing a transaction must carry that
   * transaction's own value.
   */
  private fun requireTransactionFollows(entry: GameEvent) {
    val previous = lastSerial()
    val serial = entry.transaction?.serial ?: PREFIX_SERIAL
    require(serial == previous || serial == previous + 1) {
      "event ${entry.ordinal} has transaction serial $serial following $previous"
    }
    if (serial == previous && serial != PREFIX_SERIAL) {
      require(entry.transaction == entryAt(entry.ordinal - 1).transaction) {
        "event ${entry.ordinal} continues transaction $serial with different metadata"
      }
    }
  }

  private fun lastSerial(): Int = if (size == 0) PREFIX_SERIAL else serialAt(size - 1)

  private fun serialAt(ordinal: Int): Int = entryAt(ordinal).transaction?.serial ?: PREFIX_SERIAL

  /** The transaction owning the most recent retained event, or null when none owns it. */
  public val lastTransaction: Transaction?
    get() = if (size == 0) null else entryAt(size - 1).transaction

  /** The transaction owning the event at [ordinal], or null for an initialization-prefix event. */
  public fun transactionAt(ordinal: Int): Transaction? {
    require(ordinal in 0 until size)
    return entryAt(ordinal).transaction
  }

  /**
   * The events owned by transaction [serial], or null when retained history has no such
   * transaction. The range is never empty.
   *
   * Serials never decrease along event ordinals and every transaction owns at least one event, so
   * transaction [serial] cannot begin before ordinal [serial] and this searches only from there.
   */
  public fun transactionRange(serial: Int): IntRange? {
    require(serial >= 0)
    val first = lowerBound(serial)
    if (first >= size || serialAt(first) != serial) return null
    return first until lowerBound(serial + 1)
  }

  /** The first ordinal whose transaction serial is at least [serial], or [nextOrdinal]. */
  private fun lowerBound(serial: Int): Int {
    var low = minOf(serial, size)
    var high = size
    while (low < high) {
      val middle = (low + high) / 2
      if (serialAt(middle) < serial) low = middle + 1 else high = middle
    }
    return low
  }

  /**
   * The first event ordinal of each retained transaction in ascending order, followed by
   * [nextOrdinal]. These are the positions between transactions: history with only an
   * initialization prefix offers just that prefix's end.
   */
  public val transactionStarts: List<Int>
    get() {
      val count = size
      return buildList {
        var previous = PREFIX_SERIAL
        for (ordinal in 0 until count) {
          val serial = serialAt(ordinal)
          if (serial != previous && serial != PREFIX_SERIAL) add(ordinal)
          previous = serial
        }
        add(count)
      }
    }

  private companion object {
    /** Stands for the absent transaction of an initialization-prefix event. */
    const val PREFIX_SERIAL = -1
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
