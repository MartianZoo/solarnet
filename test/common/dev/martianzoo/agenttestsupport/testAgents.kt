package dev.martianzoo.agenttestsupport

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.engine.World
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

private var retainedAgents: Agents? = null

/**
 * Retains Agent identity within a test without retaining every World created by a long suite.
 *
 * TODO: Have the remaining standalone engine test fixtures hold the [Agents] they create, and
 *   delete this.
 */
internal fun World.testAgents(): Agents {
  retainedAgents?.let { if (it.world === this) return it }
  return Agents(this).also { retainedAgents = it }
}

/** Test-only seam for obtaining this World's retained Agent for [actor]. */
internal fun World.testAgent(actor: Actor): Agent = testAgents()[actor]

/** Test-only seam for obtaining Terraforming Mars gameplay for [actor]. */
internal fun World.testTfm(actor: Actor): TfmGameplay = testAgents().tfm(actor)

/**
 * Starts this World's game for a synthetic scenario that stays in SetupPhase: setup's pending
 * `FINALLY` handoff to the corporation phase is dropped, so no turn or later phase begins.
 */
internal fun World.startSetupOnly() {
  val agents = testAgents()
  agents.tfm(ADMIN).startGame()
  tasks.extract { it }.filter { it.deferred }.forEach { agents[it.assignee].dropTask(it.id) }
}
