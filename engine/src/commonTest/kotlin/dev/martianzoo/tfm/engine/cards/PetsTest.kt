package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DeadEndException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class PetsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    engine.phase("Action")
  }

  @Test
  fun `with an animal protected by Pets, p1 uses Predators`() {
    val p2 = requireP2()
    p2.manual("Pets")
    p1.manual("Predators, Animal<Predators>")

    p1.cardAction1("Predators") {
      shouldThrow<DeadEndException> { doTask("-Animal<Player2, Pets<Player2>>") }
      doTask("-Animal<Predators>")
    }
  }

  @Test
  fun `with only an animal protected by Pets, p1 tries to use Predators`() {
    val p2 = requireP2()
    p2.manual("Pets")
    p1.manual("Predators")
    shouldThrow<DeadEndException> { p1.cardAction1("Predators") }
  }

  @Test
  fun `with an animal on Pets, p1 tries to remove the card`() {
    p1.manual("Pets")
    // Removing the card would mean having to remove the animals on it first -- can't!
    shouldThrow<DeadEndException> { p1.manual("-Pets") }
  }
}
