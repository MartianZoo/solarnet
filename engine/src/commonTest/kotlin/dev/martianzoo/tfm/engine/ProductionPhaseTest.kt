package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ProductionPhaseTest {
  @Test
  fun existingEnergyBecomesHeatBeforeNewEnergyIsProduced() {
    val game = setUpGame()
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("2 Energy, PROD[Energy]")
    val manual = engine.godMode().also { it.autoExecMode = NONE }

    manual.beginManual("ProductionPhase FROM Phase") {
      p1.count("Energy") shouldBe 0
      p1.count("Heat") shouldBe 2
      p1.doTask("Energy")
    }

    p1.count("Energy") shouldBe 1
    p1.count("Heat") shouldBe 2
  }
}
