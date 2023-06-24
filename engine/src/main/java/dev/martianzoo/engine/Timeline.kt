package dev.martianzoo.engine

import dev.martianzoo.data.TaskResult

interface Timeline {
  fun checkpoint(): Checkpoint
  fun rollBack(checkpoint: Checkpoint)

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
