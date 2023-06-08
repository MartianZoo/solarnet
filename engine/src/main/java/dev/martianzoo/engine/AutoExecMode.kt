package dev.martianzoo.tfm.engine

enum class AutoExecMode {
  /** Nothing is executed automatically. */
  NONE,

  /**
   * A task is only executed automatically if it is clear that no other task could succeed at this
   * time. That is, this mode should never remove an option from the player that is allowed by the
   * game rules.
   */
  SAFE,

  /**
   * Performs [SAFE] autoexec, then selects an order for remaining tasks only if it determines that
   * all outcomes are identical. This technically removes options from the player, but only when it
   * can't matter.
   */
  // SMART, // not yet

  /**
   * Performs [SMART] autoexec, then selects the earliest-queued task that can execute successfully,
   * then repeats. This might make suboptimal moves for the player... but it's convenient.
   */
  FIRST,
}
