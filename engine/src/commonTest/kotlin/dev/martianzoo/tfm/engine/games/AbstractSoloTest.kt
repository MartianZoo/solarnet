package dev.martianzoo.tfm.engine.games

import dev.martianzoo.engine.AutoExecMode
import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.canonicalPremise
import kotlin.test.BeforeTest

/** Follow-along solo fixtures driven by the engine-owned game workflow. */
abstract class AbstractSoloTest : AbstractFullGameTest() {
  protected lateinit var me: TfmGameplay
  protected lateinit var workflow: TfmWorkflow.Auto

  override fun setup() =
      canonicalPremise(
          HellasMapOption,
          VenusNextExpansion,
          PreludeExpansion,
          PromoCardPack,
          Tr63SoloVariant,
          players = 1,
      )

  protected abstract fun cityAreas(): Pair<String, String>

  protected abstract fun greeneryAreas(): Pair<String, String>

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()

    me = p1
    workflow = TfmWorkflow.Auto(game).launch()

    engine.doFirstTask("CityTile<${cityAreas().first}, SoloOpponent>")
    engine.doTask("GreeneryTile<${greeneryAreas().first}, SoloOpponent>")
    engine.doFirstTask("CityTile<${cityAreas().second}, SoloOpponent>")
    engine.doTask("GreeneryTile<${greeneryAreas().second}, SoloOpponent>")
  }

  protected fun nextRound(wgt: String, cardsBought: Int) {
    p1.pass()
    me.doTask("$wgt! BY Engine")
    p1.doTask(if (cardsBought > 0) "$cardsBought BuyCard" else "Ok")
  }

  /** Leaves the following workflow task unprepared while [body] makes a log correction. */
  protected fun <T> withAutoExecLoweredAfterOperation(
      mode: AutoExecMode,
      operation: (() -> Unit) -> T,
      body: () -> Unit,
  ): T {
    val previousMode = me.autoExecMode
    return try {
      val result = operation { me.autoExecMode = mode }
      body()
      result
    } finally {
      me.autoExecMode = previousMode
    }
  }
}
