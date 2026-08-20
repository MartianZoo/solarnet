package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.Factorum
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class FactorumTest : CardTest() {
  @Test
  fun `without energy, raises energy production`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$Factorum")

    p1.cardAction1(Factorum).expect("PROD[Energy]")
  }

  @Test
  fun `with energy, cannot raise energy production`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$Factorum, Energy")

    shouldThrow<RequirementException> { p1.cardAction1(Factorum) }
  }
}
