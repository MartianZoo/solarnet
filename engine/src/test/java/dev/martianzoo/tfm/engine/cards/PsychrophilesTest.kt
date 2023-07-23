package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PsychrophilesTest {
  val game = Engine.newGame(GameSetup(Canon, "BRMP", 2))
  val p1 = game.tfm(PLAYER1)
  val Psychrophiles = "Psychrophiles"

  @BeforeEach
  fun setUp() {
    with(p1) {
      phase("Corporation")
      playCorp("Ecoline", 5)

      phase("Action")
      playProject(Psychrophiles, 2)
      cardAction1(Psychrophiles)

      godMode().sneak("4 Microbe<Psychrophiles>")

      assertCounts(5 to "Microbe", 0 to "AdaptedLichen")
    }
  }

  @Test
  fun spendNone() {
    with(p1) {
      playProject("AdaptedLichen", 9) { doTask("Ok") }
      assertCounts(5 to "Microbe", 1 to "AdaptedLichen")
    }
  }

  @Test
  fun spendOne() {
    with(p1) {
      playProject("AdaptedLichen", 7) { doTask("PayPsychrophile FROM Microbe<Psychrophiles>") }
      assertCounts(4 to "Microbe", 1 to "AdaptedLichen")
    }
  }

  @Test
  fun overspend() {
    with(p1) {
      playProject("AdaptedLichen", 0) { doTask("5 PayPsychrophile FROM Microbe<Psychrophiles>") }
      assertCounts(0 to "Microbe", 1 to "AdaptedLichen")
    }
  }
}
