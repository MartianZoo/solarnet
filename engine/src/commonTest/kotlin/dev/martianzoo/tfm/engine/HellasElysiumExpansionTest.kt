package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HellasElysiumExpansionTest : CardTest() {
  @Test
  fun `Specialist uses printed megacredit production`() {
    newGame(TestOption.ElysiumMapOption)
    p1.manual("PROD[9 Megacredit]")
    shouldThrow<RequirementException> { p1.manual("Specialist") }

    p1.manual("PROD[Megacredit], Specialist")
    p1.count("Specialist") shouldBe 1
  }
}
