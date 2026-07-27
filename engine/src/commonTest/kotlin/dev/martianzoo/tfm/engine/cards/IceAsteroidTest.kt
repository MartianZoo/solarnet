package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class IceAsteroidTest : CardTest() {
  @Test
  fun `with eight oceans, plays Ice Asteroid`() {
    newGame()
    val waterAreas = p1.list("WaterArea")
    val existingOceans = waterAreas.take(8).joinToString { "OceanTile<$it>" }
    val ninthArea = waterAreas.elementAt(8)
    p1.manual("23, ProjectCard, $existingOceans")
    engine.phase("Action")

    p1.playProject("IceAsteroid", 23) {
      game.tasks
          .extract { "${it.instruction}" }
          .also { pending ->
            pending.shouldHaveSize(2)
            pending.toSet() shouldBe setOf("OceanTile<WaterArea>.")
          }

      doFirstTask("OceanTile<$ninthArea>")

      game.tasks.isEmpty() shouldBe true
    }

    p1.assertCounts(9 to "OceanTile")
  }
}
