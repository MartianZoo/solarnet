package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.Factorum
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class FactorumTest : CardTest() {
  @Test
  fun `Can raise energy production when it has no energy`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$Factorum")

    p1.cardAction1(Factorum).expect("PROD[Energy]")
  }

  @Test
  fun `Cannot raise energy production while it has energy`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$Factorum, Energy")

    shouldThrow<RequirementException> { p1.cardAction1(Factorum) }
  }
}
