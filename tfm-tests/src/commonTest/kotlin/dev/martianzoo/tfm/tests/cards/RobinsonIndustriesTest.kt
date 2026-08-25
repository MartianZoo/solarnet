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
  internal fun `Can raise uniquely lowest megacredit production`() {
    p1.manual("PROD[Steel, Titanium, Plant, Energy, Heat]")
    p1.cardAction1(RobinsonIndustries).expect("-4, PROD[Megacredit]")
  }

  @Test
  internal fun `Can raise megacredit production from below the production floor`() {
    p1.manual("PROD[-Megacredit]")
    p1.cardAction1(RobinsonIndustries).expect("-4, PROD[Megacredit]")
  }

  @Test
  internal fun `Can raise uniquely lowest titanium production`() {
    p1.manual("PROD[Megacredit, Steel, Plant, Energy, Heat]")
    p1.cardAction1(RobinsonIndustries).expect("-4, PROD[Titanium]")
  }

  @Test
  internal fun `Can choose megacredit production when tied for lowest`() {
    seedProductionTie()

    p1.cardAction1(RobinsonIndustries) { doTask("PROD[Megacredit]") }.expect("-4, PROD[Megacredit]")
  }

  @Test
  internal fun `Can choose titanium production when tied for lowest`() {
    seedProductionTie()
    p1.cardAction1(RobinsonIndustries) { doTask("PROD[Titanium]") }.expect("-4, PROD[Titanium]")
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
