package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NewPromoCardsTest : CardTest() {
  @Test
  fun `with Carbon Nanosystems in hand, plays a space card`() {
    newGame("BMX")

    engine.phase("Action")
    p1.manual("25, 2 ProjectCard")

    p1.playProject("CarbonNanosystems", 14).expect("Graphene<CarbonNanosystems>")

    p1.playProject("IcyImpactors", 11) {
          doTask("-Graphene<CarbonNanosystems>! THEN -4 Owed.")
        }
        .expect("-Graphene<CarbonNanosystems>")
  }

  @Test
  fun `with Martian Lumber Corporation, plays a building card`() {
    newGame("BMRX")

    engine.phase("Action")
    p1.manual("ProjectCard, MartianLumberCorp, 2 Plant, 20")
    p1.playProject("Mine", 1) {
          doTask("-Plant! THEN -3 Owed.")
        }
        .expect("-Plant")
  }

  @Test
  fun `with Homeostasis Bureau, each actor raises temperature`() {
    newGame("BMX")
    val p2 = requireP2()
    p1.manual("HomeostasisBureau")
    p1.count("Megacredit") shouldBe 0

    p2.manual("TemperatureStep")
    engine.manual("TemperatureStep")
    p1.count("Megacredit") shouldBe 0

    p1.manual("TemperatureStep").expect("3 Megacredit")
  }

  @Test
  fun `with a greenery selected, plays Kaguya Tech`() {
    newGame("BMX")
    engine.phase("Action")
    p1.manual("10, ProjectCard, GreeneryTile<M42>")
    p1.playProject("KaguyaTech", 10) { doTask("CityTile<M42> FROM GreeneryTile<M42>") }
        .expect("-GreeneryTile<M42>, CityTile<M42>")
  }

  @Test
  fun `with a p2 city, p1 builds a cathedral`() {
    newGame("BMX")
    val p2 = requireP2()
    p1.autoExecMode = NONE
    p2.autoExecMode = NONE

    p1.manual("StJosephOfCupertinoMission")
    p2.manual("CityTile<Player2, M42>") { doTask("Plant") }

    p1.godMode().beginManual("Cathedral<CityTile<Player2, M42>>") {
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    }

    p2.doTask("Ok")
    p1.autoExecMode = FIRST
    p2.autoExecMode = FIRST
    engine.phase("End")
    p1.assertCounts(21 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }
}
