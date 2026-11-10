package dev.martianzoo.tfm.tests

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2Cards
import dev.martianzoo.tfm.tests.cards.cardnames.AquiferPumping
import kotlin.test.Test

internal class TfmPaymentHelpersTest : TfmTest() {
  @Test
  internal fun `Card action leaves the metal payment choice for its callback`() {
    game = setUpGame()
    agents[PLAYER1].exMachina("AquiferPumping, Steel, 8 MC")
    val kim = game.testTfm(PLAYER1)
    kim.addTasks("UseAction<StandardAction>")

    kim.cardAction1(AquiferPumping) {
          kim.pay(6, steel = 1)
          kim.placeTile(1, 2)
        }
        .expect("-6 MC, Steel, OceanTile, TerraformRating")
  }

  @Test
  internal fun `Default milestone payment acknowledges Nirgal's free bill only once`() {
    game = setUpGame(Prelude2Cards)
    agents[PLAYER1].exMachina("NirgalEnterprises, 15 TerraformRating")
    val kim = game.testTfm(PLAYER1)
    kim.addTasks("UseAction<StandardAction>")

    kim.claimMilestone(cn("Terraformer35")).expect("Terraformer35, 0 MC")
  }

  @Test
  internal fun `Default heat conversion keeps unused Stormcraft floaters`() {
    game = setUpGame(ColoniesExpansion, colonyTiles = setOf(cn("Ceres")))
    agents[PLAYER1].exMachina("StormcraftIncorporated, Floater<StormcraftIncorporated>, 8 Heat")
    val kim = game.testTfm(PLAYER1)
    kim.addTasks("UseAction<StandardAction>")

    kim.convertHeat().expect("TemperatureStep, -8 Heat, 0 Floater<StormcraftIncorporated>")
  }

  @Test
  internal fun `Explicit heat payment keeps unused Stormcraft floaters`() {
    game = setUpGame(ColoniesExpansion, colonyTiles = setOf(cn("Ceres")))
    agents[PLAYER1].exMachina("StormcraftIncorporated, Floater<StormcraftIncorporated>, 8 Heat")
    val kim = game.testTfm(PLAYER1)
    kim.addTasks("UseAction<StandardAction>")

    kim.stdAction("UseAction<ConvertHeatAction, Action1>", payment = { kim.pay(heat = 8) })
        .expect("TemperatureStep, -8 Heat, 0 Floater<StormcraftIncorporated>")
  }
}
