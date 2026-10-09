package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class IceAsteroidTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Cannot select an occupied area when eight oceans are in play`() {
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, " +
            "OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, " +
            "OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>"
    )

    shouldThrow<NarrowingException> {
      kim.playProject(IceAsteroid, 23) {
        doTask("OceanTile<Tharsis_1_2>")
      }
    }
  }
}
