package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NewPromoCardsTest : CardTest() {
  @Test
  fun `Solar Logistics draws for space events played by its owner and either opponent`() {
    newGame(PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p1.manual("$SolarLogistics")

    p1.manual("$ImportedGhg")
    p1.count("ProjectCard") shouldBe 1
    p2.manual("$TechnologyDemonstration")
    p1.count("ProjectCard") shouldBe 2
    p3.manual("$InterstellarColonyShip")

    p1.count("ProjectCard") shouldBe 3
  }

  @Test
  fun `Icy Impactors lets the first player choose an ocean placed by the card owner`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    val oceanArea = "Tharsis_2_6"
    p2.manual("$IcyImpactors, Asteroid<$IcyImpactors>")
    engine.phase("Action")
    p2.cardAction2(IcyImpactors) {
      p1.doTask("OceanTile<$oceanArea> BY Player2")
    }

    p2.count("TerraformRating") shouldBe 21
    p1.count("TerraformRating") shouldBe 20
    p2.count("ProjectCard") shouldBe 2
    p1.count("ProjectCard") shouldBe 0
  }

  @Test
  fun `Icy Impactors owner chooses their own ocean when they are first player`() {
    newGame(PromoCardPack)
    p1.manual("$IcyImpactors, Asteroid<$IcyImpactors>")
    engine.phase("Action")
    p1.cardAction2(IcyImpactors) {
      doTask("OceanTile<Tharsis_1_2> BY Player1")
    }

    p1.count("TerraformRating") shouldBe 21
  }

  @Test
  fun `Icy Impactors suspends its owner while a third-player start player chooses`() {
    newGame(PromoCardPack, players = 3)
    val p3 = game.tfm(PLAYER3)
    engine.manual("StartToken<Player3> FROM StartToken<Player1>")
    p1.manual("$IcyImpactors, Asteroid<$IcyImpactors>")
    engine.phase("Action")

    p1.cardAction2(IcyImpactors) {
      shouldThrow<TaskException> { p1.doTask("OceanTile<Tharsis_1_2> BY Player1") }
      p3.doTask("OceanTile<Tharsis_1_2> BY Player1")
    }

    p1.count("TerraformRating") shouldBe 21
    p3.count("TerraformRating") shouldBe 20
  }

  @Test
  fun `Floyd Continuum pays for every completed parameter`() {
    newGame(PromoCardPack, VenusNextExpansion)
    engine.phase("Action")
    val oceans = p1.list("WaterArea").take(9).joinToString { "OceanTile<$it>" }
    p1.manual("$FloydContinuum, 19 TemperatureStep, 14 OxygenStep, 15 VenusStep, $oceans")

    p1.cardAction1(FloydContinuum).expect("12 Megacredit")
  }

  @Test
  fun `Carbon Nanosystems graphene can pay for a space card`() {
    newGame(PromoCardPack)

    engine.phase("Action")
    p1.manual("25, 2 ProjectCard")

    p1.playProject(CarbonNanosystems, 14).expect("Graphene<$CarbonNanosystems>")

    p1.playProject(IcyImpactors, 11) {
          doTask("PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
        }
        .expect("-Graphene<$CarbonNanosystems>")
  }

  @Test
  fun `Martian Lumber Corporation plants can pay for a building card`() {
    newGame(PromoCardPack)

    engine.phase("Action")
    p1.manual("ProjectCard, $MartianLumberCorp, 2 Plant, 20")
    p1.playProject(Mine, 1) {
          doTask("-Plant! THEN -3 Owed<Class<Megacredit>>.")
        }
        .expect("-Plant")
  }

  @Test
  fun `Homeostasis Bureau lets each actor raise temperature`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$HomeostasisBureau")
    p1.count("Megacredit") shouldBe 0

    p2.manual("TemperatureStep")
    engine.manual("TemperatureStep")
    p1.count("Megacredit") shouldBe 0

    p1.manual("TemperatureStep").expect("3 Megacredit")
  }

  @Test
  fun `Kaguya Tech can replace a greenery with its city`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("10, ProjectCard, GreeneryTile<Tharsis_4_2>")
    p1.playProject(KaguyaTech, 10) {
          shouldThrow<NarrowingException> {
            doTask("CityTile<Tharsis_4_3> FROM GreeneryTile<Tharsis_4_2>")
          }
          doTask("CityTile<Tharsis_4_2> FROM GreeneryTile<Tharsis_4_2>")
        }
        .expect("-GreeneryTile<Tharsis_4_2>, CityTile<Tharsis_4_2>")
  }

  @Test
  fun `St Joseph of Cupertino Mission scores beside an opponent's city`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.autoExecMode = NONE
    p2.autoExecMode = NONE
    p1.manual("$StJosephOfCupertinoMission")
    p2.manual("CityTile<Player2, Tharsis_4_2>") { doTask("Plant") }

    p1.godMode().beginManual("Cathedral<CityTile<Player2, Tharsis_4_2>>")
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
    p1.manual("$RedShips, CityTile<Tharsis_1_3>, OceanTile<Tharsis_1_2>")
    p1.manual("Card067_SpecialTile<Tharsis_2_2>")

    p1.cardAction1(RedShips).expect("2 Megacredit")
  }
}
