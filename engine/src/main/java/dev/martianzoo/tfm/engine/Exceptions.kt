package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.types.MType

internal class Exceptions {
  class RequirementException(message: String) : RuntimeException(message)

  // These should all be rectifiable by narrowing the instruction
  class AbstractInstructionException(message: String) : RuntimeException(message) {
    constructor(type: MType) : this("Component type $type is abstract")
    constructor(
        instruction: Change,
        intensity: Intensity?,
    ) : this("Change instruction has intensity $intensity: $instruction")

    constructor(instruction: Or) : this("Can't execute an OR instruction: $instruction")
  }

  class DependencyException(message: String) : RuntimeException(message) {
    companion object {
      fun gaining(gaining: Component, dependencies: Collection<Component>) =
          DependencyException(
              "Can't gain $gaining as dependencies are missing: $dependencies")
      fun removing(removing: Component, dependents: Collection<Component>) =
          DependencyException("Can't remove $removing as it has dependents: $dependents")
    }
  }

  class InvalidExpressionException(message: String) : RuntimeException(message)
}
