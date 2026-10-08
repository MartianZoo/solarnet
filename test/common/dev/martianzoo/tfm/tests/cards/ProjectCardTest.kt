package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.exMachina
import dev.martianzoo.engine.Engine
import dev.martianzoo.state.GameConfig
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TfmTest
import kotlin.test.BeforeTest

internal abstract class ProjectCardTest : TfmTest() {
  protected lateinit var kim: TfmGameplay
    private set

  protected lateinit var stan: TfmGameplay
    private set

  protected lateinit var rob: TfmGameplay
    private set

  @BeforeTest
  fun initializeProjectCardGame() {
    game = Engine.newGame(PREMISE)
    val workflow = TfmWorkflow.Stepwise(agents)
    val players = game.actors.filterIsInstance<Player>().map { game.testTfm(it) }
    kim = players[0]
    stan = players[1]
    rob = players[2]

    workflow.setupPhase()
    players.forEach { it.doTask("BeginnerMode") }

    workflow.corporationPhase()
    players.zip(BEGINNER_CORPORATIONS).forEach { (player, corporation) ->
      player.startTurn()
      player.doTask("PlayCard<Class<BeginnerCard>, Class<$corporation>, Hand>")
      player.pay()
    }

    workflow.actionPhase()
  }

  protected fun TfmGameplay.setToExMachina(targetCount: Int, type: String) {
    val difference = targetCount - count(type)
    if (difference == 0) return
    exMachina("$difference $type")
  }

  protected fun TfmGameplay.exMachina(adjustment: String) {
    agents.exMachina(actor, adjustment)
  }

  private companion object {
    private val PREMISE by lazy {
      Canon.gamePremise(
          GameConfig(
              """
              TharsisMap, CorporateEraExpansion, VenusNextExpansion, ColoniesExpansion,
              PromoCardPack, BeginnerVariant, QuickStartVariant
              """,
              "Kim",
              "Stan",
              "Rob",
          )
      )
    }

    private val BEGINNER_CORPORATIONS =
        listOf("BeginnerCorporation1", "BeginnerCorporation2", "BeginnerCorporation3")
  }
}
