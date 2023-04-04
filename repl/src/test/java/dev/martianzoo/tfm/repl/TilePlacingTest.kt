package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import org.junit.jupiter.api.Test

class TilePlacingTest {
  @Test
  fun citiesRepel() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))
    val eng = InteractiveSession(game)
    val p2 = eng.asPlayer(PLAYER2)

    eng.execute("ActionPhase")

    p2.execute("100")

    fun citySp() {
      p2.execute("Turn", "UseAction1<UseStandardProject>", "UseAction1<CitySP>")
    }

    citySp()
    p2.doTask("-25 THEN CityTile<M46> THEN PROD[1]")

    citySp()
    p2.doTask("-25 THEN CityTile<M44> THEN PROD[1]")

    citySp()

    // TODO this should not work
    p2.doTask("-25 THEN CityTile<M34> THEN PROD[1]")
  }
}
