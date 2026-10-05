package dev.martianzoo.engine

import dev.martianzoo.state.TaskResult

/**
 * Coordinates nested game mutations as one failure-atomic interaction.
 *
 * The outermost call settles configured policy and temporary cleanup, validates the caller's
 * completion rule, then processes continuations and records the resulting position. Nested calls
 * share that transaction. A cleanup step can create new work; [settleAndCleanUp] settles it before
 * another cleanup step can be attempted. Corrections use [correct] to validate and record state
 * without advancing gameplay.
 */
internal class WorldTransaction(
    private val timeline: Timeline,
    private val recordingPositions: RecordingPositions,
    private val removeTemporaryComponent: () -> Boolean,
    private val removeContinuation: () -> Boolean,
) {
  private var depth: Int = 0

  internal fun run(
      block: () -> Unit,
      validateCompletion: () -> Unit = {},
      settle: () -> Unit,
  ): TaskResult {
    val outermost = depth == 0
    depth++
    return try {
      timeline
          .atomic {
            block()
            if (outermost) {
              settleAndCleanUp(settle)
              validateCompletion()
              continueAfterCompletion(settle)
            }
          }
          .also {
            if (outermost) recordingPositions.record(timeline.checkpoint().ordinal)
          }
    } finally {
      depth--
    }
  }

  /** Corrections preserve state validity without advancing gameplay. */
  internal fun correct(block: () -> Unit): TaskResult =
      timeline.atomic(block).also {
        if (depth == 0) recordingPositions.record(timeline.checkpoint().ordinal)
      }

  private fun settleAndCleanUp(settle: () -> Unit) {
    do {
      settle()
    } while (removeTemporaryComponent())
  }

  /** Continuations deliberately start work beyond the operation that created them. */
  private fun continueAfterCompletion(settle: () -> Unit) {
    while (removeContinuation()) {
      settleAndCleanUp(settle)
    }
  }
}
