package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.SOLE_TASK
import dev.martianzoo.agenttestsupport.withTestSoloSetup
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmEngine

/**
 * Scenarios following the game's own turns and phases. Focused phase tests may prepare their
 * starting position with visible corrections, then follow the real game flow. Players submit
 * offered tasks through plain Agents with a fixed policy. Explicit autoExecNow calls handle
 * competing consequences when their order is not the subject.
 */
internal abstract class TfmGameplayTest(private val playerPolicy: AutoExecPolicy = SOLE_TASK) {
  lateinit var game: World
    private set

  lateinit var agents: Agents
    private set

  protected open val admin: Agent
    get() = agents[ADMIN]

  protected fun TaskResult.expect(expected: String) =
      TestHelpers.assertNetChanges(this, game, expected)

  /** Includes phase follow-up work that runs after an individual TaskResult is recorded. */
  protected fun expectChanges(expected: String, body: () -> Unit) {
    val checkpoint = game.events.nextOrdinal
    body()
    TaskResult(changes = game.events.changesSince(checkpoint)).expect(expected)
  }

  protected fun Agent.auditGainsSince(eventCursor: Int): Int =
      game.events.changesSince(eventCursor).count { it.change.gaining?.type == resolve("Audit") }

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
      firstGlobalEvents: List<String> = emptyList(),
  ) =
      prepareTestGame(
          addOptions,
          playerCount,
          kimCorporation,
          playCorporations = true,
          firstGlobalEvents = firstGlobalEvents,
      )

  /** Prepares Kim to choose and play a corporation explicitly. */
  protected fun newTestGameBeforeCorporationPlay(
      addOptions: String = "",
      playerCount: Int = 3,
      firstGlobalEvents: List<String> = emptyList(),
  ) =
      prepareTestGame(
          addOptions,
          playerCount,
          null,
          playCorporations = false,
          firstGlobalEvents = firstGlobalEvents,
      )

  private fun prepareTestGame(
      addOptions: String,
      playerCount: Int,
      kimCorporation: ClassName?,
      playCorporations: Boolean,
      firstGlobalEvents: List<String>,
  ) {
    createWorld(addOptions, playerCount, firstGlobalEvents)
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

  private fun createWorld(addOptions: String, playerCount: Int, firstGlobalEvents: List<String>) {
    require(playerCount in 1..PLAYER_NAMES.size) {
      "The fixture supports one to ${PLAYER_NAMES.size} players: $playerCount"
    }
    val options = GameConfig(addOptions, *PLAYER_NAMES.take(playerCount).toTypedArray())
    val defaults = GameConfig(BASE_GAME_OPTIONS)
    val config = fixtureConfig(options, defaults)
    val premise = canonicalCatalog(config).gamePremise(config)
    initializeGame(withTestTurmoilDeck(withTestSoloSetup(premise), firstGlobalEvents))
  }

  /** Creates the supplied game before setup choices or card selection. */
  protected fun initializeGame(premise: GamePremise) {
    game = TfmEngine.newGame(premise)
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
