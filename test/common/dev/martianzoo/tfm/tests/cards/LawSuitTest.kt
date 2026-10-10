package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class LawSuitTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can charge an opponent who lowered the owner's production`() {
    stan.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Kim>]") }

    kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }.expect("MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Transfers the event to the attacker selected for payment`() {
    stan.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Kim>]") }

    kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }
        .expect("0 PlayedEvent<Kim, Class<$LawSuit>>, PlayedEvent<Stan, Class<$LawSuit>>")
  }

  @Test
  internal fun `Can charge an opponent who removed the owner's resources`() {
    kim.exMachina("Plant")
    stan.playProject(AsteroidCard, 14) { doTask("-Plant<Kim>") }

    kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }.expect("MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Can be played with only enough money for the card cost`() {
    stan.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Kim>]") }
    kim.setToExMachina(2, "MC")

    kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }.expect("MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Triggers its player's Media Group`() {
    kim.exMachina("$MediaGroup")
    stan.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Kim>]") }

    kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }.expect("4 MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Does not trigger the charged player's Media Group`() {
    stan.exMachina("$MediaGroup")
    stan.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Kim>]") }

    kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }.expect("MC<Kim>, -3 MC<Stan>")
  }

  @Test
  internal fun `Cannot qualify by lowering the owner's own production`() {
    kim.setToExMachina(2, "PROD[Heat]")
    kim.playProject(HeatTrappers, 6) { doTask("PROD[-2 Heat<Kim>]") }

    shouldThrow<NarrowingException> {
      kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }
    }
    kim.assertCounts(36 to "MC", 9 to "ProjectCard")
  }

  @Test
  internal fun `Cannot charge for an attack in the previous generation`() {
    stan.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Kim>]") }
    nextGeneration()
    val money = kim.count("MC")

    shouldThrow<NarrowingException> {
      kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }
    }
    kim.assertCounts(money to "MC", 10 to "ProjectCard")
  }

  @Test
  internal fun `Cannot charge when every attacker has less than three MC`() {
    kim.exMachina("2 Plant")
    stan.playProject(AsteroidCard, 14) { doTask("-Plant<Kim>") }
    rob.playProject(SmallAsteroid, 10) { doTask("-Plant<Kim>") }
    stan.setToExMachina(2, "MC")
    rob.setToExMachina(2, "MC")

    shouldThrow<LimitsException> {
      kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }
    }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard")
    stan.assertCounts(2 to "MC")
    rob.assertCounts(2 to "MC")
  }

  @Test
  internal fun `Can choose among multiple attackers`() {
    kim.exMachina("2 Plant")
    stan.playProject(AsteroidCard, 14) { doTask("-Plant<Kim>") }
    rob.playProject(SmallAsteroid, 10) { doTask("-Plant<Kim>") }

    kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }
        .expect("MC<Kim>, -3 MC<Stan>, 0 MC<Rob>")
  }

  @Test
  internal fun `Cannot charge a funded player who did not attack`() {
    stan.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Kim>]") }

    shouldThrow<NarrowingException> {
      kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Rob>") }
    }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard")
    stan.assertCounts(39 to "MC")
    rob.assertCounts(42 to "MC")
  }

  @Test
  internal fun `Costs the charged player a victory point`() {
    stan.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Kim>]") }
    kim.playProject(LawSuit, 2) { doTask("3 MC FROM MC<Stan>") }

    // Energy Tapping and Law Suit each cost Stan a point.
    victoryPoints() shouldBe listOf(20, 18, 20)
  }
}
