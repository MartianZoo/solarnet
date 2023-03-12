package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.LogEntry
import dev.martianzoo.tfm.data.LogEntry.ChangeEvent
import dev.martianzoo.tfm.data.LogEntry.ChangeEvent.Cause
import dev.martianzoo.tfm.data.LogEntry.TaskAddedEvent
import dev.martianzoo.tfm.data.LogEntry.TaskRemovedEvent
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.SingleExecution.ExecutionResult

class GameLog(val logEntries: MutableList<LogEntry> = mutableListOf()) {
  val size: Int by logEntries::size

  fun addEntry(entry: LogEntry) {
    if (entry is ChangeEvent) require(entry.ordinal == size) // TODO all
    logEntries += entry
  }

  fun addChangeEvent(change: StateChange, cause: Cause?): ChangeEvent =
      ChangeEvent(size, change, cause).also { addEntry(it) }

  data class Checkpoint(val ordinal: Int) {
    init {
      require(ordinal >= 0)
    }
  }

  fun checkpoint() = Checkpoint(size)

  fun changesSince(checkpoint: Checkpoint): List<ChangeEvent> =
      entriesSince(checkpoint).filterIsInstance<ChangeEvent>()

  fun newTasksSince(checkpoint: Checkpoint): Set<TaskId> {
    val ids = mutableSetOf<TaskId>()
    entriesSince(checkpoint).forEach {
      when (it) {
        is TaskAddedEvent -> ids += it.task.id
        is TaskRemovedEvent -> ids -= it.task.id
        else -> {}
      }
    }
    return ids
  }

  fun entriesSince(checkpoint: Checkpoint): List<LogEntry> =
      logEntries.subList(checkpoint.ordinal, size).toList()

  fun resultsSince(checkpoint: Checkpoint, fullSuccess: Boolean) =
      ExecutionResult(changesSince(checkpoint), newTasksSince(checkpoint), fullSuccess)
}
