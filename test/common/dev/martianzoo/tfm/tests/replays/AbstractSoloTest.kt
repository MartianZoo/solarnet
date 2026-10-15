package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.tfm.engine.TfmGameplay
import kotlin.test.BeforeTest

/** Follow-along solo tests, played as the game's own rules offer each turn and phase. */
internal abstract class AbstractSoloTest : AbstractFullGameTest() {
  protected lateinit var me: TfmGameplay
    private set

  protected abstract fun cityAreas(): Pair<String, String>

  protected abstract fun greeneryAreas(): Pair<String, String>

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()

    me = p1
    admin.startGame()
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
