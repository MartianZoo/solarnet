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

    kim.cardAction1(StJosephOfCupertinoMission) {
          kim.pay(5)
          doTask("Cathedral<CityTile<Anyone, Tharsis_4_2>>")
          stan.doTask("UseAction<CathedralOption, Action1>")
          stan.pay(2)
        }
        .expect("Cathedral, ProjectCard<Stan>")
  }

  @Test
  internal fun `Places a Cathedral on Capital's city`() {
    newTestGame()
    kim.exMachina("$StJosephOfCupertinoMission")
    stan.exMachina("$Capital, CapitalTile<$Capital, Tharsis_2_5>")

    kim.cardAction1(StJosephOfCupertinoMission) {
          kim.pay(5)
          doTask("Cathedral<CityTile<Anyone, Tharsis_2_5>>")
          // Decline paying 2 MC for the city owner's card draw.
          stan.declineTask()
        }
        .expect("Cathedral")
  }

  @Test
  internal fun `Steel can pay for a Cathedral`() {
    newTestGame()
    kim.exMachina("$StJosephOfCupertinoMission, 2 Steel, NormalCityTile<Kim, Tharsis_4_2>")

    kim.cardAction1(StJosephOfCupertinoMission) {
          kim.pay(mc = 1, steel = 2)
          doTask("Cathedral<CityTile<Anyone, Tharsis_4_2>>")
          // Decline paying 2 MC for the city owner's card draw.
          declineTask()
        }
        .expect("Cathedral, -2 Steel, -1 MC, 0 ProjectCard")
  }

  @Test
  internal fun `Places a Cathedral on a neutral solo city`() {
    newTestGame(playerCount = 1)
    kim.exMachina("$StJosephOfCupertinoMission")

    kim.cardAction1(StJosephOfCupertinoMission) {
          kim.pay(5)
          doTask("Cathedral<CityTile<Anyone, Tharsis_4_1>>")
        }
        .expect("Cathedral")
    game.isIdle() shouldBe true
  }
}
