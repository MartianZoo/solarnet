package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.types.TypeUniverse

/** The live, complete implementation of an [EngineState]. */
internal class WholeEngineState
internal constructor(
    override val components: ComponentGraph,
    override val events: EventLog,
    override val tasks: TaskQueue,
    override val timeline: Timeline,
    override val reader: GameReader,
    override val typeUniverse: TypeUniverse,
) : EngineState {

  private lateinit var gameplayByActor: Map<Actor, Gameplay>

  internal fun initializeGameplay(gameplayByActor: Map<Actor, Gameplay>) {
    check(!this::gameplayByActor.isInitialized)
    this.gameplayByActor = gameplayByActor
  }

  override fun gameplay(actor: Actor): Gameplay = gameplayByActor[actor]!!

  override var onAtomicComplete: () -> Unit = {}
}

/** Couples generic live engine state to the setup metadata exposed by a playable [Game]. */
internal class PlayableGame(
    state: EngineState,
    override val setup: GameSetup,
) : Game, EngineState by state
