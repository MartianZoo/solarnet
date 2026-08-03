package dev.martianzoo.tfm.engine.games

import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.canonicalPremise
import kotlin.test.BeforeTest

/** Follow-along solo fixtures intentionally drive phases manually. */
abstract class AbstractSoloTest : AbstractFullGameTest() {
  protected lateinit var me: TfmGameplay
  protected lateinit var workflow: TfmWorkflow.Manual

  override fun setup() =
      canonicalPremise(
          HellasMapOption,
          VenusNextExpansion,
          PreludeExpansion,
          PromoCardPack,
          players = 1,
      )

  protected abstract fun cityAreas(): Pair<String, String>

  protected abstract fun greeneryAreas(): Pair<String, String>

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()

    me = p1
    workflow = TfmWorkflow.Manual(game)
    workflow.setupPhase()

    engine.doFirstTask("CityTile<${cityAreas().first}, SoloOpponent>")
    engine.doTask("GreeneryTile<${greeneryAreas().first}, SoloOpponent>")
    engine.doFirstTask("CityTile<${cityAreas().second}, SoloOpponent>")
    engine.doTask("GreeneryTile<${greeneryAreas().second}, SoloOpponent>")

    engine.phase("Corporation")
  }

  protected fun nextRound(wgt: String, cardsBought: Int) {
    p1.pass()
    workflow.productionPhase()
    workflow.solarPhase()
    me.doTask("$wgt! BY Engine")
    workflow.generation()
    workflow.researchPhase { p1.doTask(if (cardsBought > 0) "$cardsBought BuyCard" else "Ok") }
    workflow.actionPhase()
  }
}
