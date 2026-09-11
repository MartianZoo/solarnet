package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PetsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    admin.phase("Action")
  }

  @Test
  internal fun `Prevents Predators from removing its animal when another target exists`() {
    val p2 = requireP2()
    p2.runOperation("$Pets")
    p1.runOperation("$Predators, Animal<$Predators>")

    p1.cardAction1(Predators) {
      shouldThrow<DeadEndException> { doTask("-Animal<Player2, $Pets<Player2>>") }
      doTask("-Animal<$Predators>")
    }
  }

  @Test
  internal fun `Prevents Predators from acting when its animal is the only target`() {
    val p2 = requireP2()
    p2.runOperation("$Pets")
    p1.runOperation("$Predators")
    shouldThrow<DeadEndException> { p1.cardAction1(Predators) }
  }
}
