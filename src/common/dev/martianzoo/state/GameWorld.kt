package dev.martianzoo.state

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskEvent

/**
 * The replayable state of one game.
 *
 * A GameWorld accepts only already-decided, fully concrete events. It keeps event history,
 * components, and pending tasks synchronized, but does not interpret instructions or calculate
 * consequences. Supplying [initialEvents] reconstructs the exact projections produced by applying
 * that complete event sequence to a fresh world with the same [classTable]. [customClasses] may be
 * empty for passive playback; querying an unavailable custom metric then fails at the query.
 */
public class GameWorld
private constructor(
    public val premise: GamePremise,
    initialEvents: List<GameEvent>,
    customClasses: Set<CustomClass>,
    copyFrom: GameWorld?,
) {
  public constructor(
      premise: GamePremise,
      initialEvents: List<GameEvent> = emptyList(),
      customClasses: Set<CustomClass> = emptySet(),
  ) : this(premise, initialEvents, customClasses, copyFrom = null)

  /** The immutable classes available to this world. */
  public val classTable: ClassTable = premise.classTable

  private val customClasses: Set<CustomClass> = customClasses.toSet()
  private val customClassesByName: Map<ClassName, List<CustomClass>> =
      this.customClasses.groupBy(CustomClass::className)

  /** The current component multiset and its observable count indexes. */
  public val components: ComponentGraph = copyFrom?.components?.fork() ?: ComponentGraph(classTable)

  /** Everything that has happened in this world. */
  public val events: EventLog = copyFrom?.events?.fork() ?: EventLog()

  private val taskStore: TaskStore = copyFrom?.taskStore?.fork() ?: TaskStore()

  /** Every task currently pending in this world. */
  public val tasks: TaskQueue = taskStore.all()

  /** Higher-level Pets queries over this world's materialized present. */
  public val reader: GameReader = GameReaderImpl(premise, this)

  /** Every Actor participating in this world, with seated Players in seat order. */
  public val actors: List<Actor> = premise.actors

  init {
    require(copyFrom == null || copyFrom.premise === premise)
    require(copyFrom == null || initialEvents.isEmpty())
    initialEvents.forEach(::apply)
  }

  /**
   * Copies this exact passive state into an independently mutable Game World. Immutable premise
   * data and values are shared; mutable collections and event commentary are copied, while
   * component listeners are not.
   */
  public fun fork(): GameWorld =
      GameWorld(premise, initialEvents = emptyList(), customClasses, copyFrom = this)

  /** Returns the executable implementation bound to [className] in this World, if supplied. */
  public fun customClassOrNull(className: ClassName): CustomClass? {
    val matches = customClassesByName[className].orEmpty()
    return when (matches.size) {
      1 -> matches.single()
      0 -> null
      else -> error("multiple custom implementations for `$className` in this Game World")
    }
  }

  /** The ordinal required for the next exact event. */
  public val nextOrdinal: Int
    get() = events.nextOrdinal

  /** A live task view restricted to [assignee]. */
  public fun tasksFor(assignee: Actor): TaskQueue = taskStore.forAssignee(assignee)

  /**
   * Applies one fully decided event, atomically updating history and the corresponding projection.
   * No instruction is interpreted and no consequence is calculated.
   */
  public fun <E : GameEvent> apply(event: E): E {
    events.requireNext(event)
    when (event) {
      is ChangeEvent ->
          with(event.change) {
            components.applyChange(count, gaining = gaining, removing = removing)
          }
      is TaskEvent -> taskStore.apply(event)
    }
    events.append(event)
    return event
  }

  /**
   * Restores the event prefix ending before [ordinal]. Returned component changes are the exact
   * reversals an engine-side derived index must observe, in application order.
   */
  public fun rollBackTo(ordinal: Int): List<ComponentChange> {
    require(ordinal in 0..nextOrdinal)
    val reversedChanges = mutableListOf<ComponentChange>()
    while (nextOrdinal > ordinal) {
      when (val event = events.last()) {
        is ChangeEvent -> {
          val reversed = event.change.reversed()
          with(reversed) {
            components.applyChange(count, gaining = gaining, removing = removing)
          }
          reversedChanges += reversed
        }
        is TaskEvent -> taskStore.reverse(event)
      }
      events.removeLast()
    }
    return reversedChanges
  }

  public fun activitySince(checkpoint: Checkpoint): TaskResult = events.activitySince(checkpoint)

  public fun markSetupStart(checkpoint: Checkpoint = Checkpoint(nextOrdinal)) {
    events.markSetupStart(checkpoint)
  }

  public fun requireNoPendingTasks() {
    val pending = taskStore.getAll()
    if (pending.isNotEmpty()) {
      throw TaskException("pending tasks:\n${pending.joinToString("\n")}")
    }
  }
}
