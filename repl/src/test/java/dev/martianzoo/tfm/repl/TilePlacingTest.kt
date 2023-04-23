package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.InteractiveSession
import dev.martianzoo.tfm.repl.TestHelpers.stdProject
import org.junit.jupiter.api.Test

class TilePlacingTest {
  @Test
  fun citiesRepel() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val eng = InteractiveSession(game)
    val p2 = eng.asPlayer(PLAYER2)

    eng.execute("ActionPhase")
    p2.execute("100")
    p2.stdProject("CitySP", "-25 THEN CityTile<M46> THEN PROD[1]")
    p2.stdProject("CitySP", "-25 THEN CityTile<M44> THEN PROD[1]")
    p2.stdProject("CitySP")

    // TODO this should not work
    p2.doTask("-25 THEN CityTile<M34> THEN PROD[1]")
  }
}
