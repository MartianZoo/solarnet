package dev.martianzoo.engine

import dev.martianzoo.data.TaskResult

/** Executes gameplay operations atomically and reports the outermost successful completion. */
internal class AtomicOperationBoundary(
    private val timeline: Timeline,
    private val onComplete: () -> Unit,
) {
  private var depth: Int = 0

  internal fun run(block: () -> Unit): TaskResult {
    depth++
    return try {
      timeline.atomic(block).also { if (depth == 1) onComplete() }
    } finally {
      depth--
    }
  }
}
