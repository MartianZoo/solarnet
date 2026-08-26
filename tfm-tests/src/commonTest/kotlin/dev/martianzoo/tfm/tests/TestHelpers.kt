package dev.martianzoo.tfm.tests

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Transformers
import dev.martianzoo.engine.World
import dev.martianzoo.engine.toComponent
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
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
import io.kotest.matchers.shouldBe

internal fun setUpGame(premise: GamePremise): World =
    Engine.newGame(premise, inputOnlySynonyms = TEST_CLASS_SYNONYMS).apply {
      TfmWorkflow.Manual(this).setupPhase()
    }

internal val TEST_CLASS_SYNONYMS: List<Pair<String, String>> =
    listOf(
        "M" to "Megacredit",
        "S" to "Steel",
        "T" to "Titanium",
        "P" to "Plant",
        "E" to "Energy",
        "H" to "Heat",
        "TR" to "TerraformRating",
        "VP" to "VictoryPoint",
    )

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
): GamePremise {
  val included = selectedOptions.filterIsInstance<TestOption>()
  val excluded = selectedOptions.filterIsInstance<ExcludedTestOption>().map { it.option }.toSet()
  return canonicalPremise(
      canonicalOptions(*included.toTypedArray()),
      players,
      colonyTiles,
      catalog,
      excluded,
  )
}

internal fun canonicalPremise(
    options: Set<TestOption>,
    players: Int = 2,
    colonyTiles: Set<ClassName> = emptySet(),
    catalog: TfmCatalog? = null,
    excludedOptions: Set<TestOption> = emptySet(),
): GamePremise {
  val config =
      GameConfig.create(
          included =
              options.map(TestOption::className) +
                  colonyTiles.map(TEST_ENGLISH_VOCABULARY::canonicalName),
          excluded = excludedOptions.map(TestOption::className),
          playerNames =
              if (players == 1) listOf(cn("Me")) else (1..players).map { cn("Player$it") },
      )
  val base = Canon.gamePremise(config)
  if (catalog == null) return base
  val extensionClassNames =
      catalog.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className } -
          Canon.explicitClassDeclarations.mapTo(hashSetOf()) { it.className }
  return base.copy(
      catalog = catalog,
      classSelections = base.classSelections + extensionClassNames.map { ClassSelection(it) },
  )
}

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
        TestOption.Utopia,
        TestOption.Cimmeria,
    )

object TestHelpers {
  fun testColonyTiles(players: Int, vararg included: String): Set<ClassName> {
    require(players in 1..5)
    val count = if (players == 1) 4 else if (players == 2) 5 else players + 2
    val selected = included.mapTo(linkedSetOf()) { TEST_ENGLISH_VOCABULARY.canonicalName(cn(it)) }
    TEST_COLONY_TILES.map { TEST_ENGLISH_VOCABULARY.canonicalName(cn(it)) }
        .filterNotTo(selected) { it in selected }
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
    val preprocessor =
        with(Transformers(game.classTable)) {
          chain(
              canonicalize(game.vocabulary),
              useFullNames(),
              insertExpressionDefaults(THIS.expression),
              transformMarkedSyntax(),
              inferredOwner?.let(::replaceOwnerWith),
          )
        }

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
    // The first change normally retains the gameplay caller. An explicit `BY Engine` loses that
    // signal, so fall back only when every owned change points to the same Player.
    (changes.firstOrNull()?.actor as? Player)?.let {
      return it
    }

    return changes
        .flatMap { listOfNotNull(it.change.gaining, it.change.removing) }
        .mapNotNull {
          game.reader.resolve(it).toComponent().owner?.className?.let(Player::fromClassNameOrNull)
        }
        .distinct()
        .singleOrNull()
  }

  private const val ZERO_SCALAR_SENTINEL = 987_654_321
  private val ZERO_SCALAR_REGEX = Regex("(?<![A-Za-z0-9_])0(?=\\s|\\]|$)")

  private val TEST_COLONY_TILES =
      listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto", "Io", "Europa", "Pluto")
}

private val TEST_ENGLISH_VOCABULARY =
    Vocabulary.create(
        Canon,
        activeClassNames = Canon.colonyTileClassNames,
    )
