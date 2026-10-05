package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.HiTechLab
import dev.martianzoo.tfm.tests.cards.cardnames.PowerInfrastructure
import dev.martianzoo.tfm.tests.cards.cardnames.TychoMagnetics
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class VariableAmountActionsTest : CardTest() {
  @Test
  internal fun `Power Infrastructure cannot choose zero energy`() {
    newGame(CorporateEraExpansion)
    admin.phase("Action")
    p1.runOperation("$PowerInfrastructure")

    shouldThrow<NotFullySpecifiedException> { p1.cardAction1(PowerInfrastructure) }
  }

  @Test
  internal fun `Hi-Tech Lab cannot choose zero energy`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("$HiTechLab")

    shouldThrow<NotFullySpecifiedException> { p1.cardAction1(HiTechLab) }
  }

  @Test
  internal fun `Hi-Tech Lab discards every card it did not keep`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("$HiTechLab, 3 Energy")

    p1.cardAction1(HiTechLab, x = 3).expect("-3 Energy, ProjectCard")
    p1.assertCounts(0 to "ProjectCard<Selecting>", 1 to "ProjectCard")
  }

  @Test
  internal fun `Tycho Magnetics discards every card it did not keep`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("$TychoMagnetics, 4 Energy")

    p1.cardAction1(TychoMagnetics, x = 4).expect("-4 Energy, ProjectCard")
    p1.assertCounts(0 to "ProjectCard<Selecting>", 1 to "ProjectCard")
  }

  @Test
  internal fun `Sell Patents cannot choose zero cards`() {
    newGame()
    admin.phase("Action")

    shouldThrow<PetSyntaxException> { p1.sellPatents(0) }
  }
}
