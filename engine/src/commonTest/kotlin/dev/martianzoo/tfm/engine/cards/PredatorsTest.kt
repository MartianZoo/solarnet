package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

class PredatorsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    p1.manual("$Predators")
    engine.phase("Action")
  }

  @Test
  fun `without an animal, Predators is unavailable`() {
    shouldThrow<LimitsException> { p1.cardAction1(Predators) }
  }

  @Test
  fun `with an animal on a p2 card, p1 uses Predators`() {
    addBirdForP2()
    p1.cardAction1(Predators).expect("Animal<$Predators>, -Animal<Player2, $Birds<Player2>>")
  }

  @Test
  fun `with two animals on one p2 card, Predators removes exactly one`() {
    addBirdForP2()
    requireP2().manual("Animal<$Birds>")

    p1.cardAction1(Predators)

    requireP2().count("Animal<$Birds>") shouldBe 1
    p1.count("Animal<$Predators>") shouldBe 1
  }

  @Test
  fun `p1 cannot decline to remove an animal from p2`() {
    addBirdForP2()
    p1.manual("Animal<$Predators>")

    p1.cardAction1(Predators) {
      shouldThrow<NarrowingException> { doTask("Ok") }
      doTask("-Animal<Player2, $Birds<Player2>>")
    }
  }

  @Test
  fun `with an animal on another p1 card, p1 can use Predators`() {
    p1.manual("PROD[2 Plant], $Birds")
    p1.manual("Animal<$Birds>")
    p1.manual("Animal<$Predators>")

    p1.cardAction1(Predators) { doTask("-Animal<$Birds>") }
        .expect("Animal<$Predators>, -Animal<$Birds>")
  }

  @Test
  fun `with an animal only on Predators, p1 uses its action`() {
    p1.manual("Animal<$Predators>")
    p1.cardAction1(Predators).expect("0 Animal<$Predators>")
  }

  @Test
  fun `Predators can remove its own animal and trigger Meat Industry when replacing it`() {
    newGame(PromoCardPack, players = 1)
    p1.manual("$Predators, $MeatIndustry, Animal<$Predators>")
    engine.phase("Action")

    p1.cardAction1(Predators) { doTask("-Animal<$Predators>") }.expect("0 Animal<$Predators>, 2")
  }

  @Test
  fun `in solo play Predators takes an animal from the neutral holder`() {
    newGame(players = 1)
    p1.manual("$Predators")
    engine.phase("Action")

    p1.cardAction1(Predators)

    p1.count("Animal<$Predators>") shouldBe 1
  }

  private fun addBirdForP2() {
    val p2 = requireP2()
    p2.manual("PROD[2 Plant], $Birds")
    p2.manual("Animal<$Birds>")
  }
}
