package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CommunityServicesTest : CardTest() {
  @Test
  internal fun `Can be played with three tagless cards`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.runOperation("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.runOperation("$Airliners") { addCardResources(AtmoCollectors) }
    p1.runOperation("PROD[2 MC]")
    // Three tagless cards: Atmo Collectors, Airliners, and Community Services itself.
    p1.runOperation("$CommunityServices").expect("PROD[3 MC]")
  }

  @Test
  internal fun `Ecology Experts is not tagless after playing its selected card`() {
    newGame(
        PreludeExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Prelude")
    p1.runOperation("5 MC, ProjectCard, PreludeCard")
    with(p1) {
      playPrelude(EcologyExperts) { playProject(Decomposers, 5) }
    }

    // Ecology Experts and Decomposers have tags; only Community Services itself is tagless.
    p1.runOperation("$CommunityServices").expect("PROD[1 MC]")
  }
}
