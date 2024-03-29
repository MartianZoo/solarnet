package dev.martianzoo.tfm.repl

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilePlacingTest {
  @Test
  fun citiesRepel() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    with(game.tfm(PLAYER2)) {
      phase("Action")
      godMode().manual("CityTile<M46>, CityTile<M44>, 25")
      assertThrows<NarrowingException> { stdProject("CitySP") { doTask("CityTile<M34>") } }
    }
  }

  @Test
  fun cantStack() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val p2 = game.tfm(PLAYER2)

    p2.godMode().manual("CityTile<M33>")
    assertThrows<LimitsException> { p2.godMode().manual("OceanTile<M33>!") }
  }

  @Test
  fun greeneryNextToOwned_notPossible() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    with(game.tfm(PLAYER1)) {
      godMode().sneak("100")
      phase("Action")
      stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_4_3>") }
      assertThrows<NarrowingException> {
        stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_7_5>") }
      }
      // Yer surrounded!
      game.tfm(PLAYER2).godMode().manual("GT<M32>, GT<M33>, GT<M42>, GT<M44>")

      // So now you should be able to do this, but oops, you can't. (#33)
      assertThrows<NarrowingException> {
        stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_7_5>") }
      }
    }
  }

  @Test
  fun greeneryNextToOwned_possible() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    with(game.tfm(PLAYER1)) {
      phase("Action")

      godMode().manual("666, CityTile<M86>") // shown as [] in comment below

      // try to fool it by having an opponent tile at the XX below
      godMode().manual("CityTile<P2, M67>")

      // Use the standard project so that the placement rule is in effect
      stdProject("GreenerySP") {
        fun checkCantPlaceGreenery(area: String) =
            assertThrows<NarrowingException>(area) { doTask("GreeneryTile<$area>") }

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

        val cp = game.timeline.checkpoint()
        doTask("GreeneryTile<M75>") // NW
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<M76>") // NE
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<M85>") // W
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<M87>") // E
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<M96>") // SW
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<M97>") // SE
      }
    }
  }
}
