package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class LandshaperTest : CardTest() {
  // BGG exact question (no written designer answer):
  // https://boardgamegeek.com/thread/3512556/article/46088087#46088087
  @Test
  internal fun `Capital and one greenery do not satisfy Landshaper`() {
    setupLandshaperCapital()
    p1.stdProject("GreeneryProject") { placeTile(5, 2) }

    shouldThrow<RequirementException> { p1.claimMilestone(cn("Landshaper")) }
    p1.count("OwnedTile") shouldBe 2
    p1.count("Landshaper") shouldBe 0
  }

  @Test
  internal fun `Capital and two greeneries do not satisfy Landshaper`() {
    setupLandshaperCapital()
    p1.runOperation("20 MC")
    p1.stdProject("GreeneryProject") { placeTile(5, 2) }
    p1.stdProject("GreeneryProject") { placeTile(6, 1) }

    shouldThrow<RequirementException> { p1.claimMilestone(cn("Landshaper")) }
    p1.count("OwnedTile") shouldBe 3
    p1.count("Landshaper") shouldBe 0
  }

  @Test
  internal fun `Capital and distinct city and greenery satisfy Landshaper`() {
    setupLandshaperCapital()
    p1.runOperation("25 MC")
    p1.stdProject("GreeneryProject") { placeTile(5, 2) }
    p1.stdProject("CityProject") { placeTile(1, 1) }

    p1.claimMilestone(cn("Landshaper")).expect("Landshaper")
  }

  private fun setupLandshaperCapital() {
    newGame(Amazonis, PreludeExpansion, CorporateEraExpansion)
    p1.playCorp(CrediCor, 1)
    requireP2().runOperation("72 MC")
    admin.phase("Prelude")
    p1.playPrelude(PowerGeneration)
    p1.playPrelude(Donation)
    admin.phase("Action")
    requireP2().stdProject("AquiferProject") { placeTile(2, 1) }
    requireP2().stdProject("AquiferProject") { placeTile(2, 6) }
    requireP2().stdProject("AquiferProject") { placeTile(3, 1) }
    requireP2().stdProject("AquiferProject") { placeTile(3, 6) }
    p1.playProject(Capital, 26) { placeTile(5, 1) }
    shouldThrow<RequirementException> { p1.claimMilestone(cn("Landshaper")) }
  }
}
