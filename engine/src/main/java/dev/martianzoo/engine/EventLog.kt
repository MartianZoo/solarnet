package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.TaskResult

/**
 * A complete record of everything that happened in a particular game (in progress or finished). A
 * complete game state could be reconstructed by replaying these events.
 */
public interface EventLog {

  val size: Int

  /**
   * Returns a [Checkpoint] that can be passed to [Game.rollBack] to return the game to its present
   * state, or to any of the `-Since` methods.
   */
  fun checkpoint(): Timeline.Checkpoint

  /** Returns all change events since game setup was concluded. */
  fun changesSinceSetup(): List<GameEvent.ChangeEvent>
  fun entriesSinceSetup(): List<GameEvent>

  /** Returns all change events since [checkpoint]. */
  fun changesSince(checkpoint: Timeline.Checkpoint): List<GameEvent.ChangeEvent>

  /** Returns the ids of all tasks created since [checkpoint] that still exist. */
  fun newTasksSince(checkpoint: Timeline.Checkpoint): Set<Task.TaskId>

  fun entriesSince(checkpoint: Timeline.Checkpoint): List<GameEvent>
  fun activitySince(checkpoint: Timeline.Checkpoint): TaskResult
}
