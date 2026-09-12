package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class IceAsteroidTest : CardTest() {
  @Test
  internal fun `Cannot select an occupied area when eight oceans are in play`() {
    newGame()
    val waterAreas = p1.list("WaterArea")
    val existingOceans = waterAreas.take(8).joinToString { "OceanTile<$it>" }
    val ninthArea = waterAreas.elementAt(8)
    p1.runOperation("23 MC, ProjectCard, $existingOceans")
    admin.phase("Action")

    p1.playProject(IceAsteroid, 23) {
      val failure = shouldThrow<NarrowingException> { doTask("OceanTile<${waterAreas.first()}>") }
      failure.message!! shouldContain "MAX 0 Tile"
      doTask("OceanTile<$ninthArea>")
    }

    p1.assertCounts(9 to "OceanTile")
  }
}
