package dev.martianzoo.engine

import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Actor
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.GameWorld

/** Engine-facing World with a reusable local event suffix. */
internal class OverlayWorld(
    gameWorld: GameWorld,
    private val overlayTimeline: TimelineImpl,
    reader: GameReader,
    classTable: ClassTable,
    actorEngines: Map<Actor, ActorEngine>,
    effector: Effector,
) :
    WholeWorld(gameWorld, overlayTimeline, reader, classTable, actorEngines, effector),
    SpeculativeWorld {
  override fun installAgentTransactionGuard(guard: AgentTransactionGuard): Nothing =
      error("a speculative World cannot install an Agent transaction guard")

  override fun reset() = overlayTimeline.resetOverlay()
}
