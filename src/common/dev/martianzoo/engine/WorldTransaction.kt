package dev.martianzoo.engine

import dev.martianzoo.pets.data.TaskResult

/**
 * Coordinates nested game mutations as one transaction, settles callback-started follow-ups, and
 * reports successful completion.
 */
internal class WorldTransaction(
    private val timeline: Timeline,
    private val onComplete: () -> Unit,
    private val recordingPositions: RecordingPositions,
    private val removeTemporaryComponent: () -> Boolean,
) {
  private var depth: Int = 0
  private var reportingCompletion: Boolean = false

  internal fun run(
      block: () -> Unit,
      validateCompletion: () -> Unit = {},
      settle: () -> Unit,
  ): TaskResult {
    val outermost = depth == 0
    val completionFollowUp = reportingCompletion && depth == 1
    depth++
    return try {
      timeline
          .atomic {
            block()
            if (outermost || completionFollowUp) {
              settleAndCleanUp(settle)
              validateCompletion()
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
              timeline.atomic { settleAndCleanUp(settle) }
              recordingPositions.record(timeline.checkpoint().ordinal)
            }
          }
    } finally {
      depth--
    }
  }

  private fun settleAndCleanUp(settle: () -> Unit) {
    do {
      settle()
    } while (removeTemporaryComponent())
  }
}
