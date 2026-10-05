package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.Unsafe
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MiningGuildTest : CardTest() {
  @Test
  internal fun `Raises steel production for metal areas without an Audit`() {
    newGame()
    p1.runOperation("$MiningGuild")
    p1.count("PROD[Steel]") shouldBe 1
    val checkpoint = game.timeline.checkpoint()

    p1.runOperation("CityTile<Tharsis_1_1>").expect("PROD[Steel]") // LSS
    p1.count("PROD[Steel]") shouldBe 2

    p1.runOperation("CityTile<Tharsis_8_9>").expect("PROD[Steel]") // Titanium
    p1.count("PROD[Steel]") shouldBe 3
    p1.auditGainsSince(checkpoint) shouldBe 0

    p1.runOperation("CityTile<Tharsis_2_1>") // L
    p1.count("PROD[Steel]") shouldBe 3

    p1.runOperation("CityTile<Tharsis_4_4>").expect("Plant, PROD[0 Steel]")
  }

  // BGG exact Mining Guild ruling:
  // https://boardgamegeek.com/thread/3403085/article/45161178#45161178
  @Test
  internal fun `A wild bonus grants steel production with an Audit when metal is chosen`() {
    newGame(Amazonis, Unsafe)
    p1.playCorp(MiningGuild, 0)
    admin.phase("Action")
    val checkpoint = game.timeline.checkpoint()

    p1.stdProject("GreeneryProject") {
          placeTile(5, 3)
          doTask("Titanium")
        }
        .expect("Titanium, PROD[Steel]")
    p1.auditGainsSince(checkpoint) shouldBe 1
  }
}
