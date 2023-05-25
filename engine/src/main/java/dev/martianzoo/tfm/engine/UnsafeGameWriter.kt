package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.pets.ast.Instruction

interface UnsafeGameWriter {
  /** Adds a manual task for the given [instruction], but does not prepare or execute it. */
  fun addTask(instruction: Instruction, firstCause: Cause? = null): TaskResult

  /** Forgets a task even existed. */
  fun dropTask(taskId: TaskId): TaskRemovedEvent

  fun sneak(changes: String, cause: Cause? = null): TaskResult

  fun sneak(changes: Instruction, cause: Cause? = null): TaskResult

  fun executeFully(instruction: Instruction, fakeCause: Cause? = null)
}
