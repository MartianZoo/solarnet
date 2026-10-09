package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PsychrophilesTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Psychrophiles")

  @Test
  internal fun `Can decline to spend a microbe on a plant-tag card`() {
    kim.exMachina("$Psychrophiles, Microbe<$Psychrophiles>")
    kim.setToExMachina(9, "MC")

    kim.playProject(AdaptedLichen, 9) { /* Decline spending a Psychrophiles microbe. */
          declineTask()
        }
        .expect("PROD[Plant]")
    kim.count("Microbe<$Psychrophiles>") shouldBe 1
  }

  @Test
  internal fun `Can spend five microbes toward a nine-cost card`() {
    kim.exMachina("$Psychrophiles, 5 Microbe<$Psychrophiles>")
    kim.setToExMachina(0, "MC")

    kim.playProject(AdaptedLichen, 0) {
          doTask("-5 Microbe<$Psychrophiles>")
        }
        .expect("-5 Microbe<$Psychrophiles>, PROD[Plant]")
  }
}
