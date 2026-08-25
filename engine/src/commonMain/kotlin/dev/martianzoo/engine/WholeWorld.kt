package dev.martianzoo.engine

import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.ClassTable

/** The live, complete implementation of a [World]. */
// TODO: Contract this temporary tfm-tests seam.
public class WholeWorld
public constructor(
    override val components: ComponentGraph,
    override val events: EventLog,
    override val tasks: TaskQueue,
    override val timeline: Timeline,
    override val reader: GameReader,
    override val classTable: ClassTable,
    override val vocabulary: Vocabulary,
    private val gameplayByActor: Map<Actor, Gameplay>,
) : World {
  /** The exact event-backed state revision, including changes later rolled back. */
  public val revision: WorldRevision
    get() = events.revision

  override fun gameplay(actor: Actor): Gameplay = gameplayByActor[actor]!!

  override var onAtomicComplete: () -> Unit = {}
}
