package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.types.MClassTable

/** Entry point to the solarnet engine -- create new games here. */
public object Engine {
  /** Creates a new game, initialized for the given [setup], and ready for gameplay to begin. */
  public fun newGame(setup: GameSetup) = newGame(MClassTable.forSetup(setup))

  /** Creates a new game using an existing class table, ready for gameplay to begin. */
  public fun newGame(table: MClassTable): Game {
    val game = Game(table)
    val session = game.session(ENGINE)

    val ord = session.operation("$ENGINE!").changes.first()
    val fakeCause = Cause(ENGINE.expression, ord.ordinal)

    table.singletons.forEach {
      session.initiateOnly(Parsing.parse("${it.expression}!"), fakeCause)
    }
    session.autoExec(false)

    session.operation("CorporationPhase FROM Phase")
    game.setupFinished()
    return game
  }
}
