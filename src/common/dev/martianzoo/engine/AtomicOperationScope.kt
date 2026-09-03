package dev.martianzoo.engine

import dev.martianzoo.pets.data.TaskResult

/** Executes Agent operations atomically and reports the outermost successful completion. */
internal class AtomicOperationScope(
    private val timeline: Timeline,
    private val onComplete: () -> Unit,
    private val recordingPositions: RecordingPositions,
    private val removeTemporaryComponents: () -> Boolean,
) {
  private var depth: Int = 0

  internal fun run(
      block: () -> Unit,
      afterIdleCleanup: () -> Unit = {},
      beforeOutermostCompletion: () -> Unit,
  ): TaskResult {
    depth++
    return try {
      timeline
          .atomic {
            block()
            if (depth == 1) {
              performIdleCleanup(beforeOutermostCompletion)
              afterIdleCleanup()
            }
          }
          .also {
            if (depth == 1) {
              recordingPositions.record(timeline.checkpoint().ordinal)
              onComplete()
              timeline.atomic { performIdleCleanup(beforeOutermostCompletion) }
              recordingPositions.record(timeline.checkpoint().ordinal)
            }
          }
    } finally {
      depth--
    }
  }

  private fun performIdleCleanup(beforeOutermostCompletion: () -> Unit) {
    do {
      beforeOutermostCompletion()
    } while (removeTemporaryComponents())
  }
}
