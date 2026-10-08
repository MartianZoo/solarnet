package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.CarbonNanosystems
import dev.martianzoo.tfm.tests.cards.cardnames.PhysicsComplex
import dev.martianzoo.tfm.tests.cards.cardnames.SolarProbe
import dev.martianzoo.tfm.tests.cards.cardnames.TransNeptuneProbe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SolarProbeTest : CardTest() {
  @Test
  internal fun `Solar Probe must be paid before its science tag supplies Carbon Nanosystems graphene`() {
    newGame(PromoCardPack, ColoniesExpansion, colonyTiles = testColonyTiles(2))
    admin.phase("Action")
    p1.runOperation("22 MC, 2 ProjectCard")
    p1.playProject(CarbonNanosystems, 14)
    p1.runOperation("-Graphene<$CarbonNanosystems>")
    p1.count("MC") shouldBe 8

    shouldThrow<LimitsException> { p1.playProject(SolarProbe, 9) }
    p1.assertCounts(
        8 to "MC",
        1 to "ProjectCard",
        0 to "Graphene<$CarbonNanosystems>",
        0 to "PlayedEvent<Class<$SolarProbe>>",
    )
  }

  @Test
  internal fun `Solar Probe counts its own science tag before entering the played-event pile`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    admin.phase("Action")
    p1.runOperation("9 MC, ProjectCard, $TransNeptuneProbe, $PhysicsComplex")

    p1.playProject(SolarProbe, 9).expect("-9 MC, 0 ProjectCard")

    p1.assertCounts(
        0 to "$SolarProbe",
        1 to "PlayedEvent<Class<$SolarProbe>>",
    )
  }
}
