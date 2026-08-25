package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.engine.*
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.AquiferPumping
import dev.martianzoo.tfm.tests.cards.cardnames.Mine
import dev.martianzoo.tfm.tests.cards.cardnames.PowerPlant
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TfmGameplayTest : CardTest() {
  @Test
  internal fun `Declining a second action rejects an unrelated optional task`() {
    newGame()

    p1.manual("UseAction<StandardAction>?") {
      shouldThrow<TaskException> { p1.declineSecondAction() }
      abort()
    }
  }

  @Test
  internal fun `Payment rejects leaving steel unspent at full value`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("10, 2 Steel, ProjectCard")

    shouldThrow<IllegalArgumentException> { p1.playProject(Mine, 4) }
    p1.count("Megacredit") shouldBe 10
    p1.count("Steel") shouldBe 2
    p1.count("ProjectCard") shouldBe 1
    p1.count("$Mine") shouldBe 0

    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("10, 2 Steel, ProjectCard")
    // Synthetic API test: no strategic reason; deliberate underpayment exercises the opt-in.
    p1.intentionalUnderpay()
    p1.playProject(Mine, 4)
  }

  @Test
  internal fun `Underpayment permission applies to only one payment`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("14, 2 Steel, 2 ProjectCard")

    // Synthetic API test: no strategic reason; deliberate underpayment exercises one-shot scope.
    p1.intentionalUnderpay()
    p1.playProject(Mine, 4)
    shouldThrow<IllegalArgumentException> { p1.playProject(PowerPlant, 4) }
  }

  @Test
  internal fun `Payment rejects steel that cannot receive full value`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("3 Steel, ProjectCard")

    shouldThrow<IllegalArgumentException> { p1.playProject(Mine, steel = 3) }

    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("3 Steel, ProjectCard")
    p1.intentionalOverpay()
    p1.playProject(Mine, steel = 3)
  }

  @Test
  internal fun `Payment rejects megacredits beyond the remainder after steel`() {
    newGame()
    engine.phase("Action")
    p1.manual("30 Megacredit, 5 Steel, ProjectCard")

    shouldThrow<LimitsException> {
      p1.playProject(AquiferPumping, megacredits = 18, steel = 5)
    }

    p1.count("Megacredit") shouldBe 30
    p1.count("Steel") shouldBe 5
    p1.count("ProjectCard") shouldBe 1
    p1.count("$AquiferPumping") shouldBe 0
  }

  @Test
  internal fun `Overpayment permission applies to only one payment`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("6 Steel, 2 ProjectCard")

    p1.intentionalOverpay()
    p1.playProject(Mine, steel = 3)
    shouldThrow<IllegalArgumentException> { p1.playProject(PowerPlant, steel = 3) }
  }
}
