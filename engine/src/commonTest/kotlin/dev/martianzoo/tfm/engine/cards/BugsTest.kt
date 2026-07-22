package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
class BugsTest : CardTest() {
  @Test
  fun `Kaguya Tech can incorrectly move the selected greenery to another area`() {
    val p1 = newGame("BMX", 2).tfm(PLAYER1)
    p1.phase("Action")
    p1.sneak("100, 5 ProjectCard, GreeneryTile<M42>")

    // TODO: Repeated LandArea occurrences should specialize together and reject this move.
    p1.playProject("KaguyaTech", 10) { doTask("CityTile<M43> FROM GreeneryTile<M42>") }
        .expect("-GreeneryTile<M42>, CityTile<M43>")
  }

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

  @Test
  fun `Local Heat Trapping cannot discard its optional animal gain`() {
    val p1 = newGame(Canon.SIMPLE_GAME).tfm(PLAYER1)
    p1.sneak("6 Heat, 2 ProjectCard")

    p1.manual("LocalHeatTrapping") {
      tasks.extract { it.whyPending }.shouldContainExactlyInAnyOrder("abstract")

      p1.prepareTask(tasks.ids().single())
      tasks.extract { it.whyPending }.shouldContainExactlyInAnyOrder("abstract")
      abort()
    }
  }

  @Test
  fun `Solar Probe can lose its card draw if event cleanup is handled first`() {
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
}
