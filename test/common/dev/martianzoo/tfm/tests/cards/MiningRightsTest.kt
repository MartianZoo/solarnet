package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Ignore
import kotlin.test.Test

internal class MiningRightsTest : TfmSandboxTest() {
  // Resolved FAQ: a wild-resource area is eligible even when its chosen resource is not metal.
  // https://boardgamegeek.com/thread/3403085/article/45161764#45161764
  @Ignore // Known defect: the production requirement rejects wild-resource areas.
  @Test
  internal fun `Can use a wild placement bonus`() {
    playOnWildBonus().expect("Steel, PROD[Steel], MiningRights_SpecialTile")
  }

  @Test
  internal fun `BUG - Cannot use a wild placement bonus`() {
    shouldThrow<RequirementException> { playOnWildBonus() }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard", 0 to "MiningRights_SpecialTile")
  }

  private fun playOnWildBonus(): TaskResult {
    newTestGame(addOptions = "AmazonisMap, Unsafe")
    return kim.playProject(MiningRights, 9) {
      placeTile(5, 3)
      doTask("Steel")
      doTask("PROD[Steel]")
    }
  }

  @Test
  internal fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    // Resolved FAQ: copying allows any originally available metal, regardless of the first choice.
    // https://boardgamegeek.com/thread/2663453/rule-opinions-mining-rights-robotic-workforce
    newTestGame(addOptions = "CimmeriaMap")

    kim.playProject(MiningRights, 9) {
          placeTile(6, 4)
          doTask("PROD[Steel]")
        }
        .expect("-9 MC, Titanium, 2 Steel, PROD[Steel]")

    kim.playProject(RoboticWorkforce, 9) {
          doTask("CopyProductionBox<$MiningRights>")
          doTask("PROD[Titanium]")
        }
        .expect("-9 MC, PROD[Titanium]")
  }

  @Test
  internal fun `Cannot select an area without a metal placement bonus`() {
    newTestGame()
    shouldThrow<GameplayException> { kim.playProject(MiningRights, 9) { placeTile(2, 1) } }
  }

  @Test
  internal fun `A mixed metal placement grants both resources and steel production`() {
    newTestGame(addOptions = "CimmeriaMap")

    kim.playProject(MiningRights, 9) {
          placeTile(6, 4)
          doTask("PROD[Steel]")
        }
        .expect("-9 MC, 2 Steel, Titanium, PROD[Steel]")
  }

  @Test
  internal fun `A mixed metal placement grants both resources and titanium production`() {
    newTestGame(addOptions = "CimmeriaMap")

    kim.playProject(MiningRights, 9) {
          placeTile(6, 4)
          doTask("PROD[Titanium]")
        }
        .expect("-9 MC, 2 Steel, Titanium, PROD[Titanium]")
  }
}
