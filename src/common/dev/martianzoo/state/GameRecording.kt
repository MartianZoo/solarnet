package dev.martianzoo.state

import dev.martianzoo.catalog.GamePremise

/**
 * Immutable snapshot of [premise], an exact event history, and the positions exposed for playback.
 * Positions may include the state immediately before a recorded player request. Events must have
 * consecutive ordinals starting at zero. [positions] must be nonempty, strictly increasing, and end
 * at the event count. The constructor copies event values and positions; [events] returns fresh
 * event snapshots so callers cannot edit the saved history through mutable commentary.
 */
public class GameRecording(
    public val premise: GamePremise,
    events: List<GameEvent>,
    positions: List<Checkpoint>,
) {
  private val recordedEvents: List<GameEvent> = events.map(GameEvent::snapshot)

  /** An isolated copy of the recorded event values, including their captured commentary. */
  public val events: List<GameEvent>
    get() = recordedEvents.map(GameEvent::snapshot)

  /** Playback positions in ascending event order; the last is the end of the recording. */
  public val positions: List<Checkpoint> = positions.toList()

  init {
    require(this.positions.isNotEmpty())
    require(this.positions == this.positions.sortedBy(Checkpoint::ordinal))
    require(this.positions.distinct() == this.positions)
    require(this.positions.last().ordinal == recordedEvents.size)
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
      world.markSetupStart(positions.first())
    }

    /**
     * Moves this view to the recorded position at [index]. For example, if [positions] have
     * ordinals 0, 4, and 9, `seek(1)` materializes the world after the first four events and sets
     * [positionIndex] to 1. An invalid index throws without changing this view.
     */
    public fun seek(index: Int) {
      require(index in positions.indices) { "recording position $index does not exist" }
      val target = positions[index].ordinal
      if (world.nextOrdinal > target) {
        world.rollBackTo(target)
      } else {
        events.subList(world.nextOrdinal, target).forEach(world::apply)
      }
      positionIndex = index
    }
  }
}
