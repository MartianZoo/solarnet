package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NewPromoCardsTest : CardTest() {
  @Test
  fun `Floyd Continuum pays for every completed parameter`() {
    newGame(PromoCardPack, VenusNextExpansion)
    engine.phase("Action")
    p1.manual(
        "FloydContinuum, " +
            "GpComplete<Class<TemperatureStep>>, " +
            "GpComplete<Class<OxygenStep>>, " +
            "GpComplete<Class<OceanTile>>, " +
            "GpComplete<Class<VenusStep>>"
    )

    p1.cardAction1("FloydContinuum").expect("12 Megacredit")
  }

  @Test
  fun `with Carbon Nanosystems in hand, plays a space card`() {
    newGame(PromoCardPack)

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
    newGame(PromoCardPack)

    engine.phase("Action")
    p1.manual("ProjectCard, MartianLumberCorp, 2 Plant, 20")
    p1.playProject("Mine", 1) {
          doTask("-Plant! THEN -3 Owed.")
        }
        .expect("-Plant")
  }

  @Test
  fun `with Homeostasis Bureau, each actor raises temperature`() {
    newGame(PromoCardPack)
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
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("10, ProjectCard, GreeneryTile<Tharsis_4_2>")
    p1.playProject("KaguyaTech", 10) {
          doTask("CityTile<Tharsis_4_2> FROM GreeneryTile<Tharsis_4_2>")
        }
        .expect("-GreeneryTile<Tharsis_4_2>, CityTile<Tharsis_4_2>")
  }

  @Test
  fun `with a p2 city, p1 builds a cathedral`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.autoExecMode = NONE
    p2.autoExecMode = NONE

    p1.manual("StJosephOfCupertinoMission")
    p2.manual("CityTile<Player2, Tharsis_4_2>") { doTask("Plant") }

    p1.godMode().beginManual("Cathedral<CityTile<Player2, Tharsis_4_2>>") {
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    }

    p2.doTask("Ok")
    p1.autoExecMode = FIRST
    p2.autoExecMode = FIRST
    engine.phase("End")
    p1.assertCounts(21 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }

  @Test
  fun `Red Ships counts each city or special tile beside an ocean`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("RedShips, CityTile<Tharsis_1_3>, OceanTile<Tharsis_1_2>")
    p1.manual("MiningRightsTile<Tharsis_2_2>")

    p1.cardAction1("RedShips").expect("2 Megacredit")
  }
}
