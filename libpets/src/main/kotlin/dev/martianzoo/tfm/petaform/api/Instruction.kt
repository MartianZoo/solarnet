package dev.martianzoo.tfm.petaform.api

sealed class Instruction : PetaformObject() {
  data class Gain(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: Expression, scalar: Int = 1, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init { qe.scalar >= 0 }
    override fun toString() = "${qe}${intensity?.petaform ?: ""}"
  }

  data class Remove(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: Expression, scalar: Int = 1, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init { qe.scalar >= 0 }
    override fun toString() = "-${qe}${intensity?.petaform ?: ""}"
  }

  data class Multi(var instructions: List<Instruction>) : Instruction() {
    override fun toString() = instructions.joinToString {
      when (it) {
        is Gated -> "(${it})"
        else -> "$it"
      }
    }
  }

  data class Or(var instructions: List<Instruction>) : Instruction() {
    override fun toString() = instructions.joinToString(" OR ") {
      // precedence is against us
      when (it) {
        is Multi -> "(${it})"
        is Gated -> "(${it})"
        else -> "$it"
      }
    }
  }

  data class Prod(val instruction: Instruction) : Instruction() {
    override fun toString() = "PROD[${instruction}]"
  }

  data class Per(val instruction: Instruction, val qe: QuantifiedExpression): Instruction() {
    override fun toString() = "${instruction} / ${qe.petaform(forceExpression = true)}"
  }

  data class Gated(val predicate: Predicate, val instruction: Instruction): Instruction() {
    override fun toString(): String {
      val pred = when (predicate) {
        is Predicate.Or -> "(${predicate})"
        is Predicate.And -> "(${predicate})"
        else -> "$predicate"
      }
      val instr = when (instruction) {
        is Or -> "(${instruction})"
        is Multi -> "(${instruction})"
        else -> "$instruction"
      }
      return "$pred: $instr"
    }
  }

  enum class Intensity(val symbol: String) {
    MANDATORY("!"),
    AMAP("."),
    OPTIONAL("?"),
    ;

    val petaform: String = symbol

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
