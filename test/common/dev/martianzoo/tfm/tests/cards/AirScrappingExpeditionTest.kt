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
    newGameWithAutoWorkflow(
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    playUntilFirstActionPhase()
  }

  @Test
  internal fun `Can add floaters to another floater card`() {
    p1.turn {
      playProject(ForcedPrecipitation, 8)
      playProject(AirScrappingExpedition, 13) { addCardResources(ForcedPrecipitation) }
          .expect("3 Floater")
    }
  }

  @Test
  internal fun `Cannot add more floaters than the target card can hold`() {
    p1.turn {
      playProject(ForcedPrecipitation, 8)
      playProject(AtmoCollectors, 15) { addCardResources(AtmoCollectors) }
    }
    requireP2().pass()

    shouldThrow<NarrowingException> {
      p1.playProject(AirScrappingExpedition, 13) { addCardResources(AtmoCollectors) }
    }
  }
}
