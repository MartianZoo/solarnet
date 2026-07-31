package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.ClassTable

/**
 * A live Pets world with transactional mutation, pending tasks, and event history. A world is the
 * aggregation of three mutable child objects, which callers access directly: a [ComponentGraph], an
 * [EventLog], and a [TaskQueue]. These types embody the present, past, and future of the world
 * (respectively).
 *
 * These three views are read-only, but are always up-to-date (i.e., they are not immutable).
 * Modifying a world is done through [gameplay].
 *
 * The component graph can be queried programmatically, but a [GameReader] is also provided which
 * can answer queries expressed as a Pets [Metric] or [Requirement]. Setup worlds and game worlds
 * are specialized uses of the same machinery.
 */
public interface World {
  /** The current component graph. */
  public val components: ComponentGraph

  /** Everything that has already happened in this world. */
  public val events: EventLog

  /** What this world is waiting on an Actor to do. */
  public val tasks: TaskQueue

  /** Checkpoint, rollback, and atomic interaction control. */
  public val timeline: Timeline

  /** Higher-level Pets queries over this world. */
  public val reader: GameReader

  /** The immutable classes available to this world. */
  public val classTable: ClassTable

  public fun gameplay(actor: Actor): Gameplay

  /** Called after every outermost atomic operation completes. */
  public var onAtomicComplete: () -> Unit
}
