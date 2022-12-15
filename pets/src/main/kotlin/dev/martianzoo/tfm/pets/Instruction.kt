package dev.martianzoo.tfm.pets

import dev.martianzoo.util.joinOrEmpty
import dev.martianzoo.util.toSetCarefulP

sealed class Instruction : PetsNode() {
  data class Gain(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: TypeExpression?, scalar: Int? = null, intensity: Intensity? = null) : this(QuantifiedExpression(expr, scalar), intensity)

    init {
      if ((qe.scalar ?: 1) <= 0) throw PetsException("Can't gain a non-positive amount")
    }

    override fun toString() = "${qe}${intensity?.pets ?: ""}"
    override val children = setOf(qe)
  }

  data class Remove(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: TypeExpression?, scalar: Int? = null, intensity: Intensity? = null) : this(QuantifiedExpression(expr, scalar), intensity)

    init {
      if ((qe.scalar ?: 1) <= 0) throw PetsException("Can't remove a non-positive amount")
    }

    override fun toString() = "-${qe}${intensity?.pets ?: ""}"
    override val children = setOf(qe)
  }

  sealed class FromExpression : PetsNode()

  data class SimpleFrom(val to: TypeExpression, val from: TypeExpression) : FromExpression() {
    init {
      if (to == from) {
        throw PetsException("to and from are the same: $to")
      }
    }
    override fun toString() = "$to FROM $from"
    override val children = setOf(to, from)
  }

  data class ComplexFrom(
      val className: String,
      val specializations: List<FromExpression> = listOf(),
      val predicate: Predicate? = null
  ) : FromExpression() {
    init {
      require(className.matches(classNamePattern())) { className }
      if (specializations.count { it is ComplexFrom || it is SimpleFrom } != 1) {
        throw PetsException("Can only have one FROM in an expression")
      }
    }

    override fun toString() =
        className +
            specializations.joinOrEmpty(surround = "<>") +
            (predicate?.let { "(HAS $it)" } ?: "")
    override val children = specializations + setOfNotNull(predicate)
  }

  data class TypeInFrom(val type: TypeExpression) : FromExpression() {
    override fun toString() = "$type"
    override val children = setOf(type)
  }

  data class Transmute(
      val trans: FromExpression,
      val scalar: Int? = null,
      val intensity: Intensity? = null) : Instruction() {
    init {
      if ((scalar ?: 1) < 1) throw PetsException("Can't do a non-positive number of transmutes")
      if (trans is TypeInFrom) throw PetsException("Should be a regular gain instruction")
    }
    override fun toString(): String {
      val intens = intensity?.pets ?: ""
      val scal = if (scalar != null) "$scalar " else ""
      return "$scal$trans$intens"
    }
    override val children = setOf(trans)
    override fun parenthesizeThisWhenInside(container: PetsNode): Boolean {
      if (trans is SimpleFrom && container is Or) {
        return true // not technically necessary, but helpful
      }
      return super.parenthesizeThisWhenInside(container)
    }

    override fun precedence() = if (trans is SimpleFrom) 7 else 10
  }

  data class Per(val instruction: Instruction, val qe: QuantifiedExpression): Instruction() {
    init {
      if (qe.typeExpression == null) throw PetsException("Use '/ 2 Megacredit', not just '/ 2'")
      if ((qe.scalar ?: 1) <= 0) throw PetsException("Can't do something 'per' a nonpositive amount")
      when (instruction) {
        is Gain, is Remove, is Transmute -> {}
        else -> throw PetsException("Per can only contain gain/remove/transmute")
      }
    }
    override fun toString() = "$instruction / $qe"
    override fun precedence() = 8
    override val children = setOf(instruction, qe)
  }

  data class Gated(val predicate: Predicate, val instruction: Instruction): Instruction() {
    init {
      if (instruction is Gated) {
        throw PetsException("You don't gate a gater")
      }
    }
    override fun toString(): String {
      return "${predicate.toStringWhenInside(this)}: ${instruction.toStringWhenInside(this)}"
    }

    // let's over-group for clarity
    override fun parenthesizeThisWhenInside(container: PetsNode) =
        container is Or || super.parenthesizeThisWhenInside(container)

    override fun precedence() = 6
    override val children = setOf(predicate, instruction)
  }

  data class Or(val instructions: Set<Instruction>) : Instruction() {
    init {
      if (instructions.any { it is Or })
        throw PetsException("Should have used Instruction.or()")
    }
    override fun toString() = instructions.joinToString(" OR ") {
      it.toStringWhenInside(this)
    }
    override fun parenthesizeThisWhenInside(container: PetsNode): Boolean {
      return container is Then || super.parenthesizeThisWhenInside(container)
    }
    override fun precedence() = 4
    override val children = instructions
  }

  data class Then(val instructions: List<Instruction>) : Instruction() {
    init {
      if (instructions.any { it is Then })
        throw PetsException()
    }
    override fun toString() = instructions.joinToString (" THEN ") {
      it.toStringWhenInside(this)
    }
    override fun precedence() = 2
    override val children = instructions
  }

  data class Multi(val instructions: List<Instruction>) : Instruction() {
    init {
      if (instructions.any { it is Multi }) {
        throw PetsException("Should have used Instruction.then()")
      }
    }
    override fun toString() = instructions.joinToString {
      it.toStringWhenInside(this)
    }
    override fun precedence() = 0
    override val children = instructions
  }

  data class Prod(val instruction: Instruction) : Instruction() {
    override fun toString() = "PROD[$instruction]"
    override val children = setOf(instruction)
    override fun countProds() = super.countProds() + 1
  }

  data class Custom(val name: String, val arguments: List<TypeExpression>) : Instruction() {
    override fun toString() = "$$name(${arguments.joinToString()})"
    override val children = arguments
  }

  enum class Intensity(val symbol: String) {
    MANDATORY("!"),
    AMAP("."),
    OPTIONAL("?"),
    ;

    val pets: String = symbol

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

    fun then(vararg instructions: Instruction) = then(instructions.toList())
    fun then(instructions: List<Instruction>): Instruction =
        if (instructions.size == 1) {
          instructions[0]
        } else {
          Then(instructions.flatMap {
            if (it is Then) it.instructions else listOf(it)
          })
        }

    fun or(instructions: List<Instruction>) = or(instructions.toSetCarefulP())
    fun or(vararg instructions: Instruction) = or(instructions.toList())
    fun or(instructions: Set<Instruction>) =
        if (instructions.size == 1) {
          instructions.first()
        } else {
          Or(instructions.flatMap { if (it is Or) it.instructions else setOf(it) }.toSetCarefulP())
        }
  }
}
