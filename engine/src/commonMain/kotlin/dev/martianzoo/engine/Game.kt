package dev.martianzoo.engine

import dev.martianzoo.tfm.data.GameSetup

/** A playable game in progress: a [World] together with the exact setup used to create it. */
public interface Game : World {
  /** Configuration. */
  public val setup: GameSetup
}
