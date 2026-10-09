package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StJosephOfCupertinoMissionTest : CardTest() {
  @Test
  internal fun `Places a Cathedral on an opponent's normal city`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.runOperation("12 MC, ProjectCard")
    p2.runOperation("2 MC, CityTile<Player2, Tharsis_4_2>")
    admin.phase("Action")

    p1.playProject(StJosephOfCupertinoMission, 7)
    p1.cardAction1(StJosephOfCupertinoMission) {
          p1.pay(5)
          doTask("Cathedral<CityTile<Anyone, Tharsis_4_2>>")
          p2.doTask("UseAction<CathedralOption, Action1>")
          p2.pay(2)
        }
        .expect("Cathedral, ProjectCard<Player2>")
  }

  @Test
  internal fun `Places a Cathedral on Capital's city`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.runOperation("12 MC, ProjectCard")
    p2.runOperation("PROD[2 Energy]")
    p2.runOperation("$Capital") { placeTile(2, 5) }
    admin.phase("Action")

    p1.playProject(StJosephOfCupertinoMission, 7)
    p1.cardAction1(StJosephOfCupertinoMission) {
          p1.pay(5)
          doTask("Cathedral<CityTile<Anyone, Tharsis_2_5>>")
          // Decline paying 2 MC for the city owner's card draw.
          p2.declineTask()
        }
        .expect("Cathedral")
  }

  @Test
  internal fun `Steel can pay for a Cathedral`() {
    newGame(PromoCardPack)
    p1.runOperation("14 MC, ProjectCard, 2 Steel, CityTile<Player1, Tharsis_4_2>")
    admin.phase("Action")

    p1.playProject(StJosephOfCupertinoMission, 7)
    p1.cardAction1(StJosephOfCupertinoMission) {
          p1.pay(mc = 1, steel = 2)
          doTask("Cathedral<CityTile<Anyone, Tharsis_4_2>>")
          // Decline paying 2 MC for the city owner's card draw.
          declineTask()
        }
        .expect("Cathedral, -2 Steel, -1 MC, 0 ProjectCard")
  }

  @Test
  internal fun `Places a Cathedral on a neutral solo city`() {
    newGame(PromoCardPack, players = 1)
    p1.runOperation("12 MC, ProjectCard")
    admin.phase("Action")

    p1.playProject(StJosephOfCupertinoMission, 7)
    p1.cardAction1(StJosephOfCupertinoMission) {
          p1.pay(5)
          doTask("Cathedral<CityTile<Anyone, Tharsis_4_1>>")
        }
        .expect("Cathedral")
    game.isIdle() shouldBe true
  }
}
