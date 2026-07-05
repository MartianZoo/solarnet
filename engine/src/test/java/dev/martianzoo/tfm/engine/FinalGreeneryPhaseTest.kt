package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

private class FinalGreeneryPhaseTest {
  @Test
  fun normalGreeneryRaisesOxygen() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Manual(game, Canon.SIMPLE_GAME)

    workflow.corporationPhase()
    p1.godMode().manual("8 Plant")
    assertThat(engine.count("Photosynthesis")).isEqualTo(1)

    workflow.actionPhase()
    p1.stdAction("ConvertPlantsSA") {
      doTask("GreeneryTile<Tharsis_3_6>")
    }

    assertThat(engine.oxygenPercent()).isEqualTo(1)
  }

  @Test
  fun finalGreeneryDoesNotRaiseOxygen() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val workflow = TfmWorkflow.Manual(game, Canon.SIMPLE_GAME)

    workflow.corporationPhase()
    p1.godMode().manual("8 Plant")
    workflow.finalGreeneryPhase()
    assertThat(engine.count("Photosynthesis")).isEqualTo(0)
    p1.startTurn()
    p1.doTask("UseAction1<ConvertPlantsSA>")
    p1.doTask("GreeneryTile<Tharsis_3_5>")

    assertThat(engine.oxygenPercent()).isEqualTo(0)
  }
}
