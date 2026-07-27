package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DeadEndException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ProtectedHabitatsTest : CardTest() {
  @Test
  fun `with p1 resources protected, p1 removes them`() {
    newGame("BMR")
    p1.manual("PROD[Plant], ProtectedHabitats, Plant, Fish, Tardigrades")
    p1.manual("Animal<Fish>, Microbe<Tardigrades>")
    p1.manual("-Plant, -Animal<Fish>, -Microbe<Tardigrades>").expect("-Plant, -Animal, -Microbe")
  }

  @Test
  fun `with p2 plants protected, p1 tries to remove one`() {
    newGame("BMR")
    seedProtectedP2Resources()
    shouldThrow<DeadEndException> { p1.manual("-Plant<Player2>") }
  }

  @Test
  fun `with p2 animals protected, p1 tries to remove one`() {
    newGame("BMR")
    seedProtectedP2Resources()
    shouldThrow<DeadEndException> { p1.manual("-Animal<Player2, Fish<Player2>>") }
  }

  @Test
  fun `with p2 microbes protected, p1 tries to remove one`() {
    newGame("BMR")
    seedProtectedP2Resources()
    shouldThrow<DeadEndException> { p1.manual("-Microbe<Player2, Tardigrades<Player2>>") }
  }

  private fun seedProtectedP2Resources() {
    val p2 = requireP2()
    p2.manual("PROD[Plant], ProtectedHabitats, Plant, Fish, Tardigrades")
    p2.manual("Animal<Fish>, Microbe<Tardigrades>")
  }
}
