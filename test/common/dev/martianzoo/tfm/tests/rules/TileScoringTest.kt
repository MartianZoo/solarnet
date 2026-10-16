package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TileScoringTest : TfmSandboxTest() {
  @Test
  internal fun `Tiles score for their owners and cities count other players greenery`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Tharsis_3_3>, GreeneryTile<Tharsis_3_4>")
    stan.exMachina("NormalCityTile<Tharsis_3_5>, GreeneryTile<Tharsis_3_2>")
    rob.exMachina("GreeneryTile<Tharsis_9_7>")

    // Kim's city has two neighboring greeneries; Stan's has Kim's greenery as its neighbor.
    victoryPoints() shouldBe listOf(23, 22, 21)
  }

  @Test
  internal fun `Solo cities count neutral greenery while neutral tiles award no points`() {
    newTestGame(playerCount = 1)
    // Setup placed neutral city-greenery pairs at 2_2/2_3 and 4_1/5_1.
    kim.exMachina(
        "NormalCityTile<Tharsis_2_4>, GreeneryTile<Tharsis_2_5>, GreeneryTile<Tharsis_4_2>"
    )

    // 14 TR, two owned greeneries, and two greeneries beside Kim's city (including a neutral tile).
    // Kim's greenery at 4_2 also neighbors a neutral city, which awards no points.
    victoryPoints() shouldBe listOf(18)
  }
}
