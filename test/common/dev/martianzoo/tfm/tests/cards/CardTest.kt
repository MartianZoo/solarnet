package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.Agent
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Engine
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
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TEST_CLASS_SYNONYMS
import dev.martianzoo.tfm.tests.TestOption as Option
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.setUpGame as setUpTfmGame
import kotlin.test.AfterTest

internal abstract class CardTest(
    private val additionalClassDeclarations: Set<ClassDeclaration> = emptySet(),
) : TfmTest() {
  protected lateinit var p1: TfmGameplay
    private set

  private var p2: TfmGameplay? = null
    private set

  private var workflow: TfmWorkflow.Auto? = null

  protected fun newGame(config: GameConfig): World = startGame(premise(config))

  protected fun newGame(
      vararg selectedOptions: Option,
      players: Int = 2,
      colonyTiles: Set<ClassName> = emptySet(),
  ): World = startGame(premise(selectedOptions, players, colonyTiles))

  protected fun newGameWithAutoWorkflow(
      vararg selectedOptions: Option,
      players: Int = 2,
      colonyTiles: Set<ClassName> = emptySet(),
  ): World = startAutoGame(premise(selectedOptions, players, colonyTiles))

  private fun premise(
      selectedOptions: Array<out Option>,
      players: Int,
      colonyTiles: Set<ClassName>,
  ): GamePremise {
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
    return premise
  }

  private val catalog: TfmCatalog by lazy {
    if (!hasAdditionalContent) {
      Canon
    } else {
      val additions =
          object : TfmCatalog() {
            override val explicitClassDeclarations = additionalClassDeclarations
          }
      TfmCatalog.compose(Canon, additions)
    }
  }

  private val hasAdditionalContent: Boolean
    get() = additionalClassDeclarations.isNotEmpty()

  private fun premise(config: GameConfig): GamePremise {
    val premise = catalog.gamePremise(config)
    if (!hasAdditionalContent) return premise
    return withAdditionalSelections(premise)
  }

  private fun withAdditionalSelections(premise: GamePremise): GamePremise {
    val additionalClassNames = additionalClassDeclarations.map(ClassDeclaration::className)
    return premise.copy(
        classSelections = premise.classSelections + additionalClassNames.map(::ClassSelection)
    )
  }

  protected fun requireP2(): TfmGameplay = requireNotNull(p2) { "This test needs two players" }

  private fun startGame(premise: GamePremise): World {
    workflow?.shutdown()
    return setUpTfmGame(premise).initializeCardTestGame()
  }

  private fun startAutoGame(premise: GamePremise): World {
    workflow?.shutdown()
    return Engine.newGame(premise, inputOnlySynonyms = TEST_CLASS_SYNONYMS).apply {
      bindPlayers()
      workflow = TfmWorkflow.Auto(this).launch()
      finishSoloSetup()
    }
  }

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

  protected fun playUntilPreludePhase(
      vararg corporations: ClassName,
      startingMc: Int = 500,
  ) {
    playCorporations(corporations.toList())
    check(engine.count("PreludePhase") == 1) { "This game has no Prelude phase" }
    p1.topOffMoney(startingMc)
  }

  protected fun playUntilFirstActionPhase(
      vararg corporations: ClassName,
      startingMc: Int = 500,
  ) {
    playCorporations(corporations.toList())
    if (engine.count("PreludePhase") == 1) {
      val players = game.actors.filterIsInstance<Player>().map { game.tfm(it) }
      players.zip(BORING_PRELUDES).forEach { (player, preludes) ->
        player.turn { preludes.forEach { playPrelude(it) } }
      }
    }
    check(engine.count("ActionPhase") == 1) { "The game did not reach its first Action phase" }
    p1.topOffMoney(startingMc)
  }

  private fun playCorporations(requested: List<ClassName>) {
    check(engine.count("CorporationPhase") == 1) { "The Corporation phase has already ended" }
    val players = game.actors.filterIsInstance<Player>().map { game.tfm(it) }
    val corporations = if (requested.isEmpty()) BORING_CORPORATIONS else requested
    require(corporations.size >= players.size) { "Provide one corporation per player" }
    players.zip(corporations).forEach { (player, corporation) ->
      player.playCorp(corporation, 5)
    }
  }

  private fun TfmGameplay.topOffMoney(target: Int) {
    val amount = target - count("MC")
    require(amount >= 0) { "$actor already has more than $target MC" }
    if (amount > 0) sneak("$amount MC")
  }

  @AfterTest
  fun shutdownWorkflow() {
    workflow?.shutdown()
  }

  /** Runs an instruction through the engine while hiding the uninteresting Agent plumbing. */
  protected fun TfmGameplay.manual(
      instruction: String,
      body: BodyLambda = {},
  ): TaskResult = manual(instruction, body)

  protected fun Agent.manual(
      instruction: String,
      body: BodyLambda = {},
  ): TaskResult = manual(instruction, body)

  private companion object {
    private val BORING_CORPORATIONS =
        listOf(
            UnitedNationsMarsInitiative,
            MiningGuild,
            Phobolog,
            SaturnSystems,
            Ecoline,
        )

    private val BORING_PRELUDES =
        listOf(
            listOf(Donation, Loan),
            listOf(BusinessEmpire, AlliedBank),
            listOf(MetalsCompany, SocietySupport),
            listOf(SupplyDrop, GalileanMining),
            listOf(PowerGeneration, Mohole),
        )

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
