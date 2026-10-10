package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.exMachina
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow

/** Focused scenarios with explicit corrections and selected phase shortcuts. */
internal abstract class TfmSandboxTest : TfmTest() {
  protected lateinit var players: List<TfmGameplay>
    private set

  protected val kim: TfmGameplay
    get() = players[0]

  protected val stan: TfmGameplay
    get() = checkNotNull(players.getOrNull(1)) { "Stan is sitting this game out" }

  protected val rob: TfmGameplay
    get() = checkNotNull(players.getOrNull(2)) { "Rob is sitting this game out" }

  protected fun newTestGame(
      addOptions: String = "",
      playerCount: Int = 3,
      kimCorporation: ClassName? = null,
      startAtCorporation: Boolean = false,
  ) {
    val key = Triple(addOptions, playerCount, kimCorporation) to startAtCorporation
    val prepared =
        preparedWorlds.getOrPut(key) {
          createWorld(addOptions, playerCount, kimCorporation)
          val workflow = TfmWorkflow.Stepwise(agents)
          workflow.setupPhase()
          chooseStartingCards(kimCorporation)
          workflow.corporationPhase()
          if (!startAtCorporation) {
            playStartingCorporations(kimCorporation)
            if (admin.count("PreludeExpansion") > 0) workflow.preludePhase()
            else workflow.actionPhase()
          }
          game
        }
    game = Engine.fork(prepared)
    bindPlayers()
  }

  /** Starts focused action tests, skipping any unplayed corporations and Preludes. */
  protected fun startActionPhase() {
    check(admin.count("CorporationPhase") + admin.count("PreludePhase") == 1) {
      "Expected Corporation or Prelude phase"
    }
    check(game.tasks.isEmpty()) { "Finish pending choices before starting Action phase" }
    TfmWorkflow.Stepwise(agents).actionPhase()
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

  private fun createWorld(addOptions: String, playerCount: Int, kimCorporation: ClassName?) {
    require(playerCount in 1..PLAYER_NAMES.size) {
      "The fixture supports one to ${PLAYER_NAMES.size} players: $playerCount"
    }
    val options =
        GameConfig(
            listOf(addOptions, kimCorporation?.toString().orEmpty()).joinToString(),
            *PLAYER_NAMES.take(playerCount).toTypedArray(),
        )
    val config =
        options.copy(
            includedClassNames =
                (GameConfig(BASE_GAME_OPTIONS).includedClassNames + options.includedClassNames) -
                    options.excludedClassNames
        )
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    bindPlayers()
  }

  private fun bindPlayers() {
    players = game.actors.filterIsInstance<Player>().map { game.testTfm(it) }
  }

  private fun chooseStartingCards(kimCorporation: ClassName?) {
    players.forEachIndexed { index, player ->
      player.selectTask("BeginnerMode OR NonBeginnerMode")
      player.narrowTask(
          if (index == 0 && kimCorporation != null) "NonBeginnerMode" else "BeginnerMode"
      )
    }
    if (kimCorporation != null) kim.keepStartingProjects(10)
    revealTurmoilSetupEvents(game)
    if (players.size == 1) {
      val placements =
          when {
            admin.count("TharsisMap") == 1 ->
                listOf("Tharsis_4_1", "Tharsis_5_1", "Tharsis_2_2", "Tharsis_2_3")
            admin.count("HellasMap") == 1 ->
                listOf("Hellas_5_1", "Hellas_6_2", "Hellas_8_4", "Hellas_9_5")
            admin.count("ElysiumMap") == 1 ->
                listOf("Elysium_2_6", "Elysium_1_5", "Elysium_8_9", "Elysium_7_8")
            else -> error("The solo fixture needs neutral placements for this map")
          }
      placements.chunked(2).forEach { (city, greenery) ->
        admin.doTask("CityTile<$city>")
        admin.doTask("GreeneryTile<$greenery>")
      }
    }
  }

  private fun playStartingCorporations(kimCorporation: ClassName?) {
    players.forEachIndexed { index, player ->
      if (index == 0 && kimCorporation != null) {
        player.playCorp(kimCorporation)
      } else {
        player.inTurn {
          doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation${index + 1}>, Hand>")
          player.pay()
        }
      }
    }
  }

  private companion object {
    private const val BASE_GAME_OPTIONS =
        "CorporateEraExpansion, VenusNextExpansion, ColoniesExpansion, " +
            "PromoCardPack, BeginnerVariant, QuickStartVariant"

    private val PLAYER_NAMES = listOf("Kim", "Stan", "Rob", "Maya", "Nadia")

    private val preparedWorlds =
        mutableMapOf<Pair<Triple<String, Int, ClassName?>, Boolean>, World>()
  }
}
