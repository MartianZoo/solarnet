package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SoloGenerationCountdownTest {
  @Test
  fun baseSoloBeginsGenerationOneWithThirteenGenerationsRemaining() {
    val setup = dev.martianzoo.tfm.engine.canonicalPremise("SoloMode", 1)
    val game = Engine.newGame(setup)
    val checkpoint = game.timeline.checkpoint()

    TfmWorkflow.Manual(game).setupPhase()

    game.events
        .changesSince(checkpoint)
        .single { it.change.gaining?.toString() == "SoloGenerationsLeft" }
        .change
        .count shouldBe 14
    game.tfm(ENGINE).count("SoloGenerationsLeft") shouldBe 13
  }

  @Test
  fun preludeSoloBeginsGenerationOneWithElevenGenerationsRemaining() {
    val setup = canonicalPremise("SoloMode,PreludeExpansion", 1)
    val game = Engine.newGame(setup)

    TfmWorkflow.Manual(game).setupPhase()

    game.tfm(ENGINE).count("SoloGenerationsLeft") shouldBe 11
  }

  @Test
  fun laterGenerationsRemoveOneGenerationLeft() {
    val game = setUpGame("SoloMode", 1)
    val engine = game.tfm(ENGINE)
    finishNeutralSetup(engine)

    engine.godMode().manual("Generation")

    engine.count("SoloGenerationsLeft") shouldBe 12
  }

  @Test
  fun enteringTheFinalSoloGenerationRecordsIt() {
    val game = setUpGame("SoloMode", 1)
    val engine = game.tfm(ENGINE)
    finishNeutralSetup(engine)
    engine.godMode().sneak("-12 SoloGenerationsLeft")

    engine.godMode().manual("-SoloGenerationsLeft")

    engine.count("SoloGenerationsLeft") shouldBe 0
    engine.count("LastCall") shouldBe 1
  }

  private fun finishNeutralSetup(engine: TfmGameplay) {
    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")
    engine.doFirstTask("CityTile<Tharsis_2_2, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, Opponent>")
  }
}
