package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.OldTfmHelpers.phase
import dev.martianzoo.tfm.engine.OldTfmHelpers.stdProject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilePlacingTest {
  @Test
  fun citiesRepel() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    with(game.session(PLAYER2)) {
      phase("Action")
      operation("CityTile<M46>, CityTile<M44>, 25")
      assertThrows<NarrowingException> { stdProject("CitySP", "CityTile<M34>") }
    }
  }

  @Test
  fun cantStack() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val p2 = game.session(PLAYER2)

    p2.operation("CityTile<M33>")
    assertThrows<LimitsException> { p2.operation("OceanTile<M33>!") }
  }

  @Test
  fun greeneryNextToOwned() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    with(game.session(PLAYER1)) {
      phase("Action")

      operation("666, CityTile<M86>") // shown as [] in comment below
      operation("CityTile<P2, M67>") // try to fool it by having an opponent tile at the XX below

      // Use the standard project so that the placement rule is in effect
      stdProject("GreenerySP") {
        fun checkCantPlaceGreenery(area: String) =
            assertThrows<NarrowingException>(area) { task("GreeneryTile<$area>") }

        //     64  65  66  XX
        //   74  75  76  77
        // 84  85  []  87  88
        //   95  96  97  98

        // 2 away - should not work

        checkCantPlaceGreenery("M64") // NW
        checkCantPlaceGreenery("M65") // N
        checkCantPlaceGreenery("M66") // NE
        checkCantPlaceGreenery("M74") // WNW
        checkCantPlaceGreenery("M77") // ENE
        checkCantPlaceGreenery("M84") // W
        checkCantPlaceGreenery("M88") // E
        checkCantPlaceGreenery("M95") // WSW
        checkCantPlaceGreenery("M98") // ESE

        // 1 away - should work

        val cp = timeline.checkpoint()
        task("GreeneryTile<M75>") // NW
        timeline.rollBack(cp)
        task("GreeneryTile<M76>") // NE
        timeline.rollBack(cp)
        task("GreeneryTile<M85>") // W
        timeline.rollBack(cp)
        task("GreeneryTile<M87>") // E
        timeline.rollBack(cp)
        task("GreeneryTile<M96>") // SW
        timeline.rollBack(cp)
        task("GreeneryTile<M97>") // SE
      }
    }
  }
}
