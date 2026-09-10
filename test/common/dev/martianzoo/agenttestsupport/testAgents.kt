package dev.martianzoo.agenttestsupport

import dev.martianzoo.agent.Agent
import dev.martianzoo.engine.World
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

/** Test-only seam for obtaining the current World's Agent for [actor]. */
internal fun World.testAgent(actor: Actor): Agent = agent(actor)

/** Test-only seam for obtaining Terraforming Mars gameplay for [actor]. */
internal fun World.testTfm(actor: Actor): TfmGameplay = tfm(actor)
