package dev.martianzoo.pets.api

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type

/**
 * Kotlin implementation of the instruction queued when a Pets `CustomInstruction` is gained. The
 * result may be one instruction, a group of independent instructions, or a no-op. An unimplemented
 * dependency arity fails with [ExpressionException].
 */
public abstract class CustomInstruction(name: String? = null) : CustomClass(name) {
  public constructor(className: ClassName) : this(className.toString())

  public open fun translate(game: GameReader): InstructionTree = noInstructionBehavior(0)

  public open fun translate(game: GameReader, type0: Type): InstructionTree =
      noInstructionBehavior(1)

  public open fun translate(game: GameReader, type0: Type, type1: Type): InstructionTree =
      noInstructionBehavior(2)

  public open fun translate(
      game: GameReader,
      type0: Type,
      type1: Type,
      type2: Type,
  ): InstructionTree = noInstructionBehavior(3)

  public open fun translate(
      game: GameReader,
      type0: Type,
      type1: Type,
      type2: Type,
      type3: Type,
  ): InstructionTree = noInstructionBehavior(4)

  private fun noInstructionBehavior(dependencies: Int): Nothing =
      throw ExpressionException(
          "custom instruction `$className` has no implementation for $dependencies " +
              if (dependencies == 1) "dependency" else "dependencies"
      )
}
