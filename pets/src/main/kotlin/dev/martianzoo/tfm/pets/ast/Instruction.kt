package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.GameApi
import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.TypeInFrom

sealed class Instruction : PetsNode() {

  override val kind = "Instruction"

  open operator fun times(value: Int): Instruction = TODO()

  abstract fun execute(game: GameApi)

  data class Gain(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {

    constructor(expr: TypeExpression? = null, scalar: Int? = null, intensity: Intensity? = null) :
        this(QuantifiedExpression(expr, scalar), intensity)

    init {
      if ((qe.scalar ?: 1) <= 0) {
        throw PetsException("Can't gain a non-positive amount")
      }
    }

    override fun times(value: Int) = copy(qe = qe.copy(scalar = qe.scalar!! * value))
    // TODO intensity
    override fun execute(game: GameApi) = game.applyChange(qe.scalar!!, gaining = qe.type!!)

    override val children = setOf(qe)
    override fun toString() = "${qe}${intensity?.pets ?: ""}"
  }

  data class Remove(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {

    constructor(
        expr: TypeExpression?,
        scalar: Int? = null,
        intensity: Intensity? = null) :
            this(QuantifiedExpression(expr, scalar), intensity)

    init {
      if ((qe.scalar ?: 1) <= 0) {
        throw PetsException("Can't remove a non-positive amount")
      }
    }

    override fun times(value: Int) = copy(qe = qe.copy(scalar = qe.scalar!! * value))
    override fun execute(game: GameApi) =
        game.applyChange(qe.scalar!!, removing = qe.type!!)

    override val children = setOf(qe)
    override fun toString() = "-${qe}${intensity?.pets ?: ""}"
  }

  data class Per(val instruction: Instruction, val qe: QuantifiedExpression): Instruction() {
    init {
      if ((qe.scalar ?: 1) <= 0) {
        throw PetsException("Can't do something 'per' a nonpositive amount")
      }
      when (instruction) {
        is Gain, is Remove, is Transmute -> {}
        else -> throw PetsException(
            "Per can only contain gain/remove/transmute")
      }
    }

    override fun times(value: Int) = copy(instruction = instruction * value)

    override fun execute(game: GameApi) {
      val measurement = game.count(qe.type!!) / qe.scalar!!
      (instruction * measurement).execute(game)
    }

    override val children = setOf(instruction, qe)
    override fun precedence() = 8

    override fun toString() = "$instruction / $qe"
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

    override val children = setOf(requirement, instruction)

    override fun toString(): String {
      return "${groupPartIfNeeded(requirement)}: ${groupPartIfNeeded(instruction)}"
    }

    // let's over-group for clarity
    override fun shouldGroupInside(container: PetsNode) =
        container is Or || super.shouldGroupInside(container)

    override fun precedence() = 6
  }

  data class Transmute(
      val fromExpression: FromExpression,
      val scalar: Int? = null,
      val intensity: Intensity? = null) : Instruction() {
    init {
      if ((scalar ?: 1) < 1) {
        throw PetsException("Can't do a non-positive number of transmutes")
      }
      if (fromExpression is TypeInFrom) {
        throw PetsException("Should be a regular gain instruction")
      }
    }

    override fun times(value: Int) = copy(scalar = scalar!! * value)

    override fun execute(game: GameApi) =
      game.applyChange(
          scalar ?: 1,
          gaining = fromExpression.toType,
          removing = fromExpression.fromType)

    override fun toString(): String {
      val intens = intensity?.pets ?: ""
      val scal = if (scalar != null) "$scalar " else ""
      return "$scal$fromExpression$intens"
    }

    override val children = setOf(fromExpression)

    override fun shouldGroupInside(container: PetsNode) =
        (fromExpression is SimpleFrom && container is Or) || super.shouldGroupInside(container)

    override fun precedence() = if (fromExpression is SimpleFrom) 7 else 10
  }

  interface CustomInstruction {
    val name: String
    fun translate(game: GameApi, types: List<TypeExpression>): Instruction
  }

  data class Custom(val functionName: String, val arguments: List<TypeExpression>) : Instruction() {
    constructor(functionName: String, vararg arguments: TypeExpression) :
        this(functionName, arguments.toList())

    override fun execute(game: GameApi) {
      Canon.customInstruction(functionName)
          .translate(game, arguments)
          .execute(game)
    }

    override val children = arguments
    override fun toString() = "$$functionName(${arguments.joinToString()})"
  }

  data class Then(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    override fun times(value: Int) = copy(instructions.map { it * value })
    override fun execute(game: GameApi) {
      Multi(instructions).execute(game)
    }

    override val children = instructions
    override fun toString() =
        instructions.joinToString(" THEN ") { groupPartIfNeeded(it) }

    override fun precedence() = 2
  }

  data class Or(val instructions: Set<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toSet())

    override fun times(value: Int) = copy(instructions.map { it * value }.toSet())
    override fun execute(game: GameApi) = error("abstract instruction")

    override val children = instructions
    override fun toString() = instructions.joinToString(" OR ") { groupPartIfNeeded(it) }

    override fun shouldGroupInside(container: PetsNode) =
        container is Then || super.shouldGroupInside(container)
    override fun precedence() = 4
  }

  data class Multi(val instructions: List<Instruction>) : Instruction() {

    constructor(vararg instructions: Instruction) : this(instructions.toList())

    override fun times(value: Int) = copy(instructions.map { it * value })

    override fun execute(game: GameApi) {
      instructions.forEach { it.execute(game) }
    }

    override val children = instructions
    override fun toString() = instructions.joinToString(transform = ::groupPartIfNeeded)

    override fun precedence() = 0
  }

  data class Prod(val instruction: Instruction) : Instruction(), ProductionBox<Instruction> {
    override fun times(value: Int) = copy(instruction * value)

    override fun execute(game: GameApi) = error("should have been deprodified by now")

    override val children = setOf(instruction)
    override fun toString() = "PROD[$instruction]"

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
}
