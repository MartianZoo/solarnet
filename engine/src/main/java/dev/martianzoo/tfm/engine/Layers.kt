package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.AutoExecMode.SAFE
import dev.martianzoo.tfm.pets.ast.Instruction.Multi

object Layers {
  // Red
  public interface Changes : Tasks {
    fun sneak(changes: String, fakeCause: Cause? = null): TaskResult
  }

  // Yellow
  public interface Tasks : Operations {
    /** Adds a manual task for the given [instruction], but does not prepare or execute it. */
    fun addTasks(instruction: String, firstCause: Cause? = null): TaskResult

    /** Removes a task for any reason or no reason at all. */
    fun dropTask(taskId: TaskId): TaskRemovedEvent

    fun changesLayer(): Changes
  }

  // Green
  interface Operations : Turns {
    fun initiate(
        initialInstruction: String,
        autoExec: AutoExecMode = SAFE,
        body: NewOperationBody.() -> Unit = {}
    ): TaskResult

    fun tasksLayer(): Tasks
  }

  interface OperationBody {
    fun task(instruction: String)
    fun matchTask(instruction: String)
    fun abortAndRollBack()
  }

  interface NewOperationBody {
    fun doTask(revised: String)
    fun tryTask(revised: String)
    fun autoExecNow()
  }

  // Blue
  interface Turns : Games {
//    fun startTurn(): TaskResult
//    fun startTurn2(): TaskResult

    fun operationsLayer(): Operations
  }

  // Purple
  interface Games {
    // A task must already be waiting
//    fun operation(
//        vararg tasksInOrder: String,
//        autoExec: AutoExecMode = SAFE,
//        body: NewOperationBody.() -> Unit = {}
//    ): TaskResult

    fun turnsLayer(): Turns

    /**
     * Voluntarily replaces a task's instruction with a strictly more specific revision, as the
     * owner of an abstract task is allowed to do. Preserves [Task.next], and if `true`, re-prepares
     * the new instruction if necessary. Executes nothing.
     *
     * @param [revised] the new instruction; may be abstract; if identical to the current
     *   instruction this method does nothing
     * @throws [TaskException] if there is no task by this id owned by the player
     * @throws [NarrowingException] if [revised] is not a valid narrowing of the task's instruction
     */
    fun reviseTask(taskId: TaskId, revised: String): TaskResult

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
     * re-prepare the task. If no possible narrowing could succeed, this method might or might not
     * recognize that fact and throw instead.
     *
     * @throws [TaskException] if no task with id [taskId] exists, or if any other task is already
     *   prepared
     * @throws [AbstractException] if the task instruction contains a [Multi] at any level; it must
     *   first be narrowed until it splits into tasks that can be prepared individually
     * @throws [NotNowException] if the prepared task would throw this exception on execution
     */
    fun prepareTask(taskId: TaskId): TaskId?

    /**
     * Carries out a concrete task. Prepares the task first if necessary. As part of this, executes
     * any *automatic* triggered effect, enqueues the remaining triggered effects and any contents
     * of [Task.then], and removes the original task from the game's task queue. Throws an exception
     * if any of this fails.
     *
     * @throws [TaskException] if no prepared task by the id [taskId] is present
     * @throws [AbstractException] if the task is abstract
     * @throws [NotNowException] if the task can't currently be prepared
     */
    fun doTask(taskId: TaskId): TaskResult
    fun doTask(revised: String): TaskResult

    fun tryTask(taskId: TaskId): TaskResult
    fun tryTask(revised: String): TaskResult

    fun tryPreparedTask(): TaskResult
  }
}
