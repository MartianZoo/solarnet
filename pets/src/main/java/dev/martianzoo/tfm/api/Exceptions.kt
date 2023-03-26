package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Or

public class Exceptions {
  open class UserException(override val message: String) : RuntimeException(message)

  // These should all be rectifiable by narrowing the instruction
  public class AbstractInstructionException(val instruction: Instruction?, message: String) :
      UserException(message) {
    constructor(
        type: Type,
        instruction: Instruction? = null
    ) : this(instruction, "abstract component: ${type.expression}")

    constructor(
        instruction: Instruction,
        intensity: Intensity?,
    ) : this(instruction, "amount is ${intensity.toString().lowercase()}")

    constructor(instruction: Or) : this(instruction, "OR requires a choice")
  }

  public class UnrecognizedClassException(className: ClassName) :
      UserException("Class name `$className` is unrecognized (with current game parameters)")

  public class InvalidReificationException(message: String) : UserException(message)

  public class RequirementException(message: String) : UserException(message)
}
