package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.tfm.engine.TfmGameplay
import kotlin.test.BeforeTest

/** Follow-along solo tests driven by the engine-owned game workflow. */
internal abstract class AbstractSoloTest : AbstractFullGameTest() {
  protected lateinit var me: TfmGameplay
    private set

  protected abstract fun cityAreas(): Pair<String, String>

  protected abstract fun greeneryAreas(): Pair<String, String>

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()

    me = p1
    agents[ADMIN].beginOperation("WorkflowStarted")
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

  /** Leaves the following workflow task unselected while [body] makes a log correction. */
  protected fun <T> withAutoExecLoweredAfterOperation(
      mode: AutoExecPolicy,
      operation: (() -> Unit) -> T,
      body: () -> Unit,
  ): T {
    val previousMode = me.autoExecPolicy
    return try {
      val result = operation { me.autoExecPolicy = mode }
      body()
      result
    } finally {
      me.autoExecPolicy = previousMode
    }
  }
}
