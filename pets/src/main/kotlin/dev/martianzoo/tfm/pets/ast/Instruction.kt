package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.CLASS_NAME_PATTERN
import dev.martianzoo.tfm.pets.GameApi
import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.util.joinOrEmpty

sealed class Instruction : PetsNode() {
  open operator fun times(value: Int): Instruction = TODO()

  abstract fun execute(game: GameApi)

  data class Gain(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: TypeExpression? = null, scalar: Int? = null, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)
    init {
      if ((qe.scalar ?: 1) <= 0) throw PetsException("Can't gain a non-positive amount")
    }

    override fun times(value: Int) = copy(qe = qe.copy(scalar = qe.scalar!! * value))

    // TODO intensity
    override fun execute(game: GameApi) = game.applyChange(qe.scalar!!, gaining = qe.type!!)

    override fun toString() = "${qe}${intensity?.pets ?: ""}"

    override val children = setOf(qe)
  }

  data class Remove(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    constructor(expr: TypeExpression?, scalar: Int? = null, intensity: Intensity? = null) : this(
        QuantifiedExpression(expr, scalar), intensity)
    init {
      if ((qe.scalar ?: 1) <= 0) throw PetsException("Can't remove a non-positive amount")
    }

    override fun times(value: Int) = copy(qe = qe.copy(scalar = qe.scalar!! * value))

    override fun execute(game: GameApi) = game.applyChange(qe.scalar!!, removing = qe.type!!)

    override fun toString() = "-${qe}${intensity?.pets ?: ""}"

    override val children = setOf(qe)
  }

  data class Per(val instruction: Instruction, val qe: QuantifiedExpression): Instruction() {
    init {
      if ((qe.scalar ?: 1) <= 0) throw PetsException("Can't do something 'per' a nonpositive amount")
      when (instruction) {
        is Gain, is Remove, is Transmute -> {}
        else -> throw PetsException("Per can only contain gain/remove/transmute")
      }
    }

    override fun execute(game: GameApi) {
      val metric = game.count(qe.type!!) / qe.scalar!!
      (instruction * metric).execute(game)
    }

    override fun toString() = "$instruction / $qe"
    override fun precedence() = 8
    override val children = setOf(instruction, qe)
  }

  data class Gated(val requirement: Requirement, val instruction: Instruction): Instruction() {
    init {
      if (instruction is Gated) {
        throw PetsException("You don't gate a gater") // TODO keep??
      }
    }

    override fun times(value: Int) = copy(instruction = instruction * value)

    override fun execute(game: GameApi) {
      if (game.isMet(requirement)) {
        instruction.execute(game)
      } else {
        throw PetsException("Die")
      }
    }

    override fun toString(): String {
      return "${groupPartIfNeeded(requirement)}: ${groupPartIfNeeded(instruction)}"
    }
    // let's over-group for clarity
    override fun parenthesizeThisWhenInside(container: PetsNode) =
        container is Or || super.parenthesizeThisWhenInside(container)

    override fun precedence() = 6

    override val children = setOf(requirement, instruction)
  }

  data class Transmute(
      val fromExpression: FromExpression,
      val scalar: Int? = null,
      val intensity: Intensity? = null) : Instruction() {
    init {
      if ((scalar ?: 1) < 1) throw PetsException("Can't do a non-positive number of transmutes")
      if (fromExpression is TypeInFrom) throw PetsException("Should be a regular gain instruction")
    }

    override fun times(value: Int) = copy(scalar = scalar!! * value)

    override fun execute(game: GameApi) =
      game.applyChange(scalar ?: 1, gaining = fromExpression.toType, removing = fromExpression.fromType)

    override fun toString(): String {
      val intens = intensity?.pets ?: ""
      val scal = if (scalar != null) "$scalar " else ""
      return "$scal$fromExpression$intens"
    }
    override val children = setOf(fromExpression)
    override fun parenthesizeThisWhenInside(container: PetsNode): Boolean {
      if (fromExpression is SimpleFrom && container is Or) {
        return true // not technically necessary, but helpful
      }
      return super.parenthesizeThisWhenInside(container)
    }
    override fun precedence() = if (fromExpression is SimpleFrom) 7 else 10

  }

  sealed class FromExpression : PetsNode() {
    override val kind = "FromExpression"
    abstract val toType: TypeExpression
    abstract val fromType: TypeExpression
  }

  data class SimpleFrom(override val toType: TypeExpression, override val fromType: TypeExpression) : FromExpression() {
    override fun toString() = "$toType FROM $fromType"
    override val children = setOf(toType, fromType)
  }

  data class ComplexFrom(
      val className: String,
      val specializations: List<FromExpression> = listOf(),
      val requirement: Requirement? = null
  ) : FromExpression() {
    init {
      require(className.matches(Regex(CLASS_NAME_PATTERN))) { className }
      if (specializations.count { it is ComplexFrom || it is SimpleFrom } != 1) {
        throw PetsException("Can only have one FROM in an expression")
      }
    }
    override val toType = TypeExpression(className, specializations.map { it.toType })
    override val fromType = TypeExpression(className, specializations.map { it.fromType })
    override fun toString() =
        className +
            specializations.joinOrEmpty(wrap="<>") +
            (requirement?.let { "(HAS $it)" } ?: "")

    override val children = specializations + setOfNotNull(requirement)
  }

  data class TypeInFrom(val type: TypeExpression) : FromExpression() {
    override val toType = type
    override val fromType = type
    override fun toString() = "$type"
    override val children = setOf(type)
  }

  interface CustomInstruction {
    val name: String
    fun translate(game: GameApi, types: List<TypeExpression>): Instruction
  }

  data class Custom(val functionName: String, val arguments: List<TypeExpression>) : Instruction() {
    constructor(functionName: String, vararg arguments: TypeExpression) :
        this(functionName, arguments.toList())

    override fun execute(game: GameApi) {
      Canon.customInstruction(functionName).translate(game, arguments).execute(game)
    }

    override fun toString() = "$$functionName(${arguments.joinToString()})"
    override val children = arguments
  }

  data class Then(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())
    override fun times(value: Int) = copy(instructions.map { it * value})
    override fun execute(game: GameApi) {
      Multi(instructions).execute(game)
    }

    override fun toString() = instructions.joinToString(" THEN ") { groupPartIfNeeded(it) }
    override fun precedence() = 2
    override val children = instructions
  }

  data class Or(val instructions: Set<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toSet())

    override fun execute(game: GameApi) {
      error("abstract instruction")
    }

    override fun times(value: Int) = copy(instructions.map { it * value}.toSet())
    override fun toString() = instructions.joinToString(" OR ") { groupPartIfNeeded(it) }
    override fun parenthesizeThisWhenInside(container: PetsNode): Boolean {
      return container is Then || super.parenthesizeThisWhenInside(container)
    }
    override fun precedence() = 4
    override val children = instructions
  }

  data class Multi(val instructions: List<Instruction>) : Instruction() {

    constructor(vararg instructions: Instruction) : this(instructions.toList())

    override fun execute(game: GameApi) {
      instructions.forEach { it.execute(game) }
    }

    override fun times(value: Int) = copy(instructions.map { it * value})
    override fun toString() = instructions.joinToString(transform = ::groupPartIfNeeded)
    override fun precedence() = 0
    override val children = instructions
  }

  data class Prod(val instruction: Instruction) : Instruction(), ProductionBox<Instruction> {
    override fun execute(game: GameApi): Unit = error("should have been deprodified by now")

    override fun times(value: Int) = copy(instruction * value)

    override fun toString() = "PROD[$instruction]"
    override val children = setOf(instruction)
    override fun extract() = instruction
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

  override val kind = "Instruction"
}
