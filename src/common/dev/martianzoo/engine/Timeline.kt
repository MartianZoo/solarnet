package dev.martianzoo.engine

import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.TaskResult

/**
 * Completed gameplay positions and atomic history control for a live [World]. Seeking changes the
 * materialized world while retaining its later events. Gameplay may resume at an earlier position
 * only after [truncate] discards those events.
 */
public interface Timeline {
  /** Captures the current event position. It is seekable only if present in [positions]. */
  public fun checkpoint(): Checkpoint

  /** Records a caller-visible position after its synchronous action has finished. */
  public fun recordPosition()

  /** Completed positions; [seek] accepts those at or after [floor]. */
  public val positions: List<Checkpoint>

  /** Earliest position this World may seek to. Initialization and forking advance this floor. */
  public val floor: Checkpoint

  /**
   * Moves to a recorded position at or after [floor], retaining the history after it. Within an
   * atomic call, the destination cannot precede that call's start.
   */
  public fun seek(checkpoint: Checkpoint)

  /** Moves to the preceding completed position, if there is one after the commit floor. */
  public fun stepBack(): Boolean

  /** Moves to the following completed position, if there is one. */
  public fun stepForward(): Boolean

  /** Discards retained history after the current recorded position, allowing new gameplay. */
  public fun truncate()

  /**
   * Marks the current position as the seek floor: [seek] will refuse to go earlier than this.
   * Called after engine initialization and at a fork point, which nothing in this World can undo.
   * Gameplay sets no floor, so seeking may cross turn and phase changes.
   */
  public fun commit()

  /**
   * Performs [block] with failure-atomicity and returning a [TaskResult] describing what changed.
   * Within the block you can call `abort` to roll everything back but still have this method
   * complete normally.
   */
  public fun atomic(block: () -> Unit): TaskResult
}
