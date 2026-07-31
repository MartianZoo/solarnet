package dev.martianzoo.engine

import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Timeline.Checkpoint

/**
 * A complete record of everything that happened in a particular game (in progress or finished). A
 * complete world could be reconstructed by replaying these events.
 */
public interface EventLog {
  /** Returns all change events since engine initialization concluded, including game setup. */
  public fun changesSinceSetup(): List<GameEvent.ChangeEvent>

  public fun entriesSinceSetup(): List<GameEvent>

  /** Returns all change events since [checkpoint]. */
  public fun changesSince(checkpoint: Checkpoint): List<GameEvent.ChangeEvent>

  /** Returns the ids of all tasks created since [checkpoint] that still exist. */
  public fun newTasksSince(checkpoint: Checkpoint): Set<Task.TaskId>

  public fun entriesSince(checkpoint: Checkpoint): List<GameEvent>

  public fun activitySince(checkpoint: Checkpoint): TaskResult
}
