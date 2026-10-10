package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PaymentSpecializationTest {
  @Test
  internal fun `Admin prepares a research bill while the player chooses the payment`() {
    val p1 = setUpGame().testTfm(PLAYER1)
    p1.autoExecPolicy = NONE
    p1.runOperation("20 MC, 4 ProjectCard<Selecting>")

    p1.beginOperation("BuySelectedCards")
    p1.count("Owed") shouldBe 12
    p1.count("ProjectCard<Selecting>") shouldBe 0
    p1.count("ProjectCard<Hand>") shouldBe 0
    p1.doTasks("-12 MC")
    p1.count("ProjectCard<Hand>") shouldBe 0
    p1.doTasks("4 ProjectCard FROM BuyCard")

    p1.count("MC") shouldBe 8
    p1.count("ProjectCard<Hand>") shouldBe 4
    p1.count("Owed") shouldBe 0
    p1.count("Billing") shouldBe 0
    p1.autoExecPolicy shouldBe NONE
  }

  @Test
  internal fun `card play rejects a front from a different deck`() {
    val player = setUpGame().testTfm(PLAYER1)

    shouldThrow<ExpressionException> {
      player.beginOperation("PlayCard<Class<CorporationCard>, Class<$AcquiredCompany>, Hand>")
    }
  }

  @Test
  internal fun `Accepting pays only with its specialized resource`() {
    val p1 = setUpGame().testTfm(PLAYER1)
    p1.runOperation("Steel, Titanium")

    p1.beginOperation(
        "Owed<Class<Steel>> THEN Billing<Class<SellPatentsProject>, Action1, Class<Steel>>"
    ) {
      shouldThrow<NarrowingException> { doTask("-Titanium") }
      doTask("-Steel")
    }
  }
}
