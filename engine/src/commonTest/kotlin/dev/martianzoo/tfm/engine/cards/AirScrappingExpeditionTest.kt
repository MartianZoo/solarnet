package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class AirScrappingExpeditionTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
  }

  @Test
  fun `with an eligible floater card, adds Air Scrapping Expedition`() {
    p1.manual("$ForcedPrecipitation")
    p1.manual("$AirScrappingExpedition") { doTask("3 Floater<$ForcedPrecipitation>") }
        .expect("3 Floater")
  }

  @Test
  fun `with two floaters on Atmo Collectors, tries to add three more`() {
    p1.manual("$ForcedPrecipitation")
    p1.manual("$AtmoCollectors") { doTask("2 Floater<$AtmoCollectors>") }
    shouldThrow<NarrowingException> {
      p1.manual("$AirScrappingExpedition") { doTask("3 Floater<$AtmoCollectors>") }
    }
  }
}
