package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.Player.Companion.players
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import java.util.concurrent.atomic.AtomicInteger

object TestHelpers {
  fun TfmGameplay.nextGeneration(vararg cardsBought: Int) {
    phase("Production")
    phase("Research") {
      for ((cards, player) in cardsBought.zip(players(5))) {
        asPlayer(player).doTask(if (cards > 0) "$cards BuyCard" else "Ok")
      }
    }
    phase("Action")
  }

  fun TfmGameplay.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { this.count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun TfmGameplay.assertProds(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { production(cn(it.second)) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun assertNetChanges(result: TaskResult, tfm: TfmGameplay, expectedAsInstructions: String) {
    val preprocessor =
        with(Transformers(tfm.game.classes)) {
          chain(
              useFullNames(),
              insertExpressionDefaults(THIS.expression),
              deprodify(),
              replaceOwnerWith(tfm.player))
        }

    // Abusing the fact that these strings just happen to resemble instruction strings... except
    // that this is currently preventing 0 TODO
    val instruction = preprocessor.transform(Parsing.parse<Instruction>(expectedAsInstructions))

    val expectedCountsToTypes: List<Pair<Int, Expression>> =
        split(instruction).map {
          when (it) {
            is Gain -> (it.scaledEx.scalar as ActualScalar).value to it.scaledEx.expression
            is Remove -> -(it.scaledEx.scalar as ActualScalar).value to it.scaledEx.expression
            else -> error("")
          }
        }

    val types: Sequence<Type> =
        expectedCountsToTypes.asSequence().map { tfm.reader.resolve(it.second) }
    val expectedCounts = expectedCountsToTypes.map { it.first }

    val zip = types.zip(generateSequence { AtomicInteger(0) }).toList()
    for (change in result.net()) {
      val g = change.gaining?.let(tfm.reader::resolve)
      val r = change.removing?.let(tfm.reader::resolve)
      for ((type, ai) in zip) {
        if (g?.narrows(type) == true) ai.addAndGet(change.count)
        if (r?.narrows(type) == true) ai.addAndGet(-change.count)
      }
    }
    val actuals: List<Int> = zip.map { it.second.toInt() }
    assertThat(actuals).containsExactlyElementsIn(expectedCounts).inOrder()
  }
}
