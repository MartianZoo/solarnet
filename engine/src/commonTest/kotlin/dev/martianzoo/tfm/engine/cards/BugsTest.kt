package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.canonicalPremise
import dev.martianzoo.tfm.engine.setUpGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Passing characterizations of known incorrect behavior. These tests illustrate what the engine
 * currently does, not what we want it to do. When a bug is fixed, its test should be changed to
 * assert the desired behavior and moved to the appropriate test class.
 */
class BugsTest : CardTest() {
  // FAQ: "Place a city tile there, regardless of placement rules."
  @Test
  fun `Kaguya Tech can incorrectly move the selected greenery to another area`() {
    newGame("PromoCardPack")
    engine.phase("Action")
    p1.manual("10, ProjectCard, GreeneryTile<Tharsis_4_2>")
    // TODO(#12): Repeated LandArea occurrences should specialize together and reject this move.
    p1.playProject("KaguyaTech", 10) {
          doTask("CityTile<Tharsis_4_3> FROM GreeneryTile<Tharsis_4_2>")
        }
        .expect("-GreeneryTile<Tharsis_4_2>, CityTile<Tharsis_4_3>")
  }

  @Test
  fun `solo setup can incorrectly link the second greenery to the first city`() {
    val setup = canonicalPremise("SoloMode", 1)
    val game = setUpGame(setup)
    val engine = game.tfm(ENGINE)

    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")

    engine.doFirstTask("CityTile<Tharsis_5_8, Opponent>")

    // TODO(#12): This area neighbors the first city at Tharsis_4_1, but not the second city at
    // Tharsis_5_8. The current unlinked Neighbor<CityTile> accepts it.
    engine.doTask("GreeneryTile<Tharsis_3_1, Opponent>")
  }

  @Test
  fun `use-card-action incorrectly leaves its selected action card abstract`() {
    newGame()
    p1.manual("SymbioticFungus")
    val p1GodMode = p1.godMode().also { it.autoExecMode = NONE }

    p1GodMode.beginManual("UseAction1<UseCardActionSA>")
    val markerChoice =
        game.tasks
            .extract { it }
            .single { it.instruction.toString().startsWith("ActionUsedMarker<") }
    markerChoice.then.toString().startsWith("UseAction<") shouldBe true

    p1GodMode.doTask("ActionUsedMarker<SymbioticFungus>")

    val actionTasks =
        game.tasks.extract { it }.filter { it.instruction.toString().startsWith("UseAction<") }
    actionTasks.shouldHaveSize(1)
    withClue(actionTasks.single()) {
      // TODO(#12): The shared ActionCard dependency should specialize in the THEN tail too.
      actionTasks.single().instruction.toString().contains("ActionCard") shouldBe true
      actionTasks.single().instruction.toString().contains("SymbioticFungus") shouldBe false
    }
  }

  // FAQ: "Those actions are considered distinct actions, but within the action of playing Head
  // Start."
  @Test
  fun `Head Start incorrectly allows its two actions to interleave`() {
    newGame("PreludeExpansion,TurmoilCardPack,PromoCardPack")
    p1.phase("Prelude")
    p1.manual("4, 10 ProjectCard, PreludeCard, 10 Heat")

    p1.playPrelude("HeadStart") {
      p1.assertCounts(2 to "Steel", 24 to "Megacredit")
      doFirstTask("UseAction1<UseStandardProjectSA>")
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

    p1.manual("LocalHeatTrapping") {
      tasks.extract { it.whyPending }.shouldContainExactlyInAnyOrder("abstract")

      p1.prepareTask(tasks.ids().single())
      tasks.extract { it.whyPending }.shouldContainExactlyInAnyOrder("abstract")
      abort()
    }
  }

  // FAQ: "Draw 1 card for every 3 science tags you have, including this."
  @Test
  fun `Solar Probe can incorrectly lose its card draw if event cleanup is handled first`() {
    newGame(
        "ColoniesExpansion",
        colonyTiles = testColonyTiles(2),
    )
    val p1GodMode = p1.godMode()
    engine.phase("Action")
    p1GodMode.manual("TransNeptuneProbe, PhysicsComplex")

    p1.count("ScienceTag") shouldBe 2

    p1GodMode.autoExecMode = NONE
    p1GodMode.beginManual("SolarProbe") {
      doTask("ProjectCard") // player deserves a card! but....
      abort()
    }

    p1GodMode.beginManual("SolarProbe") {
      // The user really shouldn't even have the option to do this first
      doTask("PlayedEvent<Class<SolarProbe>> FROM SolarProbe")

      // Now they can't get their card
      shouldThrow<TaskException> { doTask("ProjectCard") }
    }
  }

  @Test
  fun `a quantified tile instruction incorrectly cannot be decomposed into placement choices`() {
    newGame()
    shouldThrow<IllegalArgumentException> { p1.manual("2 CityTile") }
  }

  @Test
  fun `Predators incorrectly throws IllegalArgumentException without a removable animal`() {
    newGame()
    p1.manual("Predators")
    engine.phase("Action")
    shouldThrow<IllegalArgumentException> { p1.cardAction1("Predators") }
  }

  @Test
  fun `Artificial Lake incorrectly throws IllegalArgumentException without an available area`() {
    newGame()
    engine.phase("Action")
    val landAreas =
        p1.list("LandArea").filterNot { it.toString() == "VolcanicArea" } + p1.list("VolcanicArea")
    p1.manual(
        "15, ProjectCard, 12 TemperatureStep, " + landAreas.joinToString { "GreeneryTile<$it>" }
    )

    shouldThrow<IllegalArgumentException> { p1.playProject("ArtificialLake", 15) }
  }
}
