package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameSetup

public object Engine {
  public fun newGame(setup: GameSetup) = Game.create(setup) // TODO inline
}
