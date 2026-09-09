package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.PoliticalAlliance
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PoliticalAllianceTest : CardTest() {
  @Test
  internal fun `requires the Turmoil expansion`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.manual("4 MC, ProjectCard")

    shouldThrow<DeadEndException> { p1.playProject(PoliticalAlliance, 4) }
  }

  @Test
  internal fun `requires two party leaders and raises terraform rating`() {
    newGame(TurmoilExpansion, PromoCardPack)
    admin.phase("Action")
    p1.manual("4 MC, ProjectCard")

    shouldThrow<RequirementException> { p1.playProject(PoliticalAlliance, 4) }

    p1.manual(
        "PartyDelegate<Scientists> FROM ReserveDelegate, " +
            "PartyDelegate<Unity> FROM ReserveDelegate"
    )
    p1.playProject(PoliticalAlliance, 4).expect("-4 MC, TerraformRating")
  }
}
