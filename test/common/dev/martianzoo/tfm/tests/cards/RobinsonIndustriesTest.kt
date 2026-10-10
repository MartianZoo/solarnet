package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RobinsonIndustriesTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(kimCorporation = RobinsonIndustries)
    listOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat").forEach {
      kim.setToExMachina(0, "PROD[$it]")
    }
  }

  @Test
  internal fun `Can raise MC production from a negative level`() {
    kim.exMachina("PROD[-1 MC]")
    kim.cardAction1(RobinsonIndustries).expect("PROD[1 MC]")
  }

  @Test
  internal fun `Can raise uniquely lowest titanium production`() {
    kim.exMachina("PROD[1 MC, Steel, Plant, Energy, Heat]")
    kim.cardAction1(RobinsonIndustries).expect("PROD[Titanium]")
  }

  @Test
  internal fun `Can choose titanium production when tied for lowest`() {
    seedProductionTie()
    kim.cardAction1(RobinsonIndustries) { doTask("PROD[Titanium]") }.expect("PROD[Titanium]")
  }

  @Test
  internal fun `Cannot choose a production that is higher than the minimum`() {
    seedProductionTie()

    shouldThrow<NarrowingException> {
      kim.cardAction1(RobinsonIndustries) { doTask("PROD[Steel]") }
    }
  }

  private fun seedProductionTie() {
    kim.exMachina("PROD[Steel, Plant, Energy, Heat]")
  }
}
