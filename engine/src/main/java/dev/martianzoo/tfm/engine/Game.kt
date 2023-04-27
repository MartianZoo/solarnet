package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.GameWriter
import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.Player
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
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MClassTable
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset

/**
 * The mutable state of a game in progress. It is essentially an aggregation of three mutable
 * objects: an [EventLog] (representing the "past"), a [ComponentGraph] (representing the
 * "present"), and a [TaskQueue] (representing the "future"). It also has a reference to an
 * [MClassTable] that it pulls component classes from.
 *
 * Most state changes require going through [asPlayer] to get a [PlayerAgent].
 */
public class Game
internal constructor(
    /**
     * Where the game will get all its information about component types from. Must already be
     * frozen.
     */
    public val table: MClassTable,

    /** Everything that has happened in this game so far ("past"). */
    public val events: EventLog = WritableEventLog(),

    /** The components that make up the game's current state ("present"). */
    public val components: ComponentGraph = WritableComponentGraph(),

    /** The tasks the game is currently waiting on ("future"). */
    public val tasks: TaskQueue = WritableTaskQueue()
) {

  // TODO can we reverse this and not cast?
  private val writableComponents = components as WritableComponentGraph
  private val writableTasks = tasks as WritableTaskQueue
  private val writableEvents = events as WritableEventLog

  public val reader: GameReader = GameReaderImpl(table, components)

  public val transformers by table::transformers

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
    fun count(parentType: MType): Int

    /**
     * Returns all component instances having the type [parentType] (or any of its subtypes), as a
     * multiset. The size of the returned collection will be `[count]([parentType])` . If
     * [parentType] is `Component` this will return the entire component multiset.
     */
    fun getAll(parentType: MType): Multiset<Component>
  }

  /**
   * A complete record of everything that happened in a particular game (in progress or finished). A
   * complete game state could be reconstructed by replaying these events.
   */
  public interface EventLog {
    val events: List<GameEvent>
    val size: Int

    data class Checkpoint(internal val ordinal: Int) {
      init {
        require(ordinal >= 0)
      }
    }

    fun checkpoint(): Checkpoint

    fun changes(): List<ChangeEvent>
    fun changesSince(checkpoint: Checkpoint): List<ChangeEvent>

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

  // PLAYER AGENT

  public fun asPlayer(player: Player) = PlayerAgent(this, player)

  public fun removeTask(id: TaskId) = writableTasks.removeTask(id, writableEvents)

  // TYPE & COMPONENT CONVERSION

  public fun resolve(expression: Expression): MType = table.resolve(expression)

  /**
   * Returns a component instance of type [expression], regardless of whether this component
   * currently exists in the game state or not.
   */
  public fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  internal val einfo =
      object : ExpressionInfo {
        override fun isAbstract(e: Expression) = resolve(e).abstract
        override fun ensureReifies(wide: Expression, narrow: Expression) {
          val abstractTarget = resolve(wide)
          val proposed = resolve(narrow)
          proposed.ensureReifies(abstractTarget)
          checkRefinements(abstractTarget, proposed)
        }
        private fun checkRefinements(abstractTarget: MType, proposed: MType) {
          val refin = abstractTarget.refinement
          if (refin != null) {
            val requirement = RefinementMangler(proposed).transform(refin)
            if (!reader.evaluate(requirement)) throw UserException.requirementNotMet(requirement)
          }
          for ((a, b) in abstractTarget.typeDependencies.zip(proposed.typeDependencies)) {
            checkRefinements(a.boundType, b.boundType)
          }
        }
      }

  // We check if MartianIndustries reifies CardFront(HAS 1 BuildingTag)
  // by testing the requirement `1 BuildingTag<MartianIndustries>`
  inner class RefinementMangler(private val proposed: MType) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      return if (node is Expression) {
        val tipo = table.resolve(node)
        try {
          val modded = tipo.specialize(listOf(proposed.expression))
          @Suppress("UNCHECKED_CAST")
          modded.expression as P
        } catch (e: Exception) {
          node // don't go deeper
        }
      } else {
        transformChildren(node)
      }
    }
  }

  // CHANGE LOG

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

  // A little odd that activeEffects is only on "writable" components but okay
  internal fun activeEffects(): List<ActiveEffect> = writableComponents.activeEffects(this)

  internal fun setupFinished() = writableEvents.setStartPoint()

  fun getTask(taskId: TaskId): Task {
    require(taskId in tasks) { taskId }
    return tasks[taskId]
  }

  public fun clone() =
      Game(table, writableEvents.clone(), writableComponents.clone(), writableTasks.clone())

  /** A view of a [Game] specific to a particular [Player] (a player or the engine). */
  public class PlayerAgent internal constructor(public val game: Game, public val player: Player) {
    public fun session(defaultAutoExec: Boolean = true) = PlayerSession(this, defaultAutoExec)

    public fun asPlayer(newPlayer: Player) = PlayerAgent(game, newPlayer)

    public val reader =
        object : GameReader by game.reader {
          override fun resolve(expression: Expression): MType = game.resolve(heyItsMe(expression))

          override fun evaluate(requirement: Requirement) =
              game.reader.evaluate(heyItsMe(requirement))

          override fun count(metric: Metric) = game.reader.count(heyItsMe(metric))
        }

    internal val writer: GameWriter = GameWriterImpl()

    private val insertOwner: PetTransformer by lazy {
      chain(game.transformers.deprodify(), replaceOwnerWith(player))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <P : PetNode?> heyItsMe(node: P): P = node?.let(insertOwner::transform) as P

    public fun getComponents(type: Expression): Multiset<Component> =
        game.components.getAll(reader.resolve(type) as MType) // TODO think about

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
      return doAtomic {
        val executor = InstructionExecutor(reader, writer, game.transformers, player, initialCause)
        fixed.forEach { executor.doInstruction(it) }
      }
    }

    fun tasks(): Map<TaskId, Task> = game.tasks.asMap()

    public fun doTask(taskId: TaskId, narrowed: Instruction? = null): Result {
      val checkpoint = game.checkpoint()
      val requestedTask: Task = game.getTask(taskId)
      // require(requestedTask.player == player)

      val prepped = heyItsMe(narrowed)
      prepped?.ensureReifies(requestedTask.instruction, game.einfo)
      val instruction = prepped ?: requestedTask.instruction

      return try {
        doAtomic {
          val executor =
              InstructionExecutor(reader, writer, game.transformers, player, requestedTask.cause)
          executor.doInstruction(instruction)
          removeTask(taskId)
        }
      } catch (e: UserException) {
        if (requestedTask.whyPending != null) {
          throw e
        }
        val explainedTask = requestedTask.copy(whyPending = e.message)
        game.writableTasks.replaceTask(explainedTask, game.writableEvents)
        game.events.activitySince(checkpoint)
      }
    }

    // TODO check owner
    public fun removeTask(taskId: TaskId) = game.removeTask(taskId)

    private fun fireTriggers(triggerEvent: ChangeEvent) {
      val firedSelfEffects: List<FiredEffect> =
          listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing)
              .map(game::toComponent)
              .flatMap { it.activeEffects(game) }
              .mapNotNull { it.onChangeToSelf(triggerEvent) }

      val firedOtherEffects: List<FiredEffect> =
          game.activeEffects().mapNotNull { it.onChangeToOther(triggerEvent) }

      val (now, later) = (firedSelfEffects + firedOtherEffects).partition { it.automatic }
      for (fx in now) {
        val executor = InstructionExecutor(reader, writer, game.transformers, player, fx.cause)
        Instruction.split(fx.instruction).forEach(executor::doInstruction)
      }
      game.writableTasks.addTasksFrom(later, game.writableEvents)
    }

    inner class GameWriterImpl : GameWriter {
      override fun update(
          count: Int,
          gaining: Type?,
          removing: Type?,
          amap: Boolean,
          cause: Cause?,
      ) {
        val g = gaining?.expression
        val r = removing?.expression

        fun tryIt(): ChangeEvent? = sneakyChange(count, g, r, amap, cause)
        val event =
            try {
              tryIt()
            } catch (e: ExistingDependentsException) {
              for (dept in e.dependents) {
                update(Int.MAX_VALUE, removing = dept.mtype, amap = true, cause = cause)
              }
              tryIt()
            } ?: return

        fireTriggers(event)
      }

      override fun addTasks(instruction: Instruction, taskOwner: Player, cause: Cause?) {
        game.writableTasks.addTasksFrom(
            instruction, taskOwner, cause, game.writableEvents)
      }
    }

    public fun doAtomic(block: () -> Unit): Result {
      val checkpoint = game.checkpoint()
      try {
        block()
      } catch (e: Exception) {
        game.rollBack(checkpoint)
        throw e
      }
      return game.events.activitySince(checkpoint)
    }
  }
}
