package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CommercialDistrictTest : TfmSandboxTest() {
  @Test
  internal fun `Scores neighboring cities including those added after placement`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Kim, Tharsis_3_2>")
    kim.playProject(CommercialDistrict, 16) { placeTile(3, 3) }
    stan.exMachina("NormalCityTile<Stan, Tharsis_3_4>, NormalCityTile<Stan, Tharsis_8_6>")

    victoryPoints() shouldBe listOf(22, 20, 20)
  }
}
