package dev.martianzoo.agent

/** Specifies to what extent you want the engine to execute tasks automatically. */
public enum class AutoExecPolicy {
  /** Nothing is executed automatically. */
  NONE,

  /**
   * A task is only executed automatically if it is clear that no other task could succeed at this
   * time. That is, this policy should never remove an option from the player that is allowed by the
   * game rules.
   */
  CONCRETE,

  /**
   * Performs `SMART` autoexec, then arbitrarily selects the first task in the implementation's
   * stable iteration order that can execute successfully, then repeats. This might make suboptimal
   * moves for the player... but it's convenient.
   */
  EAGER,
}
