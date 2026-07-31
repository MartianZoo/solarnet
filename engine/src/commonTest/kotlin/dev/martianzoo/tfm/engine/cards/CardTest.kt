package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.Player
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameOptions
import dev.martianzoo.tfm.engine.TestHelpers
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.setUpGame as setUpTfmGame

abstract class CardTest {
  protected lateinit var game: World
  protected lateinit var p1: TfmGameplay
    private set

  protected var p2: TfmGameplay? = null
    private set

  protected val engine: TfmGameplay
    get() = game.tfm(ENGINE)

  protected fun newGame(premise: GamePremise): World =
      setUpTfmGame(premise).initializeCardTestGame()

  protected fun newGame(
      optionNames: String = "TerraformingMars,TharsisMapOption",
      players: Int = 2,
      colonyTiles: Set<ClassName> = emptySet(),
  ): World {
    val options = optionNames.split(',').mapTo(linkedSetOf(), ::cn)
    if (players == 1) options += cn("SoloMode")
    return setUpTfmGame(cachedSetup(options, players, colonyTiles)).initializeCardTestGame()
  }

  protected fun requireP2(): TfmGameplay = requireNotNull(p2) { "This test needs two players" }

  private fun World.initializeCardTestGame(): World = apply {
    bindPlayers()
    finishSoloSetup()
    tfm(ENGINE).phase("Corporation")
  }

  private fun finishSoloSetup() {
    if (p2 != null) return
    val (cities, greeneries) =
        when {
          p1.count("Tharsis") == 1 ->
              listOf("Tharsis_4_1", "Tharsis_2_2") to listOf("Tharsis_5_1", "Tharsis_2_3")
          p1.count("Hellas") == 1 ->
              listOf("Hellas_5_1", "Hellas_8_4") to listOf("Hellas_6_2", "Hellas_9_5")
          p1.count("Elysium") == 1 ->
              listOf("Elysium_2_6", "Elysium_8_9") to listOf("Elysium_1_5", "Elysium_7_8")
          else -> return
        }

    cities.zip(greeneries).forEach { (city, greenery) ->
      engine.doFirstTask("CityTile<$city, Opponent>")
      engine.doTask("GreeneryTile<$greenery, Opponent>")
    }
  }

  private fun World.bindPlayers(): World = apply {
    game = this
    val players = Player.players(reader.getComponents("Player").size)
    p1 = tfm(players.first())
    p2 = players.getOrNull(1)?.let { tfm(it) }
  }

  /** Runs an instruction through the engine while hiding the uninteresting GodMode plumbing. */
  protected fun TfmGameplay.manual(
      instruction: String,
      body: BodyLambda = {},
  ): TaskResult = godMode().manual(instruction, body)

  protected fun Gameplay.manual(
      instruction: String,
      body: BodyLambda = {},
  ): TaskResult = godMode().manual(instruction, body)

  protected fun TaskResult.expect(string: String) =
      TestHelpers.assertNetChanges(this, game, engine, string)

  private companion object {
    private data class SetupKey(
        val options: Set<ClassName>,
        val players: Int,
        val colonyTiles: Set<ClassName>,
    )

    private val setupCache = mutableMapOf<SetupKey, GamePremise>()

    private fun cachedSetup(
        options: Set<ClassName>,
        players: Int,
        colonyTiles: Set<ClassName>,
    ): GamePremise =
        setupCache.getOrPut(SetupKey(options, players, colonyTiles)) {
          Canon.gamePremise(GameOptions(players, options, colonyTiles))
        }
  }
}
