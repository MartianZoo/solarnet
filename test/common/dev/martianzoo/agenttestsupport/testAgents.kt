package dev.martianzoo.agenttestsupport

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.createAgents
import dev.martianzoo.engine.World
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

private var retainedWorld: World? = null
private var retainedAgents: Map<Actor, Agent>? = null

/** Retains Agent identity within a test without retaining every World created by a long suite. */
internal fun World.testAgents(): Map<Actor, Agent> {
  if (retainedWorld === this) return checkNotNull(retainedAgents)
  retainedWorld = this
  return createAgents(this).also { retainedAgents = it }
}

/** Test-only seam for obtaining this World's retained Agent for [actor]. */
internal fun World.testAgent(actor: Actor): Agent = testAgents().getValue(actor)

/** Test-only seam for obtaining Terraforming Mars gameplay for [actor]. */
internal fun World.testTfm(actor: Actor): TfmGameplay = tfm(testAgents(), actor)
