package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CommunityServicesTest : CardTest() {
  @Test
  internal fun `Ecology Experts is not tagless after playing its selected card`() {
    newGame(
        GameConfig(
            "PreludeExpansion, ColoniesExpansion, EcologyExperts, Unsafe, " +
                testColonyTiles(2).joinToString(),
            "Player1",
            "Player2",
        )
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
