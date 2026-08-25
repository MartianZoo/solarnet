package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.engine.AutoExecMode
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import kotlin.test.BeforeTest

/** Follow-along solo tests driven by the engine-owned game workflow. */
internal abstract class AbstractSoloTest : CardTrackingFullGameTest() {
  protected lateinit var me: TfmGameplay
  private lateinit var workflow: TfmWorkflow.Auto

  protected abstract fun cityAreas(): Pair<String, String>

  protected abstract fun greeneryAreas(): Pair<String, String>

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()

    me = p1
    workflow = TfmWorkflow.Auto(game).launch()

    engine.doTask("CityTile<${cityAreas().first}, SoloOpponent>")
    engine.doTask("GreeneryTile<${greeneryAreas().first}, SoloOpponent>")
    engine.doTask("CityTile<${cityAreas().second}, SoloOpponent>")
    engine.doTask("GreeneryTile<${greeneryAreas().second}, SoloOpponent>")
  }

  protected fun nextRound(worldGovernmentChoice: String, cardsBought: Int) {
    p1.pass()
    me.wgt(worldGovernmentChoice)
    p1.buyCards(cardsBought)
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
