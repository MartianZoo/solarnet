package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
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

/**
 * Focused scenarios with explicit corrections and selected phase shortcuts. The game is set up and
 * enters the corporation phase normally, but scenarios then act outside the game's turn order.
 */
internal abstract class TfmSandboxTest : TfmTest() {
  protected lateinit var players: List<Agent>
    private set

  protected val kim: Agent
    get() = players[0]

  protected val stan: Agent
    get() = checkNotNull(players.getOrNull(1)) { "Stan is sitting this game out" }

  protected val rob: Agent
    get() = checkNotNull(players.getOrNull(2)) { "Rob is sitting this game out" }

  protected fun newTestGame(
      addOptions: String = "",
      playerCount: Int = 3,
      kimCorporation: ClassName? = null,
  ) = prepareTestGame(addOptions, playerCount, kimCorporation, playCorporations = true)

  /** Prepares Kim to choose and play a corporation explicitly. */
  protected fun newTestGameBeforeCorporationPlay(
      addOptions: String = "",
      playerCount: Int = 3,
  ) = prepareTestGame(addOptions, playerCount, null, playCorporations = false)

  private fun prepareTestGame(
      addOptions: String,
      playerCount: Int,
      kimCorporation: ClassName?,
      playCorporations: Boolean,
  ) {
    val key = Triple(addOptions, playerCount, kimCorporation) to playCorporations
    val prepared =
        preparedWorlds.getOrPut(key) {
          createWorld(addOptions, playerCount)
          admin.startGame()
          if (
              playerCount == 1 &&
                  GameConfig(addOptions).includedClassNames.none {
                    it in Canon.colonyTileClassNames
                  } &&
                  admin.count("SoloColoniesSetup") == 1
          ) {
            kim.doTask("-SelectedColonyTile<Class<Callisto>>")
          }
          chooseStartingCards(kimCorporation, playCorporations)
          leaveTurnOrder()
          if (playCorporations) {
            playStartingCorporations(kimCorporation)
            admin.phase(if (admin.count("PreludeExpansion") > 0) "Prelude" else "Action")
          }
          game.timeline.recordPosition()
          game
        }
    // Move the prepared world to the newest position before evicting the oldest.
    preparedWorlds.remove(key)
    preparedWorlds[key] = prepared
    if (preparedWorlds.size > MAX_PREPARED_WORLDS) {
      preparedWorlds.remove(preparedWorlds.keys.first())
    }
    game = Engine.fork(prepared)
    bindPlayers()
    players.forEach { it.autoExecPolicy = NONE }
  }

  /**
   * Advances to the next Action phase. From Action, expires generation-scoped state without
   * simulating production or research; from Corporation or Prelude, skips remaining setup plays.
   * Pending choices must be completed before advancing.
   */
  protected fun advanceUntilActionPhase() {
    check(game.tasks.isEmpty()) { "Finish pending choices before advancing to Action phase" }
    if (admin.count("ActionPhase") == 1) {
      admin.runOperation("Generation")
    } else {
      check(admin.count("CorporationPhase") + admin.count("PreludePhase") == 1) {
        "Expected Corporation, Prelude, or Action phase"
      }
      admin.phase("Action")
    }
  }

  /**
   * Setup has handed off to the corporation phase, whose turn order now offers the start player a
   * corporation. Drops that pending turn order, so that scenarios take their turns ex machina.
   */
  private fun leaveTurnOrder() {
    game.tasks
        .extract { it.id to it.assignee }
        .forEach { (id, assignee) ->
          agents[assignee].dropTask(id)
        }
  }

  protected fun Agent.setToExMachina(targetCount: Int, type: String) {
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
      val difference =
          targetCount - count("PROD[$resource]") + count("ProdOffset<Class<$resource>>")
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

  /** Scores the current position in seat order without retaining scoring effects or history. */
  protected fun victoryPoints(): List<Int> {
    check(game.tasks.isEmpty()) { "Finish pending choices before querying victory points" }
    game.timeline.recordPosition()
    val checkpoint = game.timeline.checkpoint()
    try {
      admin.runOperation("End FROM Phase")
      check(game.tasks.isEmpty()) { "Final scoring has unresolved choices" }
      return players.map { it.count("VictoryPoint") }
    } finally {
      game.timeline.recordPosition()
      game.timeline.seek(checkpoint)
      game.timeline.truncate()
    }
  }

  protected fun Agent.exMachina(adjustment: String) {
    agents.exMachina(actor, adjustment)
  }

  private fun createWorld(addOptions: String, playerCount: Int) {
    require(playerCount in 1..PLAYER_NAMES.size) {
      "The fixture supports one to ${PLAYER_NAMES.size} players: $playerCount"
    }
    val options = GameConfig(addOptions, *PLAYER_NAMES.take(playerCount).toTypedArray())
    val defaults = GameConfig(BASE_GAME_OPTIONS)
    val config = fixtureConfig(options, defaults)
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    bindPlayers()
    players.forEach { it.autoExecPolicy = NONE }
  }

  private fun bindPlayers() {
    players = game.actors.filterIsInstance<Player>().map { agents[it] }
  }

  private fun chooseStartingCards(kimCorporation: ClassName?, playCorporations: Boolean) {
    players.forEachIndexed { index, player ->
      player.selectTask("BeginnerMode OR NonBeginnerMode")
      player.narrowTask(
          if (index == 0 && (kimCorporation != null || !playCorporations)) "NonBeginnerMode"
          else "BeginnerMode"
      )
      player.doTask(if (players.size == 1) "14 TerraformRating" else "20 TerraformRating")
      if (index == 0 && (kimCorporation != null || !playCorporations)) {
        player.doTasks(
            "2 CorporationCard",
            "-CorporationCard",
            "10 ProjectCard<Selecting>",
            "-5 ProjectCard<Selecting>",
        )
      } else {
        player.doTask("BeginnerCard")
      }
      if (admin.count("PreludeExpansion") > 0) {
        player.doTasks("4 PreludeCard", "-2 PreludeCard")
      }
    }
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
        player.inTurn {
          doTask("PlayCard<Class<CorporationCard>, Class<$kimCorporation>>")
          doWithAutoExecPolicy(EAGER) { doTask("-MC / Owed") }
        }
      } else {
        player.inTurn {
          doTasks(
              "PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation${index + 1}>, Hand>",
              "BeginnerCorporation${index + 1} FROM BeginnerCard",
              "42 MC",
              "10 ProjectCard",
          )
        }
      }
    }
  }

  private companion object {
    private const val MAX_PREPARED_WORLDS = 8

    private const val BASE_GAME_OPTIONS =
        "CorporateEraExpansion, VenusNextExpansion, ColoniesExpansion, " +
            "promos:cards, BeginnerVariant, QuickStartVariant"

    private val PLAYER_NAMES = listOf("Kim", "Stan", "Rob", "Maya", "Nadia")

    private val preparedWorlds =
        linkedMapOf<Pair<Triple<String, Int, ClassName?>, Boolean>, World>()
  }
}
