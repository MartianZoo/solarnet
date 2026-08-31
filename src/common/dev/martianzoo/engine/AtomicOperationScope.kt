package dev.martianzoo.engine

import dev.martianzoo.pets.data.TaskResult

/** Executes Agent operations atomically and reports the outermost successful completion. */
internal class AtomicOperationScope(
    private val timeline: Timeline,
    private val onComplete: () -> Unit,
    private val recordingPositions: RecordingPositions,
) {
  private var depth: Int = 0
  private var outermostStartOrdinal: Int? = null

  internal val currentOperationStartOrdinal: Int
    get() = checkNotNull(outermostStartOrdinal)

  internal fun run(block: () -> Unit, beforeOutermostCompletion: () -> Unit): TaskResult {
    val outermost = depth == 0
    if (outermost) outermostStartOrdinal = timeline.checkpoint().ordinal
    depth++
    return try {
      timeline
          .atomic {
            block()
            if (depth == 1) beforeOutermostCompletion()
          }
          .also {
            if (depth == 1) {
              recordingPositions.record(timeline.checkpoint().ordinal)
              onComplete()
              recordingPositions.record(timeline.checkpoint().ordinal)
            }
          }
    } finally {
      depth--
      if (outermost) outermostStartOrdinal = null
    }
  }
}
