package dev.martianzoo.tfm.petaform

sealed class Instruction : PetaformNode() {
  data class Gain(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: TypeExpression?, scalar: Int? = null, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init { if ((qe.scalar ?: 1) < 1) throw PetaformException() }
    override fun toString() = "${qe}${intensity?.petaform ?: ""}"
    override val children = listOf(qe)
  }

  data class Remove(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: TypeExpression?, scalar: Int? = null, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init { if ((qe.scalar ?: 1) <= 0) throw PetaformException() }
    override fun toString() = "-${qe}${intensity?.petaform ?: ""}"
    override val children = listOf(qe)
  }

  data class Transmute(
      val toExpression: TypeExpression,
      val fromExpression: TypeExpression,
      val scalar: Int? = null,
      val intensity: Intensity? = null) : Instruction() {
    init {
      if ((scalar ?: 1) < 1) throw PetaformException()
    }
    override fun toString() =
        (scalar?.let { "$it " } ?: "") + "$toExpression${intensity?.petaform ?: ""} FROM $fromExpression"
    override val children = listOf(toExpression, fromExpression)
  }

  data class Per(val instruction: Instruction, val qe: QuantifiedExpression): Instruction() {
    init {
      if (qe.typeExpression == null) throw PetaformException()
      if ((qe.scalar ?: 1) <= 0) throw PetaformException()
      when (instruction) {
        is Gain -> { if (instruction.qe.scalar == 0) throw PetaformException() }
        is Remove, is Transmute -> {}
        else -> throw PetaformException()
      }
    }
    override fun toString() = "$instruction / $qe"
    override fun precedence() = 5
    override val children = listOf(instruction, qe)
  }

  data class Gated(val predicate: Predicate, val instruction: Instruction): Instruction() {
    init {
      if (instruction is Gated) {
        // you don't gate a gater
        throw PetaformException()
      }
    }
    override fun toString(): String {
      return "${predicate.toStringWithin(this)}: ${instruction.toStringWithin(this)}"
    }

    // let's over-group for clarity
    override fun groupWithin(container: PetaformNode) =
        container is Or || super.groupWithin(container)

    override fun precedence() = 4
    override val children = listOf(predicate, instruction)
  }

  data class Or(val instructions: List<Instruction>) : Instruction() {
    init {
      if (instructions.any { it is Or })
        throw PetaformException()
    }
    override fun toString() = instructions.joinToString(" OR ") {
      it.toStringWithin(this)
    }
    override fun groupWithin(container: PetaformNode): Boolean {
      return container is Then || super.groupWithin(container)
    }
    override fun precedence() = 3
    override val children = instructions
  }

  data class Then(val instructions: List<Instruction>) : Instruction() {
    init {
      if (instructions.any { it is Then })
        throw PetaformException()
    }
    override fun toString() = instructions.joinToString (" THEN ") {
      it.toStringWithin(this)
    }
    override fun precedence() = 2
    override val children = instructions
  }

  data class Multi(val instructions: List<Instruction>) : Instruction() {
    init {
      if (instructions.any { it is Multi })
        throw PetaformException()
    }
    override fun toString() = instructions.joinToString {
      it.toStringWithin(this)
    }
    override fun precedence() = 1
    override val children = instructions
  }

  data class Prod(val instruction: Instruction) : Instruction() {
    override fun toString() = "PROD[$instruction]"
    override val children = listOf(instruction)
    override fun countProds() = super.countProds() + 1
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

    fun then(vararg instructions: Instruction) = then(instructions.toList())
    fun then(instructions: List<Instruction>): Instruction =
        if (instructions.size == 1) {
          instructions[0]
        } else {
          Then(instructions.flatMap {
            if (it is Then) it.instructions else listOf(it)
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
