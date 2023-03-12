package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.types.MType

public class Exceptions {
  public class RequirementException(message: String) : RuntimeException(message)

  // These should all be rectifiable by narrowing the instruction
  public class AbstractInstructionException(message: String) : RuntimeException(message) {
    constructor(type: MType) : this("Component type ${type.expression} is abstract")
    constructor(
        instruction: Change,
        intensity: Intensity?,
    ) : this("Instruction has intensity $intensity: $instruction")

    constructor(instruction: Or) : this("OR instructions are abstract: $instruction")
  }

  public class DependencyException(message: String) : RuntimeException(message) {
    companion object {
      fun gaining(gaining: Component, dependencies: Collection<Component>) =
          DependencyException("Missing dependencies: ${dependencies.joinToString()}")

      fun removing(removing: Component, dependents: Collection<Component>) =
          DependencyException("Existing dependents: ${dependents.joinToString()}")
    }
  }

  public class InvalidExpressionException(message: String) : RuntimeException(message)
}
