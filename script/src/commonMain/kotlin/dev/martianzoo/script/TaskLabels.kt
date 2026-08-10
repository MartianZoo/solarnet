package dev.martianzoo.script

import dev.martianzoo.data.Task.TaskId

/** Lazily assigns stable, short selection handles and restores their state across rollback. */
internal class TaskLabels {
  private val labelsByTaskId: MutableMap<TaskId, String> = linkedMapOf()
  private val snapshotsByOrdinal: MutableMap<Int, Map<TaskId, String>> = mutableMapOf()

  fun labelsFor(
      existingIds: Set<TaskId>,
      requestedIds: List<TaskId>,
      checkpointOrdinal: Int,
  ): Map<TaskId, String> {
    discardFinished(existingIds)
    requestedIds.forEach { id -> labelsByTaskId.getOrPut(id, ::nextLabel) }
    remember(checkpointOrdinal)
    return labelsByTaskId.filterKeys { it in requestedIds }
  }

  fun resolve(existingIds: Set<TaskId>, label: String, checkpointOrdinal: Int): TaskId? {
    discardFinished(existingIds)
    remember(checkpointOrdinal)
    return labelsByTaskId.entries.singleOrNull { it.value == label }?.key
  }

  fun clear() {
    labelsByTaskId.clear()
    snapshotsByOrdinal.clear()
  }

  fun restoreAfterRollback(existingIds: Set<TaskId>, rollbackOrdinal: Int) {
    val snapshot =
        snapshotsByOrdinal.filterKeys { it <= rollbackOrdinal }.maxByOrNull { it.key }?.value
            ?: emptyMap()
    labelsByTaskId.clear()
    labelsByTaskId.putAll(snapshot.filterKeys { it in existingIds })
    snapshotsByOrdinal.keys.removeAll { it > rollbackOrdinal }
    remember(rollbackOrdinal)
  }

  private fun discardFinished(existingIds: Set<TaskId>) {
    labelsByTaskId.keys.retainAll(existingIds)
  }

  private fun remember(checkpointOrdinal: Int) {
    snapshotsByOrdinal[checkpointOrdinal] = labelsByTaskId.toMap()
  }

  private fun nextLabel(): String {
    val nextIndex = labelsByTaskId.values.maxOfOrNull(::labelIndex)?.plus(1) ?: 0
    return labelFor(nextIndex)
  }

  private fun labelIndex(label: String): Int {
    var index = 0
    label.forEach { index = index * 26 + (it - 'A' + 1) }
    return index - 1
  }

  private fun labelFor(index: Int): String {
    var remaining = index
    return buildString {
      do {
        append('A' + remaining % 26)
        remaining = remaining / 26 - 1
      } while (remaining >= 0)
    }
        .reversed()
  }
}
