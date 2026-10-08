package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class IceAsteroidTest : ProjectCardTest() {
  @Test
  internal fun `Cannot select an occupied area when eight oceans are in play`() {
    val waterAreas = kim.list("WaterArea")
    val ninthArea = waterAreas.elementAt(8)
    kim.exMachina(waterAreas.take(8).joinToString { "OceanTile<$it>" })

    kim.playProject(IceAsteroid, 23) {
      val failure = shouldThrow<NarrowingException> { doTask("OceanTile<${waterAreas.first()}>") }
      failure.message!! shouldContain "MAX 0 Tile"
      doTask("OceanTile<$ninthArea>")
    }

    kim.assertCounts(9 to "OceanTile")
  }
}
