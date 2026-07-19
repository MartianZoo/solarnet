package dev.martianzoo.tfm.engine.games

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import kotlin.test.BeforeTest

/**
 * Follow-along solo fixtures intentionally drive phases manually and inject the app's
 * world-government action between generations, which [TfmWorkflow.Auto] does not support yet.
 */
abstract class AbstractSoloTest : AbstractFullGameTest() {
  protected lateinit var me: TfmGameplay

  override fun setup() = GameSetup(Canon, "BRHVPX", 1)

  protected abstract fun cityAreas(): Pair<String, String>

  protected abstract fun greeneryAreas(): Pair<String, String>

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()

    me = p1

    engine.doFirstTask("CityTile<${cityAreas().first}, Opponent>")
    engine.doTask("GreeneryTile<${greeneryAreas().first}, Opponent>")
    engine.doFirstTask("CityTile<${cityAreas().second}, Opponent>")
    engine.doTask("GreeneryTile<${greeneryAreas().second}, Opponent>")

    engine.phase("Corporation")
  }

  protected fun nextRound(wgt: String, cardsBought: Int) {
    p1.pass()
    engine.godMode().manual(wgt)
    engine.nextGeneration(cardsBought)
  }
}
