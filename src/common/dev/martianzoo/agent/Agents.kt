package dev.martianzoo.agent

import dev.martianzoo.engine.World
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.data.Actor

/**
 * A [World] together with the Agents that act on it: exactly one stable [Agent] per Actor, sharing
 * one autoexecution loop. Clients hold this rather than a World and a separate collection of
 * Agents, so an Agent can never be paired with a World it does not act on.
 */
public class Agents(public val world: World) {
  private val autoExecLoop = AutoExecLoop(world)
  private val elaborator = PetElaborator(world.classTable)

  private val agents: Map<Actor, Agent> =
      world.actors.associateWith {
        AgentImpl(world, world.actorEngine(it), elaborator, autoExecLoop)
      }

  /** This world's Agent for [actor]. */
  public operator fun get(actor: Actor): Agent = agents.getValue(actor)
}
