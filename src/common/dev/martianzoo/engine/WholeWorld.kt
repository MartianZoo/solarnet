package dev.martianzoo.engine

import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.types.ClassTable

/** The live, complete implementation of a [World]. */
internal class WholeWorld
internal constructor(
    override val components: ComponentGraph,
    override val events: EventLog,
    override val tasks: TaskQueue,
    override val timeline: Timeline,
    override val reader: GameReader,
    override val classTable: ClassTable,
    override val vocabulary: Vocabulary,
    private val gameplayByActor: Map<Actor, Gameplay>,
    private val timelineImpl: TimelineImpl,
    private val recordingPositions: RecordingPositions,
    private val premise: GamePremise,
) : World {
  /** The exact event-backed state revision, including changes later rolled back. */
  internal val revision: WorldRevision
    get() = events.revision

  override fun gameplay(actor: Actor): Gameplay = gameplayByActor[actor]!!

  override fun export(): String = exportWorld(this, premise)

  override var onAtomicComplete: () -> Unit = {}

  internal fun recording(): GameRecording {
    val entries = events.entriesSince(Timeline.Checkpoint(0))
    val positions =
        (recordingPositions.snapshot().filter { it.ordinal <= entries.size } +
                Timeline.Checkpoint(entries.size))
            .distinct()
    timelineImpl.sealRecording(positions)
    return GameRecording(this, timelineImpl, entries, positions)
  }
}
