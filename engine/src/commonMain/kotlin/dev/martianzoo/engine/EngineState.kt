package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.TypeUniverse

/**
 * A live Pets component system with transactional mutation, pending tasks, and event history. This
 * state is the aggregation of three mutable child objects, which callers access directly: a
 * [ComponentGraph], an [EventLog], and a [TaskQueue]. These types embody the present, past, and
 * future of the state (respectively).
 *
 * These three state objects are read-only, but are always up-to-date (i.e., they are not
 * immutable). Modifying engine state is done through [gameplay].
 *
 * The component graph can be queried programmatically, but a [GameReader] is also provided which
 * can answer queries expressed as a Pets [Metric] or [Requirement].
 */
public interface EngineState {
  /** The current component graph. */
  public val components: ComponentGraph

  /** Everything that has already happened in this state. */
  public val events: EventLog

  /** What this state is waiting on an [Actor] to do. */
  public val tasks: TaskQueue

  /** Checkpoint, rollback, and atomic interaction control. */
  public val timeline: Timeline

  /** Higher-level Pets queries over this state. */
  public val reader: GameReader

  /** The immutable classes available to this state. */
  public val typeUniverse: TypeUniverse

  /** Returns the mutation API scoped to [actor]. */
  public fun gameplay(actor: Actor): Gameplay

  /** Called after every outermost atomic operation completes. */
  public var onAtomicComplete: () -> Unit
}
