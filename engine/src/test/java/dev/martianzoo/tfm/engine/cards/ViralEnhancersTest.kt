package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class ViralEnhancersTest {

  @Test
  fun test() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)

    fun TaskResult.expect(string: String) = TestHelpers.assertNetChanges(this, game, p1, string)

    p1.godMode().manual("100, 4 ProjectCard")
    engine.phase("Action")
    p1.playProject("ViralEnhancers", 9).expect("Plant")
    p1.playProject("IndustrialMicrobes", 12).expect("Plant")
    p1.playProject("NitriteReducingBacteria", 11) {
      doTask("Microbe<NitriteReducingBacteria>")
    }.expect("4 Microbe")

    p1.playProject("RegolithEaters", 13) {
      doTask("Plant")
      abort()
    }
    p1.playProject("RegolithEaters", 13) {
      doTask("Microbe<RegolithEaters>")
      abort()
    }

    // TODO it should not be allowing this!
    p1.playProject("RegolithEaters", 13) {
      doTask("Microbe<NitriteReducingBacteria>")
    }
  }
}
