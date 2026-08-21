package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ProtectedHabitatsTest : CardTest() {
  @Test
  fun `Allows its owner to remove their own protected resources`() {
    newGame()
    p1.manual("PROD[Plant], $ProtectedHabitats, Plant, $Fish, $Tardigrades")
    p1.manual("Animal<$Fish>, Microbe<$Tardigrades>")
    p1.manual("-Plant, -Animal<$Fish>, -Microbe<$Tardigrades>").expect("-Plant, -Animal, -Microbe")
  }

  @Test
  fun `Prevents an opponent from removing protected plants`() {
    newGame()
    val p2 = requireP2()
    seedProtectedP2Resources()
    shouldThrow<DeadEndException> { p1.manual("-Plant<Player2>") }
    p2.count("Plant") shouldBe 1
  }

  @Test
  fun `Prevents an opponent from removing protected animals`() {
    newGame()
    val p2 = requireP2()
    seedProtectedP2Resources()
    shouldThrow<DeadEndException> { p1.manual("-Animal<Player2, $Fish<Player2>>") }
    p2.count("Animal<$Fish>") shouldBe 1
  }

  @Test
  fun `Prevents an opponent from removing protected microbes`() {
    newGame()
    val p2 = requireP2()
    seedProtectedP2Resources()
    shouldThrow<DeadEndException> { p1.manual("-Microbe<Player2, $Tardigrades<Player2>>") }
    p2.count("Microbe<$Tardigrades>") shouldBe 1
  }

  private fun seedProtectedP2Resources() {
    val p2 = requireP2()
    p2.manual("PROD[Plant], $ProtectedHabitats, Plant, $Fish, $Tardigrades")
    p2.manual("Animal<$Fish>, Microbe<$Tardigrades>")
  }
}
