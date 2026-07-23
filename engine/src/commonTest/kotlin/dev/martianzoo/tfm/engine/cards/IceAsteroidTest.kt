package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class IceAsteroidTest : CardTest() {
  @Test
  fun `second ocean task disappears after the ninth ocean is placed`() {
    newGame(Canon.SIMPLE_GAME)
    val waterAreas = player1.list("WaterArea")
    val existingOceans = waterAreas.take(8).joinToString { "OceanTile<$it>" }
    val ninthArea = waterAreas.elementAt(8)
    player1.sneak("100, 5 ProjectCard, $existingOceans")
    player1.phase("Action")

    player1.playProject("IceAsteroid", 23) {
      game.tasks
          .extract { "${it.instruction}" }
          .also { pending ->
            pending.shouldHaveSize(2)
            pending.toSet() shouldBe setOf("OceanTile<WaterArea>.")
          }

      doFirstTask("OceanTile<$ninthArea>")

      game.tasks.isEmpty() shouldBe true
    }

    player1.assertCounts(9 to "OceanTile")
  }
}
