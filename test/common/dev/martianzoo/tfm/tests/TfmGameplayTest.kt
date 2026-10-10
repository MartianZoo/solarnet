package dev.martianzoo.tfm.tests

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import kotlin.test.AfterTest

/**
 * Scenarios reached through play under the real automatic workflow. Tests must follow the offered
 * turn order; the current gameplay helpers do not reject out-of-turn actions.
 */
internal abstract class TfmGameplayTest : TfmTest() {
  protected lateinit var players: List<TfmGameplay>
    private set

  protected val kim: TfmGameplay
    get() = players[0]

  protected val stan: TfmGameplay
    get() = checkNotNull(players.getOrNull(1)) { "Stan is sitting this game out" }

  protected val rob: TfmGameplay
    get() = checkNotNull(players.getOrNull(2)) { "Rob is sitting this game out" }

  private var workflow: TfmWorkflow.Automatic? = null

  protected fun newTestGame(
      addOptions: String = "",
      playerCount: Int = 3,
      kimCorporation: ClassName? = null,
      startAtCorporation: Boolean = false,
  ) {
    shutdownWorkflow()
    createWorld(addOptions, playerCount, kimCorporation)
    workflow = TfmWorkflow.Automatic(agents).launch()
    chooseStartingCards(kimCorporation)
    if (!startAtCorporation) playStartingCorporations(kimCorporation)
  }

  @AfterTest
  fun shutdownWorkflow() {
    workflow?.shutdown()
    workflow = null
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
      player.doTask(if (index == 0 && kimCorporation != null) "NonBeginnerMode" else "BeginnerMode")
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
  }
}
