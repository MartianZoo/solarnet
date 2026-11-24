package dev.martianzoo.engine

import dev.martianzoo.state.Actor
import dev.martianzoo.state.TaskResult

/** Groups engine commands and mandatory work in a restoring call, with scoped session checks. */
internal class WorldTransaction(
    private val timeline: TimelineImpl,
    private val transactions: Transactions,
    private val runAdmin: (() -> Unit) -> Unit,
) {
  internal var enabled: Boolean = false
  private var depth = 0
  private var handlingAdmin = false
  private var validateApplication: () -> Unit = {}

  /** Task interpretation trials and nested engine implementation calls do not drive later work. */
  internal fun <T> command(actor: Actor, block: () -> T): T {
    if (!enabled || handlingAdmin || transactions.isOpen) return block()
    return timeline.restoring {
      if (actor != Actor.ADMIN) finishAdmin()
      block().also {
        finishAdmin()
        validateApplication()
      }
    }
  }

  private fun finishAdmin() {
    if (!enabled || handlingAdmin) return
    handlingAdmin = true
    try {
      runAdmin(validateApplication)
    } finally {
      handlingAdmin = false
    }
  }

  internal fun run(
      block: () -> Unit,
      validateCompletion: () -> Unit = {},
      afterChanges: () -> Unit = {},
      validateApplication: (() -> Unit)? = null,
  ): TaskResult {
    check(!transactions.isOpen) {
      "a call scope must enclose engine transactions, not start inside one"
    }
    val outermost = depth == 0
    val previousCheck = this.validateApplication
    this.validateApplication = validateApplication ?: previousCheck
    depth++
    return try {
      timeline.atomic {
        finishAdmin()
        block()
        finishAdmin()
        if (outermost) {
          afterChanges()
          finishAdmin()
          validateCompletion()
        }
        this.validateApplication()
      }
    } finally {
      depth--
      this.validateApplication = previousCheck
    }
  }

  /** Applies an atomic correction without handling pending tasks. */
  internal fun correct(block: () -> Unit): TaskResult = timeline.atomic(block)
}
