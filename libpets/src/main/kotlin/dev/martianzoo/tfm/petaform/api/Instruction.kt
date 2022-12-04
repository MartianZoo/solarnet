package dev.martianzoo.tfm.petaform.api

sealed class Instruction : PetaformNode() {
  data class Gain(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: Expression, scalar: Int = 1, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init { qe.scalar >= 0 }
    override val children = listOf(qe)
    override fun toString() = "${qe}${intensity?.petaform ?: ""}"
  }

  data class Remove(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: Expression, scalar: Int = 1, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init { qe.scalar >= 0 }
    override val children = listOf(qe)
    override fun toString() = "-${qe}${intensity?.petaform ?: ""}"
  }

  data class Multi(var instructions: List<Instruction>) : Instruction() {
    override val children = instructions
    override fun toString() = instructions.joinToString {
      it.toStringWithin(this)
    }
    override fun precedence() = 1
  }

  data class Or(var instructions: List<Instruction>) : Instruction() {
    override val children = instructions
    override fun toString() = instructions.joinToString(" OR ") {
      it.toStringWithin(this)
    }
    override fun precedence() = 3
  }

  data class Prod(val instruction: Instruction) : Instruction(), ProdBox {
    override val children = listOf(instruction)
    override fun toString() = "PROD[$instruction]"
    override fun countProds() = super.countProds() + 1
  }

  data class Per(val instruction: Instruction, val qe: QuantifiedExpression): Instruction() {
    override val children = listOf(instruction, qe)
    override fun toString() = "$instruction / ${qe.petaform(forceExpression = true)}"
    override fun precedence() = 5
  }

  data class Gated(val predicate: Predicate, val instruction: Instruction): Instruction() {
    override val children = listOf(predicate, instruction)
    override fun toString(): String {
      val pred = when (predicate) { // TODO generalize somehow
        is Predicate.Or -> "(${predicate})"
        is Predicate.And -> "(${predicate})"
        else -> "$predicate"
      }
      val instr = instruction.toStringWithin(this)
      return "$pred: $instr"
    }

    override fun precedence() = 4

    // let's over-group for clarity
    override fun groupWithin(container: Instruction) =
        container is Or || super.groupWithin(container)
  }

  enum class Intensity(val symbol: String) {
    MANDATORY("!"),
    AMAP("."),
    OPTIONAL("?"),
    ;

    val petaform: String = symbol

    companion object {
      fun intensity(symbol: String?) =
          symbol?.let { s -> values().first { it.symbol == s } }
    }
  }

  companion object {
    fun multi(vararg instructions: Instruction) = multi(instructions.toList())
    fun multi(instructions: List<Instruction>): Instruction =
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

  open fun precedence(): Int = 99

  fun toStringWithin(container: Instruction) =
      if (groupWithin(container)) "(${this})" else "$this"

  open fun groupWithin(container: Instruction) = precedence() <= container.precedence()
}
