package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PsychrophilesTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PreludeExpansion)
    engine.phase("Action")
    p1.manual("10, ProjectCard")
  }

  @Test
  internal fun `Can play a plant-tag card without spending microbes`() {
    p1.manual("$Psychrophiles")
    p1.playProject(AdaptedLichen, 9) { /* Decline spending a Psychrophiles microbe. */
          declineTask()
        }
        .expect("PROD[Plant]")
  }

  @Test
  internal fun `Can decline to spend a microbe on a plant-tag card`() {
    p1.manual("$Psychrophiles, Microbe<$Psychrophiles>")

    p1.playProject(AdaptedLichen, 9) { /* Decline spending a Psychrophiles microbe. */
          declineTask()
        }
        .expect("PROD[Plant]")
    p1.count("Microbe<$Psychrophiles>") shouldBe 1
  }

  @Test
  internal fun `Can add a microbe with its action`() {
    p1.manual("$Psychrophiles")
    p1.cardAction1(Psychrophiles).expect("Microbe<$Psychrophiles>")
  }

  @Test
  internal fun `Can spend a microbe toward a plant-tag card`() {
    p1.manual("$Psychrophiles, Microbe<$Psychrophiles>")
    p1.playProject(AdaptedLichen, 7) {
          doTask("PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
        }
        .expect("-Microbe<$Psychrophiles>, PROD[Plant]")
  }

  @Test
  internal fun `Can spend five microbes toward a nine-cost card`() {
    p1.manual("$Psychrophiles, 5 Microbe<$Psychrophiles>")
    p1.playProject(AdaptedLichen, 0) {
          doTask("5 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
        }
        .expect("-5 Microbe<$Psychrophiles>, PROD[Plant]")
  }

  @Test
  internal fun `Cannot be played above its temperature limit`() {
    p1.manual("6 TemperatureStep")
    shouldThrow<RequirementException> { p1.playProject(Psychrophiles, 2) }
  }
}
