package dev.martianzoo.engine

import dev.martianzoo.data.TaskResult

public interface Timeline {
  fun checkpoint(): Checkpoint
  fun rollBack(checkpoint: Checkpoint)

  /**
   * Marks the current position as the rollback floor: [rollBack] will refuse to go earlier than
   * this. Called by the workflow after each phase transition, before suspending for player input,
   * so that players cannot undo the engine's structural decisions.
   */
  fun commit()

  /**
   * Performs [block] with failure-atomicity and returning a [TaskResult] describing what changed.
   * Within the block you can call `abort` to roll everything back but still have this method
   * complete normally.
   */
  fun atomic(block: () -> Unit): TaskResult

  public data class Checkpoint(internal val ordinal: Int) {
    init {
      require(ordinal >= 0)
    }
    override fun toString() = "$ordinal"
  }
}
