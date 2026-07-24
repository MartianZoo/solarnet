package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.types.MClassTable

/** The live, complete state and control implementation for a [Game]. */
internal class WholeGameState
internal constructor(
    override val components: ComponentGraph,
    override val events: EventLog,
    override val tasks: TaskQueue,
    override val timeline: Timeline,
    override val reader: GameReader,
    override val setup: GameSetup,
    override val classes: MClassTable,
) : Game {

  private lateinit var gameplayByActor: Map<Actor, Gameplay>

  internal fun initializeGameplay(gameplayByActor: Map<Actor, Gameplay>) {
    check(!this::gameplayByActor.isInitialized)
    this.gameplayByActor = gameplayByActor
  }

  override fun gameplay(actor: Actor): Gameplay = gameplayByActor[actor]!!

  override var onAtomicComplete: () -> Unit = {}
}
