package dev.martianzoo.engine

import dev.martianzoo.pets.data.TaskResult

/** Coordinates nested game mutations as one transaction and reports successful completion. */
internal class WorldTransaction(
    private val timeline: Timeline,
    private val onComplete: () -> Unit,
    private val recordingPositions: RecordingPositions,
    private val removeTemporaryComponent: () -> Boolean,
) {
  private var depth: Int = 0

  internal fun run(
      block: () -> Unit,
      validateCompletion: () -> Unit = {},
      settle: () -> Unit,
  ): TaskResult {
    depth++
    return try {
      timeline
          .atomic {
            block()
            if (depth == 1) {
              settleAndCleanUp(settle)
              validateCompletion()
            }
          }
          .also {
            if (depth == 1) {
              recordingPositions.record(timeline.checkpoint().ordinal)
              onComplete()
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
