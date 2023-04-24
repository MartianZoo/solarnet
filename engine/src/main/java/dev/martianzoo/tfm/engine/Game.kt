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
import dev.martianzoo.tfm.pets.PureTransformers
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset

/**
 * The mutable state of a game in progress. It is essentially an aggregation of three mutable
 * objects: an [EventLog] (representing the "past"), a [ComponentGraph] (representing the
 * "present"), and a [TaskQueue] (representing the "future"). It also has a reference to a frozen
 * [MClassLoader] that it pulls component classes from.
 *
 * Most state changes require going through [asPlayer] to get a [PlayerAgent].
 */
public class Game(
    /**
     * Where the game will get all its information about component types from. Must already be
     * frozen. (TODO MClassTable)
     */
    public val loader: MClassLoader
) {
  // TODO expose only read-only interfaces for these three things

  private val writableEvents = WritableEventLog()

  /** Everything that has happened in this game so far ("past"). */
  public val events: EventLog by ::writableEvents

  /** The components that make up the game's current state ("present"). */
  private val writableComponents = WritableComponentGraph()

  public val components: ComponentGraph by ::writableComponents

  /** The tasks the game is currently waiting on ("future"). */
  private val writableTasks = WritableTaskQueue(writableEvents)

  public val tasks: TaskQueue by ::writableTasks

  public val reader: GameReader = GameReaderImpl(loader, components)

  init {
    require(loader.frozen)

    // TODO BAD HACK
    require(loader.game == null)
    loader.game = this
  }

  /** A checkpoint taken when the game initialization process ends. */
  public lateinit var start: Checkpoint
    private set

  // PLAYER AGENT

  public fun asPlayer(player: Player) = PlayerAgent(this, player)

  public fun removeTask(id: TaskId) = writableTasks.removeTask(id)

  // TYPE & COMPONENT CONVERSION

  public fun resolve(expression: Expression): MType = loader.resolve(expression)

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

          // wide might be CityTile<P1, LA(HAS MAX 0 NBR<CT<ANY>>)>
          // narrow might be CityTile<P1, M11>
          // as pure types they check out
          // but check whether `HAS MAX 0 NBR<CT<ANY>, M11>` is true
        }
        private fun checkRefinements(abstractTarget: MType, proposed: MType) {
          val refin = abstractTarget.refinement
          if (refin != null) {
            val requirement = RefinementMangler(proposed).transform(refin)
            if (!reader.evaluate(requirement)) throw UserException.requirementNotMet(requirement)

            for ((a, b) in abstractTarget.typeDependencies.zip(proposed.typeDependencies)) {
              checkRefinements(a.boundType, b.boundType)
            }
          }
        }
      }

  // Check MartianIndustries against CardFront(HAS BuildingTag)
  // by testing requirement `BuildingTag<MartianIndustries>`
  // in that example, `proposed` will be `MartianIndustries`
  // and the node being transformed is `BuildingTag`
  inner class RefinementMangler(private val proposed: MType) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      return if (node is Expression) {
        val tipo = loader.resolve(node)
        try {
          val modded = tipo.specialize(listOf(proposed.expression))
          @Suppress("UNCHECKED_CAST")
          modded.expressionFull as P
        } catch (e: Exception) {
          println(e.message)
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
      PureTransformers.transformInSeries(
          game.loader.transformers.deprodify(),
          { PureTransformers.replaceOwnerWith(player.className) }.orNullIf(player == Player.ENGINE),
      )
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
            InstructionExecutor(reader, writer, game.loader.transformers, player, initialCause)
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
                  reader, writer, game.loader.transformers, player, requestedTask.cause)
          executor.doInstruction(instruction)
          removeTask(taskId)
        }
      } catch (e: UserException) {
        if (requestedTask.whyPending != null) throw e
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
            InstructionExecutor(reader, writer, game.loader.transformers, player, fx.cause)
        Instruction.split(fx.instruction).forEach(executor::doInstruction)
      }
      game.writableTasks.addTasksFrom(later) // TODO what was this TODO about?
    }

    inner class GameWriterImpl : GameWriter {
      override fun update(
          count: Int,
          gaining: Type?,
          removing: Type?,
          amap: Boolean,
          cause: Cause?,
      ) {
        val g = gaining?.expressionFull
        val r = removing?.expressionFull

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
