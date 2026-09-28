package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PotatoesTest : CardTest() {
  @Test
  internal fun `Plant loss does not qualify for Cutting Edge Technology's discount`() {
    newGame(PromoCardPack)
    p1.playCorp(CrediCor, 5)
    admin.phase("Action")
    p1.playProject(CuttingEdgeTechnology, 12)
    p1.runOperation("2 Plant")

    p1.playProject(Potatoes, 2).expect("-2 MC, -2 Plant, PROD[2 MC]")
    // A printed requirement still receives the discount.
    p1.playProject(DustSeals, 0).expect("0 MC")
  }

  @Test
  internal fun `Viral Enhancers can supply the second plant before Potatoes loses two`() {
    newGame(PromoCardPack, CorporateEraExpansion)
    p1.playCorp(CrediCor, 5)
    admin.phase("Action")
    p1.playProject(ViralEnhancers, 9).expect("Plant")

    p1.playProject(Potatoes, 2).expect("-Plant, PROD[2 MC]")
  }

  @Test
  internal fun `Plant loss does not count as a requirement for Tactician`() {
    newGame(GameConfig("PromoCardPack, Tactician, Landlord, Banker", "Player1", "Player2"))
    p1.playCorp(CrediCor, 5)
    admin.phase("Action")
    p1.runOperation("2 Plant")
    p1.playProject(Archaebacteria, 6)
    p1.playProject(DustSeals, 2)
    p1.playProject(SearchForLife, 3)
    p1.playProject(Potatoes, 2)

    shouldThrow<RequirementException> { p1.claimMilestone(cn("Tactician")) }
    p1.playProject(ColonizerTrainingCamp, 8)
    // The sole selected milestone resolves automatically once its requirement is met.
    p1.stdAction("ClaimMilestoneAction").expect("Tactician")
  }
}
