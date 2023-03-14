package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Or

public class Exceptions {
  abstract class UserException(override val message: String) : RuntimeException(message)

  // These should all be rectifiable by narrowing the instruction
  public class AbstractInstructionException(val instruction: Instruction?, message: String) :
      UserException(message) {
    constructor(type: Type) :
        this(null, "Component type ${type.expression} is abstract in instruction")

    constructor(instruction: Instruction, intensity: Intensity?) :
        this(instruction, "An instruction has intensity $intensity: $instruction")

    constructor(instruction: Or) :
        this(instruction, "An OR instruction must be reified (i.e., pick one): $instruction")
  }

  public class UnrecognizedClassException(className: ClassName) :
      UserException("Class name `$className` is unrecognized (with current game parameters)")

  public class InvalidReificationException(message: String) : UserException(message)
}
