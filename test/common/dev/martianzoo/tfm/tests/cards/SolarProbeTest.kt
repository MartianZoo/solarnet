package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.PhysicsComplex
import dev.martianzoo.tfm.tests.cards.cardnames.SolarProbe
import dev.martianzoo.tfm.tests.cards.cardnames.TransNeptuneProbe
import kotlin.test.Test

internal class SolarProbeTest : CardTest() {
  @Test
  internal fun `Solar Probe counts its own science tag before entering the played-event pile`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    engine.phase("Action")
    p1.manual("9 MC, ProjectCard, $TransNeptuneProbe, $PhysicsComplex")

    p1.playProject(SolarProbe, 9).expect("-9 MC, 0 ProjectCard")

    p1.assertCounts(
        0 to "$SolarProbe",
        1 to "PlayedEvent<Class<$SolarProbe>>",
    )
  }
}
