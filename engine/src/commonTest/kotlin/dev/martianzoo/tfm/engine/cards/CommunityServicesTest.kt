package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class CommunityServicesTest : CardTest() {
  @Test
  fun `Can be played with three tagless cards`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.manual("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.manual("$Airliners") { addCardResources(AtmoCollectors) }
    p1.manual("PROD[2]")
    // Three tagless cards: Atmo Collectors, Airliners, and Community Services itself.
    p1.manual("$CommunityServices").expect("PROD[3]")
  }

  @Test
  fun `Ecology Experts is not tagless after playing its selected card`() {
    newGame(
        PreludeExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    engine.phase("Prelude")
    p1.manual("5 Megacredit, ProjectCard, PreludeCard")
    p1.playPrelude(EcologyExperts) { p1.playProject(Decomposers, 5) }

    // Ecology Experts and Decomposers have tags; only Community Services itself is tagless.
    p1.manual("$CommunityServices").expect("PROD[1]")
  }
}
