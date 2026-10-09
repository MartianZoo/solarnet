package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AirScrappingExpeditionTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Cannot overfill a floater card`() {
    kim.exMachina("$ForcedPrecipitation, $AtmoCollectors, 2 Floater<$AtmoCollectors>")

    shouldThrow<NarrowingException> {
      kim.playProject(AirScrappingExpedition, 13) { addCardResources(AtmoCollectors) }
    }
  }
}
