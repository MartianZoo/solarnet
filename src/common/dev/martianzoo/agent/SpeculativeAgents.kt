package dev.martianzoo.agent

import dev.martianzoo.engine.SpeculativeWorld
import dev.martianzoo.state.Actor

/** Agents for a reusable speculative World. */
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
