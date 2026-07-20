package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class ViralEnhancersTest : CardTest() {

  @Test
  fun `characterizes current resource target choices`() {
    val game = newGame("BRM", 2)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)

    p1.sneak("100, 4 ProjectCard")
    engine.phase("Action")
    p1.playProject("ViralEnhancers", 9).expect("Plant")
    p1.playProject("IndustrialMicrobes", 12).expect("Plant")
    p1.playProject("NitriteReducingBacteria", 11) { doTask("Microbe<NitriteReducingBacteria>") }
        .expect("4 Microbe")

    p1.playProject("RegolithEaters", 13) {
      doTask("Plant")
      abort()
    }
    p1.playProject("RegolithEaters", 13) {
      doTask("Microbe<RegolithEaters>")
      abort()
    }

    // TODO(#12): The printed card says the resource goes on "that card", so this should be
    // rejected. Keep the successful different-card choice as a known-wrong characterization: when
    // trigger-to-instruction specialization is linked, this test should fail until it is updated.
    p1.playProject("RegolithEaters", 13) { doTask("Microbe<NitriteReducingBacteria>") }
  }
}
