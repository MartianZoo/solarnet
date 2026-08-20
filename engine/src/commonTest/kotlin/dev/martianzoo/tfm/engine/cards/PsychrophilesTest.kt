package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

class PsychrophilesTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PreludeExpansion)
    engine.phase("Action")
    p1.manual("10, ProjectCard")
  }

  @Test
  fun `without microbes, plays a plant-tag card using Psychrophiles`() {
    p1.manual("$Psychrophiles")
    p1.playProject(AdaptedLichen, 9) { doTask("Ok") }.expect("PROD[Plant]")
  }

  @Test
  fun `with a microbe, declines to use Psychrophiles`() {
    p1.manual("$Psychrophiles, Microbe<$Psychrophiles>")

    p1.playProject(AdaptedLichen, 9) { doTask("Ok") }.expect("PROD[Plant]")
    p1.count("Microbe<$Psychrophiles>") shouldBe 1
  }

  @Test
  fun `with Psychrophiles, uses its action`() {
    p1.manual("$Psychrophiles")
    p1.cardAction1(Psychrophiles).expect("Microbe<$Psychrophiles>")
  }

  @Test
  fun `with a microbe, plays a plant-tag card using Psychrophiles`() {
    p1.manual("$Psychrophiles, Microbe<$Psychrophiles>")
    p1.playProject(AdaptedLichen, 7) {
          doTask("PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
        }
        .expect("-Microbe<$Psychrophiles>, PROD[Plant]")
  }

  @Test
  fun `with five microbes, plays a nine-cost card using Psychrophiles`() {
    p1.manual("$Psychrophiles, 5 Microbe<$Psychrophiles>")
    p1.playProject(AdaptedLichen, 0) {
          doTask("5 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
        }
        .expect("-5 Microbe<$Psychrophiles>, PROD[Plant]")
  }

  @Test
  fun `above its temperature limit, tries to play Psychrophiles`() {
    p1.manual("6 TemperatureStep")
    shouldThrow<RequirementException> { p1.playProject(Psychrophiles, 2) }
  }
}
