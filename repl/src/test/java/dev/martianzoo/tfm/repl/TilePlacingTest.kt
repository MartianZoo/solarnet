package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.api.UserException.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.repl.TestHelpers.stdProject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilePlacingTest {
  @Test
  fun citiesRepel() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val eng = PlayerSession(game)
    val p2 = eng.asPlayer(PLAYER2)

    eng.execute("ActionPhase")
    p2.execute("100")
    p2.stdProject("CitySP", "-25 THEN CityTile<M46> THEN PROD[1]")
    p2.stdProject("CitySP", "-25 THEN CityTile<M44> THEN PROD[1]")
    p2.stdProject("CitySP")

    assertThrows<RequirementException> { p2.doTask("-25 THEN CityTile<M34> THEN PROD[1]") }
  }

  @Test
  fun cantStack() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val eng = PlayerSession(game)
    val p2 = eng.asPlayer(PLAYER2)

    p2.execute("CityTile<M33>")
    // TODO should throw LimitsException
    assertThrows<UserException> { p2.execute("OceanTile<M33>!") }
  }

  @Test
  fun greeneryNextToOwned() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val eng = PlayerSession(game)
    val p1 = eng.asPlayer(PLAYER1)
    val p2 = eng.asPlayer(PLAYER2)

    eng.execute("ActionPhase")
    p1.execute("150")

    p1.execute("CityTile<M86>") // can place this one by fiat

    // TODO currently an opponent tile will fool it, because Owner isn't getting replaced
    // p2.execute("CityTile<M67>")

    // Use the standard project so that the placement rule is in effect
    p1.stdProject("GreenerySP")

    fun checkCantPlaceGreenery(area: String) =
        assertThrows<RequirementException> { p1.doTask("-23 THEN GreeneryTile<$area>") }

    //     64  65  66  XX
    //   74  75  76  77
    // 84  85  **  87  88
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
    p1.doTask("-23 THEN GreeneryTile<M75>") // NW 1
    p1.game.rollBack(cp)
    p1.doTask("-23 THEN GreeneryTile<M76>") // NE 1
    p1.game.rollBack(cp)
    p1.doTask("-23 THEN GreeneryTile<M85>") // W 1
    p1.game.rollBack(cp)
    p1.doTask("-23 THEN GreeneryTile<M87>") // E 1
    p1.game.rollBack(cp)
    p1.doTask("-23 THEN GreeneryTile<M96>") // SW 1
    p1.game.rollBack(cp)
    p1.doTask("-23 THEN GreeneryTile<M97>") // SE 1
  }
}
