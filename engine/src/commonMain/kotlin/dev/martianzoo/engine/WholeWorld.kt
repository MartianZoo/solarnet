package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.types.ClassTable

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
    override val actors: List<Actor>,
    private val gameplayByActor: Map<Actor, Gameplay>,
) : World {
  /** The exact event-backed state revision, including changes later rolled back. */
  internal val revision: WorldRevision
    get() = events.revision

  override fun gameplay(actor: Actor): Gameplay = gameplayByActor[actor]!!

  override var onAtomicComplete: () -> Unit = {}
}
