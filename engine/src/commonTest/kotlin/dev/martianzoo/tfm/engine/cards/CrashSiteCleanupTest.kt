package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class CrashSiteCleanupTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame("TerraformingMars,TharsisMap,PromoCardPack")
    engine.phase("Action")
    p1.manual("4, ProjectCard")
    requireP2().manual("Plant")
  }

  @Test
  fun `after p1 removes a p2 plant, plays Crash Site Cleanup`() {
    p1.manual("-Plant<Player2>")
    p1.playProject("CrashSiteCleanup", 4) { doTask("Titanium") }.expect("Titanium")
  }

  @Test
  fun `without a plant loss, tries to play Crash Site Cleanup`() {
    shouldThrow<RequirementException> { p1.playProject("CrashSiteCleanup", 4) }
  }

  @Test
  fun `after losing an own plant, tries to play Crash Site Cleanup`() {
    p1.manual("Plant, -Plant")
    shouldThrow<RequirementException> { p1.playProject("CrashSiteCleanup", 4) }
  }

  @Test
  fun `after p2 removes an own plant, tries to play Crash Site Cleanup`() {
    requireP2().manual("-Plant")
    shouldThrow<RequirementException> { p1.playProject("CrashSiteCleanup", 4) }
  }

  @Test
  fun `a generation after removing a p2 plant, tries to play Crash Site Cleanup`() {
    p1.manual("-Plant<Player2>")
    engine.manual("Generation")
    shouldThrow<RequirementException> { p1.playProject("CrashSiteCleanup", 4) }
  }
}
