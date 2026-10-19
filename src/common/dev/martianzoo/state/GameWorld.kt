package dev.martianzoo.state

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskEvent

/**
 * The replayable state of a game or a speculative event suffix over another game.
 *
 * A GameWorld accepts only already-decided, fully concrete events. It keeps event history,
 * components, and pending tasks synchronized, but does not interpret instructions or calculate
 * consequences. Supplying [initialEvents] reconstructs the exact projections produced by applying
 * that complete event sequence to a fresh world with the same [classTable]. [customClasses] may be
 * empty for passive playback; querying an unavailable custom metric then fails at the query.
 *
 * @property premise Immutable resolved game configuration shared with worlds forked from this one.
 */
public class GameWorld
private constructor(
    public val premise: GamePremise,
    initialEvents: List<GameEvent>,
    customClasses: Set<CustomClass>,
    copyFrom: GameWorld?,
    private val backing: GameWorld?,
) {
  /**
   * Creates an independently mutable world from [premise]. [initialEvents] must be a complete,
   * ordered prefix of concrete game events starting at the first ordinal. [customClasses] supplies
   * runtime implementations for custom queries; an empty set supports passive playback.
   */
  public constructor(
      premise: GamePremise,
      initialEvents: List<GameEvent> = emptyList(),
      customClasses: Set<CustomClass> = emptySet(),
  ) : this(premise, initialEvents, customClasses, copyFrom = null, backing = null)

  private val backingRevision = backing?.revision
  private var localRevision = 0L
  private var resettingOverlay = false

  /** Changes even when an event is subsequently rolled back to its old ordinal. */
  internal val revision: Long
    get() {
      requireValid()
      return localRevision
    }

  private fun requireValid() {
    check(backing == null || backing.revision == backingRevision) {
      "source Game World changed after speculative fork creation"
    }
  }

  /** Premise-selected class universe shared with worlds forked from this world. */
  public val classTable: ClassTable = premise.classTable

  private val customClasses: Set<CustomClass> = customClasses.toSet()
  private val customClassesByName: Map<ClassName, List<CustomClass>> =
      this.customClasses.groupBy(CustomClass::className)

  /** Live component counts, updated by [apply] and [rollBackTo]. */
  public val components: ComponentGraph =
      when {
        copyFrom != null -> copyFrom.components.fork()
        backing != null -> backing.components.overlay(::requireValid)
        else -> ComponentGraph(classTable)
      }

  /** Live event prefix; rollback removes its suffix. */
  public val events: EventLog =
      when {
        copyFrom != null -> copyFrom.events.fork()
        backing != null -> backing.events.overlay(::requireValid)
        else -> EventLog()
      }

  private val taskStore: TaskStore =
      when {
        copyFrom != null -> copyFrom.taskStore.fork()
        backing != null -> backing.taskStore.overlay(::requireValid)
        else -> TaskStore()
      }

  /** Live view of every pending task, without an actor filter. */
  public val tasks: TaskQueue = taskStore.all()

  /** Live type and component queries over this world's materialized present. */
  public val reader: GameReader = GameReaderImpl(premise, this)

  /** Every Actor participating in this world, with seated Players in seat order. */
  public val actors: List<Actor> = premise.actors

  init {
    require(copyFrom == null || copyFrom.premise === premise)
    require(backing == null || backing.premise === premise)
    require(copyFrom == null || backing == null)
    require((copyFrom == null && backing == null) || initialEvents.isEmpty())
    initialEvents.forEach { apply(it) }
  }

  /**
   * Copies this exact passive state into an independently mutable Game World. Immutable premise
   * data and values are shared; mutable collections and event commentary are copied, while
   * component listeners are not.
   */
  public fun fork(): GameWorld =
      GameWorld(
          premise,
          initialEvents = emptyList(),
          customClasses,
          copyFrom = this,
          backing = null,
      )

  /**
   * Creates a disposable event-suffix World without changing this World. Applying or rolling back
   * source events invalidates the overlay and its retained read views. Editing event commentary
   * does not change gameplay state. Tentative source events that are later rolled back still
   * invalidate it. [fork] on the overlay materializes its complete history and current state into
   * an independent World.
   */
  public fun overlay(): GameWorld =
      GameWorld(
          premise,
          initialEvents = emptyList(),
          customClasses,
          copyFrom = null,
          backing = this,
      )

  /**
   * Discards only this overlay's events and local projections, retaining its source and read views.
   * [afterClear] runs after state is cleared but before count listeners are notified. Gameplay
   * mutations are refused until the reset and its listener notifications finish.
   */
  public fun resetOverlay(afterClear: () -> Unit = {}) {
    check(backing != null) { "Only a speculative Game World can be reset" }
    check(!resettingOverlay) { "Game World reset is already in progress" }
    requireValid()
    resettingOverlay = true
    try {
      components.resetOverlayState()
      taskStore.resetOverlay()
      events.resetOverlay()
      localRevision++
      afterClear()
      components.notifyResetListeners()
    } finally {
      resettingOverlay = false
    }
  }

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
    check(!resettingOverlay) { "cannot apply an event during Game World reset" }
    requireValid()
    events.requireNext(event)
    when (event) {
      is ChangeEvent ->
          with(event.change) {
            components.applyChange(count, gaining = gaining, removing = removing)
          }
      is TaskEvent -> taskStore.apply(event)
    }
    events.append(event)
    localRevision++
    return event
  }

  /**
   * Restores the event prefix ending before [ordinal]. Returned component changes are the exact
   * reversals an engine-side derived index must observe, in application order. An overlay cannot
   * roll back before its source point.
   */
  public fun rollBackTo(ordinal: Int): List<ComponentChange> {
    check(!resettingOverlay) { "cannot roll back during Game World reset" }
    requireValid()
    require(ordinal in (backing?.nextOrdinal ?: 0)..nextOrdinal)
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
      localRevision++
    }
    return reversedChanges
  }

  /**
   * Returns change events since [checkpoint] in event order, plus task ids added in that interval
   * that have not also been removed in it.
   */
  public fun activitySince(checkpoint: Checkpoint): TaskResult = events.activitySince(checkpoint)

  /**
   * Marks [checkpoint] as the start of gameplay history for queries such as
   * [EventLog.entriesSinceSetup]. Defaults to the current event position.
   */
  public fun markSetupStart(checkpoint: Checkpoint = Checkpoint(nextOrdinal)) {
    check(!resettingOverlay) { "cannot change setup during Game World reset" }
    events.markSetupStart(checkpoint)
    localRevision++
  }

  /** Throws [TaskException] if any task remains pending anywhere in this world. */
  public fun requireNoPendingTasks() {
    val pending = taskStore.getAll()
    if (pending.isNotEmpty()) {
      throw TaskException("pending tasks:\n${pending.joinToString("\n")}")
    }
  }
}
