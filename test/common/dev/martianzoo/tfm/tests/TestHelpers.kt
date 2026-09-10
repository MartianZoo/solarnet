package dev.martianzoo.tfm.tests

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.engine.toComponent
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.fake.FakeCanon
import io.kotest.matchers.shouldBe

internal fun setUpGame(
    premise: GamePremise,
    retainedStartingProjects: Int = 0,
): World =
    Engine.newGame(premise).apply {
      TfmWorkflow.Manual(this).setupPhase()
      retainStartingProjects(
          *IntArray(actors.filterIsInstance<Player>().size) { retainedStartingProjects },
      )
    }

internal fun World.retainStartingProjects(vararg retainedCounts: Int) {
  val players = actors.filterIsInstance<Player>()
  require(retainedCounts.size == players.size) {
    "expected one starting-project count for each of ${players.size} players"
  }
  players.zip(retainedCounts.asIterable()).forEach { (player, retained) ->
    require(retained in 0..10) { "cannot retain $retained of 10 starting projects" }
    val discarded = 10 - retained
    tfm(player).doTask(if (discarded == 0) "Ok" else "-$discarded ProjectCard<Hand>")
  }
}

internal fun setUpGame(
    vararg selectedOptions: TestSelection,
    players: Int = 2,
    colonyTiles: Set<ClassName> = emptySet(),
): World =
    setUpGame(canonicalPremise(*selectedOptions, players = players, colonyTiles = colonyTiles))

internal fun canonicalPremise(
    vararg selectedOptions: TestSelection,
    players: Int = 2,
    colonyTiles: Set<ClassName> = emptySet(),
    catalog: TfmCatalog? = null,
    initialComponentTypes: Set<Expression> = emptySet(),
): GamePremise {
  val included = selectedOptions.filterIsInstance<TestOption>()
  val excluded = selectedOptions.filterIsInstance<ExcludedTestOption>().map { it.option }.toSet()
  return canonicalPremise(
      canonicalOptions(*included.toTypedArray()),
      players,
      colonyTiles,
      catalog,
      excluded,
      initialComponentTypes,
  )
}

internal fun canonicalPremise(
    options: Set<TestOption>,
    players: Int = 2,
    colonyTiles: Set<ClassName> = emptySet(),
    catalog: TfmCatalog? = null,
    excludedOptions: Set<TestOption> = emptySet(),
    initialComponentTypes: Set<Expression> = emptySet(),
): GamePremise {
  val config =
      GameConfig.create(
          included = options.map(TestOption::className) + colonyTiles,
          excluded = excludedOptions.map(TestOption::className),
          playerNames = Player.players(players).map(Player::className),
      )
  val defaultCatalog = canonicalCatalog(config)
  val resolvedCatalog = (catalog ?: defaultCatalog).withPlayers(players)
  val base = resolvedCatalog.gamePremise(config, initialComponentTypes)
  if (catalog == null) return base
  val extensionClassNames =
      catalog.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className } -
          defaultCatalog.explicitClassDeclarations.mapTo(hashSetOf()) { it.className }
  return base.copy(
      classSelections = base.classSelections + extensionClassNames.map { ClassSelection(it) },
  )
}

internal fun canonicalCatalog(config: GameConfig): TfmCatalog =
    canonicalCatalog(cn("FakeStuffBundle") in config.includedClassNames)

internal fun canonicalCatalog(includeFakeCards: Boolean): TfmCatalog =
    if (includeFakeCards) CANON_WITH_FAKE_CARDS else Canon

private val CANON_WITH_FAKE_CARDS: TfmCatalog by lazy { TfmCatalog.compose(Canon, FakeCanon) }

private fun canonicalOptions(vararg selectedOptions: TestOption): Set<TestOption> {
  val selectedMaps = selectedOptions.filterTo(linkedSetOf()) { it in MAP_OPTIONS }
  require(selectedMaps.size <= 1) { "select at most one map" }
  return selectedOptions.toSet()
}

private val MAP_OPTIONS =
    setOf(
        TestOption.Tharsis,
        TestOption.Hellas,
        TestOption.Elysium,
        TestOption.Amazonis,
        TestOption.Vastitas,
        TestOption.Utopia,
        TestOption.Cimmeria,
    )

object TestHelpers {
  fun testColonyTiles(players: Int, vararg included: String): Set<ClassName> {
    require(players > 0)
    val count = if (players == 1) 4 else if (players == 2) 5 else players + 2
    val selected = included.mapTo(linkedSetOf(), ::cn)
    TEST_COLONY_TILES.map(::cn).filterNotTo(selected) { it in selected }
    return selected.take(count).toSet()
  }

  fun TfmGameplay.assertCounts(vararg pairs: Pair<Int, String>) =
      pairs.map { this.count(it.second) } shouldBe pairs.map { it.first }

  fun TfmGameplay.assertProds(vararg pairs: Pair<Int, String>) =
      pairs.map { production(cn(it.second)) } shouldBe pairs.map { it.first }

  fun assertNetChanges(
      result: TaskResult,
      game: World,
      expectedAsInstructions: String,
  ) {
    val inferredOwner = result.inferredExpectationOwner(game)
    val elaborator = PetElaborator(game.classTable)
    // Gain/Remove are only signed-count notation in this assertion DSL. Elaborating the whole
    // instruction would wrongly apply mutation defaults and atomization, so elaborate each queried
    // Expression through the public input operation. The final dispatcher handles marked syntax
    // above Expression level, such as PROD; marked syntax nested within an Expression has already
    // been consumed or deliberately preserved by its elaboration.
    val preprocessor =
        chain(
            object : PetTransformer() {
              override fun transformNode(node: PetNode): PetNode =
                  if (node is Expression) {
                    elaborator.elaborateInput(node, inferredOwner)
                  } else {
                    transformChildren(node)
                  }
            },
            game.classTable.transformDispatcher(),
        )

    // Zero is not a valid instruction scalar, so preserve its position with a value that can pass
    // through the normal parser and transformers before restoring it as an expected count.
    val parseableExpectations =
        expectedAsInstructions.replace(ZERO_SCALAR_REGEX, ZERO_SCALAR_SENTINEL.toString())
    val parsedExpectations = Parsing.parse<InstructionTree>(parseableExpectations)
    val emptyArgumentList =
        parsedExpectations.descendantsOfType<Expression>().firstOrNull {
          it.argumentsSpecified && it.arguments.isEmpty()
        }
    if (emptyArgumentList != null) {
      throw IllegalArgumentException(
          "empty argument lists are not allowed in net-change expectations; write `${emptyArgumentList.className}` instead of `$emptyArgumentList`"
      )
    }
    val instruction = preprocessor.transformInstructionTree(parsedExpectations)

    val expectedCountsToTypes: List<Pair<Int, Expression>> =
        InstructionGroup.of(instruction).instructions.map {
          when (it) {
            is Gain ->
                (it.scaledEx.scalar as ActualScalar).value.expectedCount() to it.scaledEx.expression
            is Remove ->
                -(it.scaledEx.scalar as ActualScalar).value.expectedCount() to
                    it.scaledEx.expression
            else -> error("")
          }
        }

    val types: List<Type> = expectedCountsToTypes.map { game.reader.resolve(it.second) }
    val expectedCounts = expectedCountsToTypes.map { it.first }

    val actuals = MutableList(types.size) { 0 }
    for (change in result.net()) {
      val g = change.gaining?.let(game.reader::resolve)
      val r = change.removing?.let(game.reader::resolve)
      for ((index, type) in types.withIndex()) {
        if (g?.isSubtypeOf(type) == true) actuals[index] += change.count
        if (r?.isSubtypeOf(type) == true) actuals[index] -= change.count
      }
    }
    actuals shouldBe expectedCounts
  }

  private fun Int.expectedCount(): Int = if (this == ZERO_SCALAR_SENTINEL) 0 else this

  private fun TaskResult.inferredExpectationOwner(game: World): Player? {
    // The first change normally retains the agent caller. An explicit `BY Admin` loses that
    // signal, so fall back only when every owned change points to the same Player.
    (changes.firstOrNull()?.actor as? Player)?.let {
      return it
    }

    return changes
        .flatMap { listOfNotNull(it.change.gaining, it.change.removing) }
        .mapNotNull {
          val ownerName = game.reader.resolve(it).toComponent().owner?.className
          game.actors.filterIsInstance<Player>().singleOrNull { player ->
            player.className == ownerName
          }
        }
        .distinct()
        .singleOrNull()
  }

  private const val ZERO_SCALAR_SENTINEL = 987_654_321
  private val ZERO_SCALAR_REGEX = Regex("(?<![A-Za-z0-9_])0(?=\\s|\\]|$)")

  private val TEST_COLONY_TILES =
      listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto", "Io", "Europa", "Pluto")
}
