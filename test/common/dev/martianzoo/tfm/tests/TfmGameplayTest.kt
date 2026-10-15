package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmEngine

/**
 * Scenarios reached through play, with the game's own rules offering every turn and beginning every
 * phase. Players submit offered tasks through plain Agents with a fixed policy. Explicit
 * autoExecNow calls settle competing consequences when their order is not the subject.
 */
internal abstract class TfmGameplayTest(private val playerPolicy: AutoExecPolicy = CONCRETE) {
  protected lateinit var game: World
    private set

  protected lateinit var agents: Agents
    private set

  protected val admin: Agent
    get() = agents[ADMIN]

  protected fun TaskResult.expect(expected: String) =
      TestHelpers.assertNetChanges(this, game, expected)

  /** Includes phase follow-up work that runs after an individual TaskResult is recorded. */
  protected fun expectChanges(expected: String, body: () -> Unit) {
    val checkpoint = game.timeline.checkpoint()
    body()
    TaskResult(changes = game.events.changesSince(checkpoint)).expect(expected)
  }

  protected fun Agent.auditGainsSince(checkpoint: Checkpoint): Int =
      game.events.changesSince(checkpoint).count { it.change.gaining?.type == resolve("Audit") }

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
    createWorld(addOptions, playerCount)
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    if (
        playerCount == 1 &&
            GameConfig(addOptions).includedClassNames.none { it in Canon.colonyTileClassNames } &&
            admin.count("SoloColoniesSetup") == 1
    ) {
      kim.doTask("-SelectedColonyTile<Class<Callisto>>")
    }
    chooseStartingCards(kimCorporation, playCorporations)
    if (playCorporations) playStartingCorporations(kimCorporation)
  }

  private fun createWorld(addOptions: String, playerCount: Int) {
    require(playerCount in 1..PLAYER_NAMES.size) {
      "The fixture supports one to ${PLAYER_NAMES.size} players: $playerCount"
    }
    val options = GameConfig(addOptions, *PLAYER_NAMES.take(playerCount).toTypedArray())
    val defaults = GameConfig(BASE_GAME_OPTIONS)
    val config = fixtureConfig(options, defaults)
    game = TfmEngine.newGame(canonicalCatalog(config).gamePremise(config))
    agents = Agents(game) { if (it == ADMIN) EAGER else playerPolicy }
    players = game.actors.filterIsInstance<Player>().map { agents[it] }
  }

  private fun chooseStartingCards(kimCorporation: ClassName?, playCorporations: Boolean) {
    players.forEachIndexed { index, player ->
      // Choose the setup path for Admin's System task; a player cannot execute that task directly.
      player.selectTask("BeginnerOption OR StandardOption")
      player.narrowTask(
          if (index == 0 && (kimCorporation != null || !playCorporations)) "StandardOption"
          else "BeginnerOption"
      )
    }
    players.forEach { it.autoExecNow(EAGER) }
    if (kimCorporation != null || !playCorporations) kim.doTask("-5 ProjectCard<Selecting>")
    if (admin.count("RevealComingEvent") > 0) {
      admin.doTasks(
          "DemocraticReform",
          "PartyLeader<MarsFirst, Neutral>",
          "MinimalImpactPolicy",
          "PartyLeader<Reds, Neutral>",
      )
    }
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
        // Resolve the corporation's concrete starting benefits before the five-card purchase.
        player.completeOperation {
          doTask("PlayCard<Class<CorporationCard>, Class<$kimCorporation>>")
          autoExecNow(EAGER)
        }
      } else {
        player.completeOperation {
          doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation${index + 1}>, Hand>")
          autoExecNow(EAGER)
        }
      }
    }
  }

  private companion object {
    private const val BASE_GAME_OPTIONS =
        "CorporateEraExpansion, VenusNextExpansion, ColoniesExpansion, " +
            "promos:cards, BeginnerVariant, QuickStartVariant"

    private val PLAYER_NAMES = listOf("Kim", "Stan", "Rob", "Maya", "Nadia")
  }
}
