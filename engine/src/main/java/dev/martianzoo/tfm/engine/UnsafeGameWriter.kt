package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.pets.ast.Instruction

interface UnsafeGameWriter {
  fun removeTask(taskId: TaskId): TaskRemovedEvent

  fun sneak(change: String, cause: Cause? = null): TaskResult

  fun sneak(change: Instruction, cause: Cause? = null): TaskResult

  /**
   * Gains [count] instances of [gaining] (if non-null) and removes [count] instances of
   * [removing] (if non-null), maintaining change-integrity. That means it modifies the component
   * graph, appends to the event log, and sends the new change event to [listener] (for example,
   * to fire triggers).
   *
   * Used during normal task execution, but can also be invoked manually to fix a broken game
   * state, break a fixed game state, or quickly set up a specific game scenario.
   */
  fun change(
          count: Int,
          gaining: Component?,
          removing: Component?,
          cause: Cause?,
          listener: (ChangeEvent) -> Unit,
  ): ChangeEvent

  /**
   * Like [change], but first removes any dependent components (recursively) that would otherwise
   * prevent the change. The same [cause] is used for all changes.
   */
  fun changeAndFixOrphans(
          count: Int = 1,
          gaining: Component? = null,
          removing: Component? = null,
          cause: Cause? = null,
          listener: (ChangeEvent) -> Unit = {},
  ) // : TaskResult? TODO

}
