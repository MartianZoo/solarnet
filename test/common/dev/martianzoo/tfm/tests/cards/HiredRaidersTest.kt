package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.HiredRaiders
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class HiredRaidersTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `An unavailable self-steal does not block stealing from an opponent`() {
    kim.setToExMachina(2, "Steel")
    kim.setToExMachina(1, "MC")
    stan.setToExMachina(3, "MC")

    kim.playProject(HiredRaiders, 1) { doTask("3 MC FROM MC<Stan>") }
        .expect("2 MC<Kim>, -3 MC<Stan>, 0 Steel<Kim>")
  }

  // Resolved FAQ: Hired Raiders may steal less than its maximum, but must steal at least one.
  @Test
  internal fun `Cannot decline when an opponent has resources`() {
    kim.setToExMachina(1, "MC")
    stan.setToExMachina(3, "MC")

    shouldThrow<NarrowingException> { kim.playProject(HiredRaiders, 1) { declineTask() } }
  }

  @Test
  internal fun `Can steal less than the offered maximum`() {
    kim.setToExMachina(2, "MC")
    stan.setToExMachina(2, "Steel")
    rob.setToExMachina(2, "Steel")

    kim.playProject(HiredRaiders, 1) {
          doTask("Steel FROM Steel<Rob>")
        }
        .expect("Steel<Kim>, -Steel<Rob>")

    stan.assertCounts(2 to "Steel")
  }
}
