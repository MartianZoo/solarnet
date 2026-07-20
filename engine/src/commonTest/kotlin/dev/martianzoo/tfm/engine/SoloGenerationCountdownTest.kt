package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SoloGenerationCountdownTest {
  @Test
  fun baseSoloBeginsGenerationOneWithThirteenGenerationsLeft() {
    val setup = Canon.SIMPLE_SOLO_GAME
    val game = Engine.newGame(setup)
    val checkpoint = game.timeline.checkpoint()

    TfmWorkflow.Manual(game, setup).setupPhase()

    game.events
        .changesSince(checkpoint)
        .single { it.change.gaining?.toString() == "GenerationsLeft" }
        .change
        .count shouldBe 14
    game.tfm(ENGINE).count("GenerationsLeft") shouldBe 13
  }

  @Test
  fun preludeSoloBeginsGenerationOneWithElevenGenerationsLeft() {
    val setup = Canon.fromOptionCodes("BSPM", 1)
    val game = Engine.newGame(setup)

    TfmWorkflow.Manual(game, setup).setupPhase()

    game.tfm(ENGINE).count("GenerationsLeft") shouldBe 11
  }

  @Test
  fun laterGenerationsRemoveOneGenerationLeft() {
    val game = setUpGame(Canon.SIMPLE_SOLO_GAME)
    val engine = game.tfm(ENGINE)
    finishNeutralSetup(engine)

    engine.godMode().manual("Generation")

    engine.count("GenerationsLeft") shouldBe 12
  }

  private fun finishNeutralSetup(engine: TfmGameplay) {
    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")
    engine.doFirstTask("CityTile<Tharsis_2_2, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, Opponent>")
  }
}
