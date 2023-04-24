package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.Task.TaskId

/**
 * A complete record of everything that happened in a particular game (in progress or finished). A
 * complete game state could be reconstructed by replaying these events.
 */
interface EventLog {
  val events: List<GameEvent>
  val size: Int

  data class Checkpoint(internal val ordinal: Int) {
    init {
      require(ordinal >= 0)
    }
  }

  fun checkpoint(): Checkpoint

  fun changesSince(checkpoint: Checkpoint): List<ChangeEvent>
  fun newTasksSince(checkpoint: Checkpoint): Set<TaskId>
  fun entriesSince(checkpoint: Checkpoint): List<GameEvent>
  fun activitySince(checkpoint: Checkpoint): Result
}
