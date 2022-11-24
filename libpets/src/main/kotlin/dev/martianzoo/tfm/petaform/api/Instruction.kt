package dev.martianzoo.tfm.petaform.api

sealed interface Instruction : PetaformObject {
  data class Gain(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction {
    constructor(expr: Expression, scalar: Int = 1, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init { qe.scalar >= 0 }
    override val petaform = "${qe.petaform}${intensity?.petaform ?: ""}"
  }

  data class Remove(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction {
    constructor(expr: Expression, scalar: Int = 1, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init { qe.scalar >= 0 }
    override val petaform = "-${qe.petaform}${intensity?.petaform ?: ""}"
  }

  data class Multi(var instructions: List<Instruction>) : Instruction {
    override val petaform = instructions.joinToString { it.petaform }
  }

  data class Or(var instructions: List<Instruction>) : Instruction {
    override val petaform = instructions.joinToString(" OR ") {
      // precedence is against us
      if (it is Multi) "(${it.petaform})" else it.petaform
    }
  }

  data class Prod(val instruction: Instruction) : Instruction {
    override val petaform = "PROD[${instruction.petaform}]"
  }

  data class Per(val instruction: Instruction, val qe: QuantifiedExpression): Instruction {
    override val petaform: String = "${instruction.petaform} / ${qe.petaform(forceExpression = true)}"
  }

  enum class Intensity(val symbol: String): PetaformObject {
    MANDATORY("!"),
    AMAP("."),
    OPTIONAL("?"),
    ;

    override val petaform: String = symbol

    companion object {
      fun forSymbol(symbol: String) = values().first { it.symbol == symbol }
    }
  }

  companion object {
    fun and(vararg instructions: Instruction) = and(instructions.toList())
    fun and(instructions: List<Instruction>): Instruction =
        if (instructions.size == 1) {
          instructions[0]
        } else {
          Multi(instructions.flatMap {
            if (it is Multi) it.instructions else listOf(it)
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
