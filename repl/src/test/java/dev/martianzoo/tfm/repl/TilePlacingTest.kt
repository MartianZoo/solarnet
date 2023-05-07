package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.UserException.LimitsException
import dev.martianzoo.tfm.api.UserException.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Humanizing.startTurn
import dev.martianzoo.tfm.engine.Humanizing.stdProject
import dev.martianzoo.tfm.repl.TestHelpers.action
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilePlacingTest {
  @Test
  fun citiesRepel() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val eng = game.asPlayer(Player.ENGINE).session()
    val p2 = eng.asPlayer(PLAYER2)

    eng.execute("ActionPhase")
    p2.execute("100")

    p2.stdProject("CitySP", "CityTile<M46>")
    assertThat(p2.agent.tasks()).isEmpty()
    p2.stdProject("CitySP", "CityTile<M44>")
    p2.startTurn("UseAction1<UseStandardProject>", "UseAction1<CitySP>")
    assertThrows<NarrowingException> { p2.doFirstTask("CityTile<M34>") }
  }

  @Test
  fun cantStack() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val eng = game.asPlayer(Player.ENGINE).session()
    val p2 = eng.asPlayer(PLAYER2)

    p2.execute("CityTile<M33>")
    assertThrows<LimitsException> { p2.execute("OceanTile<M33>!") }
    assertThat(p2.agent.tasks()).isEmpty()
  }

  @Test
  fun greeneryNextToOwned() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val eng = game.asPlayer(Player.ENGINE).session()
    val p1 = eng.asPlayer(PLAYER1)
    val p2 = eng.asPlayer(PLAYER2)

    p1.action("666, CityTile<M86>") {} // shown as [] in comment below
    p2.action("CityTile<M67>") {} // try to fool it by having an opponent tile at the XX below

    // Use the standard project so that the placement rule is in effect
    p1.action("UseAction1<GreenerySP>") {
      fun checkCantPlaceGreenery(area: String) =
          assertThrows<NarrowingException>(area) {
            doFirstTask("GreeneryTile<$area>")
          }

      //     64  65  66  XX
      //   74  75  76  77
      // 84  85  []  87  88
      //   95  96  97  98

      checkCantPlaceGreenery("M64") // NW 2
      checkCantPlaceGreenery("M65") // N 2
      checkCantPlaceGreenery("M66") // NE 2
      checkCantPlaceGreenery("M74") // NW then W
      checkCantPlaceGreenery("M77") // NE then E
      checkCantPlaceGreenery("M84") // W 2
      checkCantPlaceGreenery("M88") // E 2
      checkCantPlaceGreenery("M95") // SW then W
      checkCantPlaceGreenery("M98") // SW then E

      val cp = p1.game.checkpoint()
      doFirstTask("GreeneryTile<M75>") // NW 1
      p1.game.rollBack(cp)
      doFirstTask("GreeneryTile<M76>") // NE 1
      p1.game.rollBack(cp)
      doFirstTask("GreeneryTile<M85>") // W 1
      p1.game.rollBack(cp)
      doFirstTask("GreeneryTile<M87>") // E 1
      p1.game.rollBack(cp)
      doFirstTask("GreeneryTile<M96>") // SW 1
      p1.game.rollBack(cp)
      doFirstTask("GreeneryTile<M97>") // SE 1
      p1.mustDrain()
    }
  }
}
