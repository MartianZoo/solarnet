package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.tests.TestOption as Option
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.setUpGame as setUpTfmGame

internal abstract class CardTest(
    /**
     * Extra declarations to compose into each game's Catalog, given the seats that game occupies. A
     * declaration naming `Player3` may only be produced for a game that seats three.
     */
    private val additionalClassDeclarations: (seats: Int) -> Set<ClassDeclaration> = { emptySet() },
) : TfmTest() {
  internal constructor(
      additionalClassDeclarations: Set<ClassDeclaration>
  ) : this({ additionalClassDeclarations })

  protected lateinit var p1: TfmGameplay
    private set

  private var p2: TfmGameplay? = null
    private set

  protected fun newGame(config: GameConfig): World = startGame(premise(config))

  protected fun newGame(
      vararg selectedOptions: Option,
      players: Int = 2,
      colonyTiles: Set<ClassName> = emptySet(),
  ): World = startGame(premise(selectedOptions, players, colonyTiles))

  private fun premise(
      selectedOptions: Array<out Option>,
      players: Int,
      colonyTiles: Set<ClassName>,
  ): GamePremise {
    val additional = additionalClassDeclarations(players)
    val premise =
        if (additional.isEmpty()) {
          commonSetup(selectedOptions.toSet(), players, colonyTiles)
        } else {
          canonicalPremise(
              *selectedOptions,
              players = players,
              colonyTiles = colonyTiles,
              catalog = canonicalCatalog(Option.FakeStuffBundle in selectedOptions),
              additionalClassDeclarations = additional,
          )
        }
    return premise
  }

  private fun premise(config: GameConfig): GamePremise {
    val additional = additionalClassDeclarations(config.playerNames.size)
    val base = canonicalCatalog(config)
    if (additional.isEmpty()) return base.gamePremise(config)
    val additionalClassNames = additional.map(ClassDeclaration::className)
    val premise = base.gamePremise(config, additionalClassDeclarations = additional)
    return premise.copy(
        classSelections = premise.classSelections + additionalClassNames.map(::ClassSelection)
    )
  }

  protected fun requireP2(): TfmGameplay = requireNotNull(p2) { "This test needs two players" }

  protected fun playCorporationWithoutStartingProjects(
      player: TfmGameplay,
      corporation: ClassName,
  ): TaskResult = player.playCorp(corporation, 0)

  protected fun TfmGameplay.playCorp(
      corporation: ClassName,
      startingProjects: Int,
      body: OperationBlock = {},
  ): TaskResult {
    if (admin.count("SetupPhase") == 1) {
      val players = game.actors.filterIsInstance<Player>().map { game.testTfm(it) }
      prepareCorporationPhase(
          *players.map { if (it.actor == actor) startingProjects else 0 }.toIntArray()
      )
    } else if (startingProjects > 0) {
      require(count("ProjectCard<Selecting>") == 0)
      runOperation("$startingProjects ProjectCard<Selecting>")
    }
    return playCorp(corporation, body)
  }

  private fun prepareCorporationPhase(vararg startingProjects: Int) {
    val players = game.actors.filterIsInstance<Player>().map { game.testTfm(it) }
    require(startingProjects.size == players.size) { "Provide a project count per player" }
    if (admin.count("SetupPhase") == 1) {
      players.zip(startingProjects.toList()).forEach { (player, count) ->
        player.keepStartingProjects(count)
      }
      admin.phase("Corporation")
    } else {
      players.zip(startingProjects.toList()).forEach { (player, count) ->
        require(player.count("ProjectCard<Selecting>") == 0)
        if (count > 0) player.runOperation("$count ProjectCard<Selecting>")
      }
    }
  }

  private fun startGame(premise: GamePremise): World {
    return setUpTfmGame(premise).initializeCardTestGame()
  }

  private fun World.initializeCardTestGame(): World = apply {
    bindPlayers()
    finishSoloSetup()
    testTfm(ADMIN).phase("Corporation")
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
      admin.doTask("CityTile<$city>")
      admin.doTask("GreeneryTile<$greenery>")
    }
  }

  private fun World.bindPlayers(): World = apply {
    game = this
    val players = actors.filterIsInstance<Player>()
    p1 = testTfm(players.first())
    p2 = players.getOrNull(1)?.let { testTfm(it) }
  }

  /** Runs an instruction through the engine while hiding the uninteresting Agent plumbing. */
  private fun TfmGameplay.runOperation(
      instruction: String,
      body: OperationBlock = {},
  ): TaskResult = runOperation(instruction, body)

  private fun Agent.runOperation(
      instruction: String,
      body: OperationBlock = {},
  ): TaskResult = runOperation(instruction, body)

  private companion object {
    private val standardTwoPlayerPremise: GamePremise by lazy { canonicalPremise(players = 2) }
    private val promoTwoPlayerPremise: GamePremise by lazy {
      canonicalPremise(Option.PromoCardPack, players = 2)
    }

    private fun commonSetup(
        selectedOptions: Set<Option>,
        players: Int,
        colonyTiles: Set<ClassName>,
    ): GamePremise {
      if (players == 2 && colonyTiles.isEmpty()) {
        if (selectedOptions.isEmpty()) return standardTwoPlayerPremise
        if (selectedOptions == setOf(Option.PromoCardPack)) return promoTwoPlayerPremise
      }
      return canonicalPremise(
          *selectedOptions.toTypedArray(),
          players = players,
          colonyTiles = colonyTiles,
      )
    }
  }
}
