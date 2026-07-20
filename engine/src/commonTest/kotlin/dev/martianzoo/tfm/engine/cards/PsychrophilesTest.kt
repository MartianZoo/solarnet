package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class PsychrophilesTest : CardTest() {
  init {
    newGame("BRMP", 2)
  }

  val Psychrophiles = "Psychrophiles"

  @BeforeTest
  fun setUp() {
    with(player1) {
      phase("Action")
      sneak("100, 5 ProjectCard")
    }
  }

  fun setupWithCard() {
    with(player1) {
      playProject(Psychrophiles, 2)
      cardAction1(Psychrophiles).expect("Microbe<Psychrophiles>")
      sneak("4 Microbe<Psychrophiles>").expect("4 Microbe<Psychrophiles>")
    }
  }

  @Test
  fun spendNone() {
    setupWithCard()
    with(player1) {
      playProject("AdaptedLichen", 9) { doTask("Ok") }.expect("AdaptedLichen")
    }
  }

  @Test
  fun spendOne() {
    setupWithCard()
    with(player1) {
      playProject("AdaptedLichen", 7) { doTask("-Microbe<Psychrophiles>! THEN -2 Owed.") }
          .expect("-Microbe<Psychrophiles>, AdaptedLichen")
    }
  }

  @Test
  fun overspend() {
    setupWithCard()
    with(player1) {
      playProject("AdaptedLichen", 0) {
            doTask("-5 Microbe<Psychrophiles>! THEN -10 Owed.")
          }
          .expect("-5 Microbe<Psychrophiles>, AdaptedLichen")
    }
  }

  @Test
  fun tooWarm() {
    player1.sneak("6 TemperatureStep")
    shouldThrow<RequirementException> { player1.playProject(Psychrophiles, 2) }
  }
}
