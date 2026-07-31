package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.World
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

abstract class TfmTest {
  protected lateinit var game: World

  protected val engine: TfmGameplay
    get() = game.tfm(ENGINE)

  protected fun TaskResult.expect(string: String) =
      TestHelpers.assertNetChanges(this, game, engine, string)
}
