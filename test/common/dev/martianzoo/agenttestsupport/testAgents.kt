package dev.martianzoo.agenttestsupport

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.engine.World
import dev.martianzoo.state.Actor

private var retainedAgents: Agents? = null

/**
 * Retains Agent identity within a test without retaining every World created by a long suite.
 *
 * TODO: Have the remaining standalone engine test fixtures hold the [Agents] they create, and
 *   delete this.
 */
internal fun World.testAgents(): Agents {
  retainedAgents?.let { if (it.world === this) return it }
  return Agents(this) { EAGER }.also { retainedAgents = it }
}

/** Test-only seam for obtaining this World's retained Agent for [actor]. */
internal fun World.testAgent(actor: Actor): Agent = testAgents()[actor]
