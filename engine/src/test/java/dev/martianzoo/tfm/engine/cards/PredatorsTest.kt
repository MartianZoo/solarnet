package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PredatorsTest {
  val game = Engine.newGame(Canon.SIMPLE_GAME)
  val engine = game.tfm(ENGINE)
  val p1 = game.tfm(PLAYER1)
  val p2 = game.tfm(PLAYER2)

  @BeforeEach
  fun setUp() {
    p1.godMode().manual("100, 5 ProjectCard")
    engine.phase("Action")
  }

  @Test
  fun test() {
    p1.godMode().manual("Predators") // skip requirements

    // There's no stealable animal - TODO exception type is wrong
    assertThrows<Exception> { p1.cardAction1("Predators") }

    p1.godMode().manual("Animal<Predators>")
    p1.cardAction1("Predators") // Now there is but it can only eat itself

    engine.nextGeneration(0, 0)
    p2.godMode().sneak("Birds, Animal<Birds>")
    p1.cardAction1("Predators") { doTask("-Animal<P2, Birds<P2>>") }
  }
}
