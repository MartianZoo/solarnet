package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.types.MType

public class Exceptions {
  public class RequirementException(message: String) : RuntimeException(message)

  // These should all be rectifiable by narrowing the instruction
  public class AbstractInstructionException(message: String) : RuntimeException(message) {
    constructor(type: MType) : this("Component type ${type.expression} is abstract")
    constructor(intensity: Intensity?) : this("An instruction has intensity $intensity")
    constructor(unused: Or) : this("An OR instruction must be reified (i.e., pick one)")
  }

  public class DependencyException(message: String) : RuntimeException(message) {
    companion object {
      fun gaining(dependencies: Collection<Component>) =
          DependencyException("Missing dependencies: ${dependencies.joinToString()}")

      fun removing(dependents: Collection<Component>) =
          DependencyException("Existing dependents: ${dependents.joinToString()}")
    }
  }

  public class InvalidExpressionException(message: String) : RuntimeException(message)
}
