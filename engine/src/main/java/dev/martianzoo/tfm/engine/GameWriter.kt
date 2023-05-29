package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Multi

/**
 * Supports modifying a game state. All operations are failure-atomic: they either fully succeed and
 * complete normally, or throw an exception leaving all game state unmodified.
 *
 * With any change to the task queue, a set of normalizations is *always* applied. Here, the
 * notation `a >> b` is used for a task whose [Task.instruction] is `a` and whose [Task.then] is
 * `b`.
 * * Removing task `a >> b` first creates task `b >> null`
 * * `Ok >> b` is removed
 * * `Die >> b` or `a >> Die` produces [DeadEndException]
 * * `a, b >> null` is split into `a >> null` and `b >> null`
 * * `a, b >> c` produces some exception (which?)
 * * `a THEN b >> null` where `a THEN b` is separable is rewritten to `a >> b`
 * * `a THEN b >> c` where `a THEN b` is separable is rewritten to `a >> b THEN c`
 * * `a, Ok` becomes `a`
 * * `a, Die` becomes `Die`
 * * A concrete task with [Task.next] set is guaranteed to execute successfully
 *
 * New tasks created have the same owner and cause as the original. Prepared tasks cannot be split.
 *
 * All methods of this type are failure-atomic: if one throws an exception, it leaves the game state
 * unmodified.
 */
public interface GameWriter {
  /**
   * Voluntarily replaces a task's instruction with a strictly more specific revision, as the owner
   * of an abstract task is allowed to do. Preserves [Task.next], and if `true`, re-prepares the new
   * instruction if necessary. Executes nothing.
   *
   * @param [revised] the new instruction; may be abstract; if identical to the current instruction
   *   this method does nothing
   * @throws [TaskException] if there is no task by this id owned by the player
   * @throws [NarrowingException] if [revised] is not a valid narrowing of the task's instruction
   */
  fun reviseTask(taskId: TaskId, revised: Instruction): TaskResult // TODO -TaskResult

  /** Tells whether [prepareTask] will complete normallly. */
  fun canPrepareTask(taskId: TaskId): Boolean

  /**
   * Sets a task's [Task.next] bit, and simplifies its instruction according to the current game
   * state. It will be impossible to change the game state except by executing this task.
   *
   * If the prepared task is concrete, but would fail to execute, that exception is thrown now
   * instead of preparing the task.
   *
   * If the return task is abstract, it will require a further call to [reviseTask], which will
   * re-prepare the task. If no possible revision could succeed, this method might or might not
   * recognize that fact and throw instead.
   *
   * @throws [TaskException] if no task with id [taskId] exists, or if any other task is already
   *   prepared
   * @throws [AbstractException] if the task instruction contains a [Multi] at any level; it must
   *   first be revised until it splits into tasks that can be prepared individually
   * @throws [NotNowException] if the prepared task would throw this exception on execution
   */
  fun prepareTask(taskId: TaskId): TaskId?

  /**
   * Carries out a concrete task. Prepares the task first if necessary. As part of this, executes
   * any *automatic* triggered effect, enqueues the remaining triggered effects and any contents of
   * [Task.then], and removes the original task from the game's task queue. Throws an exception if
   * any of this fails.
   *
   * @throws [TaskException] if no prepared task by the id [taskId] is present
   * @throws [AbstractException] if the task is abstract
   * @throws [NotNowException] if the task can't currently be prepared
   */
  fun executeTask(taskId: TaskId): TaskResult

  /** Replaces the [Task.whyPending] property of the specified task with [reason]. */
  fun explainTask(taskId: TaskId, reason: String)

  fun executeFully(instruction: Instruction, fakeCause: Cause? = null)

  /** Adds a manual task for the given [instruction], but does not prepare or execute it. */
  fun addTasks(instruction: Instruction, firstCause: Cause? = null): TaskResult

  /** Forgets a task even existed. */
  fun dropTask(taskId: TaskId): TaskRemovedEvent

  fun sneak(changes: String, cause: Cause? = null): TaskResult

  fun sneak(changes: Instruction, cause: Cause? = null): TaskResult
}
