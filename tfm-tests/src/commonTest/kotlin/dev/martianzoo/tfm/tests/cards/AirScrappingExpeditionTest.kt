package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AirScrappingExpeditionTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
  }

  @Test
  internal fun `Can add floaters to another floater card`() {
    p1.manual("$ForcedPrecipitation")
    p1.manual("$AirScrappingExpedition") { addCardResources(ForcedPrecipitation) }
        .expect("3 Floater")
  }

  @Test
  internal fun `Cannot add more floaters than the target card can hold`() {
    p1.manual("$ForcedPrecipitation")
    p1.manual("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    shouldThrow<NarrowingException> {
      p1.manual("$AirScrappingExpedition") { addCardResources(AtmoCollectors) }
    }
  }
}
