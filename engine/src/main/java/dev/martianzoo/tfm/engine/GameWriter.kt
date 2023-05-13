package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.pets.ast.Instruction

/** Supports modifying a game state. */
public abstract class GameWriter {
  /**
   * Prepares a task for execution. The task can only execute against the game state as it existed
   * when this method was called (i.e. it must be the next task executed, and any rollback would
   * erase the effects of calling this).
   * * Ensures no task is already marked as prepared (including this one)
   * * Marks this task as the next one that must be executed
   * * Auto-narrows the task based on current game state
   * * If the task is impossible to perform in the *current* game state, throws NotNowException
   * * The task might remain abstract; f so it will have to be reified by [tryTask]
   *
   * Failure-atomic.
   *
   * There is currently no way to manually narrow a task without executing it.
   */
  abstract fun prepareTask(taskId: TaskId): Boolean

  /**
   * Carries out a task and any automatic triggered effects. Enqueues the "THEN" clause of the task
   * (if it exists) and any non-automatic triggered effects. Removes the original task from the
   * game's task queue.
   *
   * Throws AbstractTaskException is the task remains abstract (even after narrowing) Throws
   * NotNowException if the task is impossible to complete in the current game state Throws
   * DoesNotReifyException if [narrowed] is not a valid reification of the task
   *
   * The [TaskResult] lists all state changes that happened as a result, and any tasks that exist
   * now but didn't before this call.
   *
   * Failure-atomic.
   */
  abstract fun tryTask(taskId: TaskId, narrowed: Instruction? = null): TaskResult

  // TODO maybe don't create the task at all
  fun tryTask(instruction: Instruction, initialCause: Cause? = null) =
      tryTask(addTask(instruction, initialCause)) // TODO result won't include task?

  /**
   * Like executeTask, but upon any failure that could possibly be remedied, merely fills in the
   * task's [Task.whyPending] property, changes no other state, and completes normally.
   *
   * TODO: the result should include replaced tasks somehow.
   */
  abstract fun doTask(taskId: TaskId, narrowed: Instruction? = null): TaskResult

  // TODO maybe don't create the task at all
  fun doTask(instruction: Instruction, initialCause: Cause? = null) =
      doTask(addTask(instruction, initialCause)) // TODO result won't include task?

  /** Just enqueues a task; does nothing about it. */
  abstract fun addTask(instruction: Instruction, initialCause: Cause? = null): TaskId
}
