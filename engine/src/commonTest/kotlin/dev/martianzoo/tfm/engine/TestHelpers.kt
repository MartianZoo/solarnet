package dev.martianzoo.tfm.engine

import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.Player
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Transformers
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ClassSynonyms
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Option
import dev.martianzoo.tfm.canon.Canon.OptionSelection
import dev.martianzoo.types.Type
import io.kotest.matchers.shouldBe

internal fun setUpGame(premise: GamePremise): World =
    Engine.newGame(premise, TEST_CLASS_SYNONYMS).apply { TfmWorkflow.Manual(this).setupPhase() }

internal val TEST_CLASS_SYNONYMS: ClassSynonyms =
    ClassSynonyms.of(
        "P1" to "Player1",
        "P2" to "Player2",
        "P3" to "Player3",
        "P4" to "Player4",
        "P5" to "Player5",
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
    vararg selectedOptions: OptionSelection,
    players: Int = 2,
    colonyTiles: Set<ClassName> = emptySet(),
): World =
    setUpGame(canonicalPremise(*selectedOptions, players = players, colonyTiles = colonyTiles))

internal fun canonicalPremise(
    vararg selectedOptions: OptionSelection,
    players: Int = 2,
    colonyTiles: Set<ClassName> = emptySet(),
    ruleset: TfmRuleset? = null,
): GamePremise =
    with(Canon.GameOptions(selectedOptions.asIterable())) {
      canonicalPremise(
          canonicalOptions(*included.toTypedArray()),
          players,
          colonyTiles,
          ruleset,
          excluded,
      )
    }

internal fun canonicalPremise(
    options: Set<Option>,
    players: Int = 2,
    colonyTiles: Set<ClassName> = emptySet(),
    ruleset: TfmRuleset? = null,
    excludedOptions: Set<Option> = emptySet(),
): GamePremise {
  val setupWorld =
      Engine.newSetupWorld(
          Canon.setupWorldDefinition(
              players,
              Canon.GameOptions(options, excludedOptions),
              colonyTiles,
          ),
          TEST_CLASS_SYNONYMS,
      )
  setupWorld.gameplay(ENGINE).godMode().manual("ValidateSetup")
  val base = Canon.assemble(setupWorld.reader)
  if (ruleset == null) return base
  val bundleNames = (base.ruleset as TfmRuleset).bundles.mapTo(linkedSetOf()) { it.bundleName }
  val selectedRuleset = ruleset.resolve(bundleNames, setupWorld.reader)
  return base.copy(
      ruleset = selectedRuleset,
      rootClassNames = base.rootClassNames + selectedRuleset.allDefinitions.classNames(),
  )
}

internal fun canonicalOptions(vararg selectedOptions: Option): Set<Option> {
  val selectedMaps = selectedOptions.filterTo(linkedSetOf()) { it in MAP_OPTIONS }
  require(selectedMaps.size <= 1) { "select at most one map" }
  val defaults =
      if (selectedMaps.isEmpty()) Canon.Option.DEFAULTS else Canon.Option.DEFAULTS - MAP_OPTIONS
  return defaults + selectedOptions
}

private val MAP_OPTIONS =
    setOf(
        Option.TharsisMapOption,
        Option.HellasMapOption,
        Option.ElysiumMapOption,
        Option.UtopiaPlanitiaMapOption,
        Option.TerraCimmeriaMapOption,
    )

object TestHelpers {
  fun testColonyTiles(players: Int, vararg included: String): Set<ClassName> {
    val count = Canon.requiredColonyTileCount(players)
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
      tfm: TfmGameplay,
      expectedAsInstructions: String,
  ) {
    val preprocessor =
        with(Transformers(game.classTable)) {
          chain(
              useFullNames(),
              insertExpressionDefaults(THIS.expression),
              Prod.deprodify(classTable),
              (tfm.actor as? Player)?.let(::replaceOwnerWith),
          )
        }

    // Zero is not a valid instruction scalar, so preserve its position with a value that can pass
    // through the normal parser and transformers before restoring it as an expected count.
    val parseableExpectations =
        expectedAsInstructions.replace(ZERO_SCALAR_REGEX, ZERO_SCALAR_SENTINEL.toString())
    val instruction = preprocessor.transform(Parsing.parse<Instruction>(parseableExpectations))

    val expectedCountsToTypes: List<Pair<Int, Expression>> =
        split(instruction).map {
          when (it) {
            is Gain ->
                (it.scaledEx.scalar as ActualScalar).value.expectedCount() to it.scaledEx.expression
            is Remove ->
                -(it.scaledEx.scalar as ActualScalar).value.expectedCount() to
                    it.scaledEx.expression
            else -> error("")
          }
        }

    val types: List<Type> = expectedCountsToTypes.map { tfm.reader.resolve(it.second) }
    val expectedCounts = expectedCountsToTypes.map { it.first }

    val actuals = MutableList(types.size) { 0 }
    for (change in result.net()) {
      val g = change.gaining?.let(tfm.reader::resolve)
      val r = change.removing?.let(tfm.reader::resolve)
      for ((index, type) in types.withIndex()) {
        if (g?.isSubtypeOf(type) == true) actuals[index] += change.count
        if (r?.isSubtypeOf(type) == true) actuals[index] -= change.count
      }
    }
    actuals shouldBe expectedCounts
  }

  private fun Int.expectedCount(): Int = if (this == ZERO_SCALAR_SENTINEL) 0 else this

  private const val ZERO_SCALAR_SENTINEL = 987_654_321
  private val ZERO_SCALAR_REGEX = Regex("(?<![A-Za-z0-9_])0(?=\\s|])")

  private val TEST_COLONY_TILES =
      listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto", "Io", "Europa", "Pluto")
}
