package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.exMachina
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.FakeCards
import dev.martianzoo.tfm.tests.TestOption.Prelude2Cards
import dev.martianzoo.tfm.tests.cards.cardnames.AquiferPumping
import dev.martianzoo.tfm.tests.cards.cardnames.Mine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class TfmPaymentHelpersTest : TfmTest() {
  @Test
  internal fun `Card action leaves the metal payment choice for its callback`() {
    game = setUpGame()
    agents.exMachina(PLAYER1, "AquiferPumping, Steel, 8 MC")
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
    agents.exMachina(PLAYER1, "NirgalEnterprises, 15 TerraformRating")
    val kim = game.testTfm(PLAYER1)
    kim.addTasks("UseAction<StandardAction>")

    kim.claimMilestone(cn("Terraformer35")).expect("Terraformer35, 0 MC")
  }

  @Test
  internal fun `Default heat conversion keeps unused Stormcraft floaters`() {
    game = setUpGame(ColoniesExpansion, colonyTiles = setOf(cn("Ceres")))
    agents.exMachina(PLAYER1, "StormcraftIncorporated, Floater<StormcraftIncorporated>, 8 Heat")
    val kim = game.testTfm(PLAYER1)
    kim.addTasks("UseAction<StandardAction>")

    kim.convertHeat().expect("TemperatureStep, -8 Heat, 0 Floater<StormcraftIncorporated>")
  }

  @Test
  internal fun `Explicit heat payment keeps unused Stormcraft floaters`() {
    game = setUpGame(ColoniesExpansion, colonyTiles = setOf(cn("Ceres")))
    agents.exMachina(PLAYER1, "StormcraftIncorporated, Floater<StormcraftIncorporated>, 8 Heat")
    val kim = game.testTfm(PLAYER1)
    kim.addTasks("UseAction<StandardAction>")

    kim.stdAction("UseAction<ConvertHeatAction, Action1>", payment = { kim.pay(heat = 8) })
        .expect("TemperatureStep, -8 Heat, 0 Floater<StormcraftIncorporated>")
  }

  @Test
  internal fun `Replay audit requires an explanation for heat when cash covers the bill`() {
    game = setUpGame(FakeCards)
    agents.exMachina(PLAYER1, "FakeHelion, 4 Heat, 4 MC, ProjectCard")
    val kim = game.testTfm(PLAYER1).requireExplicitPaymentChoices()
    kim.addTasks("UseAction<StandardAction>")

    shouldThrow<IllegalArgumentException> { kim.playProject(Mine, heat = 4) }
        .message
        .shouldContain("intentionalUnderpay()")
    kim.assertCounts(4 to "MC", 4 to "Heat", 1 to "ProjectCard", 0 to "Mine", 0 to "Owed")

    kim.intentionalUnderpay()
    kim.playProject(Mine, heat = 4).expect("Mine, PROD[Steel], -4 Heat, 0 MC")
  }

  @Test
  internal fun `Replay audit allows heat when cash cannot cover the bill`() {
    game = setUpGame(FakeCards)
    agents.exMachina(PLAYER1, "FakeHelion, 4 Heat, 3 MC, ProjectCard")
    val kim = game.testTfm(PLAYER1).requireExplicitPaymentChoices()
    kim.addTasks("UseAction<StandardAction>")

    kim.playProject(Mine, heat = 4).expect("Mine, PROD[Steel], -4 Heat, 0 MC")
  }
}
