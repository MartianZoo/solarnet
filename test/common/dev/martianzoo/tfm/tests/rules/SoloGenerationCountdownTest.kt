package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SoloGenerationCountdownTest {

  @Test
  internal fun laterGenerationsRemoveOneGenerationLeft() {
    val game = setUpGame(players = 1)
    val admin = game.tfm(ADMIN)
    finishNeutralSetup(admin)

    admin.manual("Generation")

    admin.count("SoloGenerationsLeft") shouldBe 12
  }

  @Test
  internal fun enteringTheFinalSoloGenerationRemovesTheLastGameEndBarrier() {
    val game = setUpGame(players = 1)
    val admin = game.tfm(ADMIN)
    finishNeutralSetup(admin)
    admin.sneak("-12 SoloGenerationsLeft")

    admin.manual("-SoloGenerationsLeft")

    admin.count("SoloGenerationsLeft") shouldBe 0
    admin.count("GameEndBarrier") shouldBe 0
  }

  @Test
  internal fun tr63SoloReplacesTheStandardObjectiveAndProvidesBufferGas() {
    val game = setUpGame(Tr63SoloObjective, players = 1)
    val admin = game.tfm(ADMIN)
    val player = game.tfm(PLAYER1)
    finishNeutralSetup(admin)

    player.count("Tr63SoloObjective") shouldBe 1
    player.count("StandardSoloObjective") shouldBe 0
    game.classTable.isActive(cn("BufferGasSP")) shouldBe true

    player.manual("16 MC")
    player.manual("UseAction<BufferGasSP, Action1>") {
      doTask("16 Pay<Class<MC>> FROM MC")
    }
    player.count("MC<Player1>") shouldBe 0
    player.count("TerraformRating<Player1>") shouldBe 15

    player.manual("48 TerraformRating")
    admin.manual("CheckGameEnd")

    player.count("Victory<Player1>") shouldBe 1
  }

  private fun finishNeutralSetup(admin: TfmGameplay) {
    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
  }
}
