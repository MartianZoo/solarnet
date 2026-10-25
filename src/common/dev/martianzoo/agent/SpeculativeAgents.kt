package dev.martianzoo.agent

import dev.martianzoo.engine.SpeculativeWorld
import dev.martianzoo.state.Actor

/**
 * Agents for a reusable speculative World.
 *
 * Caller-controlled exploration can include later tasks; it remains distinct from the engine's
 * internal copies used to prove a requested transaction. The public copy/reset surface remains
 * subject to review under ENGINE_CONTRACT.
 */
public class SpeculativeAgents
internal constructor(
    private val speculativeWorld: SpeculativeWorld,
    initialPolicy: (Actor) -> AutoExecPolicy,
) : Agents(speculativeWorld, initialPolicy) {
  /** Discards this branch's local work so these same Agents can try another continuation. */
  public fun reset() {
    speculativeWorld.reset()
    taskLog.resetAfterOverlayReset()
  }
}
