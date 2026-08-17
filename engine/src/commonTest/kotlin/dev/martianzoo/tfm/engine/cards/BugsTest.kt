package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
class BugsTest : CardTest() {
  // FAQ: "Those actions are considered distinct actions, but within the action of playing Head
  // Start."
  @Test
  fun `Head Start incorrectly allows its two actions to interleave`() {
    newGame(PreludeExpansion, TurmoilCardPack, PromoCardPack)
    p1.phase("Prelude")
    p1.manual("4, 10 ProjectCard, PreludeCard, 10 Heat")

    p1.playPrelude(HeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "Megacredit")
      doTask("UseAction1<UseStandardProjectSA>")
      doTask("UseAction1<ConvertHeatSA>")
      doTask("UseAction1<AquiferSP>")
      doTask("OceanTile<Tharsis_5_5>")
    }
  }

  // FAQ: Ecology Experts' tags trigger both Splice and the card it plays. Splice's payment should
  // therefore be available to pay for that card, while Decomposers should still receive 2 microbes.
  @Test
  fun `Ecology Experts incorrectly cannot use Splice income to pay for Decomposers`() {
    newGame(PreludeExpansion, PromoCardPack)
    val p2 = requireP2()
    p1.playCorp(TychoMagnetics, 9)
    p2.playCorp(SpliceTacticalGenomics, 0) {
      doTask("Microbe<$SpliceTacticalGenomics>!")
    }
    engine.phase("Prelude")

    p1.playPrelude(ExcentricSponsor) {
      p1.playProject(GiantIceAsteroid, 11) {
        doTask("OceanTile<Tharsis_1_2>")
        doTask("OceanTile<Tharsis_1_4>")
        doTask("Ok")
      }
    }
    p1.assertCounts(4 to "Megacredit")

    p1.playPrelude(EcologyExperts) {
      shouldThrow<LimitsException> { p1.playProject(Decomposers, 5) }
      abort()
    }
  }

  // FAQ: "If you do not have cards that hold those resources, you may still play the card and
  // ignore that effect."
  @Test
  fun `Local Heat Trapping incorrectly cannot discard its optional animal gain`() {
    newGame()
    p1.manual("6 Heat, 2 ProjectCard")

    p1.manual("$LocalHeatTrapping") {
      doTask("4 Plant")
      shouldThrow<TaskException> { doTask("Ok") }
      abort()
    }
  }

  // Solar Probe should count its own science tag and draw one card for all three tags.
  @Test
  fun `Solar Probe incorrectly loses its card draw during normal play`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    engine.phase("Action")
    p1.manual("9, ProjectCard, $TransNeptuneProbe, $PhysicsComplex")

    p1.playProject(SolarProbe, 9).expect("-9, -ProjectCard")
  }

  @Test
  fun `Predators incorrectly remains abstract instead of unavailable without an animal`() {
    newGame()
    p1.manual("$Predators")
    engine.phase("Action")
    shouldThrow<AbstractException> { p1.cardAction1(Predators) }
  }

  @Test
  fun `Artificial Lake incorrectly remains abstract instead of unavailable without an area`() {
    newGame()
    engine.phase("Action")
    val landAreas =
        p1.list("LandArea").filterNot { it.toString() == "VolcanicArea" } + p1.list("VolcanicArea")
    p1.manual(
        "15, ProjectCard, 12 TemperatureStep, " + landAreas.joinToString { "GreeneryTile<$it>" }
    )

    shouldThrow<AbstractException> { p1.playProject(ArtificialLake, 15) }
  }
}
