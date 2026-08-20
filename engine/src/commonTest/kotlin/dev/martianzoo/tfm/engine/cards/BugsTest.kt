package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
class BugsTest : CardTest() {
  // NOTE: Established Methods says that an unaffordable second standard project is replaced by
  // NOTE: losing 10 M€ (or as much as possible). Fake Established Methods intentionally omits
  // NOTE: that fallback.
  @Test
  fun `Established Methods without its note dead-ends when no second project is affordable`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.phase("Prelude")
    p1.manual("PreludeCard")

    val deadEnd =
        shouldThrow<AbstractException> {
          p1.playPrelude(FakeEstablishedMethods) {
            p1.manual("-20")
            doTask("UseAction1<UseStandardProjectSA>")
            doTask("UseAction1<GreenerySP>")
            p1.autoExecNow()
          }
        }
    deadEnd.message shouldContain "CardX54F"
  }

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

  @Test
  fun `stealing zero is incorrectly permitted, and even avoids Mons Insurance compensation`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$MonsInsurance, 10 Megacredit")
    p2.manual("5 Megacredit")

    p1.manual("3 Megacredit FROM Megacredit<Player2>?") { doTask("Ok") }
        .expect("0 Megacredit<Player1>, 0 Megacredit<Player2>")
  }

  @Test
  fun `Air Raid incorrectly remains playable when only its player has money`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    val p2 = requireP2()
    engine.phase("Action")
    p1.manual("$AtmoCollectors") { doTask("2 Floater<$AtmoCollectors>") }
    p1.manual("ProjectCard, 5 Megacredit")

    p1.playProject(AirRaid, 0).expect("-Floater<$AtmoCollectors>, 0 Megacredit<Player1>")
    p2.assertCounts(0 to "Megacredit")
  }

  @Test
  fun `Public Plans incorrectly remains playable while revealing no other cards`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("7 Megacredit, ProjectCard")

    p1.playProject(PublicPlans, 7)

    p1.assertCounts(0 to "ProjectCard", 1 to "PlayedEvent<Class<$PublicPlans>>")
  }
}
