package dev.martianzoo.engine

import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.engine.TimelineImpl.AbortOperationException
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.KindException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.Task.TaskId
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.Multiset
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

  /**
   * Parses and preprocesses [text]. Preprocessing may change its major kind; callers that require a
   * particular result kind should use [parse].
   */
  public fun parseInternal(type: KClass<out PetElement>, text: String): PetElement

  public fun has(requirement: String): Boolean

  public fun count(metric: String): Int

  public fun list(type: String): Multiset<Expression>

  public fun resolve(expression: String): Type

  // Purple mode (and below)

  /**
   * Narrows this Actor's selected task and resolves it again. A partial narrowing remains selected;
   * a concrete result executes before this call returns.
   *
   * @param [narrowing] the new instruction tree; may be abstract or a grouped arm selected from an
   *   `OR`; a group replaces this one task with one task per member; if identical to the current
   *   instruction this method does nothing; an omitted intensity retains a stronger pending
   *   intensity when the Class default would weaken it
   * @throws [TaskException] if this Actor has no selected task
   * @throws [NarrowingException] if [narrowing] does not narrow the selected task's instruction
   */
  public fun narrowTask(narrowing: String): TaskResult

  /** Tells whether [selectTask] will complete normally. */
  public fun canSelectTask(taskId: TaskId): Boolean

  /**
   * Selects one pending task and resolves its instruction against the current World. An abstract
   * result remains selected for later [narrowTask] calls. A concrete result executes before this
   * call returns.
   *
   * If resolution produces independent instructions, selecting the structural task completes it and
   * admits those instructions as ordinary pending siblings.
   *
   * @throws [TaskException] if no task with id [taskId] exists, or if any other task is already
   *   selected
   * @throws [NotNowException] if the selected task cannot execute in the current World
   */
  public fun selectTask(taskId: TaskId): TaskResult

  /**
   * Selects the single pending task whose current instruction is [instruction]. Equivalent tasks
   * that differ only by id are interchangeable.
   */
  public fun selectTask(instruction: String): TaskResult

  /**
   * Carries out the task matched by the source-level [narrowing] instruction tree. A grouped tree
   * can select a grouped choice and replace the matched task with independent tasks; preprocessing
   * can produce the same replacement, for example when atomizing a multi-step global parameter
   * gain. Explicitly submitted grouped instructions execute as one bundled command; siblings
   * exposed only by preprocessing or resolution remain ordinary pending tasks. Selects and resolves
   * the matched task first if necessary. As part of this, executes triggered instructions from
   * *automatic* effects, enqueues tasks for queued effects and any contents of [Task.then], and
   * removes the original task from the game's task queue. Throws an exception if any of this fails.
   *
   * A selected task always wins. Otherwise, the narrowing must match exactly one task, except that
   * fully identical tasks are interchangeable. [taskNumber], when supplied, selects the 1-based
   * position in this Actor's current task list. When the narrowing omits an intensity and its Class
   * default would weaken the pending task's intensity, the pending intensity is retained; an
   * explicitly written intensity must narrow normally.
   *
   * @throws [AbstractException] if the task is abstract
   * @throws [NotNowException] if the task can't currently be resolved
   */
  public fun doTask(narrowing: String, taskNumber: Int? = null): TaskResult

  public fun tryTask(narrowing: String, taskNumber: Int? = null): TaskResult

  public fun autoExecNow(): TaskResult

  public var autoExecMode: AutoExecMode

  public fun godMode(): GodMode

  // Blue mode

  public interface TurnLayer : Gameplay {
    public fun startTurn(): TaskResult

    public fun inTurn(body: BodyLambda = {}): TaskResult
  }

  // Green mode

  public interface OperationLayer : TurnLayer {
    /** Starts and completes an operation seeded by one or more independent instructions. */
    public fun manual(initialInstructions: String, body: BodyLambda = {}): TaskResult

    /** Starts a resumable operation seeded by one or more independent instructions. */
    public fun beginManual(initialInstructions: String, body: BodyLambda = {}): TaskResult

    public fun continueManual(body: BodyLambda = {}): TaskResult

    public fun finish(body: BodyLambda = {}): TaskResult
  }

  public interface OperationBody {
    public val tasks: TaskQueue
    public val reader: GameReader

    public fun doTask(narrowing: String, taskNumber: Int? = null)

    public fun tryTask(narrowing: String, taskNumber: Int? = null)

    public fun autoExecNow()

    public fun abort(): Nothing = throw AbortOperationException()
  }

  // Yellow
  public interface TaskLayer : OperationLayer {
    /** Adds a manual task for the given [instruction], but does not select or execute it. */
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
    public inline fun <reified P : PetElement> Gameplay.parse(text: String): P {
      val parsed = parseInternal(P::class, text)
      if (parsed !is P) {
        throw KindException(
            "Preprocessing produced `$parsed`, which is not a ${P::class.simpleName}"
        )
      }
      return parsed
    }
  }
}

public typealias BodyLambda = OperationBody.() -> Unit
