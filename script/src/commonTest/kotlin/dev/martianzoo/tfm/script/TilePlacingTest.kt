package dev.martianzoo.tfm.script

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TilePlacingTest {
  @Test
  fun citiesRepel() {
    val game = setUpGame(Canon.SIMPLE_GAME)
    with(game.tfm(PLAYER2)) {
      phase("Action")
      godMode().manual("CityTile<M46>, CityTile<M44>, 25")
      assertFailsWith<NarrowingException> { stdProject("CitySP") { doTask("CityTile<M34>") } }
    }
  }

  @Test
  fun cantStack() {
    val game = setUpGame(Canon.SIMPLE_GAME)
    val p2 = game.tfm(PLAYER2)

    p2.godMode().manual("CityTile<M33>")
    assertFailsWith<LimitsException> { p2.godMode().manual("OceanTile<M33>!") }
  }

  @Test
  fun greeneryCanBePlacedAnywhereWhenOwnedTilesAreSurrounded() {
    val game = setUpGame(Canon.SIMPLE_GAME)

    with(game.tfm(PLAYER1)) {
      godMode().sneak("100")
      phase("Action")
      stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_4_3>") }
      assertFailsWith<NarrowingException> {
        stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_7_5>") }
      }
      // Yer surrounded!
      game.tfm(PLAYER2).godMode().manual("GT<M32>, GT<M33>, GT<M42>, GT<M44>")

      stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_7_5>") }
    }
  }

  @Test
  fun greeneryRequirementDoesntCareIfItDeadEndsYourTurn() {
    val game = setUpGame(Canon.fromOptionCodes("BH", 2))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    // P1 has greenery next to south pole
    p1.godMode().manual("4, GreeneryTile<H98>")

    // P2 completely surrounds it except for south pole (H97)
    p2.godMode().manual("GreeneryTile<H87>, GreeneryTile<H88>, GreeneryTile<H99>")

    // P1 is 2 money short of what they need to place on the south pole
    assertFailsWith<LimitsException> { // do we care which step fails?
      p1.godMode().manual("GreeneryTile") {
        doTask("GreeneryTile<H97>")
        doTask("OceanTile<H46>")
      }
    }
    assertEquals(0, p1.count("GreeneryTile<H97>")) // rolled back

    // But too bad, they don't get permission to place elsewhere!
    assertFailsWith<NarrowingException> {
      p1.godMode().manual("GreeneryTile") { doTask("GreeneryTile<H75>") }
    }

    // That concludes our test. But for funsies,
    // Suppose there had already been an ocean to place next to - now it works
    p2.godMode().manual("OceanTile<H56>")
    p1.godMode().manual("GreeneryTile") {
      doTask("GreeneryTile<H97>")
      doTask("OceanTile<H46>")
    }
    assertEquals(0, p1.count("Megacredit"))
    assertEquals(1, p1.count("GreeneryTile<H97>"))
  }

  @Test
  fun greeneryNextToOwned_possible() {
    val game = setUpGame(Canon.SIMPLE_GAME)

    with(game.tfm(PLAYER1)) {
      phase("Action")

      godMode().manual("666, CityTile<M86>") // shown as [] in comment below

      // try to fool it by having an opponent tile at the XX below
      godMode().manual("CityTile<P2, M67>")

      // Use the standard project so that the placement rule is in effect
      stdProject("GreenerySP") {
        fun checkCantPlaceGreenery(area: String) =
            assertFailsWith<NarrowingException>(area) { doTask("GreeneryTile<$area>") }

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
