package dev.martianzoo.agent

import dev.martianzoo.engine.SpeculativeWorld
import dev.martianzoo.engine.World
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Task

/** Agents for a reusable speculative World. */
public class SpeculativeAgents
internal constructor(
    private val speculativeWorld: SpeculativeWorld,
    safeTask: (World, Task) -> Boolean,
    initialPolicy: (Actor) -> AutoExecPolicy,
) : Agents(speculativeWorld, safeTask, initialPolicy) {
  /** Discards this branch's local work so these same Agents can try another continuation. */
  public fun reset() {
    speculativeWorld.reset()
    taskLog.resetAfterOverlayReset()
  }
}
