package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class LandClaimTest : CardTest() {
  @Test
  internal fun `Places a claim marker on an empty land area`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()

    p1.manual("$LandClaim") { doTask("LandClaimMarker<Tharsis_1_1>") }

    shouldThrow<DeadEndException> { p2.manual("CityTile<Tharsis_1_1>") }
    p1.manual("GreeneryTile<Tharsis_1_1>")
    p1.assertCounts(1 to "LandClaimMarker<Tharsis_1_1>")
  }

  @Test
  internal fun `Artificial Lake respects a claim according to its owner`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()
    p1.manual("$LandClaim") { doTask("LandClaimMarker<Tharsis_1_3>") }

    shouldThrow<DeadEndException> {
      p2.manual("$ArtificialLake") { placeTile(1, 3) }
    }
    p1.manual("$ArtificialLake") { placeTile(1, 3) }
    p1.assertCounts(1 to "OceanTile<Tharsis_1_3>")
  }

  @Test
  internal fun `Another player's claim on the only adjacent area enables greenery fallback`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()
    p1.manual("GreeneryTile<Tharsis_1_1>")
    p2.manual("CityTile<Tharsis_2_1>")
    p2.manual("$LandClaim") { doTask("LandClaimMarker<Tharsis_2_2>") }

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
      p1.manual("$LandClaim") { doTask("LandClaimMarker<Tharsis_1_1>") }
    }
    p1.manual("$LandClaim") { doTask("LandClaimMarker<Tharsis_1_3>") }
    shouldThrow<NarrowingException> {
      p2.manual("$LandClaim") { doTask("LandClaimMarker<Tharsis_1_3>") }
    }
  }
}
