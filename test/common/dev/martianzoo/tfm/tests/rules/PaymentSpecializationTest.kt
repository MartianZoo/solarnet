package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.*
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PaymentSpecializationTest {
  @Test
  internal fun `Admin prepares a research bill while the player chooses the payment`() {
    val p1 = Agents(setUpGame()) { NONE }[PLAYER1]
    p1.autoExecPolicy = NONE
    p1.runOperation("20 MC, 4 ProjectCard<Selecting>")

    p1.beginOperation("BuySelectedCards")
    p1.count("Owed") shouldBe 12
    p1.count("ProjectCard<Selecting>") shouldBe 4
    p1.count("ProjectCard<Hand>") shouldBe 0
    p1.doTasks("12 Pay<> FROM MC")
    p1.count("ProjectCard<Hand>") shouldBe 0
    p1.doTask("4 ProjectCard<Hand FROM Selecting>")

    p1.count("MC") shouldBe 8
    p1.count("ProjectCard<Hand>") shouldBe 4
    p1.count("Owed") shouldBe 0
    p1.count("PaymentScope") shouldBe 0
    p1.autoExecPolicy shouldBe NONE
  }
}
