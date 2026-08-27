package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.CardDefinition
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TestOption as Option
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.setUpGame as setUpTfmGame

internal abstract class CardTest(
    private val additionalClassDeclarations: Set<ClassDeclaration> = emptySet(),
    private val additionalCardDefinitions: Set<CardDefinition> = emptySet(),
) : TfmTest() {
  protected lateinit var p1: TfmGameplay
    private set

  private var p2: TfmGameplay? = null
    private set

  protected fun newGame(config: GameConfig): World =
      setUpTfmGame(premise(config)).initializeCardTestGame()

  protected fun newGame(
      vararg selectedOptions: Option,
      players: Int = 2,
      colonyTiles: Set<ClassName> = emptySet(),
  ): World {
    val premise =
        if (!hasAdditionalContent) {
          cachedSetup(selectedOptions.toSet(), players, colonyTiles)
        } else {
          withAdditionalSelections(
              canonicalPremise(
                  *selectedOptions,
                  players = players,
                  colonyTiles = colonyTiles,
                  catalog = catalog,
              )
          )
        }
    return setUpTfmGame(premise).initializeCardTestGame()
  }

  private val catalog: TfmCatalog by lazy {
    if (!hasAdditionalContent) {
      Canon
    } else {
      val additions =
          object : TfmCatalog() {
            override val explicitClassDeclarations = buildSet {
              addAll(additionalClassDeclarations)
              additionalCardDefinitions.forEach { card ->
                if (none { it.className == card.className }) add(card.asClassDeclaration)
                card.extraClasses.forEach { supporting ->
                  if (none { it.className == supporting.className }) add(supporting)
                }
              }
            }
            override val cardDefinitions = additionalCardDefinitions
          }
      TfmCatalog.compose(Canon, additions)
    }
  }

  private val hasAdditionalContent: Boolean
    get() = additionalClassDeclarations.isNotEmpty() || additionalCardDefinitions.isNotEmpty()

  private fun premise(config: GameConfig): GamePremise {
    val premise = catalog.gamePremise(config)
    if (!hasAdditionalContent) return premise
    return withAdditionalSelections(premise)
  }

  private fun withAdditionalSelections(premise: GamePremise): GamePremise {
    val additionalClassNames =
        additionalClassDeclarations.map(ClassDeclaration::className) +
            additionalCardDefinitions.map(CardDefinition::className)
    return premise.copy(
        classSelections = premise.classSelections + additionalClassNames.map(::ClassSelection)
    )
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
