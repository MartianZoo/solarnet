package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class IceAsteroidTest : CardTest() {
  @Test
  internal fun `Cannot select an occupied area when eight oceans are in play`() {
    newGame()
    val waterAreas = p1.list("WaterArea")
    val existingOceans = waterAreas.take(8).joinToString { "OceanTile<$it>" }
    val ninthArea = waterAreas.elementAt(8)
    p1.manual("23, ProjectCard, $existingOceans")
    engine.phase("Action")

    p1.playProject(IceAsteroid, 23) {
      shouldThrow<NarrowingException> { doTask("OceanTile<${waterAreas.first()}>") }
      doTask("OceanTile<$ninthArea>")
    }

    p1.assertCounts(9 to "OceanTile")
  }
}
