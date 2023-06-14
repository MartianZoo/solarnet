package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.engine.Timeline.AbortOperationException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.util.Multiset
import kotlin.reflect.KClass

interface Gameplay {

  // READ OPERATIONS

  val player: Player

  fun <P : PetElement> parseInternal(type: KClass<P>, text: String): P

  fun has(requirement: String): Boolean

  fun count(metric: String): Int

  fun list(type: String): Multiset<Expression>

  fun resolve(expression: String): Type

  // Purple mode (and below)

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

  fun doFirstTask(revised: String? = null): TaskResult

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
  fun doTask(taskId: TaskId): TaskResult
  fun doTask(revised: String): TaskResult

  fun tryTask(taskId: TaskId): TaskResult
  fun tryTask(revised: String): TaskResult

  fun tryPreparedTask(): TaskResult

  fun autoExecNow(): TaskResult
  var autoExecMode: AutoExecMode

  fun godMode(): GodMode

  // Blue mode

  interface TurnLayer : Gameplay {
    fun startTurn(): TaskResult

    fun turn(body: BodyLambda = {}): TaskResult
  }

  // Green mode

  interface OperationLayer : TurnLayer {
    fun manual(initialInstruction: String, body: BodyLambda = {}): TaskResult

    fun beginManual(initialInstruction: String, body: BodyLambda = {}): TaskResult

    fun finish(body: BodyLambda = {}): TaskResult
  }

  interface OperationBody {
    val tasks: TaskQueue
    val reader: GameReader

    fun doFirstTask(revised: String)

    fun doTask(revised: String)
    fun tryTask(revised: String)

    fun autoExecNow()

    fun abort(): Nothing = throw AbortOperationException()
  }

  // Yellow
  public interface TaskLayer : OperationLayer {
    /** Adds a manual task for the given [instruction], but does not prepare or execute it. */
    fun addTasks(instruction: String, firstCause: Cause? = null): List<TaskId>

    /** Removes a task for any reason or no reason at all. */
    fun dropTask(taskId: TaskId): TaskRemovedEvent
  }

  // Red
  public interface GodMode : TaskLayer {
    fun sneak(changes: String, fakeCause: Cause? = null): TaskResult
  }

  companion object {
    public inline fun <reified P : PetElement> Gameplay.parse(text: String): P =
        parseInternal(P::class, text)
  }
}

typealias BodyLambda = OperationBody.() -> Unit
