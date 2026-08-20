package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class IceAsteroidTest : CardTest() {
  @Test
  fun `with eight oceans, Ice Asteroid cannot select an occupied area`() {
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
