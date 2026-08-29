package dev.martianzoo.engine

import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.data.GameEvent

/** A completed world's event history, navigable only at completed gameplay positions. */
public class GameRecording
internal constructor(
    public val world: World,
    private val timeline: TimelineImpl,
    private val entries: List<GameEvent>,
    positions: List<Checkpoint>,
) {
  public val positions: List<Checkpoint> = positions

  public var positionIndex: Int = this.positions.lastIndex
    private set

  init {
    require(positions.isNotEmpty())
    require(positions.last().ordinal == entries.size)
    require(this.positions == this.positions.sortedBy { it.ordinal })
  }

  /** Moves the live read model to [positions]' [index]. */
  public fun seek(index: Int) {
    require(index in positions.indices) { "recording position $index does not exist" }
    timeline.seek(entries, positions[index])
    positionIndex = index
  }
}

/**
 * Captures this world's current history and completed gameplay positions for read-only navigation.
 */
public fun World.recording(): GameRecording {
  val wholeWorld = this as? WholeWorld ?: error("Unknown World implementation: ${this::class}")
  return wholeWorld.recording()
}
