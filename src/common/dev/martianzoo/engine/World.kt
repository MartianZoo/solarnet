package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Actor
import dev.martianzoo.state.ComponentGraph
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.TaskQueue

/**
 * Live game state and the engines that interpret Actor commands. [components], [events], [tasks],
 * and [reader] are read surfaces over this same World; retaining them does not capture a snapshot.
 * Later gameplay and history navigation change what those views report. Use [recording] for an
 * independent record and [actorEngine] for Actor-attributed gameplay.
 *
 * The [proposed interaction contract](../../../../../docs/agents/ENGINE_CONTRACT.md) is under human
 * review. It describes a transaction as a client command plus its immediate consequences and
 * invariant checks, with resulting Admin tasks handled by an outside client in later transactions.
 * The synchronous API should run to quiescence before returning and report all changes made during
 * the call. The individual members identify current differences from that intent.
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

  /**
   * Optional application check shared by clients using the Agent API; null until installed. Direct
   * [ActorEngine] calls do not invoke it. See [AgentTransactionGuard] for its restricted mutation
   * contract and the pending review of its placement in the engine module.
   */
  public val agentTransactionGuard: AgentTransactionGuard?

  /**
   * Installs [guard] for this World. It cannot subsequently be removed or replaced, even by the
   * same instance. A [SpeculativeWorld] rejects installation.
   *
   * Install before submitting gameplay that depends on the check. Currently this method enforces
   * only installation once; it does not verify that no earlier client command has run, validate
   * existing history, or invoke [AgentTransactionGuard.validate].
   */
  public fun installAgentTransactionGuard(guard: AgentTransactionGuard)

  /**
   * Hook invoked when an outermost [ActorEngine.transact] finishes its initial restoring block
   * normally, whether called directly or through an Agent. A cancellation caught by that block also
   * counts as normal completion and invokes the hook. Defaults to doing nothing. Corrections do not
   * invoke it. Synchronous commands started here can change the World before the originating call
   * returns. Currently those changes and subsequent task handling are omitted from its returned
   * result; they must be included under the proposed TaskResult rule.
   *
   * Proposed contract, pending human review: completion must not expose a Player-facing state with
   * eligible Admin work pending. Current placement does not establish that guarantee: the hook runs
   * outside the initial failure-restoring block. If it throws, the originating call's earlier
   * changes survive and the exception propagates. For an Agent call, the outer Agent method then
   * records no position, although nested calls may already have recorded theirs.
   *
   * TODO(ENGINE_CONTRACT): Investigate whether this callback belongs in the public engine API. Its
   *   name must not imply notification after every individual Actor transaction. Review its timing
   *   while preserving quiescence before return and complete result reporting.
   */
  public var onTransactionComplete: () -> Unit
}
