package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.HiTechLab
import dev.martianzoo.tfm.tests.cards.cardnames.PowerInfrastructure
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class VariableAmountActionsTest : CardTest() {
  @Test
  internal fun `Power Infrastructure cannot choose zero energy`() {
    newGame(CorporateEraExpansion)
    engine.phase("Action")
    p1.manual("$PowerInfrastructure")

    shouldThrow<AbstractException> { p1.cardAction1(PowerInfrastructure) }
  }

  @Test
  internal fun `Hi-Tech Lab cannot choose zero energy`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("$HiTechLab")

    shouldThrow<AbstractException> { p1.cardAction1(HiTechLab) }
  }

  @Test
  internal fun `Sell Patents cannot choose zero cards`() {
    newGame()
    engine.phase("Action")

    shouldThrow<PetSyntaxException> { p1.sellPatents(0) }
  }
}
