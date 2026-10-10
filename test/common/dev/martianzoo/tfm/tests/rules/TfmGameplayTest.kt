package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.AquiferPumping
import dev.martianzoo.tfm.tests.cards.cardnames.DevelopmentCenter
import dev.martianzoo.tfm.tests.cards.cardnames.FakeHelion
import dev.martianzoo.tfm.tests.cards.cardnames.Mine
import dev.martianzoo.tfm.tests.cards.cardnames.PowerPlant
import dev.martianzoo.tfm.tests.cards.cardnames.TitaniumMine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TfmGameplayTest : TfmSandboxTest() {
  @Test
  internal fun `No-argument pass asserts there are no unused action cards`() {
    newTestGame()
    kim.requireExplicitUnusedActionCards()

    kim.pass()

    newTestGame()
    kim.requireExplicitUnusedActionCards()
    kim.exMachina("AquiferPumping")

    shouldThrow<IllegalArgumentException> { kim.pass() }
    kim.pass(unused = AquiferPumping)
  }

  @Test
  internal fun `Standard action helper rejects a non-standard action provider`() {
    newTestGame()
    kim.exMachina("StJosephOfCupertinoMission")

    shouldThrow<IllegalArgumentException> { kim.stdAction("CathedralOption") }.message shouldBe
        "CathedralOption is not a StandardAction"
  }

  @Test
  internal fun `Payment rejects leaving steel unspent at full value`() {
    newTestGame()
    kim.requireExplicitPaymentChoices()
    kim.setToExMachina(10, "MC")
    kim.exMachina("2 Steel")
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<IllegalArgumentException> { kim.playProject(Mine, 4) }
    kim.count("MC") shouldBe 10
    kim.count("Steel") shouldBe 2
    kim.count("ProjectCard") shouldBe 1
    kim.count("$Mine") shouldBe 0

    newTestGame()
    kim.requireExplicitPaymentChoices()
    kim.setToExMachina(10, "MC")
    kim.exMachina("2 Steel")
    kim.setToExMachina(1, "ProjectCard")
    // Synthetic API test: no strategic reason; deliberate underpayment exercises the opt-in.
    kim.intentionalUnderpay()
    kim.playProject(Mine, 4).expect("-4 MC, PROD[Steel]")
  }

  @Test
  internal fun `Payment may preserve an accepted one-to-one resource without an opt-in`() {
    newTestGame(addOptions = "FakeStuffBundle", kimCorporation = FakeHelion)
    kim.requireExplicitPaymentChoices()
    kim.setToExMachina(10, "MC")
    kim.exMachina("2 Heat")
    kim.setToExMachina(1, "ProjectCard")

    kim.playProject(Mine, 4).expect("-4 MC, 0 Heat, PROD[Steel]")
  }

  @Test
  internal fun `Payment requires an opt-in to spend a one-to-one resource before money`() {
    newTestGame(addOptions = "FakeStuffBundle", kimCorporation = FakeHelion)
    kim.requireExplicitPaymentChoices()
    kim.setToExMachina(10, "MC")
    kim.exMachina("2 Heat")
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<IllegalArgumentException> {
      kim.playProject(Mine, 2, heat = 2)
    }
    kim.intentionalUnderpay()
    kim.playProject(Mine, 2, heat = 2).expect("-2 MC, -2 Heat, PROD[Steel]")
  }

  @Test
  internal fun `Required one-to-one resource is not audited as an alternative to money`() {
    newTestGame()
    kim.requireExplicitPaymentChoices()
    kim.setToExMachina(10, "MC")
    kim.setToExMachina(0, "ProjectCard")
    kim.exMachina("Energy, DevelopmentCenter")

    kim.cardAction1(DevelopmentCenter).expect("-Energy, ProjectCard, 0 MC")
  }

  @Test
  internal fun `Underpayment permission applies to only one payment`() {
    newTestGame()
    kim.requireExplicitPaymentChoices()
    kim.exMachina("14 MC, 2 Steel, 2 ProjectCard")

    // Synthetic API test: no strategic reason; deliberate underpayment exercises one-shot scope.
    kim.intentionalUnderpay()
    kim.playProject(Mine, 4).expect("-4 MC, PROD[Steel]")
    shouldThrow<IllegalArgumentException> { kim.playProject(PowerPlant, 4) }
  }

  @Test
  internal fun `Payment rejects a payment containing a unit that could be kept`() {
    newTestGame()
    kim.exMachina("3 Steel, ProjectCard")

    // Mine costs 4; two steel already settle it, so the third is returnable.
    shouldThrow<LimitsException> { kim.playProject(Mine, steel = 3) }

    kim.count("Steel") shouldBe 3
    kim.count("$Mine") shouldBe 0
  }

  @Test
  internal fun `Payment allows excess no single unit could have avoided`() {
    newTestGame()
    kim.exMachina("4 Steel, ProjectCard")

    // Titanium Mine costs 7; three steel are not enough, so the fourth may waste one M€.
    kim.playProject(TitaniumMine, steel = 4).expect("-4 Steel, 0 MC, $TitaniumMine, PROD[Titanium]")
  }

  @Test
  internal fun `Payment rejects mc beyond the remainder after steel`() {
    newTestGame()
    kim.setToExMachina(30, "MC")
    kim.exMachina("5 Steel")
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<LimitsException> {
      kim.playProject(AquiferPumping, mc = 18, steel = 5)
    }

    kim.count("MC") shouldBe 30
    kim.count("Steel") shouldBe 5
    kim.count("ProjectCard") shouldBe 1
    kim.count("$AquiferPumping") shouldBe 0
  }

  // This tests rejection of an arbitrary optional task, not a game rule.
  internal class TaskSelection : TfmTest() {
    @Test
    internal fun `Declining a second action rejects an unrelated optional task`() {
      game = setUpGame(canonicalPremise())
      val player = game.testTfm(dev.martianzoo.testsupport.PLAYER1)
      player.runOperation("UseAction<StandardAction>?") {
        shouldThrow<TaskException> { player.declineSecondAction() }
        abort()
      }
    }
  }

  internal class Research : dev.martianzoo.tfm.tests.TfmGameplayTest() {
    @Test
    internal fun `Buying 0 research cards with NONE completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.NONE, 0)

    @Test
    internal fun `Buying 2 research cards with NONE completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.NONE, 2)

    @Test
    internal fun `Buying 4 research cards with NONE completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.NONE, 4)

    @Test
    internal fun `Buying 0 research cards with CONCRETE completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.CONCRETE, 0)

    @Test
    internal fun `Buying 2 research cards with CONCRETE completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.CONCRETE, 2)

    @Test
    internal fun `Buying 4 research cards with CONCRETE completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.CONCRETE, 4)

    @Test
    internal fun `Buying 0 research cards with EAGER completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.EAGER, 0)

    @Test
    internal fun `Buying 2 research cards with EAGER completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.EAGER, 2)

    @Test
    internal fun `Buying 4 research cards with EAGER completes payment and transfer`() =
        checkPurchase(AutoExecPolicy.EAGER, 4)

    private fun checkPurchase(policy: AutoExecPolicy, bought: Int) {
      newTestGame(addOptions = "-VenusNextExpansion", playerCount = 2)
      kim.pass()
      stan.pass()
      stan.buyCards(0)
      kim.autoExecPolicy = policy

      kim.buyCards(bought).expect("${-3 * bought} MC, $bought ProjectCard<Hand>")

      kim.count("ProjectCard<Selecting>") shouldBe 0
      kim.count("Owed") shouldBe 0
      kim.autoExecPolicy shouldBe policy
    }
  }
}
