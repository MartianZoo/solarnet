package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.CarbonNanosystems
import dev.martianzoo.tfm.tests.cards.cardnames.PhysicsComplex
import dev.martianzoo.tfm.tests.cards.cardnames.SolarProbe
import dev.martianzoo.tfm.tests.cards.cardnames.TransNeptuneProbe
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SolarProbeTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Solar Probe must be paid before its science tag supplies Carbon Nanosystems graphene`() {
    kim.exMachina("$CarbonNanosystems")
    kim.setToExMachina(8, "MC")
    val cardsBefore = kim.count("ProjectCard")

    shouldThrow<LimitsException> { kim.playProject(SolarProbe, 9) }
    kim.assertCounts(
        8 to "MC",
        cardsBefore to "ProjectCard",
        0 to "Graphene<$CarbonNanosystems>",
        0 to "PlayedEvent<Class<$SolarProbe>>",
    )
  }

  @Test
  internal fun `Solar Probe counts its own science tag before entering the played-event pile`() {
    kim.exMachina("$TransNeptuneProbe, $PhysicsComplex")
    kim.setToExMachina(9, "MC")

    kim.playProject(SolarProbe, 9).expect("-9 MC, 0 ProjectCard")

    kim.assertCounts(
        0 to "$SolarProbe",
        1 to "PlayedEvent<Class<$SolarProbe>>",
    )
  }
}
