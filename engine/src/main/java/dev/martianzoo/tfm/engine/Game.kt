package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.UserException.AbstractException
import dev.martianzoo.tfm.api.UserException.ExistingDependentsException
import dev.martianzoo.tfm.api.UserException.LimitsException
import dev.martianzoo.tfm.api.UserException.NotNowException
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.EventLog
import dev.martianzoo.tfm.engine.Game.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.Game.PlayerAgentImpl
import dev.martianzoo.tfm.engine.Game.TaskQueue
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MClassTable
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset

/**
 * The mutable state of a game in progress. This state is the aggregation of three mutable child
 * objects, which callers accesses directly: a [ComponentGraph], a [TaskQueue], and an [EventLog].
 * These types don't expose mutation operations, but the objects are mutable and always represent
 * the most current state.
 *
 * To read game state at a higher level (e.g. via Pets expressions), use [reader]. To change state
 * requires going through [asPlayer] to get a [PlayerAgentImpl].
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

    fun findLimit(gaining: Component?, removing: Component?): Int
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

    fun activitySince(checkpoint: Checkpoint): TaskResult
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
    fun ids(): Set<TaskId>

    operator fun contains(id: TaskId): Boolean
    operator fun get(id: TaskId): Task

    fun isEmpty(): Boolean
    fun nextAvailableId(): TaskId
    fun toStrings(): List<String>
    fun asMap(): Map<TaskId, Task>

    fun preparedTask(): TaskId?
  }

  // Don't allow actual game logic to depend on the event log
  public val reader: GameReader = GameReaderImpl(table, components)

  internal val transformers by table::transformers

  public fun asPlayer(player: Player): PlayerAgent = PlayerAgentImpl(this, player)

  public fun resolve(expression: Expression): MType = table.resolve(expression)

  /**
   * Returns a component instance of type [expression], whether such a component is part of the
   * current game state or not.
   */
  public fun toComponent(expression: Expression): Component = Component.ofType(resolve(expression))

  @JvmName("toNullableComponent")
  public fun toComponent(expression: Expression?): Component? =
      expression?.let { Component.ofType(resolve(it)) }

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

  public fun doAtomic(block: () -> Unit): TaskResult {
    val checkpoint = checkpoint()
    return try {
      block()
      events.activitySince(checkpoint)
    } catch (e: Exception) {
      rollBack(checkpoint)
      throw e
    }
  }

  internal fun activeEffects(classes: Collection<MClass>): List<ActiveEffect> =
      writableComponents.activeEffects(classes)

  internal fun setupFinished() = writableEvents.setStartPoint()

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
      fired.forEach { writableTasks.addTasksFrom(it, writableEvents) }

  internal class PlayerAgentImpl(val game: Game, override val player: Player) : PlayerAgent() {

    override fun asPlayer(other: Player) = game.asPlayer(other)

    override fun session() = PlayerSession(game, this)

    override val reader =
        object : GameReader by game.reader {
          override fun resolve(expression: Expression): MType = game.resolve(heyItsMe(expression))

          override fun evaluate(requirement: Requirement) =
              game.reader.evaluate(heyItsMe(requirement)) // TODO expInfo doesn't want this

          override fun count(metric: Metric) = game.reader.count(heyItsMe(metric))
        }

    internal fun <P : PetNode?> heyItsMe(node: P) = // TODO private?
    if (node == null) node else insertOwner.transform(node)

    override fun getComponents(type: Expression): Multiset<Component> =
        game.components.getAll(reader.resolve(type) as MType, reader) // TODO think about

    override fun tasks(): Map<TaskId, Task> = game.tasks.asMap()

    override fun prepareTask(taskId: TaskId): Boolean {
      val already = game.tasks.preparedTask()
      if (already == taskId) return true
      if (already != null) {
        throw NotNowException("earlier committed task hasn't executed yet: $already")
      }

      val task: Task = game.tasks[taskId]
      checkOwner(task) // TODO use myTasks() instead?

      val preparer = Preparer(reader, game.components)
      val prepared = preparer.toPreparedForm(task.instruction)
      if (prepared == NoOp) {
        game.writableTasks.removeTask(taskId, game.writableEvents)
        return false
      }
      val replacement = task.copy(instruction = prepared, next = true, whyPending = null) // why -why?
      game.writableTasks.replaceTask(replacement, game.writableEvents)
      return true
    }

    override fun tryTask(taskId: TaskId, narrowed: Instruction?): TaskResult {
      var message: String? = null
      val result =
          try {
            game.doAtomic {
              if (prepareTask(taskId)) {
                doTask(taskId, narrowed)
              }
              return@doAtomic
            }
          } catch (e: NotNowException) {
            message = e.message
            TaskResult(listOf(), setOf())
          } catch (e: AbstractException) {
            message = e.message
            TaskResult(listOf(), setOf())
          }
      if (message == null) return result
      val newTask = game.tasks[taskId]
      val explainedTask = newTask.copy(whyPending = message)
      game.writableTasks.replaceTask(explainedTask, game.writableEvents)
      return result
    }

    override fun doTask(taskId: TaskId, narrowed: Instruction?): TaskResult {
      prepareTask(taskId)
      val nrwd: Instruction? = narrowed?.let { session().prep(it) }
      val task = game.tasks[taskId]
      checkOwner(task)
      nrwd?.ensureNarrows(task.instruction, game.reader)
      return game.doAtomic {
        val prepared = Preparer(reader, game.components).toPreparedForm(nrwd ?: task.instruction)
        InstructionExecutor(game, this, task.cause).execute(prepared)
        task.then?.let { addTasks(it, task.owner, task.cause) }
        removeTask(taskId)
      }
    }

    // Danger

    override fun addTask(instruction: Instruction, initialCause: Cause?): TaskId {
      val events = addTasks(heyItsMe(session().prep(instruction)), player, initialCause)
      return events.single().task.id
    }

    override fun removeTask(taskId: TaskId): TaskRemovedEvent {
      checkOwner(game.tasks[taskId])
      return game.writableTasks.removeTask(taskId, game.writableEvents)
    }

    /**
     * Updates the component graph and event log, but does not fire triggers. This exists as a
     * public method so that a broken game state can be fixed, or a game state broken on purpose, or
     * specific game scenario set up very explicitly.
     */
    override fun sneakyChange(
        count: Int,
        gaining: Component?,
        removing: Component?,
        cause: Cause?,
    ): ChangeEvent? {
      val change = try {
        game.writableComponents.update(count, gaining = gaining, removing = removing)
      } catch (e: IllegalArgumentException) { // TODO meh
        throw LimitsException(e.message ?: "")
      }
      return game.writableEvents.addChangeEvent(change, player, cause)
    }

    internal fun addTasks(instruction: Instruction, owner: Player, cause: Cause?) =
        game.writableTasks.addTasksFrom(instruction, owner, cause, game.writableEvents)

    internal fun update(
        count: Int,
        gaining: Component?,
        removing: Component?,
        cause: Cause?
    ): TaskResult {
      val cp = game.checkpoint()
      removingDependents(cause) { sneakyChange(count, gaining, removing, cause) }
      return game.events.activitySince(cp)
    }

    private fun removingDependents(cause: Cause?, tryIt: () -> ChangeEvent?) {
      try {
        tryIt()
      } catch (e: ExistingDependentsException) {
        e.dependents.forEach { removeAll(game.toComponent(it.expressionFull), cause) }
        // TODO better way to remove dependents?
        tryIt()
      }
    }

    private val insertOwner: PetTransformer =
        chain(game.transformers.deprodify(), replaceOwnerWith(player))

    private fun removeAll(removing: Component, cause: Cause?) =
        removingDependents(cause) {
          val change = game.writableComponents.removeAll(removing)
          game.writableEvents.addChangeEvent(change, player, cause = cause)
        }

    private fun checkOwner(task: Task) {
      require(player == task.owner || player == ENGINE) {
        "$player can't access task owned by ${task.owner}"
      }
    }
  }
}
