package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Or

// These should all be rectifiable by narrowing the instruction
public class AbstractInstructionException(message: String) : RuntimeException(message) {
  constructor(type: Type) : this("Component type ${type.expression} is abstract")
  constructor(intensity: Intensity?) : this("An instruction has intensity $intensity")
  constructor(unused: Or) : this("An OR instruction must be reified (i.e., pick one)")
}
