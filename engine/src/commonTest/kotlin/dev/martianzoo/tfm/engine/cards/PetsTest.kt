package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class PetsTest : CardTest() {
  init {
    newGame(Canon.SIMPLE_GAME)
  }

  val p1 = game.tfm(PLAYER1)
  val p2 = game.tfm(PLAYER2)

  @BeforeTest
  fun setUp() {
    p1.sneak("100, 5 ProjectCard")
    engine.phase("Action")
  }

  @Test
  fun cantPredate() {
    p1.playProject("Pets", 10).expect("Animal")

    p2.sneak("Predators") // skip requirement

    // There's no stealable animal
    shouldThrow<DeadEndException> { p2.cardAction1("Predators") }

    p2.sneak("Animal<Predators>")

    // Now there is but it can only eat itself
    p2.cardAction1("Predators") {
      shouldThrow<DeadEndException> { doTask("-Animal<P1, Pets<P1>>") }
      doTask("-Animal<Predators>")
    }
  }

  @Test
  fun cantRemoveSelf() {
    p1.playProject("Pets", 10).expect("Animal")

    // Removing the card would mean having to remove the animals on it first -- can't!
    shouldThrow<DeadEndException> { p1.manual("-Pets") }
  }
}
