package dev.martianzoo.engine

import dev.martianzoo.tfm.data.GameSetup

/**
 * A playable game in progress: a live [EngineState] together with the exact setup used to create
 * it.
 */
public interface Game : EngineState {
  /** Configuration. */
  public val setup: GameSetup
}
