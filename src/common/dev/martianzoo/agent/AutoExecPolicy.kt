package dev.martianzoo.agent

/**
 * Per-actor rule used by [Agent] when it settles pending tasks. The shared loop considers current
 * tasks repeatedly until no eligible task can complete. Admin tasks are considered first; task
 * iteration order is stable for a given queue but has no gameplay meaning.
 */
public enum class AutoExecPolicy {
  /** This actor's tasks remain pending until selected explicitly. */
  NONE,

  /**
   * Considers this actor's task only when it is the sole currently selectable candidate assigned to
   * that actor. The task must still complete without an unresolved choice, and another actor's
   * offered option can prevent the loop from taking it.
   */
  CONCRETE,

  /**
   * Performs this actor's task when it involves no unresolved choice and either it is the only
   * ordinary task in the World or the game's safe-task predicate accepts taking it early. A game
   * may accept early draws while undo is available even if another task could change their timing.
   * SAFE never chooses among card identities or discard options. It is checked again after every
   * completed command.
   */
  SAFE,

  /**
   * Allows the loop to try this actor's selectable tasks even when several compete. It takes the
   * first task that completes in Admin-first, then queue order and repeats, so a caller using this
   * policy accepts that ordering as its choice.
   */
  EAGER,
}
