package dev.martianzoo.script

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import kotlin.reflect.KClass

internal object PetsCompletionProbe {
  fun words(
      root: PetsCompletionRoot,
      sourceBeforePrefix: String,
      prefix: String,
      candidates: List<ScriptCompletion>,
  ): List<ScriptCompletion> =
      candidates
          .filter { it.startsWith(prefix, ignoreCase = true) }
          .filter { root.accepts(sourceBeforePrefix, it.value) }

  private fun PetsCompletionRoot.accepts(sourceBeforePrefix: String, candidate: String): Boolean =
      parserTypes.any { it.acceptsPetsCompletion(sourceBeforePrefix, candidate) }

  private fun KClass<out PetNode>.acceptsPetsCompletion(
      sourceBeforePrefix: String,
      candidate: String,
  ): Boolean = Parsing.acceptsNextToken(this, sourceBeforePrefix, candidate)
}

internal enum class PetsCompletionRoot {
  ANY,
  EXPRESSION,
  INSTRUCTION,
  METRIC,
  REQUIREMENT,
  ;

  val parserTypes: List<KClass<out PetNode>>
    get() =
        when (this) {
          ANY -> listOf(Instruction::class, Expression::class, Metric::class, Requirement::class)
          EXPRESSION -> listOf(Expression::class)
          INSTRUCTION -> listOf(Instruction::class)
          METRIC -> listOf(Metric::class)
          REQUIREMENT -> listOf(Requirement::class)
        }
}
