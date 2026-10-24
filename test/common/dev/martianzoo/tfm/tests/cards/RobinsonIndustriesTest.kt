package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RobinsonIndustriesTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(addOptions = "RobinsonIndustries", kimCorporation = RobinsonIndustries)
    kim.setToExMachina(0, "PROD[MC]")
    kim.setToExMachina(0, "PROD[Steel]")
    kim.setToExMachina(0, "PROD[Titanium]")
    kim.setToExMachina(0, "PROD[Plant]")
    kim.setToExMachina(0, "PROD[Energy]")
    kim.setToExMachina(0, "PROD[Heat]")
  }

  @Test
  internal fun `Can raise MC production from a negative level`() {
    kim.exMachina("PROD[-1 MC]")
    kim.inTurn {
          doTasks(*cardActionTasks("RobinsonIndustries"), "4 Pay<> FROM MC")
          doTask("PROD[MC]")
        }
        .expect("PROD[1 MC]")
  }

  @Test
  internal fun `Can raise uniquely lowest titanium production`() {
    kim.exMachina("PROD[1 MC, Steel, Plant, Energy, Heat]")
    kim.inTurn {
      doTasks(*cardActionTasks("RobinsonIndustries"), "4 Pay<> FROM MC")
      doTask("PROD[Titanium]")
    }
  }

  @Test
  internal fun `Can choose titanium production when tied for lowest`() {
    seedProductionTie()
    kim.inTurn {
      doTasks(
          *cardActionTasks("RobinsonIndustries"),
          "4 Pay<> FROM MC",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Cannot choose a production that is higher than the minimum`() {
    seedProductionTie()

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(*cardActionTasks("RobinsonIndustries"))
        doTask("4 Pay<> FROM MC")
        doTask("PROD[Steel]")
      }
    }
  }

  private fun seedProductionTie() {
    kim.exMachina("PROD[Steel, Plant, Energy, Heat]")
  }
}
