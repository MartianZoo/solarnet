package dev.martianzoo.engine

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.ComponentGraph
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.state.WorldRevision

/** The live, complete implementation of a [World]. */
internal class WholeWorld
internal constructor(
    internal val gameWorld: GameWorld,
    override val timeline: Timeline,
    override val reader: GameReader,
    override val classTable: ClassTable,
    private val actorEngines: Map<Actor, ActorEngine>,
    private val timelineImpl: TimelineImpl,
    private val recordingPositions: RecordingPositions,
) : World {
  override val components: ComponentGraph
    get() = gameWorld.components

  override val events: EventLog
    get() = gameWorld.events

  override val tasks: TaskQueue
    get() = gameWorld.tasks

  /** The exact event-backed state revision, including changes later rolled back. */
  internal val revision: WorldRevision
    get() = gameWorld.revision

  override fun actorEngine(actor: Actor): ActorEngine = actorEngines.getValue(actor)

  override var onTransactionComplete: () -> Unit = {}

  internal fun recording(): GameRecording {
    val entries = events.entriesSince(Checkpoint(0))
    val positions =
        (recordingPositions.snapshot().filter { it.ordinal <= entries.size } +
                Checkpoint(entries.size))
            .distinct()
    timelineImpl.sealRecording(positions)
    return GameRecording(this, timelineImpl, entries, positions)
  }
}
