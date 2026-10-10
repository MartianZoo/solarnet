package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.LakefrontResorts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class HellasMapTest : TfmSandboxTest() {
  @Test
  internal fun `An unaffordable south pole remains a structurally available adjacent greenery area`() {
    newTestGame(addOptions = "HellasMap")
    kim.exMachina("GreeneryTile<Hellas_9_6>")
    stan.exMachina("GreeneryTile<Hellas_8_6>, GreeneryTile<Hellas_8_5>, GreeneryTile<Hellas_9_5>")
    kim.exMachina("8 Plant")
    kim.setToExMachina(0, "MC")

    kim.stdAction("ConvertPlants") {
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Hellas_1_5>") }
      abort()
    }
  }

  @Test
  internal fun `Ocean income from the south pole bonus can fund its payment`() {
    newTestGame(addOptions = "HellasMap", kimCorporation = LakefrontResorts)
    kim.exMachina("OceanTile<Hellas_4_7>, OceanTile<Hellas_5_6>")
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(8, "Plant")

    kim.stdAction("ConvertPlants") {
          doTask("GreeneryTile<Hellas_9_7>")
          placeTile(5, 7)
        }
        .expect("0 MC, OceanTile<Hellas_5_7>")
  }
}
