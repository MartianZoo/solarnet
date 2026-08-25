package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PredatorsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    p1.manual("$Predators")
    engine.phase("Action")
  }

  @Test
  internal fun `Cannot act when no animal can be removed`() {
    shouldThrow<LimitsException> { p1.cardAction1(Predators) }
  }

  @Test
  internal fun `Can remove an opponent's animal`() {
    addBirdForP2()
    p1.cardAction1(Predators).expect("Animal<$Predators>, -Animal<Player2, $Birds<Player2>>")
  }

  @Test
  internal fun `Removes exactly one of two animals on the target card`() {
    addBirdForP2()
    requireP2().manual("Animal<$Birds>")

    p1.cardAction1(Predators)

    requireP2().count("Animal<$Birds>") shouldBe 1
    p1.count("Animal<$Predators>") shouldBe 1
  }

  @Test
  internal fun `Cannot decline to remove an opponent's animal`() {
    addBirdForP2()
    p1.manual("Animal<$Predators>")

    p1.cardAction1(Predators) {
      shouldThrow<NarrowingException> { doTask("Ok") }
      doTask("-Animal<Player2, $Birds<Player2>>")
    }
  }

  @Test
  internal fun `Can remove an animal from another card its player owns`() {
    p1.manual("PROD[2 Plant], $Birds")
    p1.manual("Animal<$Birds>")
    p1.manual("Animal<$Predators>")

    p1.cardAction1(Predators) { doTask("-Animal<$Birds>") }
        .expect("Animal<$Predators>, -Animal<$Birds>")
  }

  @Test
  internal fun `Can remove and replace its own animal`() {
    p1.manual("Animal<$Predators>")
    p1.cardAction1(Predators).expect("0 Animal<$Predators>")
  }

  @Test
  internal fun `Predators can remove its own animal and trigger Meat Industry when replacing it`() {
    newGame(PromoCardPack, players = 1)
    p1.manual("$Predators, $MeatIndustry, Animal<$Predators>")
    engine.phase("Action")

    p1.cardAction1(Predators) { doTask("-Animal<$Predators>") }.expect("0 Animal<$Predators>, 2")
  }

  @Test
  internal fun `Takes an animal from the neutral holder in solo play`() {
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
