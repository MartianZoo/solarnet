package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StJosephOfCupertinoMissionTest : TfmSandboxTest() {
  @Test
  internal fun `Places a Cathedral on an opponent's normal city`() {
    newTestGame()
    kim.exMachina("$StJosephOfCupertinoMission")
    stan.exMachina("NormalCityTile<Stan, Tharsis_4_2>")

    kim.inTurn {
      doTasks("UseCardAction<StJosephOfCupertinoMission, Action1>", "-5 MC")
      doTasks("Ok" /* No Steel payment. */, "Cathedral<CityTile<Anyone, Tharsis_4_2>>")
      doTask("CathedralOffer<Stan>")
      stan.doTasks("UseAction<CathedralOption, Action1>")
      stan.doTasks("-2 MC")
      stan.doTasks("ProjectCard")
    }
  }

  @Test
  internal fun `Places a Cathedral on Capital's city`() {
    newTestGame()
    kim.exMachina("$StJosephOfCupertinoMission")
    stan.exMachina("$Capital, CapitalTile<$Capital, Tharsis_2_5>")

    kim.inTurn {
      // Decline paying 2 MC for the city owner's card draw.
      doTasks("UseCardAction<StJosephOfCupertinoMission, Action1>", "-5 MC")
      doTasks("Ok" /* No Steel payment. */, "Cathedral<CityTile<Anyone, Tharsis_2_5>>")
      doTask("CathedralOffer<Stan>")
      stan.doTask("0 UseAction<CathedralOption>")
    }
  }

  @Test
  internal fun `Steel can pay for a Cathedral`() {
    newTestGame()
    kim.exMachina("$StJosephOfCupertinoMission, 2 Steel, NormalCityTile<Kim, Tharsis_4_2>")

    kim.inTurn {
          // Decline paying 2 MC for the city owner's card draw.
          doTasks(
              "UseCardAction<StJosephOfCupertinoMission, Action1>",
              "-2 Steel",
              "-1 MC",
              "Cathedral<CityTile<Anyone, Tharsis_4_2>>",
          )
          doTask("CathedralOffer")
          doTask("0 UseAction<CathedralOption>")
        }
        .expect("0 ProjectCard")
  }

  @Test
  internal fun `Places a Cathedral on a neutral solo city`() {
    newTestGame(playerCount = 1)
    kim.exMachina("$StJosephOfCupertinoMission")

    kim.inTurn {
      doTasks("UseCardAction<StJosephOfCupertinoMission, Action1>", "-5 MC")
      doTasks("Ok" /* No Steel payment. */, "Cathedral<CityTile<Anyone, Tharsis_4_1>>")
      doTask("CathedralOffer<SoloOpponent>")
    }

    game.isIdle() shouldBe true
  }
}
