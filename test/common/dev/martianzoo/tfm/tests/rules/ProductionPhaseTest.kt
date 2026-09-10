package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecMode.NONE
import dev.martianzoo.engine.*
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ProductionPhaseTest {
  @Test
  internal fun existingEnergyBecomesHeatBeforeNewEnergyIsProduced() {
    val game = setUpGame()
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    p1.manual("2 Energy, PROD[Energy]")
    val manual = admin.also { it.autoExecMode = NONE }

    manual.beginManual("ProductionPhase FROM Phase") {
      p1.count("Energy") shouldBe 0
      p1.count("Heat") shouldBe 2
      p1.doTask("Energy")
    }

    p1.count("Energy") shouldBe 1
    p1.count("Heat") shouldBe 2
  }
}
