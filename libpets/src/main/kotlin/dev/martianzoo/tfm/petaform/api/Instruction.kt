package dev.martianzoo.tfm.petaform.api

sealed interface Instruction : PetaformObject {
  data class Gain(val qe: QuantifiedExpression) : Instruction {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    override val petaform = qe.petaform
  }

  data class Remove(val qe: QuantifiedExpression) : Instruction {
    constructor(expr: Expression, scalar: Int = 1) : this(QuantifiedExpression(expr, scalar))
    override val petaform = "-${qe.petaform}"
  }

  data class And(var instructions: List<Instruction>) : Instruction {
    override val petaform = instructions.joinToString { it.petaform }
  }

  data class Or(var instructions: List<Instruction>) : Instruction {
    override val petaform = instructions.joinToString(" OR ") {
      // precedence is against us ... TODO: does that fix it?
      if (it is And) "(${it.petaform})" else it.petaform
    }
  }

  data class Prod(val instruction: Instruction) : Instruction {
    override val petaform = "PROD[$instruction]"
  }

  companion object {
    fun and(vararg instructions: Instruction) = and(instructions.toList())
    fun and(instructions: List<Instruction>): Instruction =
        if (instructions.size == 1) {
          instructions[0]
        } else {
          And(instructions.flatMap {
            if (it is And) it.instructions else listOf(it)
          })
        }

    fun or(vararg instructions: Instruction) = or(instructions.toList())
    fun or(instructions: List<Instruction>) =
        if (instructions.size == 1) {
          instructions[0]
        } else {
          Or(instructions.flatMap {
            if (it is Or) it.instructions else listOf(it)
          })
        }
  }
}