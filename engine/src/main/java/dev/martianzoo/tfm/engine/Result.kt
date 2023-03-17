package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.util.toSetStrict

data class Result constructor(
    val changes: List<ChangeEvent>, // TODO Set
    val newTaskIdsAdded: Set<TaskId>,
    val fullSuccess: Boolean,
) {
  companion object {
    fun concat(results: List<Result>): Result {
      return Result(
          results.flatMap { it.changes },
          results.flatMap { it.newTaskIdsAdded }.toSetStrict(),
          results.all { it.fullSuccess })
    }
  }
}
