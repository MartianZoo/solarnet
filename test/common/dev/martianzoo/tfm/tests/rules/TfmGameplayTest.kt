package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.AquiferPumping
import dev.martianzoo.tfm.tests.cards.cardnames.DevelopmentCenter
import dev.martianzoo.tfm.tests.cards.cardnames.Mine
import dev.martianzoo.tfm.tests.cards.cardnames.PowerPlant
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val oneToOnePaymentDeclarations =
    parseClasses(
        """
        CLASS OneToOnePaymentSource : Owned {
          This:: BaseResourceValue<Class<Heat>>
          Billing<HasActions, ActionSlot, Class<MC>> IF Owed<Class<MC>>:: Accepting<Class<Heat>>
        }
        """
            .trimIndent()
    )

internal class TfmGameplayTest :
    CardTest(additionalClassDeclarations = oneToOnePaymentDeclarations.toSet()) {
  @Test
  internal fun `No-argument pass asserts there are no unused action cards`() {
    newGame()
    p1.requireExplicitUnusedActionCards()
    engine.phase("Action")

    p1.pass()

    newGame()
    p1.requireExplicitUnusedActionCards()
    engine.phase("Action")
    p1.manual("AquiferPumping")

    shouldThrow<IllegalArgumentException> { p1.pass() }
    p1.pass(unused = AquiferPumping)
  }

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
    p1.manual("10 MC, 2 Steel, ProjectCard")

    shouldThrow<IllegalArgumentException> { p1.playProject(Mine, 4) }
    p1.count("MC") shouldBe 10
    p1.count("Steel") shouldBe 2
    p1.count("ProjectCard") shouldBe 1
    p1.count("$Mine") shouldBe 0

    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("10 MC, 2 Steel, ProjectCard")
    // Synthetic API test: no strategic reason; deliberate underpayment exercises the opt-in.
    p1.intentionalUnderpay()
    p1.playProject(Mine, 4)
  }

  @Test
  internal fun `Payment may preserve an accepted one-to-one resource without an opt-in`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("10 MC, 2 Heat, OneToOnePaymentSource, ProjectCard")

    p1.playProject(Mine, 4)

    p1.count("MC") shouldBe 6
    p1.count("Heat") shouldBe 2
  }

  @Test
  internal fun `Payment requires an opt-in to spend a one-to-one resource before money`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("10 MC, 2 Heat, OneToOnePaymentSource, ProjectCard")

    shouldThrow<IllegalArgumentException> {
      p1.turn { playProject(Mine, 2, heat = 2) }
    }
    p1.intentionalOneToOneResourcePayment()
    p1.turn { playProject(Mine, 2, heat = 2) }

    p1.count("MC") shouldBe 8
    p1.count("Heat") shouldBe 0
  }

  @Test
  internal fun `Required one-to-one resource is not audited as an alternative to money`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("10 MC, Energy, DevelopmentCenter")

    p1.cardAction1(DevelopmentCenter)

    p1.count("MC") shouldBe 10
    p1.count("Energy") shouldBe 0
    p1.count("ProjectCard") shouldBe 1
  }

  @Test
  internal fun `Underpayment permission applies to only one payment`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("14 MC, 2 Steel, 2 ProjectCard")

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
    p1.intentionalOverpay(1)
    shouldThrow<IllegalArgumentException> { p1.playProject(Mine, steel = 3) }

    newGame()
    p1.requireExplicitPaymentChoices()
    engine.phase("Action")
    p1.manual("3 Steel, ProjectCard")
    p1.intentionalOverpay(2)
    p1.playProject(Mine, steel = 3)
  }

  @Test
  internal fun `Payment rejects mc beyond the remainder after steel`() {
    newGame()
    engine.phase("Action")
    p1.manual("30 MC, 5 Steel, ProjectCard")

    shouldThrow<LimitsException> {
      p1.playProject(AquiferPumping, mc = 18, steel = 5)
    }

    p1.count("MC") shouldBe 30
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

    p1.intentionalOverpay(2)
    p1.playProject(Mine, steel = 3)
    shouldThrow<IllegalArgumentException> { p1.playProject(PowerPlant, steel = 3) }
  }
}
