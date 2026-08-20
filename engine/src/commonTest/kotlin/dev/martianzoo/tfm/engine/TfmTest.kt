package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

abstract class TfmTest {
  protected lateinit var game: World

  protected val engine: TfmGameplay
    get() = game.tfm(ENGINE)

  protected fun TaskResult.expect(string: String) = TestHelpers.assertNetChanges(this, game, string)

  protected fun TfmGameplay.buyCards(count: Int): TaskResult =
      doTask(if (count == 0) "Ok" else "$count BuyCard")

  protected fun OperationBody.buyCards(count: Int) {
    doTask(if (count == 0) "Ok" else "$count BuyCard")
  }

  protected fun TfmGameplay.playCorp(cardName: ClassName, body: BodyLambda): TaskResult = inTurn {
    doTask("PlayCard<Class<CorporationCard>, Class<$cardName>>")
    body()
  }
}
