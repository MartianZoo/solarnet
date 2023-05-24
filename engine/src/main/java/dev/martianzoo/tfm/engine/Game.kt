package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.EventLog
import dev.martianzoo.tfm.engine.Game.TaskQueue
import dev.martianzoo.tfm.engine.Game.Timeline.Checkpoint
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.tfm.types.Transformers
import dev.martianzoo.util.Multiset
import javax.inject.Inject

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
@Inject
internal constructor(
    /** The components that make up the game's current state ("present"). */
    public val components: ComponentGraph,

    /** Everything that has happened in this game so far ("past"). */
    public val events: EventLog,

    /** The tasks the game is currently waiting on ("future"). */
    public val tasks: TaskQueue,
    public val reader: SnReader,
    public val timeline: Timeline,
    private val writers: GameWriterFactory,
) {
  init {
    println(this)
  }

  public fun writer(player: Player) = writers.writer(player)

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
  }

  /**
   * A complete record of everything that happened in a particular game (in progress or finished). A
   * complete game state could be reconstructed by replaying these events.
   */
  public interface EventLog {

    val size: Int

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
   * It is possible to retrieve the [Task] corresponding to a [TaskId] but this is generally
   * discouraged and the API doesn't make it easy.
   */
  public interface TaskQueue {
    /** Returns the id of each task currently in the queue, in order from oldest to newest. */
    fun ids(): Set<TaskId>

    operator fun contains(id: TaskId): Boolean

    /** Returns true if the queue is empty. */
    fun isEmpty() = ids().none()

    /** Returns all task ids whose task data matches the given predicate. */
    fun matching(predicate: (Task) -> Boolean): Set<TaskId>

    /** Returns the results of executing a function against every task in the queue. */
    fun <T> extract(extractor: (Task) -> T): List<T>

    /** Returns the id of the task marked with [Task.next] if there is one. */
    fun preparedTask(): TaskId?
  }

  public interface SnReader : GameReader {

    override fun resolve(expression: Expression): MType
    override fun getComponents(type: Type): Multiset<out MType>
    fun countComponent(component: Component): Int
    public val transformers: Transformers
  }

  interface Timeline {
    /** A point in history. */
    public data class Checkpoint(internal val ordinal: Int) {
      init {
        require(ordinal >= 0)
      }
    }

    fun checkpoint(): Checkpoint

    fun rollBack(checkpoint: Checkpoint)

    /**
     * Performs [block] with failure-atomicity and returning a [TaskResult] describing what changed.
     */
    fun atomic(block: () -> Unit): TaskResult
  }
}
