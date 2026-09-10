package dev.martianzoo.engine

import dev.martianzoo.pets.data.TaskResult

/**
 * Executes Agent operations atomically, settles callback-started follow-ups, and reports the
 * outermost successful completion.
 */
internal class AtomicOperationScope(
    private val timeline: Timeline,
    private val onComplete: () -> Unit,
    private val recordingPositions: RecordingPositions,
    private val removeTemporaryComponents: () -> Boolean,
) {
  private var depth: Int = 0
  private var reportingCompletion: Boolean = false

  internal fun run(
      block: () -> Unit,
      afterIdleCleanup: () -> Unit = {},
      beforeOutermostCompletion: () -> Unit,
  ): TaskResult {
    val outermost = depth == 0
    val completionFollowUp = reportingCompletion && depth == 1
    depth++
    return try {
      timeline
          .atomic {
            block()
            if (outermost || completionFollowUp) {
              performIdleCleanup(beforeOutermostCompletion)
              afterIdleCleanup()
            }
          }
          .also {
            if (outermost) {
              recordingPositions.record(timeline.checkpoint().ordinal)
              reportingCompletion = true
              try {
                onComplete()
              } finally {
                reportingCompletion = false
              }
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
