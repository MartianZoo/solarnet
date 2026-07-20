package dev.martianzoo.tfm.engine.games

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.setUpGame
import kotlin.test.Test

class SoloSetupSpecializationCharacterizationTest {
  @Test
  fun `currently allows second greenery to neighbor only the first neutral city`() {
    val setup = Canon.fromOptionCodes("BRMS", 1)
    val game = setUpGame(setup)
    val engine = game.tfm(ENGINE)

    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")

    engine.doFirstTask("CityTile<Tharsis_5_8, Opponent>")

    // TODO(#12): This area neighbors the first city at Tharsis_4_1, but not the second city at
    // Tharsis_5_8. The current unlinked Neighbor<CityTile> accepts it; keep characterizing that
    // known-wrong behavior until linked specialization makes this operation fail.
    engine.doTask("GreeneryTile<Tharsis_3_1, Opponent>")
  }
}
