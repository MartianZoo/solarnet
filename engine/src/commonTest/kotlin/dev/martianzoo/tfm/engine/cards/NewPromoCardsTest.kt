package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NewPromoCardsTest : CardTest() {
  @Test
  fun carbonNanosystemsIncludesItsOwnScienceTagAndPaysForASpaceCard() {
    val game = newGame("BMX", 2)
    val p1 = game.tfm(PLAYER1)

    p1.phase("Action")
    p1.sneak("100, 5 ProjectCard")

    p1.playProject("CarbonNanosystems", 14).expect("Graphene<CarbonNanosystems>")

    p1.playProject("IcyImpactors", 11) {
          doTask("-Graphene<CarbonNanosystems>! THEN -4 Owed.")
        }
        .expect("-Graphene<CarbonNanosystems>")
  }

  @Test
  fun martianLumberCorpPaysThreeWithAPlantForABuildingCard() {
    val game = newGame("BMRX", 2)
    val p1 = game.tfm(PLAYER1)

    p1.phase("Action")
    p1.sneak("5 ProjectCard, MartianLumberCorp, 2 Plant, 20")
    p1.playProject("Mine", 1) {
          doTask("-Plant! THEN -3 Owed.")
        }
        .expect("-Plant")
  }

  @Test
  fun homeostasisBureauRewardsOnlyItsOwnerForRaisingTemperature() {
    val game = newGame("BMX", 2)
    val owner = game.tfm(PLAYER1)
    val other = game.tfm(PLAYER2)
    val engine = game.gameplay(ENGINE).godMode()
    owner.sneak("HomeostasisBureau")
    val moneyBefore = owner.count("Megacredit")

    other.manual("TemperatureStep")
    engine.manual("TemperatureStep")
    owner.count("Megacredit") shouldBe moneyBefore

    owner.manual("TemperatureStep").expect("3 Megacredit")
  }

  @Test
  fun kaguyaTechReplacesTheSelectedGreeneryInPlace() {
    val game = newGame("BMX", 2)
    val p1 = game.tfm(PLAYER1)

    p1.phase("Action")
    p1.sneak("100, 5 ProjectCard, GreeneryTile<M42>")

    p1.playProject("KaguyaTech", 10) { doTask("CityTile<M42> FROM GreeneryTile<M42>") }
        .expect("-GreeneryTile<M42>, CityTile<M42>")
  }

  @Test
  fun cathedralOfferBelongsToTheCityOwnerAndVpBelongsToTheBuilder() {
    val game = newGame("BMX", 2)
    val builder = game.tfm(PLAYER1).also { it.autoExecMode = NONE }
    val cityOwner = game.tfm(PLAYER2).also { it.autoExecMode = NONE }

    builder.sneak("StJosephOfCupertinoMission")
    cityOwner.manual("CityTile<Player2, M42>") { doTask("Plant") }

    builder.godMode().beginManual("Cathedral<CityTile<Player2, M42>>") {
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    }

    cityOwner.doTask("Ok")
    builder.autoExecMode = FIRST
    cityOwner.autoExecMode = FIRST
    game.tfm(ENGINE).phase("End")
    builder.assertCounts(21 to "VictoryPoint")
    cityOwner.assertCounts(20 to "VictoryPoint")
  }
}
