package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PredatorsTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame()
    kim.exMachina("$Predators")
  }

  @Test
  internal fun `Cannot act when no animal can be removed`() {
    shouldThrow<LimitsException> { kim.cardAction1(Predators) }
  }

  @Test
  internal fun `Removes exactly one of two animals on the target card`() {
    stan.exMachina("$Birds, 2 Animal<$Birds>")

    kim.cardAction1(Predators).expect("-Animal<$Birds<Stan>>, Animal<$Predators>")
  }

  @Test
  internal fun `Cannot decline to remove an opponent's animal`() {
    stan.exMachina("$Birds, Animal<$Birds>")
    kim.exMachina("Animal<$Predators>")

    shouldThrow<NarrowingException> {
      kim.cardAction1(Predators) { doTask("Ok") }
    }
  }

  @Test
  internal fun `Can remove an animal from another card its player owns`() {
    kim.exMachina("$Birds")
    kim.exMachina("Animal<$Birds>")
    kim.exMachina("Animal<$Predators>")

    kim.cardAction1(Predators) { doTask("-Animal<$Birds>") }
        .expect("Animal<$Predators>, -Animal<$Birds>")
  }

  @Test
  internal fun `Can remove and replace its own animal`() {
    kim.exMachina("Animal<$Predators>")
    kim.cardAction1(Predators).expect("0 Animal<$Predators>")
  }

  @Test
  internal fun `Predators can remove its own animal and trigger Meat Industry when replacing it`() {
    newTestGame()
    kim.exMachina("$Predators, $MeatIndustry, Animal<$Predators>")

    kim.cardAction1(Predators).expect("0 Animal<$Predators>, 2 MC")
  }

  @Test
  internal fun `Takes an animal from the neutral holder in solo play`() {
    newTestGame(playerCount = 1)
    kim.exMachina("$Predators")

    kim.cardAction1(Predators).expect("Animal<$Predators>")
  }
}
