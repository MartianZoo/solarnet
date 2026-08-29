package dev.martianzoo.engine

import dev.martianzoo.pets.data.TaskResult

/** Executes gameplay operations atomically and reports the outermost successful completion. */
internal class AtomicOperationScope(
    private val timeline: Timeline,
    private val onComplete: () -> Unit,
    private val recordingPositions: RecordingPositions,
) {
  private var depth: Int = 0

  internal fun run(block: () -> Unit, beforeOutermostCompletion: () -> Unit): TaskResult {
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
    }
  }
}
