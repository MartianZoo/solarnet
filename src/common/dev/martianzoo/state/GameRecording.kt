package dev.martianzoo.state

import dev.martianzoo.catalog.GamePremise

/**
 * Immutable snapshot of [premise], an exact event history, and the positions exposed for playback.
 * Each position is immediately before a transaction, including the end before the next transaction.
 * Events have consecutive ordinals starting at zero. Positions are derived from their transaction
 * metadata. The constructor copies event values; [events] returns fresh event snapshots so callers
 * cannot edit the saved history through mutable commentary.
 */
public class GameRecording(
    public val premise: GamePremise,
    events: List<GameEvent>,
) {
  private val recordedEvents: List<GameEvent> = events.map(GameEvent::snapshot)

  /** An isolated copy of the recorded event values, including their captured commentary. */
  public val events: List<GameEvent>
    get() = recordedEvents.map(GameEvent::snapshot)

  private val offsets =
      EventLog().also { log -> recordedEvents.forEach(log::append) }.transactionStarts

  /** Transaction positions in ascending order; the last is the end of the recording. */
  public val positions: List<Checkpoint> = offsets.indices.map(::Checkpoint)

  init {
    recordedEvents.forEachIndexed { ordinal, event ->
      require(event.ordinal == ordinal) {
        "expected event ordinal $ordinal, got ${event.ordinal}"
      }
    }
  }

  /** Opens an independent passive view initially positioned at the recording's end. */
  public fun open(): Playback = Playback(this)

  /** One independently navigable materialization of a [GameRecording]. */
  public class Playback internal constructor(private val recording: GameRecording) {
    private val events = recording.recordedEvents.map(GameEvent::snapshot)
    /** Passive, independently mutable world at the currently selected [positionIndex]. */
    public val world: GameWorld = GameWorld(recording.premise, events)
    /** Recorded positions available to [seek]. */
    public val positions: List<Checkpoint>
      get() = recording.positions

    /** Index into [positions] currently materialized by [world]. Initially the last position. */
    public var positionIndex: Int = positions.lastIndex
      private set

    init {
      world.markSetupStart(recording.offsets.first())
    }

    /**
     * Moves this view immediately before transaction [index], without executing instructions. The
     * final index selects the end of the history. Invalid indices leave the view unchanged.
     */
    public fun seek(index: Int) {
      require(index in positions.indices) { "recording position $index does not exist" }
      val target = recording.offsets[index]
      if (world.nextOrdinal > target) {
        world.rollBackTo(target)
      } else {
        events.subList(world.nextOrdinal, target).forEach(world::apply)
      }
      positionIndex = index
    }
  }
}
