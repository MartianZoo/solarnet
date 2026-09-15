package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskEvent

/**
 * The replayable state of one game.
 *
 * A GameWorld accepts only already-decided, fully concrete events. It keeps event history,
 * components, and pending tasks synchronized, but does not interpret instructions or calculate
 * consequences.
 */
public class GameWorld(public val classTable: ClassTable) {
  /** The current component multiset and its observable count indexes. */
  public val components: ComponentGraph = ComponentGraph(classTable)

  /** Everything that has happened in this world. */
  public val events: EventLog = EventLog()

  private val taskStore = TaskStore()

  /** Every task currently pending in this world. */
  public val tasks: TaskQueue = taskStore.all()

  /** The ordinal required for the next exact event. */
  public val nextOrdinal: Int
    get() = events.nextOrdinal

  /** An identity which advances on every forward or reverse event application. */
  public val revision: WorldRevision
    get() = events.revision

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

  public fun markSetupStart() {
    events.markSetupStart()
  }

  public fun requireNoPendingTasks() {
    val pending = taskStore.getAll()
    if (pending.isNotEmpty()) {
      throw TaskException("pending tasks:\n${pending.joinToString("\n")}")
    }
  }
}
