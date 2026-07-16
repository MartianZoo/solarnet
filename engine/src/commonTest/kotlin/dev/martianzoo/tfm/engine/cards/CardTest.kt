package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

abstract class CardTest {
  protected lateinit var game: Game

  protected fun newGame(setup: GameSetup): Game = Engine.newGame(setup).also { game = it }

  protected fun TaskResult.expect(string: String) =
      TestHelpers.assertNetChanges(this, game, game.tfm(ENGINE), string)
}
