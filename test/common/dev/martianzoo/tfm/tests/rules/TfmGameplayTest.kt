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
import dev.martianzoo.tfm.tests.cards.cardnames.TitaniumMine
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
    admin.phase("Action")

    p1.pass()

    newGame()
    p1.requireExplicitUnusedActionCards()
    admin.phase("Action")
    p1.runOperation("AquiferPumping")

    shouldThrow<IllegalArgumentException> { p1.pass() }
    p1.pass(unused = AquiferPumping)
  }

  @Test
  internal fun `Declining a second action rejects an unrelated optional task`() {
    newGame()

    p1.runOperation("UseAction<StandardAction>?") {
      shouldThrow<TaskException> { p1.declineSecondAction() }
      abort()
    }
  }

  @Test
  internal fun `Payment rejects leaving steel unspent at full value`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    admin.phase("Action")
    p1.runOperation("10 MC, 2 Steel, ProjectCard")

    shouldThrow<IllegalArgumentException> { p1.playProject(Mine, 4) }
    p1.count("MC") shouldBe 10
    p1.count("Steel") shouldBe 2
    p1.count("ProjectCard") shouldBe 1
    p1.count("$Mine") shouldBe 0

    newGame()
    p1.requireExplicitPaymentChoices()
    admin.phase("Action")
    p1.runOperation("10 MC, 2 Steel, ProjectCard")
    // Synthetic API test: no strategic reason; deliberate underpayment exercises the opt-in.
    p1.intentionalUnderpay()
    p1.playProject(Mine, 4)
  }

  @Test
  internal fun `Payment may preserve an accepted one-to-one resource without an opt-in`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    admin.phase("Action")
    p1.runOperation("10 MC, 2 Heat, OneToOnePaymentSource, ProjectCard")

    p1.playProject(Mine, 4)

    p1.count("MC") shouldBe 6
    p1.count("Heat") shouldBe 2
  }

  @Test
  internal fun `Payment requires an opt-in to spend a one-to-one resource before money`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    admin.phase("Action")
    p1.runOperation("10 MC, 2 Heat, OneToOnePaymentSource, ProjectCard")

    shouldThrow<IllegalArgumentException> {
      p1.turn { playProject(Mine, 2, heat = 2) }
    }
    p1.intentionalUnderpay()
    p1.turn { playProject(Mine, 2, heat = 2) }

    p1.count("MC") shouldBe 8
    p1.count("Heat") shouldBe 0
  }

  @Test
  internal fun `Required one-to-one resource is not audited as an alternative to money`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    admin.phase("Action")
    p1.runOperation("10 MC, Energy, DevelopmentCenter")

    p1.cardAction1(DevelopmentCenter)

    p1.count("MC") shouldBe 10
    p1.count("Energy") shouldBe 0
    p1.count("ProjectCard") shouldBe 1
  }

  @Test
  internal fun `Underpayment permission applies to only one payment`() {
    newGame()
    p1.requireExplicitPaymentChoices()
    admin.phase("Action")
    p1.runOperation("14 MC, 2 Steel, 2 ProjectCard")

    // Synthetic API test: no strategic reason; deliberate underpayment exercises one-shot scope.
    p1.intentionalUnderpay()
    p1.playProject(Mine, 4)
    shouldThrow<IllegalArgumentException> { p1.playProject(PowerPlant, 4) }
  }

  @Test
  internal fun `Payment rejects a tender containing a unit that could be kept`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("3 Steel, ProjectCard")

    // Mine costs 4; two steel already settle it, so the third is returnable.
    shouldThrow<LimitsException> { p1.playProject(Mine, steel = 3) }

    p1.count("Steel") shouldBe 3
    p1.count("$Mine") shouldBe 0
  }

  @Test
  internal fun `Payment allows excess no single unit could have avoided`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("4 Steel, ProjectCard")

    // Titanium Mine costs 7; three steel are not enough, so the fourth may waste one M€.
    p1.playProject(TitaniumMine, steel = 4)

    p1.count("Steel") shouldBe 0
    p1.count("$TitaniumMine") shouldBe 1
  }

  @Test
  internal fun `Payment rejects mc beyond the remainder after steel`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("30 MC, 5 Steel, ProjectCard")

    shouldThrow<LimitsException> {
      p1.playProject(AquiferPumping, mc = 18, steel = 5)
    }

    p1.count("MC") shouldBe 30
    p1.count("Steel") shouldBe 5
    p1.count("ProjectCard") shouldBe 1
    p1.count("$AquiferPumping") shouldBe 0
  }
}
