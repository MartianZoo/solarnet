package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import kotlin.test.BeforeTest

/** Follow-along solo tests driven by the engine-owned game workflow. */
internal abstract class AbstractSoloTest : AbstractFullGameTest() {
  protected lateinit var me: TfmGameplay
    private set

  private lateinit var workflow: TfmWorkflow.Automatic

  protected abstract fun cityAreas(): Pair<String, String>

  protected abstract fun greeneryAreas(): Pair<String, String>

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()

    me = p1
    workflow = TfmWorkflow.Automatic(agents).launch()
    resolveExpansionSetupTasks()

    admin.doTask("CityTile<${cityAreas().first}, SoloOpponent>")
    admin.doTask("GreeneryTile<${greeneryAreas().first}, SoloOpponent>")
    admin.doTask("CityTile<${cityAreas().second}, SoloOpponent>")
    admin.doTask("GreeneryTile<${greeneryAreas().second}, SoloOpponent>")
  }

  protected open fun resolveExpansionSetupTasks() {}

  protected fun nextRound(worldGovernmentChoice: String, cardsBought: Int) {
    p1.pass()
    me.wgt(worldGovernmentChoice)
    p1.buyCards(cardsBought)
  }
}
