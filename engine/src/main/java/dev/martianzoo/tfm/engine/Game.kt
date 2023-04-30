package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Result
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.EventLog
import dev.martianzoo.tfm.engine.Game.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.Game.PlayerAgent
import dev.martianzoo.tfm.engine.Game.TaskQueue
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MClassTable
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset
import kotlin.Int.Companion.MAX_VALUE

/**
 * The mutable state of a game in progress. This state is the aggregation of three mutable child
 * objects, which callers accesses directly: a [ComponentGraph], a [TaskQueue], and an [EventLog].
 * These types don't expose mutation operations, but the objects are mutable and always represent
 * the most current state.
 *
 * To read game state at a higher level (e.g. via Pets expressions), use [reader]. To change state
 * requires going through [asPlayer] to get a [PlayerAgent].
 */
public class Game
internal constructor(
    private val table: MClassTable,
    private val writableEvents: WritableEventLog = WritableEventLog(),
    private val writableComponents: WritableComponentGraph = WritableComponentGraph(),
    private val writableTasks: WritableTaskQueue = WritableTaskQueue(),
) {
  /** The components that make up the game's current state ("present"). */
  public val components: ComponentGraph = writableComponents

  /** The tasks the game is currently waiting on ("future"). */
  public val tasks: TaskQueue = writableTasks

  /** Everything that has happened in this game so far ("past"). */
  public val events: EventLog = writableEvents

  /**
   * A multiset of [Component] instances; the "present" state of a game in progress. It is a plain
   * multiset, but called a "graph" because these component instances have references to their
   * dependencies which are also stored in the multiset.
   */
  public interface ComponentGraph {
    /**
     * Does at least one instance of [component] exist currently? (That is, is [countComponent]
     * nonzero?
     */
    operator fun contains(component: Component): Boolean

    /** How many instances of the exact component [component] currently exist? */
    fun countComponent(component: Component): Int

    /** How many total component instances have the type [parentType] (or any of its subtypes)? */
    fun count(parentType: MType, einfo: ExpressionInfo): Int

    /**
     * Returns all component instances having the type [parentType] (or any of its subtypes), as a
     * multiset. The size of the returned collection will be `[count]([parentType])` . If
     * [parentType] is `Component` this will return the entire component multiset.
     */
    fun getAll(parentType: MType, einfo: ExpressionInfo): Multiset<Component>
  }

  /**
   * A complete record of everything that happened in a particular game (in progress or finished). A
   * complete game state could be reconstructed by replaying these events.
   */
  public interface EventLog {
    val size: Int

    public data class Checkpoint(internal val ordinal: Int) {
      init {
        require(ordinal >= 0)
      }
    }

    /**
     * Returns a [Checkpoint] that can be passed to [Game.rollBack] to return the game to its
     * present state, or to any of the `-Since` methods.
     */
    fun checkpoint(): Checkpoint

    /** Returns all change events since game setup was concluded. */
    fun changesSinceSetup(): List<ChangeEvent>

    /** Returns all change events since [checkpoint]. */
    fun changesSince(checkpoint: Checkpoint): List<ChangeEvent>

    /** Returns the ids of all tasks created since [checkpoint] that still exist. */
    fun newTasksSince(checkpoint: Checkpoint): Set<TaskId>

    fun entriesSince(checkpoint: Checkpoint): List<GameEvent>

    fun activitySince(checkpoint: Checkpoint): Result
  }

  /**
   * Contains tasks: what the game is waiting on someone to do. Each task is owned by some [Player]
   * (which could be the engine itself). Normally, a state should never been observed in which
   * engine tasks remain, as the engine should always be able to take care of them itself before
   * returning.
   *
   * This interface speaks entirely in terms of [TaskId]s.
   */
  public interface TaskQueue {
    val size: Int
    val ids: Set<TaskId>

    operator fun contains(id: TaskId): Boolean
    operator fun get(id: TaskId): Task

    fun isEmpty(): Boolean
    fun nextAvailableId(): TaskId
    fun toStrings(): List<String>
    fun asMap(): Map<TaskId, Task>
  }

  // Don't allow actual game logic to depend on the event log
  public val reader: GameReader = GameReaderImpl(table, components)

  internal val transformers by table::transformers

  public fun asPlayer(player: Player) = PlayerAgent(this, player)

  public fun removeTask(id: TaskId) = writableTasks.removeTask(id, writableEvents)

  public fun resolve(expression: Expression): MType = table.resolve(expression)

  /**
   * Returns a component instance of type [expression], whether such a component is part of the
   * current game state or not.
   */
  public fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  public fun checkpoint() = events.checkpoint()

  public fun rollBack(checkpoint: Checkpoint) {
    writableEvents.rollBack(checkpoint) {
      when (it) {
        is TaskEvent -> writableTasks.reverse(it)
        is ChangeEvent -> {
          writableComponents.reverse(
              it.change.count,
              removeWhatWasGained = it.change.gaining?.let(::toComponent),
              gainWhatWasRemoved = it.change.removing?.let(::toComponent),
          )
        }
      }
    }
  }

  public fun doAtomic(block: () -> Unit): Result {
    val checkpoint = checkpoint()
    return try {
      block()
      events.activitySince(checkpoint)
    } catch (e: Exception) {
      rollBack(checkpoint)
      throw e
    }
  }

  // A little odd that activeEffects is only on "writable" components but okay
  internal fun activeEffects(classes: Collection<MClass>): List<ActiveEffect> =
      writableComponents.activeEffects(classes)

  internal fun setupFinished() = writableEvents.setStartPoint()

  fun getTask(taskId: TaskId): Task {
    require(taskId in tasks) { taskId }
    return tasks[taskId]
  }

  fun isSystem(event: ChangeEvent): Boolean {
    val g = event.change.gaining
    val r = event.change.removing

    val system = resolve(ClassName.cn("System").expression)
    if (listOfNotNull(g, r).all { resolve(it).isSubtypeOf(system) }) return true

    if (r != null) {
      val signal = resolve(ClassName.cn("Signal").expression)
      if (resolve(r).isSubtypeOf(signal)) return true
    }
    return false
  }

  public fun clone() =
      Game(table, writableEvents.clone(), writableComponents.clone(), writableTasks.clone())

  internal fun addTriggeredTasks(fired: List<FiredEffect>) =
      writableTasks.addTasksFrom(fired, writableEvents)

  /**
   * A view of a [Game] specific to a particular [Player] (a player or the engine). Personalizes the
   * data to that player's perspective, and supports executing instructions to modify the game
   * state.
   */
  public class PlayerAgent internal constructor(public val game: Game, public val player: Player) {
    public fun session(defaultAutoExec: Boolean = true) = PlayerSession(this, defaultAutoExec)

    public val reader =
        object : GameReader by game.reader {
          override fun resolve(expression: Expression): MType = game.resolve(heyItsMe(expression))

          override fun evaluate(requirement: Requirement) =
              game.reader.evaluate(heyItsMe(requirement)) // TODO expInfo doesn't want this

          override fun count(metric: Metric) = game.reader.count(heyItsMe(metric))
        }

    private val insertOwner: PetTransformer =
        chain(game.transformers.deprodify(), replaceOwnerWith(player))

    @Suppress("UNCHECKED_CAST")
    private fun <P : PetNode?> heyItsMe(node: P): P = node?.let(insertOwner::transform) as P

    public fun getComponents(type: Expression): Multiset<Component> =
        game.components.getAll(reader.resolve(type) as MType, reader) // TODO think about

    public fun sneakyChange(
        count: Int = 1,
        gaining: Expression? = null,
        removing: Expression? = null,
        amap: Boolean = false,
        cause: Cause? = null,
    ): ChangeEvent? {
      when (gaining?.className) {
        OK -> return if (removing == null) null else throw UserException.removingOk(cause)
        DIE -> return if (amap) null else throw UserException.die(cause)
      }

      val toGain: Component? = gaining?.let { game.toComponent(heyItsMe(it)) }
      val toRemove: Component? = removing?.let { game.toComponent(heyItsMe(it)) }
      val change: StateChange? = game.writableComponents.update(count, toGain, toRemove, amap)
      return game.writableEvents.addChangeEvent(change, player, cause)
    }

    /**
     * Attempts to carry out the entirety of [instruction] "manually" or "out of the blue", plus any
     * *automatic* triggered effects that result. If any of that fails the game state will remain
     * unchanged and an exception will be thrown. If it succeeds, any non-automatic triggered
     * effects will be left in the task queue. No other changes to the task queue will happen (for
     * example, existing tasks are left alone, and [instruction] itself is never left enqueued.
     *
     * @param [instruction] an instruction to be performed as-is (no transformations will be
     *   applied)
     */
    fun initiate(instruction: Instruction, initialCause: Cause? = null): Result {
      val fixed = Instruction.split(heyItsMe(instruction))
      return game.doAtomic {
        val executor = InstructionExecutor(this, initialCause)
        fixed.forEach { executor.doInstruction(it) }
      }
    }

    fun tasks(): Map<TaskId, Task> = game.tasks.asMap()

    public fun doTask(taskId: TaskId, narrowed: Instruction? = null): Result {
      val requestedTask: Task = game.getTask(taskId)
      if (player != ENGINE && player != requestedTask.owner) {
        throw UserException("$player can't perform task owned by ${requestedTask.owner}")
      }

      val queued = requestedTask.instruction
      val specified = heyItsMe(narrowed)

      val toExecute: Instruction =
          if (specified == null) {
            queued
          } else {
            if (specified.isAbstract(game.reader)) {
              throw UserException.abstractInstruction(specified) // TODO be more specific
            }
            specified.ensureNarrows(queued, game.reader)
            specified
          }

      val checkpoint = game.checkpoint()
      try {
        val executor = InstructionExecutor(this, requestedTask.cause)
        executor.doInstruction(toExecute)
        removeTask(taskId)
      } catch (e: Exception) {
        game.rollBack(checkpoint)
        if (e is UserException && requestedTask.whyPending == null) {
          val explainedTask = requestedTask.copy(whyPending = e.message)
          game.writableTasks.replaceTask(explainedTask, game.writableEvents)
        } else {
          throw e
        }
      }
      return game.events.activitySince(checkpoint)
    }

    // TODO check owner
    public fun removeTask(taskId: TaskId) = game.removeTask(taskId)

    fun update(
        count: Int,
        gaining: Type? = null,
        removing: Type? = null,
        amap: Boolean,
        cause: Cause?,
    ): Result {
      val g = gaining?.expression
      val r = removing?.expression

      fun tryIt(): ChangeEvent? = sneakyChange(count, g, r, amap, cause)

      val cp = game.checkpoint()
      try {
        tryIt()
      } catch (e: ExistingDependentsException) {
        // There's sorta no better way to find out we need to remove dependents
        e.dependents.forEach { update(MAX_VALUE, removing = it.mtype, amap = true, cause = cause) }
        tryIt()
      }
      return game.events.activitySince(cp)
    }

    fun addTasks(instruction: Instruction, taskOwner: Player, cause: Cause?) {
      game.writableTasks.addTasksFrom(instruction, taskOwner, cause, game.writableEvents)
    }
  }
}
