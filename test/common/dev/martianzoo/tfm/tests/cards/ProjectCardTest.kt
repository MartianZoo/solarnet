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
  protected lateinit var players: List<TfmGameplay>
    private set

  protected val kim: TfmGameplay
    get() = players[0]

  protected val stan: TfmGameplay
    get() = players[1]

  protected val rob: TfmGameplay
    get() = checkNotNull(players.getOrNull(2)) { "Rob is sitting this game out" }

  protected fun newTestGame(addOptions: String = "", playerCount: Int = 3) {
    require(playerCount in 2..PLAYER_NAMES.size) {
      "ProjectCardTest supports two to ${PLAYER_NAMES.size} players: $playerCount"
    }
    game = Engine.fork(preparedWorld(addOptions, playerCount))
    bindPlayers()
  }

  private fun preparedWorld(addOptions: String, playerCount: Int): World =
      preparedWorlds.getOrPut(addOptions to playerCount) { prepareWorld(addOptions, playerCount) }

  private fun prepareWorld(addOptions: String, playerCount: Int): World {
    val options = listOf(BASE_GAME_OPTIONS, addOptions).filter(String::isNotBlank).joinToString()
    val playerNames = PLAYER_NAMES.take(playerCount).toTypedArray()
    game = TfmEngine.newGame(Canon.gamePremise(GameConfig(options, *playerNames)))
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
    players = game.actors.filterIsInstance<Player>().map { game.testTfm(it) }
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

    private val PLAYER_NAMES = listOf("Kim", "Stan", "Rob", "Maya", "Nadia")

    private val preparedWorlds = mutableMapOf<Pair<String, Int>, World>()

    private val BEGINNER_CORPORATIONS = (1..PLAYER_NAMES.size).map { "BeginnerCorporation$it" }
  }
}
