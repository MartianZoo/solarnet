package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class RobinsonIndustriesTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PreludeExpansion)
    p1.playCorp(RobinsonIndustries, 0)
    engine.phase("Action")
  }

  @Test
  fun `with megacredit uniquely lowest, uses Robinson Industries`() {
    p1.manual("PROD[Steel, Titanium, Plant, Energy, Heat]")
    p1.cardAction1(RobinsonIndustries).expect("-4, PROD[Megacredit]")
  }

  @Test
  fun `with megacredit below the production floor, uses Robinson Industries`() {
    p1.manual("PROD[-Megacredit]")
    p1.cardAction1(RobinsonIndustries).expect("-4, PROD[Megacredit]")
  }

  @Test
  fun `with titanium uniquely lowest, uses Robinson Industries`() {
    p1.manual("PROD[Megacredit, Steel, Plant, Energy, Heat]")
    p1.cardAction1(RobinsonIndustries).expect("-4, PROD[Titanium]")
  }

  @Test
  fun `with megacredit and titanium tied, chooses megacredit using Robinson Industries`() {
    seedProductionTie()

    p1.cardAction1(RobinsonIndustries) {
          doTask("PROD[Megacredit]")
        }
        .expect("-4, PROD[Megacredit]")
  }

  @Test
  fun `with megacredit and titanium tied, chooses titanium using Robinson Industries`() {
    seedProductionTie()
    p1.cardAction1(RobinsonIndustries) { doTask("PROD[Titanium]") }.expect("-4, PROD[Titanium]")
  }

  @Test
  fun `with megacredit and titanium tied, rejects every higher production`() {
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
