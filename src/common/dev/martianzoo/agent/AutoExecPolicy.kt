package dev.martianzoo.agent

/**
 * Existing configurable rule for handling tasks without an explicit client choice. The current
 * implementation applies it to every Actor and tries Admin tasks first.
 *
 * TODO(ENGINE_CONTRACT): investigate removing these Player policies from the core Agent API. Player
 *   choices are explicit in the proposed contract; mandatory Admin handling must not use a
 *   configurable policy. In particular, NONE must never disable Admin work.
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
   * Allows the loop to try this actor's selectable tasks even when several compete. It takes the
   * first task that completes in Admin-first, then queue order and repeats, so a caller using this
   * policy accepts that ordering as its choice.
   */
  EAGER,
}
