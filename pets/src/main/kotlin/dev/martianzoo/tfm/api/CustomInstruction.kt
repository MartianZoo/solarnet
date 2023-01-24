package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.TypeExpr

abstract class CustomInstruction(val functionName: String) {
  /** name should be lowerCamelCase; it will be usable in PETS by prefixing it with `@`. */

  /** Preferred! */
  open fun translate(game: ReadOnlyGameState, arguments: List<TypeExpr>): Instruction {
    throw ExecuteInsteadException()
  }

  class ExecuteInsteadException : RuntimeException("foo")

  open fun execute(game: GameState, arguments: List<TypeExpr>) {
    throw NotImplementedError()
  }
}
