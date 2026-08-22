package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.Player
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestOption as Option
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmTest
import dev.martianzoo.tfm.engine.canonicalPremise
import dev.martianzoo.tfm.engine.setUpGame as setUpTfmGame

abstract class CardTest : TfmTest() {
  protected lateinit var p1: TfmGameplay
    private set

  protected var p2: TfmGameplay? = null
    private set

  protected fun newGame(config: GameConfig): World =
      setUpTfmGame(Canon.gamePremise(config)).initializeCardTestGame()

  protected fun newGame(
      vararg selectedOptions: Option,
      players: Int = 2,
      colonyTiles: Set<ClassName> = emptySet(),
  ): World {
    return setUpTfmGame(cachedSetup(selectedOptions.toSet(), players, colonyTiles))
        .initializeCardTestGame()
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
          p1.count("TharsisMap") == 1 ->
              listOf("Tharsis_4_1", "Tharsis_2_2") to listOf("Tharsis_5_1", "Tharsis_2_3")
          p1.count("HellasMap") == 1 ->
              listOf("Hellas_5_1", "Hellas_8_4") to listOf("Hellas_6_2", "Hellas_9_5")
          p1.count("ElysiumMap") == 1 ->
              listOf("Elysium_2_6", "Elysium_8_9") to listOf("Elysium_1_5", "Elysium_7_8")
          else -> return
        }

    cities.zip(greeneries).forEach { (city, greenery) ->
      engine.doTask("CityTile<$city, SoloOpponent>")
      engine.doTask("GreeneryTile<$greenery, SoloOpponent>")
    }
  }

  private fun World.bindPlayers(): World = apply {
    game = this
    val players = actors.filterIsInstance<Player>()
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

  private companion object {
    private data class SetupKey(
        val selectedOptions: Set<Option>,
        val players: Int,
        val colonyTiles: Set<ClassName>,
    )

    private val setupCache = mutableMapOf<SetupKey, GamePremise>()

    private fun cachedSetup(
        selectedOptions: Set<Option>,
        players: Int,
        colonyTiles: Set<ClassName>,
    ): GamePremise =
        setupCache.getOrPut(SetupKey(selectedOptions, players, colonyTiles)) {
          canonicalPremise(
              *selectedOptions.toTypedArray(),
              players = players,
              colonyTiles = colonyTiles,
          )
        }
  }
}
