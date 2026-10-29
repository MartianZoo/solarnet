package dev.martianzoo.engine

import dev.martianzoo.state.TaskResult

/**
 * Groups nested commands and subsequent task handling inside a rollback scope.
 *
 * The name predates the intended transaction definition: this block can include several Actor
 * transactions. The outermost call handles pending work and checks completion inside the same
 * restoring block. Corrections use [correct] without those steps.
 *
 * Every call reports the retained events of its own whole interval, so an outermost call covers its
 * initial block and its task handling. A failure during task handling restores the initial work
 * too.
 *
 * TODO(ENGINE_CONTRACT): Review ownership of the code that groups commands, runs subsequent Admin
 *   work, and reports completion.
 */
internal class WorldTransaction(
    private val timeline: TimelineImpl,
) {
  private var depth: Int = 0

  internal fun run(
      block: () -> Unit,
      validateCompletion: () -> Unit = {},
      afterChanges: () -> Unit,
  ): TaskResult {
    val outermost = depth == 0
    val callStart = timeline.nextEventOrdinal
    depth++
    return try {
      timeline.atomic {
        block()
        if (outermost) {
          afterChanges()
          validateCompletion()
        }
      }
      timeline.activitySince(callStart)
    } finally {
      depth--
    }
  }

  /** Corrections preserve state validity without advancing gameplay. */
  internal fun correct(block: () -> Unit): TaskResult = timeline.atomic(block)
}
