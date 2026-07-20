package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class PsychrophilesTest : CardTest() {
  val tfmGame = newGame("BRMP", 2)
  val p1 = tfmGame.tfm(PLAYER1)
  val Psychrophiles = "Psychrophiles"

  @BeforeTest
  fun setUp() {
    with(p1) {
      phase("Corporation")
      playCorp("Ecoline", 5)
      phase("Action")
    }
  }

  fun setupWithCard() {
    with(p1) {
      playProject(Psychrophiles, 2)
      cardAction1(Psychrophiles).expect("Microbe<Psychrophiles>")
      godMode().sneak("4 Microbe<Psychrophiles>").expect("4 Microbe<Psychrophiles>")
    }
  }

  @Test
  fun spendNone() {
    setupWithCard()
    with(p1) {
      playProject("AdaptedLichen", 9) { doTask("Ok") }.expect("AdaptedLichen")
    }
  }

  @Test
  fun spendOne() {
    setupWithCard()
    with(p1) {
      playProject("AdaptedLichen", 7) { doTask("-Microbe<Psychrophiles>! THEN -2 Owed.") }
          .expect("-Microbe<Psychrophiles>, AdaptedLichen")
    }
  }

  @Test
  fun overspend() {
    setupWithCard()
    with(p1) {
      playProject("AdaptedLichen", 0) {
            doTask("-5 Microbe<Psychrophiles>! THEN -10 Owed.")
          }
          .expect("-5 Microbe<Psychrophiles>, AdaptedLichen")
    }
  }

  @Test
  fun tooWarm() {
    p1.godMode().manual("6 TemperatureStep")
    shouldThrow<RequirementException> { p1.playProject(Psychrophiles, 2) }
  }
}
