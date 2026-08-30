package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HellasElysiumExpansionTest : CardTest() {
  @Test
  internal fun `Specialist uses printed mc production`() {
    newGame(TestOption.Elysium)
    p1.manual("PROD[9 MC]")
    shouldThrow<RequirementException> { p1.manual("Specialist") }

    p1.manual("PROD[1 MC], Specialist")
    p1.count("Specialist") shouldBe 1
  }
}
