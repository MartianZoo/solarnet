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
    p1.runOperation("4 MC, ProjectCard")

    shouldThrow<DeadEndException> { p1.playProject(PoliticalAlliance, 4) }
  }

  @Test
  internal fun `requires two party leaders rather than a chairman and one leader`() {
    newGame(TurmoilExpansion, PromoCardPack)
    admin.phase("Action")
    p1.runOperation("4 MC, ProjectCard")

    shouldThrow<RequirementException> { p1.playProject(PoliticalAlliance, 4) }

    admin.runOperation("ReserveDelegate<Neutral> FROM Chairman<Neutral>")
    p1.runOperation("Chairman FROM ReserveDelegate, PartyDelegate<Scientists> FROM ReserveDelegate")
    shouldThrow<RequirementException> { p1.playProject(PoliticalAlliance, 4) }

    p1.runOperation("PartyDelegate<Unity> FROM ReserveDelegate")
    p1.playProject(PoliticalAlliance, 4).expect("-4 MC, TerraformRating")
  }
}
