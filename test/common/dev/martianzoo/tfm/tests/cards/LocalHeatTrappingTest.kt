package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class LocalHeatTrappingTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can choose unavailable animals instead of plants`() {
    kim.setToExMachina(5, "Heat")

    kim.playProject(LocalHeatTrapping, 1) {
          // Decline gaining plants by choosing animals when no animal holder exists.
          declineTask()
        }
        .expect("-5 Heat, 0 Plant")
  }

  @Test
  internal fun `Cannot decline animals when an eligible holder exists`() {
    kim.setToExMachina(5, "Heat")
    kim.exMachina("$Pets")

    shouldThrow<NarrowingException> {
      kim.playProject(LocalHeatTrapping, 1) { declineTask() }
    }
  }
}
