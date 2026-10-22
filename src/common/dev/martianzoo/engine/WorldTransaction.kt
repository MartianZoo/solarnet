package dev.martianzoo.engine

import dev.martianzoo.state.TaskResult

/**
 * Groups nested commands and subsequent task handling inside a rollback scope.
 *
 * The name predates the proposed transaction definition: this block can include several Actor
 * transactions. The outermost call handles pending work and checks completion, then invokes its
 * completion callback and handles work added there. Corrections use [correct] without those steps.
 *
 * Every call reports the retained events of its own whole interval, so an outermost call covers
 * its initial block, its task handling, its completion callback, and the handling that follows
 * the callback. History navigation is refused while the callback runs, so the World is still at
 * the end of its history afterward.
 *
 * TODO(ENGINE_CONTRACT): Review how this coordinator and the outside Admin driver run the engine to
 *   quiescence. Failure restoration remains separate: a failing callback leaves earlier work in
 *   place.
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
    val callStart = timeline.nextEventOrdinal
    depth++
    return try {
      timeline.atomic {
        block()
        if (outermost || completionFollowUp) {
          afterChanges()
          validateCompletion()
        }
      }
      if (outermost) {
        reportingCompletion = true
        try {
          timeline.duringCompletionCallback(onComplete)
        } finally {
          reportingCompletion = false
        }
        timeline.atomic { afterChanges() }
      }
      timeline.activitySince(callStart)
    } finally {
      depth--
    }
  }

  /** Corrections preserve state validity without advancing gameplay. */
  internal fun correct(block: () -> Unit): TaskResult = timeline.atomic(block)
}
