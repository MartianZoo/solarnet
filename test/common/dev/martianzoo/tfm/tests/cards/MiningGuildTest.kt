package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class MiningGuildTest : TfmSandboxTest() {
  @Test
  internal fun `A two-steel placement bonus raises production by a single step without an Audit`() {
    newTestGame(kimCorporation = MiningGuild)
    kim.setToExMachina(25, "MC")
    val checkpoint = game.timeline.checkpoint()

    kim.stdProject("CityProject") { placeTile(1, 1) }.expect("2 Steel, PROD[Steel]")
    kim.auditGainsSince(checkpoint) shouldBe 0
  }

  @Test
  internal fun `A titanium placement bonus raises steel production without an Audit`() {
    newTestGame(kimCorporation = MiningGuild)
    kim.setToExMachina(25, "MC")
    val checkpoint = game.timeline.checkpoint()

    kim.stdProject("CityProject") { placeTile(8, 9) }.expect("Titanium, PROD[Steel]")
    kim.auditGainsSince(checkpoint) shouldBe 0
  }

  @Test
  internal fun `A plant placement bonus does not raise steel production`() {
    newTestGame(kimCorporation = MiningGuild)
    kim.setToExMachina(25, "MC")

    kim.stdProject("CityProject") { placeTile(4, 4) }.expect("Plant, PROD[0 Steel]")
  }

  // BGG exact Mining Guild ruling:
  // https://boardgamegeek.com/thread/3403085/article/45161178#45161178
  @Test
  internal fun `A wild bonus grants steel production with an Audit when metal is chosen`() {
    newTestGame(addOptions = "AmazonisMap, Unsafe", kimCorporation = MiningGuild)
    kim.setToExMachina(23, "MC")
    val checkpoint = game.timeline.checkpoint()

    kim.stdProject("GreeneryProject") {
          placeTile(5, 3)
          doTask("Titanium")
        }
        .expect("Titanium, PROD[Steel]")
    kim.auditGainsSince(checkpoint) shouldBe 1
  }

  // https://boardgamegeek.com/thread/3403085/article/45161178#45161178
  @Ignore // Mining Guild responds to the area instead of the chosen resource.
  @Test
  internal fun `Nonmetal wild bonus grants no steel production`() {
    takeNonmetalWildBonus().expect("Plant, PROD[0 Steel]")
  }

  @Test
  internal fun `BUG - Nonmetal wild bonus grants steel production`() {
    takeNonmetalWildBonus().expect("Plant, PROD[Steel]")
  }

  private fun takeNonmetalWildBonus(): TaskResult {
    newTestGame(addOptions = "AmazonisMap, Unsafe", kimCorporation = MiningGuild)
    kim.setToExMachina(23, "MC")
    return kim.stdProject("GreeneryProject") {
      placeTile(5, 3)
      doTask("Plant")
    }
  }
}
