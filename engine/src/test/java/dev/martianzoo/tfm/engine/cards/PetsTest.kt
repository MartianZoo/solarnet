package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertNetChanges
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PetsTest {
  val game = Engine.newGame(Canon.SIMPLE_GAME)
  val engine = game.tfm(ENGINE)
  val p1 = game.tfm(PLAYER1)
  val p2 = game.tfm(PLAYER2)

  private fun TaskResult.expect(string: String) = assertNetChanges(this, game, p1, string)

  @BeforeEach
  fun setUp() {
    p1.godMode().manual("100, 5 ProjectCard")
    engine.phase("Action")
  }

  @Test
  fun cantPredate() {
    p1.playProject("Pets", 10).expect("Animal")

    p2.godMode().manual("Predators") // skip requirement

    // There's no stealable animal
    assertThrows<DeadEndException> { p2.cardAction1("Predators") }

    p2.godMode().manual("Animal<Predators>")

    // Now there is but it can only eat itself
    p2.cardAction1("Predators") {
      assertThrows<DeadEndException> { doTask("-Animal<P1, Pets<P1>>") }
      doTask("-Animal<Predators>")
    }
  }

  @Test
  fun cantRemoveSelf() {
    p1.playProject("Pets", 10).expect("Animal")

    // Removing the card would mean having to remove the animals on it first -- can't!
    assertThrows<DeadEndException> { p1.godMode().manual("-Pets") }
  }
}
