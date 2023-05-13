package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.EventLog
import dev.martianzoo.tfm.engine.Game.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.Game.TaskQueue
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MClassTable
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.tfm.types.Transformers
import dev.martianzoo.util.Multiset

/**
 * The mutable state of a game in progress. This state is the aggregation of three mutable child
 * objects, which callers accesses directly: a [ComponentGraph], a [TaskQueue], and an [EventLog].
 * These types don't expose mutation operations, but the objects are mutable and always represent
 * the most current state.
 *
 * To read game state at a higher level (e.g. via Pets expressions), use [reader]. To change state
 * use [writer].
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
    fun count(parentType: MType, info: TypeInfo): Int

    /**
     * Returns all component instances having the type [parentType] (or any of its subtypes), as a
     * multiset. The size of the returned collection will be `[count]([parentType])` . If
     * [parentType] is `Component` this will return the entire component multiset.
     */
    fun getAll(parentType: MType, info: TypeInfo): Multiset<Component>

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
  public interface TaskQueue : Set<Task> {
    fun ids(): Set<TaskId>

    operator fun contains(id: TaskId): Boolean
    operator fun get(id: TaskId): Task

    fun nextAvailableId(): TaskId

    fun preparedTask(): TaskId?
  }

  public interface SnReader : GameReader {
    override fun resolve(expression: Expression): MType
    override fun getComponents(type: Type): Multiset<out MType>
    fun countComponent(component: Component): Int

    public val transformers: Transformers
  }

  // Don't allow actual game logic to depend on the event log
  public val reader: SnReader = GameReaderImpl(table, components)

  internal val transformers by table::transformers

  public fun writer(player: Player): GameWriter = GameWriterImpl(this, player)

  public fun resolve(expression: Expression): MType = table.resolve(expression)

  public fun checkpoint() = events.checkpoint()

  public fun rollBack(checkpoint: Checkpoint) {
    writableEvents.rollBack(checkpoint) {
      when (it) {
        is TaskEvent -> writableTasks.reverse(it)
        is ChangeEvent -> {
          writableComponents.reverse(
              it.change.count,
              removeWhatWasGained = it.change.gaining?.toComponent(reader),
              gainWhatWasRemoved = it.change.removing?.toComponent(reader),
          )
        }
      }
    }
  }

  /**
   * Performs [block] with failure-atomicity and returning a [TaskResult] describing what changed.
   */
  public fun atomic(block: () -> Unit): TaskResult {
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

  internal fun addTriggeredTasks(fired: List<FiredEffect>) =
      fired.forEach { writableTasks.addTasksFrom(it, writableEvents) }

  /*
   * Implementation of GameWriter - would be nice to have in a separate file but we'd have to
   * make some things in Game non-private.
   *
   * TODO: is it cool that this seems to mix internally-focused and externally-focused APIs?
   */
  internal class GameWriterImpl(val game: Game, private val player: Player) :
      GameWriter(), UnsafeGameWriter {

    override fun prepareTask(taskId: TaskId): Boolean {
      val already = game.tasks.preparedTask()
      if (already == taskId) return true
      if (already != null) {
        throw NotNowException("already-prepared task hasn't executed yet: $already")
      }

      val task: Task = game.tasks[taskId]
      checkOwner(task) // TODO use myTasks() instead?

      val prepared = Instructor(this, player).prepare(task.instruction)
      if (prepared == null) {
        game.writableTasks.removeTask(taskId, game.writableEvents)
        return false
      }
      val replacement = task.copy(instruction = prepared, next = true, whyPending = null) // TODO
      game.writableTasks.replaceTask(replacement, game.writableEvents)
      return true
    }

    override fun tryTask(taskId: TaskId, narrowed: Instruction?): TaskResult {
      return try {
        doTask(taskId, narrowed)
      } catch (e: Exception) {
        when (e) {
          is TaskException -> return TaskResult()
          is NotNowException,
          is AbstractException -> {
            val newTask = game.tasks[taskId]
            val explainedTask = newTask.copy(whyPending = e.message!!)
            game.writableTasks.replaceTask(explainedTask, game.writableEvents)
          }
          else -> throw e
        }
        TaskResult()
      }
    }

    override fun doTask(taskId: TaskId, narrowed: Instruction?): TaskResult {
      return game.atomic {
        if (!prepareTask(taskId)) return@atomic

        val nrwd: Instruction? = narrowed?.let(::preprocess)
        val task = game.tasks[taskId]
        checkOwner(task)
        nrwd?.ensureNarrows(task.instruction, game.reader)

        val instructor = Instructor(this, player, task.cause)
        val prepared = instructor.prepare(nrwd ?: task.instruction)
        prepared?.let(instructor::execute)
        task.then?.let { addTasks(it, task.owner, task.cause) }
        removeTask(taskId)
      }
    }

    override fun addTask(instruction: Instruction, initialCause: Cause?): TaskId {
      // require(game.tasks.none()) { game.tasks.joinToString("\n")} TODO enable??
      val events = addTasks(instruction, player, initialCause)
      return events.single().task.id
    }

    internal fun addTasks(
        instruction: Instruction,
        owner: Player,
        cause: Cause?,
    ): List<TaskAddedEvent> =
        game.writableTasks.addTasksFrom(instruction, owner, cause, game.writableEvents)

    override fun unsafe(): UnsafeGameWriter = this

    override fun removeTask(taskId: TaskId): TaskRemovedEvent {
      checkOwner(game.tasks[taskId])
      return game.writableTasks.removeTask(taskId, game.writableEvents)
    }

    override fun sneak(change: String, cause: Cause?) = sneak(preprocess(parse(change)), cause)

    // TODO: in theory any instruction would be sneakable, and it only means disabling triggers
    override fun sneak(changes: Instruction, cause: Cause?): TaskResult {
      val events = split(changes).map {
        val count = (it as Change).count as ActualScalar
        change(
            count.value,
            it.gaining?.toComponent(game.reader),
            it.removing?.toComponent(game.reader),
            cause) {}
      }
      return TaskResult(events)
    }

    override fun change(
        count: Int,
        gaining: Component?,
        removing: Component?,
        cause: Cause?,
        listener: (ChangeEvent) -> Unit,
    ): ChangeEvent {
      // Can't remove if it would create orphans -- but this is caught by changeAndFixOrphans
      removing?.let { game.writableComponents.checkDependents(count, it) }

      val change = game.writableComponents.update(count, gaining, removing)
      val event = game.writableEvents.addChangeEvent(change, player, cause)

      // This is how triggers get fired
      listener(event)
      return event
    }

    override fun changeAndFixOrphans(
        count: Int,
        gaining: Component?,
        removing: Component?,
        cause: Cause?,
        listener: (ChangeEvent) -> Unit,
    ) {
      fun tryIt() = change(count, gaining, removing, cause, listener)
      try {
        tryIt()
      } catch (e: ExistingDependentsException) {
        // TODO better way to remove dependents?
        e.dependents.forEach {
          val dependent = it.toComponent(game.reader)
          val depCount = game.reader.countComponent(dependent)
          changeAndFixOrphans(depCount, removing = dependent, cause = cause, listener = listener)
        }
        tryIt()
      }
    }

    private fun checkOwner(task: Task) {
      if (player != task.owner && player != ENGINE) {
        throw TaskException("$player can't access task owned by ${task.owner}")
      }
    }

    private val xer = chain(game.transformers.standardPreprocess(), replaceOwnerWith(player))

    internal fun <P : PetElement> preprocess(node: P) = xer.transform(node)
  }

  public companion object {
    /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
    public fun create(setup: GameSetup) = create(MClassTable.forSetup(setup))

    /** Creates a new game using an existing class table, ready for gameplay to begin. */
    public fun create(table: MClassTable): Game {
      val game = Game(table)
      val session = game.session(ENGINE)

      fun gain(thing: HasExpression, cause: Cause? = null): TaskResult {
        val instr: Instruction = parse("${thing.expression}!")
        return game.atomic {
          session.writer.doTask(instr, cause)
          session.tryToDrain()
        }
      }

      val event: ChangeEvent = gain(ENGINE).changes.single()
      val becauseISaidSo = Cause(ENGINE.expression, event.ordinal)

      singletonTypes(table).forEach { gain(it, becauseISaidSo) }
      gain(ClassName.cn("SetupPhase"), becauseISaidSo)
      game.setupFinished()
      return game
    }

    private fun singletonTypes(table: MClassTable): List<Component> =
        table.allClasses
            .filter { 0 !in it.componentCountRange }
            .flatMap { it.baseType.concreteSubtypesSameClass() }
            .map(Component::ofType)
  }
}
