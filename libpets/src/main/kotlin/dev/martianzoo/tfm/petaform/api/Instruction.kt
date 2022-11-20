package dev.martianzoo.tfm.petaform.api

sealed interface Instruction : PetaformObject {
  data class GainInstruction(val qe: QuantifiedExpression) : Instruction {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    override val petaform = qe.petaform
  }

  data class RemoveInstruction(val qe: QuantifiedExpression) : Instruction {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    override val petaform = "-${qe.petaform}"
  }

  data class AndInstruction(var instructions: List<Instruction>) : Instruction {
    companion object {
      fun from(vararg instructions: Instruction) = from(instructions.toList())
      fun from(instructions: List<Instruction>) =
          if (instructions.size == 1)
            instructions[0]
          else
            AndInstruction(instructions.flatMap {
              if (it is AndInstruction) it.instructions else listOf(it)
            })

    }

    override val petaform = instructions.joinToString { it.petaform }
  }

  data class OrInstruction(var instructions: List<Instruction>) : Instruction {
    companion object {
      fun from(vararg instructions: Instruction) = from(instructions.toList())
      fun from(instructions: List<Instruction>) =
          if (instructions.size == 1)
            instructions[0]
          else
            OrInstruction(instructions.flatMap {
              if (it is OrInstruction) it.instructions else listOf(it)
            })
    }

    override val petaform = instructions.joinToString(" OR ") {
      // precedence is against us ... TODO: does that fix it?
      if (it is AndInstruction) "(${it.petaform})" else it.petaform
    }
  }

  data class ProdInstruction(var instruction: Instruction) : Instruction {
    override val petaform = "PROD[$instruction]"
  }
}
