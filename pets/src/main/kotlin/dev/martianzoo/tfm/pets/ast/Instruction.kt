package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.standardResourceNames
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.TypeInFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.typeExpression
import dev.martianzoo.tfm.pets.deprodify

sealed class Instruction : PetNode() {

  open operator fun times(value: Int): Instruction = TODO()

  abstract fun execute(game: GameState)

  data class Gain(val sat: ScalarAndType, val intensity: Intensity? = null) : Instruction() {
    init {
      if (sat.scalar == 0) {
        throw PetException("Can't gain zero")
      }
    }

    override fun times(value: Int): Instruction {
      require(value >= 1)
      return copy(sat = sat.copy(scalar = sat.scalar * value))
    }

    // TODO intensity
    override fun execute(game: GameState) {
      // treat null as MANDATORY (TODO)
      if (intensity == OPTIONAL || game.resolve(sat.type).abstract) {
        throw AbstractInstructionException("${sat.type}")
      }
      game.applyChange(sat.scalar, gaining = sat.type.asGeneric(), amap = (intensity == AMAP))
    }

    override fun toString() = "$sat${intensity?.symbol ?: ""}"
  }

  data class Remove(
      val sat: ScalarAndType,
      val intensity: Intensity? = null,
  ) : Instruction() {
    init {
      if (sat.scalar == 0) {
        throw PetException("Can't remove zero")
      }
    }

    override fun times(value: Int): Instruction {
      require(value >= 1)
      return copy(sat = sat.copy(scalar = sat.scalar * value))
    }

    override fun execute(game: GameState) {
      // treat null as MANDATORY (TODO)
      if (intensity == OPTIONAL || game.resolve(sat.type).abstract) {
        throw AbstractInstructionException("${sat.type}")
      }
      game.applyChange(sat.scalar, removing = sat.type.asGeneric(), amap = (intensity == AMAP))
    }

    override fun toString() = "-$sat${intensity?.symbol ?: ""}"
  }

  data class Per(val instruction: Instruction, val sat: ScalarAndType) : Instruction() {
    init {
      if (sat.scalar == 0) {
        throw PetException("Can't do something 'per' zero")
      }
      when (instruction) {
        is Gain,
        is Remove,
        is Transmute -> {}
        else -> throw PetException("Per can only contain gain/remove/transmute") // TODO more
      }
    }

    override fun times(value: Int) = copy(instruction = instruction * value)

    override fun execute(game: GameState) {
      val measurement = game.count(sat.type) / sat.scalar
      if (measurement > 0) {
        (instruction * measurement).execute(game)
      }
    }

    override fun precedence() = 8

    override fun toString() = "$instruction / ${sat.toString(forceType = true)}"
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

    override fun times(value: Int): Instruction {
      require(value >= 1)
      return if (value == 1) {
        this
      } else {
        copy(scalar = (scalar ?: 1) * value)
      }
    }

    override fun execute(game: GameState) {
      // treat null as MANDATORY (TODO)
      if (intensity == OPTIONAL) {
        throw AbstractInstructionException("optional")
      }
      if (game.resolve(fromExpression.fromType).abstract) {
        throw AbstractInstructionException("${fromExpression.fromType}")
      }
      if (game.resolve(fromExpression.toType).abstract) {
        throw AbstractInstructionException("${fromExpression.toType}")
      }
      game.applyChange(
          scalar ?: 1,
          removing = fromExpression.fromType.asGeneric(),
          gaining = fromExpression.toType.asGeneric(),
          amap = intensity == AMAP)
    }

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
    constructor(
        functionName: String,
        vararg arguments: TypeExpression
    ) : this(functionName, arguments.toList())

    override fun execute(game: GameState) {
      val instr = game.authority.customInstruction(functionName)
      try {
        val oops = arguments.filter { game.resolve(it).abstract }
        if (oops.any()) {
          throw PetException("Abstract types given to $functionName: $oops")
        }
        // We're not going to get far with this approach of just trying to execute directly.
        // Already with my first attempt, Robinson, it returns an "OR", which can't be exec'd.
        var translated = instr.translate(game, arguments)

        // TODO what other conversions can we make...
        if (translated.childNodesOfType<PetNode>().any {
          // TODO deprodify could do this??
          it is GenericTransform<*> && it.transform == "PROD"
        }) {
          translated = deprodify(translated, standardResourceNames(game))
        }
        translated.execute(game) // TODO!
      } catch (e: ExecuteInsteadException) {
        instr.execute(game, arguments)
      }
    }

    override fun toString() = "@$functionName(${arguments.joinToString()})"
  }

  data class Then(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }

    override fun times(value: Int) = copy(instructions.map { it * value })

    override fun execute(game: GameState) = instructions.forEach { it.execute(game) }

    override fun toString() = instructions.joinToString(" THEN ") { groupPartIfNeeded(it) }

    override fun precedence() = 2
  }

  data class Or(val instructions: Set<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toSet())

    init {
      require(instructions.size >= 2)
    }

    override fun times(value: Int) = copy(instructions.map { it * value }.toSet())

    override fun execute(game: GameState) = error("abstract instruction")

    override fun toString() = instructions.joinToString(" OR ") { groupPartIfNeeded(it) }

    override fun shouldGroupInside(container: PetNode) =
        container is Then || super.shouldGroupInside(container)

    override fun precedence() = 4
  }

  data class Multi(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }

    override fun times(value: Int) = copy(instructions.map { it * value })

    override fun execute(game: GameState) = Then(instructions).execute(game)

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

  override val kind = "Instruction"

  enum class Intensity(val symbol: String) {
    /** The full amount must be gained/removed/transmuted. */
    MANDATORY("!"),
    /** Do "as much as possible" of the amount. */
    AMAP("."),
    /** The player can choose how much of the amount to do, including none of it. */
    OPTIONAL("?"),
    ;

    companion object {
      fun from(symbol: String) = values().first { it.symbol == symbol }
    }
  }

  class AbstractInstructionException(message: String) : RuntimeException(message)

  companion object : PetParser() {
    fun instruction(text: String): Instruction = Parsing.parse(parser(), text)

    internal fun parser(): Parser<Instruction> {
      return parser {
        val sat = ScalarAndType.parser()

        val optlIntens = optional(intensity)
        val gain = sat and optlIntens map { (sat, intens) -> Gain(sat, intens) }
        val remove = skipChar('-') and gain map { Remove(it.sat, it.intensity) }

        val transmute =
            optional(scalar) and
            FromExpression.parser() and
            optional(intensity) map {
              (scal, fro, intens) -> Transmute(fro, scal, intens)
            }

        val perable = transmute or group(transmute) or gain or remove

        val maybePer =
            perable and
            optional(skipChar('/') and sat) map {
              (instr, sat) -> if (sat == null) instr else Per(instr, sat)
            }

        val maybeTransform =
            maybePer or (transform(parser()) map { (node, type) -> Transform(node, type) })

        val arguments = separatedTerms(typeExpression, char(','), acceptZero = true)
        val custom =
            skipChar('@') and
            _lowerCamelRE and
            group(arguments) map {
              (name, args) -> Custom(name.text, args)
            }
        val atom = group(parser()) or maybeTransform or custom

        val gated =
            optional(Requirement.atomParser() and skipChar(':')) and
            atom map {
              (one, two) -> if (one == null) two else Gated(one, two)
            }
        val orInstr =
            separatedTerms(gated, _or) map {
              val set = it.toSet()
              if (set.size == 1) set.first() else Or(set)
            }
        val then = separatedTerms(orInstr, _then) map { if (it.size == 1) it.first() else Then(it) }
        commaSeparated(then) map { if (it.size == 1) it.first() else Multi(it) }
      }
    }
  }
}
