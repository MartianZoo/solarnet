package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
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
public class Game internal constructor(private val table: MClassTable) {
  public companion object {
    /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
    public fun create(setup: GameSetup) = create(MClassTable.forSetup(setup))

    /** Creates a new game using an existing class table, ready for gameplay to begin. */
    public fun create(table: MClassTable): Game {
      val game = Game(table)
      val session = game.session(ENGINE)

      fun gain(thing: HasExpression) = session.action(parse("${thing.expression}!"))

      gain(ENGINE)
      singletonTypes(table).forEach { gain(it) }
      gain(ClassName.cn("SetupPhase"))

      game.setupFinished()
      return game
    }

    private fun singletonTypes(table: MClassTable): List<Component> =
        table.allClasses
            .filter { 0 !in it.componentCountRange }
            .flatMap { it.baseType.concreteSubtypesSameClass() }
            .map { it.toComponent() }
  }

  private val effector: Effector = Effector()

  private val writableComponents: WritableComponentGraph = WritableComponentGraph(effector)

  /** The components that make up the game's current state ("present"). */
  public val components: ComponentGraph by ::writableComponents

  private val writableEvents: WritableEventLog = WritableEventLog()

  /** Everything that has happened in this game so far ("past"). */
  public val events: EventLog by ::writableEvents

  private val writableTasks: WritableTaskQueue = WritableTaskQueue(writableEvents)

  /** The tasks the game is currently waiting on ("future"). */
  public val tasks: TaskQueue by ::writableTasks

  public val reader: SnReader = GameReaderImpl(table, components)

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
    fun hasPreparedTask(): Boolean = preparedTask() != null
  }

  public interface SnReader : GameReader {

    override fun resolve(expression: Expression): MType
    override fun getComponents(type: Type): Multiset<out MType>
    fun countComponent(component: Component): Int
    public val transformers: Transformers
  }

  internal val transformers by table::transformers

  public fun writer(player: Player): GameWriter = GameWriterImpl(this, player)

  public fun resolve(expression: Expression): MType = table.resolve(expression)

  public fun checkpoint() = events.checkpoint()

  public fun rollBack(checkpoint: Checkpoint) {
    writableEvents.rollBack(checkpoint) {
      when (it) {
        is TaskEvent -> writableTasks.reverse(it)
        is ChangeEvent ->
            writableComponents.update(
                it.change.count,
                gaining = it.change.removing?.toComponent(reader),
                removing = it.change.gaining?.toComponent(reader))
      }
    }
  }

  /**
   * Performs [block] with failure-atomicity and returning a [TaskResult] describing what changed.
   */
  public fun atomic(block: () -> Unit): TaskResult {
    val checkpoint = checkpoint()
    try {
      block()
    } catch (e: Exception) {
      rollBack(checkpoint)
      throw e
    }
    return events.activitySince(checkpoint)
  }

  internal fun setupFinished() = writableEvents.setStartPoint()

  /*
   * Implementation of GameWriter - would be nice to have in a separate file but we'd have to
   * make some things in Game non-private.
   *
   * TODO: is it cool that this seems to mix internally-focused and externally-focused APIs?
   */
  public class GameWriterImpl(val game: Game, val player: Player) : GameWriter(), UnsafeGameWriter {

    private val tasks by game::writableTasks

    internal val instructor =
        Instructor(this, game.reader, game.effector, game.components::findLimit)

    override fun initiateTask(instruction: Instruction, firstCause: Cause?) =
        game.atomic {
          val prepped = preprocess(instruction)
          tasks.addTasks(prepped, player, firstCause)
        }

    override fun narrowTask(taskId: TaskId, narrowed: Instruction): TaskResult {
      val task = tasks[taskId]
      checkOwner(task)

      val fixedNarrowing = preprocess(narrowed)
      if (fixedNarrowing == task.instruction) return TaskResult()
      fixedNarrowing.ensureNarrows(task.instruction, game.reader)

      val replacement = if (task.next) instructor.prepare(fixedNarrowing) else fixedNarrowing
      return editButCheckCardinality(task.copy(instructionIn = replacement))
    }

    override fun prepareTask(taskId: TaskId): TaskId? {
      val task = tasks[taskId]
      val result = doPrepare(task)

      // let's just look ahead though
      if (taskId in tasks) {
        try {
          game.atomic {
            executeTask(taskId)
            throw AbstractException("") // just getting this rolled back is all
          }
        } catch (ignore: AbstractException) {
          // we don't guarantee execute won't throw this
        }
      }
      return result
    }

    private fun doPrepare(task: Task): TaskId? {
      checkOwner(task) // TODO use myTasks() instead?
      dontCutTheLine(task.id)

      val replacement = instructor.prepare(task.instruction)
      editButCheckCardinality(task.copy(instructionIn = replacement, next = true))
      return tasks.preparedTask()
    }

    // Use this to edit a task if the replacement instruction might be NoOp, in which case the
    // task is handleTask'd instead.
    private fun editButCheckCardinality(replacement: Task): TaskResult {
      return game.atomic {
        val split = split(replacement.instruction)
        if (split.size == 1) {
          val reason = replacement.whyPending?.let { "(was: $it)" }
          tasks.editTask(replacement.copy(whyPending = reason))
        } else {
          // All the nows and thens would get enqueued side by side But this is why we don't let a
          // task whose instruction contains a Multi at any depth have a THEN.
          tasks.addTasks(replacement.instruction, replacement.owner, replacement.cause) // TODO
          handleTask(replacement.id)
        }
      }
    }

    override fun canPrepareTask(taskId: TaskId): Boolean {
      dontCutTheLine(taskId)
      val unprepared = tasks[taskId].instruction
      return try {
        instructor.prepare(unprepared)
        true
      } catch (e: Exception) {
        false
      }
    }

    override fun explainTask(taskId: TaskId, reason: String) {
      tasks.editTask(tasks[taskId].copy(whyPending = reason))
    }

    override fun executeTask(taskId: TaskId): TaskResult {
      val task = tasks[taskId]
      checkOwner(task)

      return game.atomic {
        val prepared = doPrepare(task) ?: return@atomic
        val preparedTask = tasks[prepared]
        val newTasks = instructor.execute(preparedTask.instruction, preparedTask.cause)
        newTasks.forEach(game.writableTasks::addTasks)
        handleTask(taskId)
      }
    }

    /**
     * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions
     * are automatically enqueued.
     */
    private fun handleTask(taskId: TaskId) {
      val task = tasks[taskId]
      checkOwner(task)
      task.then?.let { tasks.addTasks(it, task.owner, task.cause) }
      dropTask(taskId)
    }

    private fun dontCutTheLine(taskId: TaskId) {
      val already = tasks.preparedTask()
      if (already != null && already != taskId) {
        throw TaskException("another prepared task must go first: ${tasks[already]}")
      }
    }

    override fun unsafe(): UnsafeGameWriter = this

    override fun dropTask(taskId: TaskId): TaskRemovedEvent {
      checkOwner(tasks[taskId])
      return tasks.removeTask(taskId)
    }

    override fun sneak(changes: String, cause: Cause?) = sneak(preprocess(parse(changes)), cause)

    // TODO: in theory any instruction would be sneakable, and it only means disabling triggers
    override fun sneak(changes: Instruction, cause: Cause?): TaskResult {
      val events =
          split(changes).map {
            val count = (it as Change).count as ActualScalar
            changeWithoutFixingDependents(
                count.value,
                it.gaining?.toComponent(game.reader),
                it.removing?.toComponent(game.reader),
                cause,
            )
          }
      return TaskResult(events)
    }

    override fun change(
        count: Int,
        gaining: Component?,
        removing: Component?,
        cause: Cause?,
    ): TaskResult {
      return game.atomic {
        fun tryIt() = changeWithoutFixingDependents(count, gaining, removing, cause)
        try {
          tryIt()
        } catch (e: ExistingDependentsException) {
          // TODO better way to remove dependents?
          e.dependents.forEach {
            val dependent = it.toComponent(game.reader)
            val depCount = game.reader.countComponent(dependent)
            change(depCount, removing = dependent, cause = cause)
          }
          tryIt()
        }
      }
    }

    override fun changeWithoutFixingDependents(
        count: Int,
        gaining: Component?,
        removing: Component?,
        cause: Cause?,
    ): ChangeEvent {
      require(gaining?.mtype?.root?.custom == null)
      // Can't remove if it would create orphans -- but this is caught by changeAndFixOrphans
      removing?.let { game.writableComponents.checkDependents(count, it) }

      val change = game.writableComponents.update(count, gaining, removing)
      return game.writableEvents.addChangeEvent(change, player, cause)
    }

    private fun checkOwner(task: Task) {
      if (player != task.owner && player != ENGINE) {
        throw TaskException("$player can't access task owned by ${task.owner}")
      }
    }

    private val xer = chain(game.transformers.standardPreprocess(), replaceOwnerWith(player))
    internal fun <P : PetElement> preprocess(node: P) = xer.transform(node)
  }
}
