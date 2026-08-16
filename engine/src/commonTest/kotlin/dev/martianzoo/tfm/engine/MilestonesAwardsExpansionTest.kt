package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestonesAwardsExpansionTest : CardTest() {
  @Test
  fun geologistUsesDeclarativeTileAndNeighborRelationships() {
    newGame(TestOption.MilestonesAwardsExpansion)
    p1.manual("Tile142<Tharsis_2_2>, GreeneryTile<Tharsis_2_1>")

    shouldThrow<RequirementException> { p1.manual("Geologist") }

    p1.manual("Tile044<Tharsis_2_3>")
    p1.manual("Geologist")
    p1.count("Geologist") shouldBe 1
  }

  @Test
  fun landscaperCountsOnlyTheLargestContiguousGroupOwnedByThePlayer() {
    newGame(TestOption.MilestonesAwardsExpansion)
    val p2 = requireP2()
    p1.manual(
        "Tile142<Tharsis_2_2>, GreeneryTile<Tharsis_2_1>, Tile044<Tharsis_2_3>, " +
            "CityTile<Tharsis_4_6>"
    )
    p2.manual("CityTile<Tharsis_8_7>, GreeneryTile<Tharsis_8_6>")

    p1.count("OwnedTile") shouldBe 4
    p2.count("OwnedTile") shouldBe 2
    p1.count("OwnedTile<Tharsis_2_2>") shouldBe 1
    p1.count("TileInLargestGroup") shouldBe 3
    p2.count("TileInLargestGroup") shouldBe 2

    p1.manual("8 M")
    engine.phase("Action")
    p1.stdAction("ClaimMilestoneSA") { doTask("Landshaper") }

    p1.count("Landshaper") shouldBe 1
    p1.count("OwnedTile") shouldBe 4
  }

  @Test
  fun merchantChecksResourcesAfterTheNormalClaimCost() {
    newGame(TestOption.MilestonesAwardsExpansion)
    p1.manual("10 M, 2 S, 2 T, 2 P, 2 E, 2 H")
    engine.phase("Action")

    p1.stdAction("ClaimMilestoneSA") { doTask("Merchant") }

    p1.count("Merchant") shouldBe 1
  }
}
