package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class PredatorsTest : CardTest() {
  init {
    newGame(Canon.SIMPLE_GAME)
  }

  val p1 = game.tfm(PLAYER1)
  val p2 = game.tfm(PLAYER2)

  @BeforeTest
  fun setUp() {
    p1.sneak("100, 5 ProjectCard, Predators")
    engine.phase("Action")
  }

  @Test
  fun test() {
    // There's no stealable animal - TODO exception type is wrong
    shouldThrow<Exception> { p1.cardAction1("Predators") }

    p1.sneak("Animal<Predators>")
    p1.cardAction1("Predators") // Now there is but it can only eat itself

    engine.nextGeneration(0, 0)
    p2.sneak("Birds, Animal<Birds>")
    p1.cardAction1("Predators") { doTask("-Animal<P2, Birds<P2>>") }
  }
}
