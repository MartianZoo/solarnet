package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.exMachina
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TfmTest

internal abstract class ProjectCardTest : TfmTest() {
  protected lateinit var kim: TfmGameplay
    private set

  protected lateinit var stan: TfmGameplay
    private set

  protected lateinit var rob: TfmGameplay
    private set

  protected fun newTestGame(addOptions: String = "") {
    game = Engine.fork(preparedWorld(addOptions))
    bindPlayers()
  }

  private fun preparedWorld(addOptions: String): World =
      preparedWorlds.getOrPut(addOptions) { prepareWorld(addOptions) }

  private fun prepareWorld(addOptions: String): World {
    val options = listOf(BASE_GAME_OPTIONS, addOptions).filter(String::isNotBlank).joinToString()
    game = TfmEngine.newGame(Canon.gamePremise(GameConfig(options, "Kim", "Stan", "Rob")))
    val workflow = TfmWorkflow.Stepwise(agents)
    val players = bindPlayers()

    workflow.setupPhase()
    players.forEach { it.doTask("BeginnerMode") }

    workflow.corporationPhase()
    players.zip(BEGINNER_CORPORATIONS).forEach { (player, corporation) ->
      player.startTurn()
      player.doTask("PlayCard<Class<BeginnerCard>, Class<$corporation>, Hand>")
      player.pay()
    }

    workflow.actionPhase()
    return game
  }

  private fun bindPlayers(): List<TfmGameplay> {
    val players = game.actors.filterIsInstance<Player>().map { game.testTfm(it) }
    kim = players[0]
    stan = players[1]
    rob = players[2]
    return players
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
    private const val BASE_GAME_OPTIONS =
        "CorporateEraExpansion, VenusNextExpansion, ColoniesExpansion, " +
            "PromoCardPack, BeginnerVariant, QuickStartVariant"

    private val preparedWorlds = mutableMapOf<String, World>()

    private val BEGINNER_CORPORATIONS =
        listOf("BeginnerCorporation1", "BeginnerCorporation2", "BeginnerCorporation3")
  }
}
