package dev.martianzoo.engine

import dev.martianzoo.state.TaskResult

/**
 * Groups nested commands and subsequent task handling inside a rollback scope.
 *
 * The name predates the proposed transaction definition: this block can include several Actor
 * transactions. The outermost call handles pending work and checks completion, then invokes its
 * completion callback and handles work added there. Corrections use [correct] without those steps.
 *
 * TODO(ENGINE_CONTRACT): Review how this coordinator and the outside Admin driver run the engine to
 *   quiescence. The synchronous call must include subsequent work in its result; it does not
 *   require a separate interaction concept.
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
      afterChanges: () -> Unit,
  ): TaskResult {
    val outermost = depth == 0
    val completionFollowUp = reportingCompletion && depth == 1
    depth++
    return try {
      timeline
          .atomic {
            block()
            if (outermost || completionFollowUp) {
              afterChanges()
              validateCompletion()
            }
          }
          // TODO(ENGINE_CONTRACT): Include callback changes and subsequent task handling in the
          // returned TaskResult. It currently describes only the initial restoring block.
          // Separately review failure restoration: a failing callback leaves earlier work in place.
          .also {
            if (outermost) {
              reportingCompletion = true
              try {
                onComplete()
              } finally {
                reportingCompletion = false
              }
              if (timeline.atEnd) timeline.atomic { afterChanges() }
            }
          }
    } finally {
      depth--
    }
  }

  /** Corrections preserve state validity without advancing gameplay. */
  internal fun correct(block: () -> Unit): TaskResult = timeline.atomic(block)
}
