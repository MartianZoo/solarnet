package dev.martianzoo.tfm.script

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TilePlacingTest {
  @Test
  internal fun citiesRepel() {
    val game = setUpGame()
    with(game.testTfm(PLAYER2)) {
      phase("Action")
      runOperation("CityTile<Tharsis_4_6>, CityTile<Tharsis_4_4>, 25 MC")
      assertFailsWith<NarrowingException> {
        stdProject("CityProject") { doTask("CityTile<Tharsis_3_4>") }
      }
    }
  }

  @Test
  internal fun cantStack() {
    val game = setUpGame()
    val p2 = game.testTfm(PLAYER2)

    p2.runOperation("CityTile<Tharsis_3_3>")
    assertFailsWith<LimitsException> { p2.runOperation("OceanTile<Tharsis_3_3>!") }
  }

  @Test
  internal fun greeneryCanBePlacedAnywhereWhenOwnedTilesAreSurrounded() {
    val game = setUpGame()

    with(game.testTfm(PLAYER1)) {
      sneak("100 MC")
      phase("Action")
      stdProject("GreeneryProject") { doTask("GreeneryTile<Tharsis_4_3>") }
      assertFailsWith<NarrowingException> {
        stdProject("GreeneryProject") { doTask("GreeneryTile<Tharsis_7_5>") }
      }
      // Yer surrounded!
      game
          .testTfm(PLAYER2)
          .runOperation(
              "GreeneryTile<Tharsis_3_2>, GreeneryTile<Tharsis_3_3>, " +
                  "GreeneryTile<Tharsis_4_2>, GreeneryTile<Tharsis_4_4>"
          )

      stdProject("GreeneryProject") { doTask("GreeneryTile<Tharsis_7_5>") }
    }
  }

  @Test
  internal fun greeneryRequirementDoesntCareIfItDeadEndsYourTurn() {
    val game = setUpGame("BH", 2)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)

    // Player1 has greenery next to south pole
    p1.runOperation("4 MC, GreeneryTile<Hellas_9_8>")

    // Player2 completely surrounds it except for south pole (Hellas_9_7)
    p2.runOperation("GreeneryTile<Hellas_8_7>, GreeneryTile<Hellas_8_8>, GreeneryTile<Hellas_9_9>")

    // Player1 is 2 money short of what they need to place on the south pole
    assertFailsWith<LimitsException> { // do we care which step fails?
      p1.runOperation("DefaultGreeneryTile") {
        doTask("GreeneryTile<Hellas_9_7>")
        doTask("OceanTile<Hellas_4_6>")
      }
    }
    assertEquals(0, p1.count("GreeneryTile<Hellas_9_7>")) // rolled back

    // But too bad, they don't get permission to place elsewhere!
    assertFailsWith<NarrowingException> {
      p1.runOperation("DefaultGreeneryTile") { doTask("GreeneryTile<Hellas_7_5>") }
    }

    // That concludes our test. But for funsies,
    // Suppose there had already been an ocean to place next to - now it works
    p2.runOperation("OceanTile<Hellas_5_6>")
    p1.runOperation("DefaultGreeneryTile") {
      doTask("GreeneryTile<Hellas_9_7>")
      doTask("OceanTile<Hellas_4_6>")
    }
    assertEquals(0, p1.count("MC"))
    assertEquals(1, p1.count("GreeneryTile<Hellas_9_7>"))
  }

  @Test
  internal fun greeneryNextToOwned_possible() {
    val game = setUpGame()

    with(game.testTfm(PLAYER1)) {
      phase("Action")

      runOperation("666 MC, CityTile<Tharsis_8_6>") // shown as [] in comment below

      // try to fool it by having an opponent tile at the XX below
      runOperation("CityTile<Player2, Tharsis_6_7>")

      // Use the standard project so that the placement rule is in effect
      stdProject("GreeneryProject") {
        fun checkCantPlaceGreenery(area: String) =
            assertFailsWith<NarrowingException>(area) { doTask("GreeneryTile<$area>") }

        //     64  65  66  XX
        //   74  75  76  77
        // 84  85  []  87  88
        //   95  96  97  98

        // 2 away - should not work

        checkCantPlaceGreenery("Tharsis_6_4") // NW
        checkCantPlaceGreenery("Tharsis_6_5") // N
        checkCantPlaceGreenery("Tharsis_6_6") // NE
        checkCantPlaceGreenery("Tharsis_7_4") // WNW
        checkCantPlaceGreenery("Tharsis_7_7") // ENE
        checkCantPlaceGreenery("Tharsis_8_4") // W
        checkCantPlaceGreenery("Tharsis_8_8") // E
        checkCantPlaceGreenery("Tharsis_9_5") // WSW
        checkCantPlaceGreenery("Tharsis_9_8") // ESE

        // 1 away - should work

        val cp = game.timeline.checkpoint()
        doTask("GreeneryTile<Tharsis_7_5>") // NW
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<Tharsis_7_6>") // NE
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<Tharsis_8_5>") // W
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<Tharsis_8_7>") // E
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<Tharsis_9_6>") // SW
        game.timeline.rollBack(cp)
        doTask("GreeneryTile<Tharsis_9_7>") // SE
      }
    }
  }
}
