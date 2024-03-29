package dev.martianzoo.data

import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.pets.ast.Expression
import kotlin.math.absoluteValue

/**
 * Returned by a successful execution to indicate what changes were performed and what new tasks
 * were added.
 */
public data class TaskResult(
    public val changes: List<ChangeEvent> = listOf(),
    public val tasksSpawned: Set<TaskId> = setOf(),
) {
  public fun net(): List<StateChange> {
    val map = mutableMapOf<Expression, Int>()
    for (change in changes.map { it.change }) {
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
        .map { (expr, count) ->
          StateChange(
              count = count.absoluteValue,
              gaining = if (count > 0) expr else null,
              removing = if (count < 0) expr else null,
          )
        }
  }
}
