package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class KaguyaTechTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Replaces a greenery with its city in the same area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_4_2>")

    kim.playProject(KaguyaTech, 10) {
          doTask("CityTile<Tharsis_4_2> FROM GreeneryTile<Tharsis_4_2>")
        }
        .expect("-GreeneryTile<Tharsis_4_2>, CityTile<Tharsis_4_2>")
  }

  @Test
  internal fun `Cannot replace a greenery with a city in another area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_4_2>")

    shouldThrow<NarrowingException> {
      kim.playProject(KaguyaTech, 10) {
        doTask("CityTile<Tharsis_4_3> FROM GreeneryTile<Tharsis_4_2>")
      }
    }
  }

  @Test
  internal fun `Replaces a greenery on an ocean area`() {
    kim.exMachina("GreeneryTile<Kim, Tharsis_1_2>")

    kim.playProject(KaguyaTech, 10) {
          doTask("CityTile<Tharsis_1_2> FROM GreeneryTile<Tharsis_1_2>")
        }
        .expect("-GreeneryTile<Tharsis_1_2>, CityTile<Tharsis_1_2>")
  }
}
