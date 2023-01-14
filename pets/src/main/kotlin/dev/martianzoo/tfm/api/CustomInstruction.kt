package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.TypeExpression

abstract class CustomInstruction(val functionName: String) {
  /** name should be lowerCamelCase; it will be usable in PETS by prefixing it with `$`. */

  /** Preferred! */
  open fun translate(game: ReadOnlyGameState, arguments: List<TypeExpression>): Instruction {
    return parsePets(translateToPets(game, arguments))
  }

  /** Second choice! */
  /*@ForOverride*/ protected open fun translateToPets(
      game: ReadOnlyGameState,
      arguments: List<TypeExpression>
  ): String {
    throw ExecuteInsteadException()
  }

  class ExecuteInsteadException : RuntimeException("foo")

  open fun execute(game: GameState, arguments: List<TypeExpression>) {
    throw NotImplementedError()
  }
}
