package dev.martianzoo.engine

import dev.martianzoo.state.TaskResult

/**
 * Coordinates nested game mutations as one failure-atomic interaction.
 *
 * The outermost gameplay call and a reentry from its completion callback settle configured policy
 * and validate the caller's completion rule. The outermost call reports completion and settles work
 * started by its callback. The caller-facing Agent records a position when its method returns.
 * Corrections use [correct] without advancing gameplay.
 */
internal class WorldTransaction(
    private val timeline: TimelineImpl,
    private val onComplete: () -> Unit,
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
              settle()
              validateCompletion()
            }
          }
          .also {
            if (outermost) {
              reportingCompletion = true
              try {
                onComplete()
              } finally {
                reportingCompletion = false
              }
              if (timeline.atEnd) timeline.atomic { settle() }
            }
          }
    } finally {
      depth--
    }
  }

  /** Corrections preserve state validity without advancing gameplay. */
  internal fun correct(block: () -> Unit): TaskResult = timeline.atomic(block)
}
