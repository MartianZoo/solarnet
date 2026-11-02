package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Actor
import dev.martianzoo.state.ComponentGraph
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.TaskQueue

/**
 * Live game world and the engines that interpret Actor commands. Current game state consists of
 * components and pending tasks; [events] is history. [components], [events], [tasks], and [reader]
 * are read surfaces over this same World; retaining them does not capture a snapshot. Later
 * gameplay and history navigation change what those views report. Use [recording] for an
 * independent record and [actorEngine] for Actor-attributed gameplay.
 *
 * The [intended interaction contract](../../../../../docs/agents/ENGINE_CONTRACT.md) assigns each
 * raw engine request its immediate consequences and invariant checks. Resulting Admin tasks belong
 * to later transactions submitted by the caller. Agent calls handle that subsequent work and report
 * their retained changes. General transaction-success uniqueness remains transition work.
 */
public interface World {
  /** Participating Actors: configured Players in seat order, followed by Admin. */
  public val actors: List<Actor>
    get() = reader.actors

  /** Live component counts and dependencies; the returned graph is not a snapshot. */
  public val components: ComponentGraph

  /**
   * Live event history, including component changes and task edits. Reading the history does not
   * execute effects. Event commentary is mutable metadata, not a game-state mutation.
   */
  public val events: EventLog

  /** Live view of every pending task, including selected tasks and deferred work, across Actors. */
  public val tasks: TaskQueue

  /**
   * Existing history navigation and failure-restoration controls; see [Timeline]'s review notes.
   */
  public val timeline: Timeline

  /** Read-only interpretation of types, counts, and requirements against the current World. */
  public val reader: GameReader

  /** Immutable class-table view selected by this World's premise. */
  public val classTable: ClassTable

  /**
   * Whether the World has neither pending tasks nor any `MustCleanUp` component. This does not mean
   * that the game has ended. A state awaiting an explicit Player choice is not idle.
   */
  public fun isIdle(): Boolean = tasks.isEmpty() && reader.has(parse("MAX 0 MustCleanUp"))

  /**
   * Returns this World's stable engine for [actor]. Calls for equal Actor identities return the
   * same instance. An Actor absent from [actors] throws [NoSuchElementException] without changing
   * the World. This selects attribution, not authentication of the program making the call.
   */
  public fun actorEngine(actor: Actor): ActorEngine
}
