package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.GameWriter
import dev.martianzoo.tfm.api.SpecialClassNames
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.engine.Game.PlayerAgent
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
public class Game(
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
    public val tasks: TaskQueue = WritableTaskQueue(events as WritableEventLog),

    /** A checkpoint taken when the game initialization process ends. */
    public var start: Checkpoint? = null // TODO move this to EventLog?
) {

  private val writableComponents = components as WritableComponentGraph
  private val writableTasks = tasks as WritableTaskQueue
  private val writableEvents = events as WritableEventLog

  public val reader: GameReader = GameReaderImpl(table, components)

  // PLAYER AGENT

  public fun asPlayer(player: Player) = PlayerAgent(this, player)

  public fun removeTask(id: TaskId) = writableTasks.removeTask(id)

  // TYPE & COMPONENT CONVERSION

  public fun resolve(expression: Expression): MType = table.resolve(expression)

  /**
   * Returns a component instance of type [expression], regardless of whether this component
   * currently exists in the game state or not.
   */
  public fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  val einfo =
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

  public fun rollBack(checkpoint: Checkpoint) = rollBack(checkpoint.ordinal)

  public fun rollBack(ordinal: Int) {
    require(ordinal <= events.size)
    if (ordinal == events.size) return
    val subList = writableEvents.events.subList(ordinal, events.size)
    for (entry in subList.asReversed()) {
      when (entry) {
        is TaskEvent -> writableTasks.undo(entry)
        is ChangeEvent -> {
          val change = entry.change
          writableComponents.reverse(
              change.count,
              removeWhatWasGained = change.gaining?.let(::toComponent),
              gainWhatWasRemoved = change.removing?.let(::toComponent),
          )
        }
      }
    }
    subList.clear()
  }

  // A little odd that activeEffects is only on "writable" components but okay
  internal fun activeEffects(): List<ActiveEffect> = writableComponents.activeEffects(this)

  internal fun setupFinished() {
    start = checkpoint()
  }

  fun getTask(taskId: TaskId): Task {
    require(taskId in tasks) { taskId }
    return tasks[taskId]
  }

  public fun clone(): Game {
    require(tasks.isEmpty())
    return Game(table, writableEvents.clone(), writableComponents.clone(), start = start)
  }

  /** A view of a [Game] specific to a particular [Player] (a player or the engine). */
  public class PlayerAgent
  internal constructor(
      public val game: Game,
      public val player: Player,
  ) {
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
      chain(game.table.transformers.deprodify(), replaceOwnerWith(player))
    }

    private fun <T> (() -> T).orNullIf(b: Boolean): T? = if (b) null else this()

    @Suppress("UNCHECKED_CAST")
    private fun <P : PetNode?> heyItsMe(node: P): P = node?.let(insertOwner::transform) as P

    public fun evaluate(requirement: Requirement) = reader.evaluate(requirement)

    public fun count(metric: Metric) = reader.count(metric)

    public fun getComponents(type: Expression): Multiset<Component> =
        game.components.getAll(reader.resolve(type) as MType) // TODO think about

    public fun sneakyChange(
        count: Int = 1,
        gaining: Expression? = null,
        removing: Expression? = null,
        amap: Boolean = false,
        cause: Cause? = null,
    ): ChangeEvent? {
      when (gaining) {
        SpecialClassNames.OK.expression -> {
          if (removing != null) throw UserException("Can't remove Ok, ok?")
          return null
        }
        SpecialClassNames.DIE.expression -> {
          return if (amap) {
            null
          } else {
            throw UserException.die(cause)
          }
        }
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
        val executor =
            InstructionExecutor(reader, writer, game.table.transformers, player, initialCause)
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
              InstructionExecutor(
                  reader, writer, game.table.transformers, player, requestedTask.cause)
          executor.doInstruction(instruction)
          removeTask(taskId)
        }
      } catch (e: UserException) {
        if (requestedTask.whyPending != null) {
          throw e
        }
        val explainedTask = requestedTask.copy(whyPending = e.message)
        game.writableTasks.replaceTask(explainedTask)
        game.events.activitySince(checkpoint)
      }
    }

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
        val executor =
            InstructionExecutor(reader, writer, game.table.transformers, player, fx.cause)
        Instruction.split(fx.instruction).forEach(executor::doInstruction)
      }
      game.writableTasks.addTasksFrom(later)
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
        game.writableTasks.addTasksFrom(instruction, taskOwner, cause)
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
