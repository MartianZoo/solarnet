package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ProtectedHabitatsTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Allows its owner to spend protected plants`() {
    kim.exMachina("$ProtectedHabitats, 8 Plant")

    kim.convertPlants { placeTile(1, 1) }.expect("-8 Plant, GreeneryTile")
  }

  @Test
  internal fun `Allows its owner's Predators to eat protected animals`() {
    kim.exMachina("$ProtectedHabitats, $Predators, $Fish, Animal<$Fish>")

    kim.cardAction1(Predators).expect("-Animal<$Fish>, Animal<$Predators>")
  }

  @Test
  internal fun `Allows its owner's Ants to eat protected microbes`() {
    kim.exMachina("$ProtectedHabitats, $Ants, $Tardigrades, Microbe<$Tardigrades>")

    kim.cardAction1(Ants).expect("-Microbe<$Tardigrades>, Microbe<$Ants>")
  }

  @Test
  internal fun `Prevents an opponent's asteroid from removing protected plants`() {
    kim.exMachina("$ProtectedHabitats, Plant")

    shouldThrow<DeadEndException> {
      stan.playProject(AsteroidCard, 14) { doTask("-Plant<Kim>") }
    }
    kim.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Prevents an opponent's Predators from eating protected animals`() {
    kim.exMachina("$ProtectedHabitats, $Fish, Animal<$Fish>")
    stan.exMachina("$Predators, Animal<$Predators>")

    shouldThrow<DeadEndException> {
      stan.cardAction1(Predators) { doTask("-Animal<Kim, $Fish<Kim>>") }
    }
    kim.assertCounts(1 to "Animal<$Fish>")
  }

  @Test
  internal fun `Prevents an opponent's Ants from eating protected microbes`() {
    kim.exMachina("$ProtectedHabitats, $Tardigrades, Microbe<$Tardigrades>")
    stan.exMachina("$Ants, Microbe<$Ants>")

    shouldThrow<DeadEndException> {
      stan.cardAction1(Ants) { doTask("-Microbe<Kim, $Tardigrades<Kim>>") }
    }
    kim.assertCounts(1 to "Microbe<$Tardigrades>")
  }
}
