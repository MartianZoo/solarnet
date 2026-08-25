package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.Factorum
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class FactorumTest : CardTest() {
  @Test
  internal fun `Can raise energy production when it has no energy`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$Factorum")

    p1.cardAction1(Factorum).expect("PROD[Energy]")
  }

  @Test
  internal fun `Cannot raise energy production while it has energy`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$Factorum, Energy")

    shouldThrow<RequirementException> { p1.cardAction1(Factorum) }
  }
}
