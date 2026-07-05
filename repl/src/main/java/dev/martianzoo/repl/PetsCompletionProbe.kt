package dev.martianzoo.repl

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
      candidates: List<ReplCompletion>,
  ): List<ReplCompletion> =
      candidates
          .filter { it.startsWith(prefix, ignoreCase = true) }
          .filter { root.accepts(sourceBeforePrefix, it.value) }

  private fun PetsCompletionRoot.accepts(sourceBeforePrefix: String, candidate: String): Boolean =
      parserTypes.any { it.acceptsPetsCompletion(sourceBeforePrefix, candidate) }

  private fun KClass<out PetNode>.acceptsPetsCompletion(
      sourceBeforePrefix: String,
      candidate: String,
  ): Boolean {
    val base = sourceBeforePrefix + candidate
    return COMPLETION_FILLERS.any { filler ->
      val filled = base + filler
      val probe = filled + closingSuffix(filled)
      parses(probe)
    }
  }

  private fun KClass<out PetNode>.parses(source: String): Boolean =
      try {
        Parsing.parse(this, source)
        true
      } catch (e: Exception) {
        false
      }

  private fun closingSuffix(source: String): String {
    val closers = ArrayDeque<Char>()
    for (c in source) {
      when (c) {
        '(' -> closers.addFirst(')')
        '[' -> closers.addFirst(']')
        '<' -> closers.addFirst('>')
        ')' -> closers.removeFirstIf(')')
        ']' -> closers.removeFirstIf(']')
        '>' -> closers.removeFirstIf('>')
      }
    }
    return closers.joinToString("")
  }

  private fun ArrayDeque<Char>.removeFirstIf(c: Char) {
    if (firstOrNull() == c) removeFirst()
  }

  private val COMPLETION_FILLERS =
      listOf(
          "",
          " Plant",
          " 1",
          " 1 Plant",
          "[Plant]",
          " FROM Plant",
          " OR Plant",
          " THEN Plant",
          " MAX 1",
      )
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
