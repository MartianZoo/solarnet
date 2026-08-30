package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RobinsonIndustriesTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PreludeExpansion)
    p1.playCorp(RobinsonIndustries, 0)
    engine.phase("Action")
  }

  @Test
  internal fun `Can raise uniquely lowest mc production`() {
    p1.manual("PROD[Steel, Titanium, Plant, Energy, Heat]")
    p1.cardAction1(RobinsonIndustries).expect("-4 MC, PROD[1 MC]")
  }

  @Test
  internal fun `Can raise mc production from below the production floor`() {
    p1.manual("PROD[-1 MC]")
    p1.cardAction1(RobinsonIndustries).expect("-4 MC, PROD[1 MC]")
  }

  @Test
  internal fun `Can raise uniquely lowest titanium production`() {
    p1.manual("PROD[1 MC, Steel, Plant, Energy, Heat]")
    p1.cardAction1(RobinsonIndustries).expect("-4 MC, PROD[Titanium]")
  }

  @Test
  internal fun `Can choose mc production when tied for lowest`() {
    seedProductionTie()

    p1.cardAction1(RobinsonIndustries) {
          doTask("PROD[1 MC]")
        }
        .expect("-4 MC, PROD[1 MC]")
  }

  @Test
  internal fun `Can choose titanium production when tied for lowest`() {
    seedProductionTie()
    p1.cardAction1(RobinsonIndustries) { doTask("PROD[Titanium]") }.expect("-4 MC, PROD[Titanium]")
  }

  @Test
  internal fun `Cannot choose a production that is higher than the minimum`() {
    seedProductionTie()

    listOf("Steel", "Plant", "Energy", "Heat").forEach { resource ->
      shouldThrow<NarrowingException> {
        p1.cardAction1(RobinsonIndustries) { doTask("PROD[$resource]") }
      }
    }
  }

  private fun seedProductionTie() {
    p1.manual("PROD[Steel, Plant, Energy, Heat]")
  }
}
