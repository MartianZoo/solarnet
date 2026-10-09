package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CrashSiteCleanupTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can be played after removing an opponent's plant`() {
    stan.exMachina("Plant")
    kim.playProject(AsteroidCard, 14) { doTask("-Plant<Stan>") }

    kim.playProject(CrashSiteCleanup, 4) { doTask("Titanium") }.expect("Titanium")
  }

  @Test
  internal fun `Cannot be played after removing the owner's plant`() {
    kim.exMachina("Plant")
    kim.playProject(AsteroidCard, 14) { doTask("-Plant<Kim>") }

    shouldThrow<RequirementException> { kim.playProject(CrashSiteCleanup, 4) }
  }

  @Test
  internal fun `Cannot be played after an opponent removes their own plant`() {
    stan.exMachina("Plant")
    stan.playProject(AsteroidCard, 14) { doTask("-Plant<Stan>") }

    shouldThrow<RequirementException> { kim.playProject(CrashSiteCleanup, 4) }
  }

  @Test
  internal fun `Cannot be played after a plant removal in the previous generation`() {
    stan.exMachina("Plant")
    kim.playProject(AsteroidCard, 14) { doTask("-Plant<Stan>") }
    nextGeneration()

    shouldThrow<RequirementException> { kim.playProject(CrashSiteCleanup, 4) }
  }

  @Test
  internal fun `Cannot be played by a player who did not remove the plant`() {
    stan.exMachina("Plant")
    rob.playProject(AsteroidCard, 14) { doTask("-Plant<Stan>") }

    shouldThrow<RequirementException> { kim.playProject(CrashSiteCleanup, 4) }
  }

  @Test
  internal fun `Cannot be played after declining an asteroid's plant attack`() {
    stan.exMachina("Plant")
    kim.playProject(AsteroidCard, 14) { declineTask() }.expect("0 Plant<Stan>")

    shouldThrow<RequirementException> { kim.playProject(CrashSiteCleanup, 4) }
  }
}
