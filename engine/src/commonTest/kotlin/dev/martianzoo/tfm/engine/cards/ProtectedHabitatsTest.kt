package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ProtectedHabitatsTest : CardTest() {
  @Test
  fun `opponents cannot remove protected resources but the owner can`() {
    val game = newGame("BRMP", 2)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p1.sneak("ProtectedHabitats, Plant, Fish, Animal<Fish>, Psychrophiles, Microbe<Psychrophiles>")

    shouldThrow<DeadEndException> { p2.manual("-Plant<Player1>") }
    p1.manual("-Plant<Player1>").expect("-Plant<Player1>")

    shouldThrow<DeadEndException> { p2.manual("-Animal<Player1, Fish<Player1>>") }
    p1.manual("-Animal<Player1, Fish<Player1>>").expect("-Animal<Player1, Fish<Player1>>")

    shouldThrow<DeadEndException> {
      p2.manual("-Microbe<Player1, Psychrophiles<Player1>>")
    }
    p1.manual("-Microbe<Player1, Psychrophiles<Player1>>")
        .expect("-Microbe<Player1, Psychrophiles<Player1>>")
  }
}
