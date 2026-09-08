package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class LandClaimTest : CardTest() {
  @Test
  internal fun `Reserves an empty land area until its owner places a tile`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()

    p1.manual("$LandClaim") { doTask("Community<Tharsis_1_1>") }

    shouldThrow<DeadEndException> { p2.manual("CityTile<Tharsis_1_1>") }
    p1.manual("GreeneryTile<Tharsis_1_1>")
    p1.assertCounts(0 to "Community<Tharsis_1_1>")
  }

  @Test
  internal fun `Artificial Lake respects a claim according to its owner`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()
    p1.manual("$LandClaim") { doTask("Community<Tharsis_1_3>") }

    shouldThrow<DeadEndException> { p2.manual("$ArtificialLake") { placeTile(1, 3) } }
    p1.manual("$ArtificialLake") { placeTile(1, 3) }
    p1.assertCounts(1 to "OceanTile<Tharsis_1_3>")
  }

  @Test
  internal fun `Placing an unrelated tile does not remove a community`() {
    newGame(CorporateEraExpansion)
    p1.manual("$LandClaim") { doTask("Community<Tharsis_1_3>") }

    p1.manual("CityTile<Tharsis_4_2>")

    p1.assertCounts(1 to "Community<Tharsis_1_3>")
  }

  @Test
  internal fun `A community does not establish greenery placement adjacency`() {
    newGame(CorporateEraExpansion)
    p1.manual("$LandClaim") { doTask("Community<Tharsis_4_2>") }
    p1.manual("CityTile<Tharsis_1_1>")

    shouldThrow<NarrowingException> {
      p1.manual("GreeneryTile<>") { doTask("GreeneryTile<Tharsis_4_3>") }
    }
    p1.manual("GreeneryTile<>") { doTask("GreeneryTile<Tharsis_2_1>") }

    p1.assertCounts(1 to "Community<Tharsis_4_2>", 1 to "GreeneryTile<Tharsis_2_1>")
  }

  @Test
  internal fun `Another player's claim on the only adjacent area enables greenery fallback`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()
    p1.manual("GreeneryTile<Tharsis_1_1>")
    p2.manual("CityTile<Tharsis_2_1>")
    p2.manual("$LandClaim") { doTask("Community<Tharsis_2_2>") }

    shouldThrow<DeadEndException> { p1.manual("GreeneryTile<Tharsis_2_2>") }
    p1.manual("GreeneryTile<Tharsis_9_7>")
    p1.assertCounts(1 to "GreeneryTile<Tharsis_9_7>")
  }

  @Test
  internal fun `Cannot claim an occupied or reserved area`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()
    p1.manual("GreeneryTile<Tharsis_1_1>")

    shouldThrow<NarrowingException> {
      p1.manual("$LandClaim") { doTask("Community<Tharsis_1_1>") }
    }
    p1.manual("$LandClaim") { doTask("Community<Tharsis_1_3>") }
    shouldThrow<NarrowingException> {
      p2.manual("$LandClaim") { doTask("Community<Tharsis_1_3>") }
    }
  }

  @Test
  internal fun `A land area intrinsically allows only one community`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()
    p1.manual("Community<Tharsis_1_3>")

    shouldThrow<LimitsException> { p2.manual("Community<Tharsis_1_3>") }

    p1.assertCounts(1 to "Community<Tharsis_1_3>")
    p2.assertCounts(0 to "Community<Tharsis_1_3>")
  }
}
