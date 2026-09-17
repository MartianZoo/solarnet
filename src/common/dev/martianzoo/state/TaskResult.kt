package dev.martianzoo.state

import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.Task.TaskId
import kotlin.math.absoluteValue

/**
 * Returned by a successful execution to indicate what changes were performed and what new tasks
 * were added.
 */
public data class TaskResult(
    public val changes: List<ChangeEvent> = emptyList(),
    public val tasksSpawned: Set<TaskId> = emptySet(),
) {
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
