package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.standardResourceNames
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.TypeInFrom
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.deprodify

sealed class Instruction : PetNode() {

  override val kind = "Instruction"

  open operator fun times(value: Int): Instruction = TODO()

  abstract fun execute(game: GameState)

  data class Gain(val qe: QuantifiedExpression, val intensity: Intensity? = null) : Instruction() {
    init {
      if (qe.scalar == 0) {
        throw PetException("Can't gain zero")
      }
    }

    override fun times(value: Int) = copy(qe = qe.copy(scalar = qe.scalar * value))

    // TODO intensity
    override fun execute(game: GameState) =
        game.applyChange(qe.scalar, gaining = qe.expression as GenericTypeExpression)

    override fun toString() = "$qe${intensity?.symbol ?: ""}"
  }

  data class Remove(
      val qe: QuantifiedExpression,
      val intensity: Intensity? = null,
  ) : Instruction() {
    init {
      if (qe.scalar == 0) {
        throw PetException("Can't remove zero")
      }
    }

    override fun times(value: Int) = copy(qe = qe.copy(scalar = qe.scalar * value))
    override fun execute(game: GameState) =
        game.applyChange(qe.scalar, removing = qe.expression as GenericTypeExpression)

    override fun toString() = "-$qe${intensity?.symbol ?: ""}"
  }

  data class Per(val instruction: Instruction, val qe: QuantifiedExpression) : Instruction() {
    init {
      if (qe.scalar == 0) {
        throw PetException("Can't do something 'per' zero")
      }
      when (instruction) {
        is Gain, is Remove, is Transmute -> {}
        else -> throw PetException("Per can only contain gain/remove/transmute")
      }
    }

    override fun times(value: Int) = copy(instruction = instruction * value)

    override fun execute(game: GameState) {
      val measurement = game.count(qe.expression) / qe.scalar
      if (measurement > 0) {
        (instruction * measurement).execute(game)
      }
    }

    override fun precedence() = 8

    override fun toString() = "$instruction / ${qe.toString(forceType = true)}"
  }

  data class Gated(val requirement: Requirement, val instruction: Instruction) : Instruction() {
    init {
      if (instruction is Gated) {
        throw PetException("You don't gate a gater") // TODO keep??
      }
    }

    override fun times(value: Int) = copy(instruction = instruction * value)

    override fun execute(game: GameState) {
      if (game.isMet(requirement)) {
        instruction.execute(game)
      } else {
        throw PetException("Die")
      }
    }

    override fun toString(): String {
      return "${groupPartIfNeeded(requirement)}: ${groupPartIfNeeded(instruction)}"
    }

    // let's over-group for clarity
    override fun shouldGroupInside(container: PetNode) =
        container is Or || super.shouldGroupInside(container)

    override fun precedence() = 6
  }

  data class Transmute(
      val fromExpression: FromExpression,
      val scalar: Int? = null,
      val intensity: Intensity? = null,
  ) : Instruction() {
    init {
      if ((scalar ?: 1) < 1) {
        throw PetException("Can't do a non-positive number of transmutes")
      }
      if (fromExpression is TypeInFrom) {
        throw PetException("Should be a regular gain instruction")
      }
    }

    override fun times(value: Int) = copy(scalar = scalar!! * value)

    override fun execute(game: GameState) = game.applyChange(
        scalar ?: 1,
        gaining = fromExpression.toType as GenericTypeExpression,
        removing = fromExpression.fromType as GenericTypeExpression)

    override fun toString(): String {
      val intens = intensity?.symbol ?: ""
      val scal = if (scalar != null) "$scalar " else ""
      return "$scal$fromExpression$intens"
    }

    override fun shouldGroupInside(container: PetNode) =
        (fromExpression is SimpleFrom && container is Or) || super.shouldGroupInside(container)

    override fun precedence() = if (fromExpression is SimpleFrom) 7 else 10
  }

  data class Custom(val functionName: String, val arguments: List<TypeExpression>) : Instruction() {
    constructor(functionName: String, vararg arguments: TypeExpression) :
        this(functionName, arguments.toList())

    override fun execute(game: GameState) {
      val instr = game.authority.customInstructionsByName[functionName]!!
      try {
        val oops = arguments.filter { game.resolve(it).abstract }
        if (oops.any()) {
          throw PetException("Abstract types given to $functionName: $oops")
        }
        // We're not going to get far with this approach of just trying to execute directly.
        // Already with my first attempt, Robinson, it returns an "OR", which can't be exec'd.
        val translated = instr.translate(game, arguments)

        // TODO what other conversions can we make...
        val deprodded: Instruction = deprodify(translated, standardResourceNames(game))
        deprodded.execute(game) // TODO!
      } catch (e: ExecuteInsteadException) {
        instr.execute(game, arguments)
      }
    }

    override fun toString() = "$$functionName(${arguments.joinToString()})"
  }

  data class Then(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    override fun times(value: Int) = copy(instructions.map { it * value })
    override fun execute(game: GameState) {
      Multi(instructions).execute(game)
    }

    override fun toString() = instructions.joinToString(" THEN ") { groupPartIfNeeded(it) }

    override fun precedence() = 2
  }

  data class Or(val instructions: Set<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toSet())

    override fun times(value: Int) = copy(instructions.map { it * value }.toSet())
    override fun execute(game: GameState) = error("abstract instruction")

    override fun toString() = instructions.joinToString(" OR ") { groupPartIfNeeded(it) }

    override fun shouldGroupInside(container: PetNode) =
        container is Then || super.shouldGroupInside(container)

    override fun precedence() = 4
  }

  data class Multi(val instructions: List<Instruction>) : Instruction() {

    constructor(vararg instructions: Instruction) : this(instructions.toList())

    override fun times(value: Int) = copy(instructions.map { it * value })

    override fun execute(game: GameState) {
      instructions.forEach { it.execute(game) }
    }

    override fun toString() = instructions.joinToString(transform = ::groupPartIfNeeded)

    override fun precedence() = 0
  }

  data class Transform(val instruction: Instruction, override val transform: String) :
      Instruction(), GenericTransform<Instruction> {
    override fun times(value: Int) = Transform(instruction * value, transform)

    override fun execute(game: GameState) = error("should have been transformed by now")

    override fun toString() = "$transform[$instruction]"

    override fun extract() = instruction
  }

  enum class Intensity(val symbol: String) {
    MANDATORY("!"),
    AMAP("."),
    OPTIONAL("?"),
    ;

    companion object {
      fun intensity(symbol: String?) = symbol?.let { s -> values().first { it.symbol == s } }
    }
  }
}
