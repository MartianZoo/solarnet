package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AirScrappingExpeditionTest : ProjectCardTest() {
  @Test
  internal fun `Cannot overfill a floater card`() {
    kim.exMachina("$ForcedPrecipitation, $AtmoCollectors, 2 Floater<$AtmoCollectors>")

    shouldThrow<NarrowingException> {
      kim.playProject(AirScrappingExpedition, 13) { addCardResources(AtmoCollectors) }
    }
  }
}
