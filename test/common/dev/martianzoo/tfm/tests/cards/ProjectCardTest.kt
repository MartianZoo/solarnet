package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.exMachina
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.revealTurmoilSetupEvents

internal abstract class ProjectCardTest : TfmTest() {
  protected lateinit var players: List<TfmGameplay>
    private set

  protected val kim: TfmGameplay
    get() = players[0]

  protected val stan: TfmGameplay
    get() = players[1]

  protected val rob: TfmGameplay
    get() = checkNotNull(players.getOrNull(2)) { "Rob is sitting this game out" }

  protected fun newTestGame(
      addOptions: String = "",
      playerCount: Int = 3,
      kimCorporation: ClassName? = null,
  ) {
    require(playerCount in 2..PLAYER_NAMES.size) {
      "ProjectCardTest supports two to ${PLAYER_NAMES.size} players: $playerCount"
    }
    game = Engine.fork(preparedWorld(addOptions, playerCount, kimCorporation))
    bindPlayers()
  }

  private fun preparedWorld(
      addOptions: String,
      playerCount: Int,
      kimCorporation: ClassName?,
  ): World =
      preparedWorlds.getOrPut(Triple(addOptions, playerCount, kimCorporation)) {
        prepareWorld(addOptions, playerCount, kimCorporation)
      }

  private fun prepareWorld(
      addOptions: String,
      playerCount: Int,
      kimCorporation: ClassName?,
  ): World {
    val playerNames = PLAYER_NAMES.take(playerCount).toTypedArray()
    val options =
        GameConfig(
            listOf(addOptions, kimCorporation?.toString().orEmpty()).joinToString(),
            *playerNames,
        )
    val config =
        options.copy(
            includedClassNames =
                (GameConfig(BASE_GAME_OPTIONS).includedClassNames + options.includedClassNames) -
                    options.excludedClassNames
        )
    game = TfmEngine.newGame(Canon.gamePremise(config))
    val workflow = TfmWorkflow.Stepwise(agents)
    val players = bindPlayers()

    workflow.setupPhase()
    players.forEachIndexed { index, player ->
      player.doTask(if (index == 0 && kimCorporation != null) "NonBeginnerMode" else "BeginnerMode")
    }
    if (kimCorporation != null) kim.keepStartingProjects(10)
    revealTurmoilSetupEvents(game)

    workflow.corporationPhase()
    players.zip(BEGINNER_CORPORATIONS).forEachIndexed { index, (player, corporation) ->
      if (index == 0 && kimCorporation != null) {
        player.playCorp(kimCorporation)
      } else {
        player.startTurn()
        player.doTask("PlayCard<Class<BeginnerCard>, Class<$corporation>, Hand>")
        player.pay()
      }
    }

    workflow.actionPhase()
    return game
  }

  private fun bindPlayers(): List<TfmGameplay> {
    players = game.actors.filterIsInstance<Player>().map { game.testTfm(it) }
    return players
  }

  protected fun TfmGameplay.setToExMachina(targetCount: Int, type: String) {
    val countedExpression =
        (parseAs(Metric::class, type) as? Metric.Count)?.expression
            ?: error("Absolute correction requires a countable type: `$type`")
    val resolvedType = reader.resolve(countedExpression)
    if (resolvedType.className == PRODUCTION) {
      val resource =
          checkNotNull(
                  resolvedType.typeDependencies
                      .single { it.key.declaringClass == PRODUCTION }
                      .boundType
                      .representedClass
              )
              .className
      val difference = targetCount - production(resource)
      if (difference == 0) return
      val adjustment =
          when (difference) {
            -1 -> "-$resource"
            1 -> resource
            else -> "$difference $resource"
          }
      exMachina("PROD[$adjustment]")
    } else {
      val difference = targetCount - count(type)
      if (difference == 0) return
      exMachina("$difference $type")
    }
  }

  protected fun nextGeneration() {
    admin.nextGeneration(*IntArray(players.size))
  }

  /** Scores the current position in seat order without retaining scoring effects or history. */
  protected fun victoryPoints(): List<Int> {
    check(game.tasks.isEmpty()) { "Finish pending choices before querying victory points" }
    val checkpoint = game.timeline.checkpoint()
    try {
      TfmWorkflow.Stepwise(agents).endPhase()
      check(game.tasks.isEmpty()) { "Final scoring has unresolved choices" }
      return players.map { it.count("VictoryPoint") }
    } finally {
      game.timeline.rollBack(checkpoint)
    }
  }

  protected fun TfmGameplay.exMachina(adjustment: String) {
    agents.exMachina(actor, adjustment)
  }

  private companion object {
    private const val BASE_GAME_OPTIONS =
        "CorporateEraExpansion, VenusNextExpansion, ColoniesExpansion, " +
            "PromoCardPack, BeginnerVariant, QuickStartVariant"

    private val PLAYER_NAMES = listOf("Kim", "Stan", "Rob", "Maya", "Nadia")

    private val preparedWorlds = mutableMapOf<Triple<String, Int, ClassName?>, World>()

    private val BEGINNER_CORPORATIONS = (1..PLAYER_NAMES.size).map { "BeginnerCorporation$it" }
  }
}
