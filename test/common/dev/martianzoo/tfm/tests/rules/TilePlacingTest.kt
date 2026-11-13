package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.tests.ExcludedTestOption
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Hellas
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.setUpGame
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TilePlacingTest : TfmSandboxTest() {
  @Test
  internal fun defaultGreeneryWrapperRunsWhilePlayerAutoexecIsOff() {
    game = setUpGame(ExcludedTestOption(CorporateEraExpansion))
    val p1 = agents[PLAYER1].also { it.autoExecPolicy = NONE }

    p1.runOperation("PlaceGreeneryTile") { doTask("GreeneryTile<Tharsis_3_3>") }

    assertEquals(1, p1.count("GreeneryTile<Tharsis_3_3>"))
  }

  @Test
  internal fun citiesRepel() {
    game = setUpGame(ExcludedTestOption(CorporateEraExpansion))
    with(agents[PLAYER2]) {
      admin.exMachina("ActionPhase FROM SetupPhase")
      runOperation("CityTile<Tharsis_4_6>, CityTile<Tharsis_4_4>, 25 MC")
      assertFailsWith<NarrowingException> {
        inTurn {
          doWithAutoExecPolicy(NONE) {
            doTasks(*standardProjectTasks("CityProject"), "25 Pay<> FROM MC")
          }
          autoExecNow()
          doTask("CityTile<Tharsis_3_4>")
        }
      }
    }
  }

  @Test
  internal fun cantStack() {
    game = setUpGame(ExcludedTestOption(CorporateEraExpansion))
    val p2 = agents[PLAYER2]

    p2.runOperation("CityTile<Tharsis_3_3>")
    assertFailsWith<LimitsException> { p2.runOperation("OceanTile<Tharsis_3_3>!") }
  }

  @Test
  internal fun greeneryCanBePlacedAnywhereWhenOwnedTilesAreSurrounded() {
    game = setUpGame(ExcludedTestOption(CorporateEraExpansion))

    with(agents[PLAYER1]) {
      exMachina("100 MC")
      admin.exMachina("ActionPhase FROM SetupPhase")
      inTurn {
        doWithAutoExecPolicy(NONE) {
          doTasks(*standardProjectTasks("GreeneryProject"), "23 Pay<> FROM MC")
        }
        autoExecNow()
        doTask("GreeneryTile<Tharsis_4_3>")
      }
      assertFailsWith<NarrowingException> {
        inTurn {
          doWithAutoExecPolicy(NONE) {
            doTasks(*standardProjectTasks("GreeneryProject"), "23 Pay<> FROM MC")
          }
          autoExecNow()
          doTask("GreeneryTile<Tharsis_7_5>")
        }
      }
      // Yer surrounded!
      agents[PLAYER2].runOperation(
          "GreeneryTile<Tharsis_3_2>, GreeneryTile<Tharsis_3_3>, " +
              "GreeneryTile<Tharsis_4_2>, GreeneryTile<Tharsis_4_4>"
      )

      inTurn {
        doWithAutoExecPolicy(NONE) {
          doTasks(*standardProjectTasks("GreeneryProject"), "23 Pay<> FROM MC")
        }
        autoExecNow()
        doTask("GreeneryTile<Tharsis_7_5>")
      }
    }
  }

  @Test
  internal fun greeneryRequirementDoesntCareIfItDeadEndsYourTurn() {
    game = setUpGame(Hellas, ExcludedTestOption(CorporateEraExpansion))
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]

    // Player1 has greenery next to south pole
    p1.runOperation("4 MC, GreeneryTile<Hellas_9_8>")

    // Player2 completely surrounds it except for south pole (Hellas_9_7)
    p2.runOperation("GreeneryTile<Hellas_8_7>, GreeneryTile<Hellas_8_8>, GreeneryTile<Hellas_9_9>")

    // Player1 is 2 money short of what they need to place on the south pole
    assertFailsWith<LimitsException> { // do we care which step fails?
      p1.runOperation("PlaceGreeneryTile") {
        doTask("GreeneryTile<Hellas_9_7>")
        doTask("OceanTile<Hellas_4_6>")
      }
    }
    assertEquals(0, p1.count("GreeneryTile<Hellas_9_7>")) // rolled back

    // But too bad, they don't get permission to place elsewhere!
    assertFailsWith<NarrowingException> {
      p1.runOperation("PlaceGreeneryTile") { doTask("GreeneryTile<Hellas_7_5>") }
    }

    // That concludes our test. But for funsies,
    // Suppose there had already been an ocean to place next to - now it works
    p2.runOperation("OceanTile<Hellas_5_6>")
    p1.runOperation("PlaceGreeneryTile") {
      doTask("GreeneryTile<Hellas_9_7>")
      doTask("OceanTile<Hellas_4_6>")
    }
    assertEquals(0, p1.count("MC"))
    assertEquals(1, p1.count("GreeneryTile<Hellas_9_7>"))
  }

  @Test
  internal fun greeneryNextToOwned_possible() {
    game = setUpGame(ExcludedTestOption(CorporateEraExpansion))

    with(agents[PLAYER1]) {
      admin.exMachina("ActionPhase FROM SetupPhase")

      runOperation("666 MC, CityTile<Tharsis_8_6>") // shown as [] in comment below

      // try to fool it by having an opponent tile at the XX below
      runOperation("CityTile<Player2, Tharsis_6_7>")

      // Use the standard project so that the placement rule is in effect.
      inTurn {
        doWithAutoExecPolicy(NONE) {
          doTasks(*standardProjectTasks("GreeneryProject"), "23 Pay<> FROM MC")
        }
        autoExecNow()

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

        val beforePlacement = game.timeline.checkpoint()
        for (area in
            listOf(
                "Tharsis_7_5",
                "Tharsis_7_6",
                "Tharsis_8_5",
                "Tharsis_8_7",
                "Tharsis_9_6",
                "Tharsis_9_7",
            )) {
          doTask("GreeneryTile<$area>")
          if (area != "Tharsis_9_7") {
            game.timeline.seek(beforePlacement)
            game.timeline.truncate()
          }
        }
      }
    }
  }
}
