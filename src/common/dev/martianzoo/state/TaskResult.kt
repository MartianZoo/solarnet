package dev.martianzoo.state

import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.Task.TaskId
import kotlin.math.absoluteValue

/**
 * Snapshot of the retained changes made during a call. The result should cover the whole call,
 * including work performed while running to quiescence after the initiating transaction. [changes]
 * contains component change events in event order. [tasksSpawned] contains task ids added during
 * that interval that remain pending at return; later world activity may remove them. This result
 * does not imply that a whole game action has finished or that all pending tasks are gone.
 *
 * Current limitation: engine and Agent completion callbacks can perform work that their returned
 * result omits. That is an implementation gap, not a smaller intended reporting interval.
 */
public data class TaskResult(
    public val changes: List<ChangeEvent> = emptyList(),
    public val tasksSpawned: Set<TaskId> = emptySet(),
) {
  /**
   * Combines [changes] into the net count change for each exact [Component], omitting components
   * whose gains and removals cancel. A transmutation contributes a removal of its source and a gain
   * of its target. For example, gaining two copies of a component and removing one produces a
   * single gain of one copy; gaining and removing equal counts produces no entry. The result is a
   * snapshot and does not include [tasksSpawned].
   */
  public fun net(): List<ComponentChange> {
    val map = mutableMapOf<Component, Int>()
    for (event in changes) {
      val change = event.change
      change.gaining?.let {
        val count = map[it] ?: 0
        map[it] = count + change.count
      }
      change.removing?.let {
        val count = map[it] ?: 0
        map[it] = count - change.count
      }
    }
    return map.filterValues { it != 0 }
        .map { (component, count) ->
          if (count > 0) {
            ComponentChange.Gain(count, component)
          } else {
            ComponentChange.Remove(count.absoluteValue, component)
          }
        }
  }
}
