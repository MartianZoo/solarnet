package dev.martianzoo.tfm.engine

import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.api.Type
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Game
import dev.martianzoo.engine.Transformers
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Companion.split
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import io.kotest.matchers.shouldBe

object TestHelpers {
  fun TfmGameplay.assertCounts(vararg pairs: Pair<Int, String>) =
      pairs.map { this.count(it.second) } shouldBe pairs.map { it.first }

  fun TfmGameplay.assertProds(vararg pairs: Pair<Int, String>) =
      pairs.map { production(cn(it.second)) } shouldBe pairs.map { it.first }

  fun assertNetChanges(
      result: TaskResult,
      game: Game,
      tfm: TfmGameplay,
      expectedAsInstructions: String,
  ) {
    val preprocessor =
        with(Transformers(game.classes)) {
          chain(
              useFullNames(),
              insertExpressionDefaults(THIS.expression),
              Prod.deprodify(classes),
              replaceOwnerWith(tfm.player),
          )
        }

    // Abusing the fact that these strings just happen to resemble instruction strings... except
    // that this is currently preventing 0, sigh
    val instruction = preprocessor.transform(Parsing.parse<Instruction>(expectedAsInstructions))

    val expectedCountsToTypes: List<Pair<Int, Expression>> =
        split(instruction).map {
          when (it) {
            is Gain -> (it.scaledEx.scalar as ActualScalar).value to it.scaledEx.expression
            is Remove -> -(it.scaledEx.scalar as ActualScalar).value to it.scaledEx.expression
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
        if (g?.narrows(type) == true) actuals[index] += change.count
        if (r?.narrows(type) == true) actuals[index] -= change.count
      }
    }
    actuals shouldBe expectedCounts
  }
}
