package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
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
    val p1 = newGame("BMX", 2).tfm(PLAYER1)
    p1.phase("Action")
    p1.sneak("100, 5 ProjectCard, GreeneryTile<M42>")

    // TODO(#12): Repeated LandArea occurrences should specialize together and reject this move.
    p1.playProject("KaguyaTech", 10) { doTask("CityTile<M43> FROM GreeneryTile<M42>") }
        .expect("-GreeneryTile<M42>, CityTile<M43>")
  }

  @Test
  fun `solo setup can incorrectly link the second greenery to the first city`() {
    val setup = Canon.fromOptionCodes("BRMS", 1)
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
    val game = setUpGame("BM", 2)
    val p1 = game.tfm(PLAYER1)
    p1.godMode().sneak("SymbioticFungus")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.beginManual("UseAction1<UseCardActionSA>")
    val markerChoice =
        game.tasks
            .extract { it }
            .single { it.instruction.toString().startsWith("ActionUsedMarker<") }
    markerChoice.then.toString().startsWith("UseAction<") shouldBe true

    manual.doTask("ActionUsedMarker<SymbioticFungus>")

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
    val p1 = newGame("BMPTX", 2).tfm(PLAYER1)
    p1.phase("Prelude")
    p1.sneak("4, 10 ProjectCard, PreludeCard, 10 Heat")

    p1.playPrelude("HeadStart") {
      p1.assertCounts(2 to "Steel", 24 to "M")
      doFirstTask("UseAction1<UseStandardProjectSA>")
      doTask("UseAction1<ConvertHeatSA>")
      doTask("UseAction1<AquiferSP>")
      doTask("OceanTile<M55>")
    }
  }

  // FAQ: "If you do not have cards that hold those resources, you may still play the card and
  // ignore that effect."
  @Test
  fun `Local Heat Trapping incorrectly cannot discard its optional animal gain`() {
    val p1 = newGame(Canon.SIMPLE_GAME).tfm(PLAYER1)
    p1.sneak("6 Heat, 2 ProjectCard")

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
    val game = newGame(Canon.fromOptionCodes("BMRC", 2, testColonyTiles(2)))
    val p1 = game.tfm(PLAYER1)
    val manual = p1.godMode()
    p1.phase("Action")
    manual.manual("TransNeptuneProbe, PhysicsComplex, 100, 5 ProjectCard")

    p1.count("ScienceTag") shouldBe 2

    manual.autoExecMode = NONE
    manual.beginManual("SolarProbe") {
      doTask("ProjectCard") // player deserves a card! but....
      abort()
    }

    manual.beginManual("SolarProbe") {
      // The user really shouldn't even have the option to do this first
      doTask("PlayedEvent<Class<SolarProbe>> FROM SolarProbe")

      // Now they can't get their card
      shouldThrow<TaskException> { doTask("ProjectCard") }
    }
  }

  @Test
  fun `a quantified tile instruction incorrectly cannot be decomposed into placement choices`() {
    val p1 = newGame("BM", 2).tfm(PLAYER1)

    shouldThrow<AbstractException> { p1.manual("2 CityTile") }
  }
}
