package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
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
    val engine = game.tfm(ENGINE)
    finishNeutralSetup(engine)

    engine.godMode().manual("Generation")

    engine.count("SoloGenerationsLeft") shouldBe 12
  }

  @Test
  internal fun enteringTheFinalSoloGenerationRemovesTheLastGameEndBarrier() {
    val game = setUpGame(players = 1)
    val engine = game.tfm(ENGINE)
    finishNeutralSetup(engine)
    engine.godMode().sneak("-12 SoloGenerationsLeft")

    engine.godMode().manual("-SoloGenerationsLeft")

    engine.count("SoloGenerationsLeft") shouldBe 0
    engine.count("GameEndBarrier") shouldBe 0
  }

  @Test
  internal fun tr63SoloReplacesTheStandardObjectiveAndProvidesBufferGas() {
    val game = setUpGame(Tr63SoloVariant, players = 1)
    val engine = game.tfm(ENGINE)
    val player = game.tfm(PLAYER1)
    finishNeutralSetup(engine)

    player.count("Tr63SoloVariant") shouldBe 1
    player.count("StandardSoloVariant") shouldBe 0
    game.classTable.isActive(cn("BufferGasSP")) shouldBe true

    player.godMode().manual("16 MC")
    player.godMode().manual("UseAction<BufferGasSP, First>") {
      doTask("16 Pay<Class<MC>> FROM MC")
    }
    player.count("MC<Me>") shouldBe 0
    player.count("TerraformRating<Me>") shouldBe 15

    player.godMode().manual("48 TerraformRating")
    engine.godMode().manual("SoloVictoryCheck")

    player.count("Victory<Me>") shouldBe 1
  }

  private fun finishNeutralSetup(engine: TfmGameplay) {
    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
  }
}
