package dev.martianzoo.engine

import dev.martianzoo.pets.data.TaskResult

public interface Timeline {
  public fun checkpoint(): Checkpoint

  public fun rollBack(checkpoint: Checkpoint)

  /**
   * Marks the current position as the rollback floor: [rollBack] will refuse to go earlier than
   * this. Called after engine initialization and by the automatic workflow after phase transitions,
   * so that players cannot undo the engine's structural decisions.
   */
  public fun commit()

  /**
   * Performs [block] with failure-atomicity and returning a [TaskResult] describing what changed.
   * Within the block you can call `abort` to roll everything back but still have this method
   * complete normally.
   */
  public fun atomic(block: () -> Unit): TaskResult

  public data class Checkpoint(public val ordinal: Int) {
    init {
      require(ordinal >= 0)
    }

    override fun toString(): String = "$ordinal"
  }
}
