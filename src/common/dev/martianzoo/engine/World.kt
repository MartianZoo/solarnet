package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Actor
import dev.martianzoo.state.ComponentGraph
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.TaskQueue

/**
 * A live Pets world with transactional mutation, pending tasks, and event history. [components],
 * [events], and [tasks] are live read surfaces: retaining them keeps a view of this same world,
 * including later gameplay and rollback. Use [reader] for typed component queries and [actorEngine]
 * for actor-attributed changes. A [Timeline] checkpoint identifies an event position within this
 * world; it is not a separate copy of the world.
 */
public interface World {
  /** Every Actor participating in this world, with seated Players in seat order. */
  public val actors: List<Actor>
    get() = reader.actors

  /** Live component graph; queries reflect subsequent gameplay and rollback. */
  public val components: ComponentGraph

  /** Live event history, including task events and component changes. */
  public val events: EventLog

  /** Live view of all pending tasks, across actors. */
  public val tasks: TaskQueue

  /** Checkpoint, rollback, and atomic interaction control. */
  public val timeline: Timeline

  /** Typed, read-only queries over the current world. */
  public val reader: GameReader

  /** The immutable classes available to this world. */
  public val classTable: ClassTable

  /** Whether no task, mandatory cleanup, or temporary component remains unfinished. */
  public fun isIdle(): Boolean =
      tasks.isEmpty() && reader.has(parse("MAX 0 MustCleanUp, MAX 0 Temporary"))

  /** Returns this world's stable mutation engine for [actor]; an unknown actor fails. */
  public fun actorEngine(actor: Actor): ActorEngine

  /**
   * Callback after an outermost gameplay transaction commits its first completed position;
   * corrections do not invoke it. Synchronous gameplay started by the callback is settled before
   * the final position is recorded. If the callback throws, its exception propagates after the
   * original transaction has committed.
   */
  public var onTransactionComplete: () -> Unit
}
