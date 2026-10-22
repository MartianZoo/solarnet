package dev.martianzoo.engine

import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.TaskResult

/**
 * Existing history controls for a live [World]. A checkpoint counts recorded events; it is not a
 * copy of the World. [positions] identifies places explicitly exposed for navigation. Pending
 * Player work may remain at such a position, so "recorded" does not mean the game action is over.
 *
 * Seeking changes the live components, tasks, and derived effect state, retaining later events for
 * forward navigation. New gameplay requires [truncate] before replacing that retained future.
 * [atomic] supplies failure restoration for a block; it does not itself interpret instructions,
 * check game invariants, run Admin tasks, or record a navigable position.
 *
 * TODO(ENGINE_CONTRACT): Review public history controls while preserving safe client-controlled
 *   rollback. The [proposed interaction contract](../../../../../docs/agents/ENGINE_CONTRACT.md)
 *   remains under human review. In particular, how far an unsuccessful call may undo earlier
 *   successful calls must not be decided by these existing methods.
 */
public interface Timeline {
  /** Captures the current event count. It is navigable only if also present in [positions]. */
  public fun checkpoint(): Checkpoint

  /**
   * Records the current event count as a navigable position, without adding an event. Recording
   * that same position again does nothing. New positions require the end of retained history.
   *
   * The caller must finish the work whose result it exposes first. Currently this method trusts the
   * caller: it checks neither invariants nor completion of required Admin work.
   */
  public fun recordPosition()

  /**
   * Snapshot of recorded positions in ascending order. It may include retained future positions
   * after [checkpoint], and positions before [floor] that can no longer be visited.
   */
  public val positions: List<Checkpoint>

  /**
   * Earliest navigable position. Initialization and [Engine.fork] set it to their resulting state.
   */
  public val floor: Checkpoint

  /**
   * Moves the live World to [checkpoint] without recalculating game consequences. Both the current
   * position and destination must be recorded. The destination must be at or after [floor] and,
   * inside [atomic], cannot precede the innermost block's start. Invalid positions fail before
   * navigation begins. Moving backward retains later events; moving forward reapplies them.
   */
  public fun seek(checkpoint: Checkpoint)

  /**
   * Moves to the nearest earlier recorded position allowed by [floor] and any active [atomic]
   * block. Returns true if it moves, false if no earlier position is allowed. [seek]'s requirement
   * that the current position be recorded still applies.
   */
  public fun stepBack(): Boolean

  /**
   * Moves to the next recorded position, returning true if it moves or false when already at the
   * last position. Uses [seek], so it does not run effects or choose task instructions again.
   */
  public fun stepForward(): Boolean

  /**
   * Discards retained events and positions after the current recorded position, allowing new
   * gameplay there. The current position must be recorded. Components and pending tasks are
   * unchanged; this cannot restore anything previously discarded.
   */
  public fun truncate()

  /**
   * Sets [floor] to the current event count, preventing later navigation to earlier history.
   * Requires the end of retained history. Earlier events remain readable; this does not record a
   * position, validate tasks, or establish agreement among Players.
   *
   * TODO(ENGINE_CONTRACT): Investigate restricting this to initialization and copy construction.
   *   Calling it from [atomic] currently changes the floor even if the block later fails.
   */
  public fun commit()

  /**
   * Runs [block] and returns the component changes and newly pending tasks recorded during it. On
   * an [Exception], restores the block's starting components, tasks, derived effects, and events
   * before rethrowing. Recorded positions after the start are removed.
   * [Exceptions.AbortTransactionException] instead calls the same restoration and a normal return
   * with an empty result.
   *
   * Requires the end of retained history. Nested calls restore only their own work unless their
   * exception also escapes an enclosing call. The block must perform any needed invariant checks
   * through the mutation API; this method alone supplies no transaction-completion checks.
   *
   * Current limits: arbitrary Kotlin side effects, existing event commentary, callback settings,
   * and changes to [floor] are not restored. A position newly recorded at the block's starting
   * event count also survives restoration. Non-[Exception] failures are not caught.
   *
   * TODO(ENGINE_CONTRACT): Review these limits against the safe rollback clients need when engine
   *   methods leave partial changes. Keep the restored state and excluded metadata explicit.
   */
  public fun atomic(block: () -> Unit): TaskResult
}
