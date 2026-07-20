package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FinalGreeneryPhaseTest {
  @Test
  fun normalGreeneryRaisesOxygen() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Auto(game, Canon.SIMPLE_GAME).launch()

    p1.playCorp("Ecoline", 0)
    game.tfm(PLAYER2).playCorp("TharsisRepublic", 0)
    p1.godMode().sneak("8 Plant")
    engine.count("Photosynthesis") shouldBe 1

    p1.stdAction("ConvertPlantsSA") {
      doTask("GreeneryTile<Tharsis_3_6>")
    }

    engine.oxygenPercent() shouldBe 1
    workflow.shutdown()
  }

  @Test
  fun finalGreeneryDoesNotRaiseOxygen() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Manual(game, Canon.SIMPLE_GAME)

    workflow.setupPhase()
    workflow.corporationPhase()
    p1.godMode().manual("8 Plant")
    workflow.finalGreeneryPhase()
    engine.count("Photosynthesis") shouldBe 0
    p1.startTurn()
    p1.doTask("UseAction1<ConvertPlantsSA>")
    p1.doTask("GreeneryTile<Tharsis_3_5>")

    engine.oxygenPercent() shouldBe 0
  }
}
