package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.ClassTable

/**
 * A live Pets world with transactional mutation, pending tasks, and event history. A world is the
 * aggregation of three mutable child objects, which callers access directly: a [ComponentGraph], an
 * [EventLog], and a [TaskQueue]. These types embody the present, past, and future of the world
 * (respectively).
 *
 * These are live objects rather than read-only/writable interface pairs. Their engine-internal
 * mutation operations live on the same types, while [gameplay] remains the public coordinated route
 * for complete world operations.
 *
 * A [GameReader] provides the public component queries, including queries expressed as a Pets
 * [Metric] or [Requirement].
 */
public interface World {
  /** Every Actor participating in this world, with seated Players in seat order. */
  public val actors: List<Actor>
    get() = reader.actors

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

  /** Session-specific localized input and rendering names. */
  public val vocabulary: Vocabulary

  /** Whether no task or temporary component remains from an unfinished operation. */
  public fun isIdle(): Boolean = tasks.isEmpty() && reader.has(parse("MAX 0 Temporary"))

  /** Exports this idle world as a versioned, workflow-driven REgo replay. */
  public fun export(): String

  public fun gameplay(actor: Actor): Gameplay

  /** Called after every outermost atomic operation completes. */
  public var onAtomicComplete: () -> Unit
}
