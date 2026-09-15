package dev.martianzoo.state

import dev.martianzoo.pets.data.GamePremise

/** Immutable event history and the completed gameplay positions it exposes. */
public class GameRecording(
    public val premise: GamePremise,
    events: List<GameEvent>,
    positions: List<Checkpoint>,
) {
  private val recordedEvents: List<GameEvent> = events.map(GameEvent::snapshot)

  /** An isolated copy of the recorded event values, including their captured commentary. */
  public val events: List<GameEvent>
    get() = recordedEvents.map(GameEvent::snapshot)

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
    public val world: GameWorld = GameWorld(recording.premise, events)
    public val positions: List<Checkpoint>
      get() = recording.positions

    public var positionIndex: Int = positions.lastIndex
      private set

    init {
      world.markSetupStart(positions.first())
    }

    /** Moves this view to the completed gameplay position at [index]. */
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

private fun GameEvent.snapshot(): GameEvent =
    when (this) {
      is GameEvent.ChangeEvent -> copy()
      is GameEvent.TaskAddedEvent -> copy()
      is GameEvent.TaskEditedEvent -> copy()
      is GameEvent.TaskRemovedEvent -> copy()
    }.also { copy -> copy.notes = notes }
