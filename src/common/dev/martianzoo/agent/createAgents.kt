package dev.martianzoo.agent

import dev.martianzoo.engine.World
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.data.Actor

/** Constructs one stable [Agent] per Actor, all sharing one legacy autoexecution loop. */
public fun createAgents(world: World): Map<Actor, Agent> {
  val autoExecLoop = AutoExecLoop(world)
  val elaborator = PetElaborator(world.classTable)
  return world.actors.associateWith { actor ->
    AgentImpl(world.actorEngine(actor), elaborator, autoExecLoop)
  }
}
