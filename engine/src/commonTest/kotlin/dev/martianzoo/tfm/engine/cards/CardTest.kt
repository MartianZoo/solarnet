package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.setUpGame as setUpTfmGame

abstract class CardTest {
  protected lateinit var game: Game

  protected val engine: TfmGameplay
    get() = game.tfm(ENGINE)

  protected val player1: TfmGameplay
    get() = game.tfm(PLAYER1)

  protected val player2: TfmGameplay
    get() = game.tfm(PLAYER2)

  protected val player3: TfmGameplay
    get() = game.tfm(PLAYER3)

  protected fun newGame(setup: GameSetup): Game = setUpTfmGame(setup).also { game = it }

  protected fun newBareGame(setup: GameSetup): Game = Engine.newGame(setup).also { game = it }

  protected fun newGame(
      optionCodes: String,
      players: Int,
      colonyTiles: Set<ClassName> = emptySet(),
  ): Game = setUpTfmGame(optionCodes, players, colonyTiles).also { game = it }

  /** Makes deliberate test-fixture changes read as such, without exposing the GodMode plumbing. */
  protected fun TfmGameplay.sneak(changes: String): TaskResult = godMode().sneak(changes)

  /** Runs an instruction through the engine while hiding the uninteresting GodMode plumbing. */
  protected fun TfmGameplay.manual(
      instruction: String,
      body: BodyLambda = {},
  ): TaskResult = godMode().manual(instruction, body)

  protected fun Gameplay.sneak(changes: String): TaskResult = godMode().sneak(changes)

  protected fun Gameplay.manual(
      instruction: String,
      body: BodyLambda = {},
  ): TaskResult = godMode().manual(instruction, body)

  protected fun TaskResult.expect(string: String) =
      TestHelpers.assertNetChanges(this, game, engine, string)
}
