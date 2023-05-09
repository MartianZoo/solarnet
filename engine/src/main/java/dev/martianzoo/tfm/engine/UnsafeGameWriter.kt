package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Task.TaskId

interface UnsafeGameWriter {
  fun removeTask(taskId: TaskId): TaskRemovedEvent

  /**
   * Updates the component graph and event log, but does not fire triggers. This exists as a
   * public method so that a broken game state can be fixed, or a game state broken on purpose, or
   * specific game scenario set up very explicitly.
   */
  fun sneakyChange(
    count: Int = 1,
    gaining: Component? = null,
    removing: Component? = null,
    cause: Cause? = null,
  ): ChangeEvent?
}
