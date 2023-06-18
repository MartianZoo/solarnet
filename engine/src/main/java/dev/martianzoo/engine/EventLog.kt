package dev.martianzoo.engine

import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Timeline.Checkpoint

/**
 * A complete record of everything that happened in a particular game (in progress or finished). A
 * complete game state could be reconstructed by replaying these events.
 */
public interface EventLog {
  /** Returns all change events since game setup was concluded. */
  fun changesSinceSetup(): List<GameEvent.ChangeEvent>
  fun entriesSinceSetup(): List<GameEvent>

  /** Returns all change events since [checkpoint]. */
  fun changesSince(checkpoint: Checkpoint): List<GameEvent.ChangeEvent>

  /** Returns the ids of all tasks created since [checkpoint] that still exist. */
  fun newTasksSince(checkpoint: Checkpoint): Set<Task.TaskId>

  fun entriesSince(checkpoint: Checkpoint): List<GameEvent>
  fun activitySince(checkpoint: Checkpoint): TaskResult
}
