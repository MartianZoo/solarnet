package dev.martianzoo.engine

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.ComponentGraph
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.TaskQueue

/** The live, complete implementation of a [World]. */
internal class WholeWorld
internal constructor(
    internal val gameWorld: GameWorld,
    override val timeline: Timeline,
    override val reader: GameReader,
    override val classTable: ClassTable,
    private val actorEngines: Map<Actor, ActorEngine>,
    internal val recordingPositions: RecordingPositions,
) : World {
  override val components: ComponentGraph
    get() = gameWorld.components

  override val events: EventLog
    get() = gameWorld.events

  override val tasks: TaskQueue
    get() = gameWorld.tasks

  override fun actorEngine(actor: Actor): ActorEngine = actorEngines.getValue(actor)

  override var onTransactionComplete: () -> Unit = {}
}
