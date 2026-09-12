package dev.martianzoo.engine

import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.ClassTable

/**
 * A disposable engine world whose event suffix and mutable projections leave its base unchanged.
 */
internal class OverlayWorld
internal constructor(
    override val components: ComponentGraph,
    override val events: EventLog,
    internal val taskQueues: TaskQueues,
    override val timeline: Timeline,
    internal val readerImpl: GameReaderImpl,
    override val classTable: ClassTable,
    private val actorEngines: Map<Actor, ActorEngine>,
) : World {
  override val tasks: TaskQueue = taskQueues.all()

  override val reader: GameReader = readerImpl

  override fun actorEngine(actor: Actor): ActorEngine = actorEngines.getValue(actor)

  override var onTransactionComplete: () -> Unit = {}
}
