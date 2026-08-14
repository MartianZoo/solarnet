package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SoloGenerationCountdownTest {
  @Test
  fun baseSoloBeginsGenerationOneWithThirteenGenerationsRemaining() {
    val setup = canonicalPremise(players = 1)
    val game = Engine.newGame(setup)
    val checkpoint = game.timeline.checkpoint()

    TfmWorkflow.Manual(game).setupPhase()

    game.classTable.getClass(cn("BufferGasSP")).phantom shouldBe true

    game.events
        .changesSince(checkpoint)
        .single { it.change.gaining?.toString() == "SoloGenerationsLeft" }
        .change
        .count shouldBe 14
    game.tfm(ENGINE).count("SoloGenerationsLeft") shouldBe 13
  }

  @Test
  fun preludeSoloBeginsGenerationOneWithElevenGenerationsRemaining() {
    val setup = canonicalPremise(PreludeExpansion, players = 1)
    val game = Engine.newGame(setup)

    TfmWorkflow.Manual(game).setupPhase()

    game.tfm(ENGINE).count("SoloGenerationsLeft") shouldBe 11
  }

  @Test
  fun laterGenerationsRemoveOneGenerationLeft() {
    val game = setUpGame(players = 1)
    val engine = game.tfm(ENGINE)
    finishNeutralSetup(engine)

    engine.godMode().manual("Generation")

    engine.count("SoloGenerationsLeft") shouldBe 12
  }

  @Test
  fun enteringTheFinalSoloGenerationRecordsIt() {
    val game = setUpGame(players = 1)
    val engine = game.tfm(ENGINE)
    finishNeutralSetup(engine)
    engine.godMode().sneak("-12 SoloGenerationsLeft")

    engine.godMode().manual("-SoloGenerationsLeft")

    engine.count("SoloGenerationsLeft") shouldBe 0
    engine.count("LastCall") shouldBe 1
  }

  @Test
  fun tr63SoloReplacesTheStandardObjectiveAndProvidesBufferGas() {
    val game = setUpGame(Tr63SoloVariant, players = 1)
    val engine = game.tfm(ENGINE)
    val player = game.tfm(PLAYER1)
    finishNeutralSetup(engine)

    player.count("Tr63SoloVariant") shouldBe 1
    player.count("StandardSoloVariant") shouldBe 0
    game.classTable.getClass(cn("BufferGasSP")).phantom shouldBe false

    player.godMode().manual("16 Megacredit")
    player.godMode().manual("UseAction1<BufferGasSP>")
    player.count("Megacredit<Player1>") shouldBe 0
    player.count("TerraformRating<Player1>") shouldBe 15

    player.godMode().manual("48 TerraformRating")
    engine.godMode().manual("SoloVictoryCheck")

    player.count("Victory<Player1>") shouldBe 1
  }

  @Test
  fun tr63SoloWithVenusDoesNotWinFromCompletedGlobalParametersBelow63Tr() {
    val game = setUpGame(VenusNextExpansion, Tr63SoloVariant, players = 1)
    val engine = game.tfm(ENGINE)
    val player = game.tfm(PLAYER1)
    finishNeutralSetup(engine)

    player.count("StandardSoloVariant") shouldBe 0
    player.count("Tr63SoloVariant") shouldBe 1
    engine
        .godMode()
        .manual(
            "GpComplete<Class<TemperatureStep>>, " +
                "GpComplete<Class<OxygenStep>>, " +
                "GpComplete<Class<OceanTile>>, " +
                "GpComplete<Class<VenusStep>>, SoloVictoryCheck"
        )

    player.count("TerraformRating<Player1>") shouldBe 14
    player.count("Victory<Player1>") shouldBe 0
  }

  private fun finishNeutralSetup(engine: TfmGameplay) {
    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
  }
}
