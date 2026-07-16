package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

class SolarProbeTest : CardTest() {
  @Test
  fun `its own science tag counts while the event is being played`() {
    val game = newGame(GameSetup(Canon, "BMRC", 2))
    val p1 = game.tfm(PLAYER1)
    with(p1) {
      phase("Action")
      godMode()
          .manual("TransNeptuneProbe, PhysicsComplex, 100, 5 ProjectCard")
          .expect("2 ScienceTag")
    }

    // Characterization: event cards currently enqueue their move to PlayedEvent alongside their
    // immediate behavior. Use NONE so this test can choose the rules-correct order explicitly.
    val manual = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    manual
        .beginManual("SolarProbe") {
          tasks
              .extract { it.instruction.toString() }
              .shouldContainExactlyInAnyOrder(
                  "ProjectCard<Player1>! / 3 ScienceTag<Player1>",
                  "PlayedEvent<Player1, Class<SolarProbe>> FROM SolarProbe<Player1>!",
              )
          doTask("ProjectCard")
          doTask("PlayedEvent<Class<SolarProbe>> FROM SolarProbe")
        }
        .expect("ProjectCard, PlayedEvent")
  }
}
