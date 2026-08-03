package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Task
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.engine.TimelineImpl.AbortOperationException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Multi
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.types.Type
import dev.martianzoo.util.Multiset
import kotlin.reflect.KClass

/**
 * All modifications to a world (not counting rollbacks) are done via this interface.
 *
 * It should not be possible to break the world through this interface, except by calling [godMode]
 * which will then let you do whatever the heck you want. Or, the instance returned by [godMode]
 * could be cast to [TurnLayer], [OperationLayer], [TaskLayer] in order to hide methods you don't
 * need; see those interfaces for more explanation.
 */
public interface Gameplay {

  // READ OPERATIONS

  public val actor: Actor

  public fun <P : PetElement> parseInternal(type: KClass<P>, text: String): P

  public fun has(requirement: String): Boolean

  public fun count(metric: String): Int

  public fun list(type: String): Multiset<Expression>

  public fun resolve(expression: String): Type

  // Purple mode (and below)

  /**
   * Voluntarily replaces a task's instruction with a strictly more specific revision, as its
   * assignee is allowed to do. Preserves [Task.next], and if `true`, re-prepares the new
   * instruction if necessary. Executes nothing.
   *
   * @param [revised] the new instruction; may be abstract; if identical to the current instruction
   *   this method does nothing
   * @throws [TaskException] if there is no task by this id assigned to this gameplay's Actor
   * @throws [NarrowingException] if [revised] is not a valid narrowing of the task's instruction
   */
  public fun reviseTask(taskId: TaskId, revised: String): TaskResult

  /**
   * Revises the single task whose current instruction is [current]. This avoids depending on its
   * generated [TaskId] when the instruction itself identifies the task.
   */
  public fun reviseTask(current: String, revised: String): TaskResult

  /** Tells whether [prepareTask] will complete normallly. */
  public fun canPrepareTask(taskId: TaskId): Boolean

  /**
   * Sets a task's [Task.next] bit, and simplifies its instruction according to the current world.
   * It will be impossible to change the world except by executing this task.
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
  public fun prepareTask(taskId: TaskId): TaskId?

  /**
   * Prepares the single task whose current instruction is [instruction]. Equivalent tasks that
   * differ only by id are interchangeable.
   */
  public fun prepareTask(instruction: String): TaskId?

  /**
   * Brittle convenience that selects by the task set's non-semantic iteration order. Prefer an
   * explicit task id unless the caller has established that only one task can apply.
   */
  public fun doFirstTask(revised: String? = null): TaskResult

  /**
   * Carries out a concrete task. Prepares the task first if necessary. As part of this, executes
   * triggered instructions from *automatic* effects, enqueues tasks for queued effects and any
   * contents of [Task.then], and removes the original task from the game's task queue. Throws an
   * exception if any of this fails.
   *
   * @throws [TaskException] if no prepared task by the id [taskId] is present
   * @throws [AbstractException] if the task is abstract
   * @throws [NotNowException] if the task can't currently be prepared
   */
  public fun doTask(taskId: TaskId): TaskResult

  public fun doTask(revised: String): TaskResult

  public fun tryTask(taskId: TaskId): TaskResult

  public fun tryTask(revised: String): TaskResult

  public fun tryPreparedTask(): TaskResult

  public fun autoExecNow(): TaskResult

  public var autoExecMode: AutoExecMode

  public fun godMode(): GodMode

  // Blue mode

  public interface TurnLayer : Gameplay {
    public fun startTurn(): TaskResult

    public fun turn(body: BodyLambda = {}): TaskResult
  }

  // Green mode

  public interface OperationLayer : TurnLayer {
    public fun manual(initialInstruction: String, body: BodyLambda = {}): TaskResult

    public fun beginManual(initialInstruction: String, body: BodyLambda = {}): TaskResult

    public fun continueManual(body: BodyLambda = {}): TaskResult

    public fun finish(body: BodyLambda = {}): TaskResult
  }

  public interface OperationBody {
    public val tasks: TaskQueue
    public val reader: GameReader

    public fun doFirstTask(revised: String)

    public fun doTask(revised: String)

    public fun tryTask(revised: String)

    public fun autoExecNow()

    public fun abort(): Nothing = throw AbortOperationException()
  }

  // Yellow
  public interface TaskLayer : OperationLayer {
    /** Adds a manual task for the given [instruction], but does not prepare or execute it. */
    public fun addTasks(instruction: String, firstCause: Cause? = null): List<TaskId>

    /** Removes a task for any reason or no reason at all. */
    public fun dropTask(taskId: TaskId): TaskRemovedEvent

    /** Removes every task assigned to this gameplay's Actor. */
    public fun dropTasks(): List<TaskRemovedEvent>
  }

  // Red
  public interface GodMode : TaskLayer {
    public fun sneak(changes: String, fakeCause: Cause? = null): TaskResult
  }

  public companion object {
    public inline fun <reified P : PetElement> Gameplay.parse(text: String): P =
        parseInternal(P::class, text)
  }
}

public typealias BodyLambda = OperationBody.() -> Unit
