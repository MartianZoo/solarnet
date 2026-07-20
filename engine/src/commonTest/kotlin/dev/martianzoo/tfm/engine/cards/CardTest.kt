package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Game
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.setUpGame as setUpTfmGame

internal fun setUpGame(setup: GameSetup): Game = setUpTfmGame(setup)

internal fun setUpGame(
    optionCodes: String,
    players: Int,
    colonyTiles: Set<ClassName> = emptySet(),
): Game = setUpTfmGame(optionCodes, players, colonyTiles)

abstract class CardTest {
  protected lateinit var game: Game

  protected fun newGame(setup: GameSetup): Game = setUpGame(setup).also { game = it }

  protected fun TaskResult.expect(string: String) =
      TestHelpers.assertNetChanges(this, game, game.tfm(ENGINE), string)
}
