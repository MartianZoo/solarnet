package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class IceAsteroidTest : ProjectCardTest() {
  @Test
  internal fun `Cannot select an occupied area when eight oceans are in play`() {
    val waterAreas = kim.list("WaterArea")
    kim.exMachina(waterAreas.take(8).joinToString { "OceanTile<$it>" })

    shouldThrow<NarrowingException> {
      kim.playProject(IceAsteroid, 23) {
        doTask("OceanTile<${waterAreas.first()}>")
      }
    }
  }
}
