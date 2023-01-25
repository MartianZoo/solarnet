package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.PetVisitor
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.TypeExpr.TypeParsers.typeExpr
import dev.martianzoo.util.suf

sealed class Instruction : PetNode() {

  sealed class Change : Instruction() {
    abstract val count: Int
    abstract val removing: TypeExpr?
    abstract val gaining: TypeExpr?
    abstract val intensity: Intensity?
  }

  data class Gain(val sat: ScalarAndType, override val intensity: Intensity? = null) : Change() {
    override val count = sat.scalar
    override val removing = null
    override val gaining = sat.typeExpr

    override fun visitChildren(v: PetVisitor) = v.visit(sat)
    override fun toString() = "$sat${intensity?.symbol ?: ""}"

    init {
      if (count == 0) {
        throw PetException("Can't do zero")
      }
    }
  }

  data class Remove(val sat: ScalarAndType, override val intensity: Intensity? = null) : Change() {
    override val count = sat.scalar
    override val removing = sat.typeExpr
    override val gaining = null

    override fun visitChildren(v: PetVisitor) = v.visit(sat)
    override fun toString() = "-$sat${intensity?.symbol ?: ""}"

    init {
      if (count == 0) {
        throw PetException("Can't do zero")
      }
    }
  }

  data class Transmute(
      val from: From,
      val scalar: Int? = null,
      override val intensity: Intensity? = null,
  ) : Change() {
    override val count = scalar ?: 1
    override val removing = from.fromType
    override val gaining = from.toType

    override fun visitChildren(v: PetVisitor) = v.visit(from)
    override fun toString(): String {
      return "${scalar.suf(' ')}$from${intensity?.symbol ?: ""}"
    }

    init {
      if (count == 0) {
        throw PetException("Can't do zero")
      }
    }

    override fun shouldGroupInside(container: PetNode) =
        (from is SimpleFrom && container is Or) || super.shouldGroupInside(container)

    override fun precedence() = if (from is SimpleFrom) 7 else 10
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
    override fun visitChildren(v: PetVisitor) = v.visit(sat, instruction)

    override fun precedence() = 8

    override fun toString() = "$instruction / ${sat.toString(forceType = true)}"
  }

  data class Gated(val gate: Requirement, val instruction: Instruction) : Instruction() {
    init {
      if (instruction is Gated) {
        throw PetException("You don't gate a gater") // TODO keep??
      }
    }

    override fun visitChildren(v: PetVisitor) = v.visit(gate, instruction)

    override fun toString(): String {
      return "${groupPartIfNeeded(gate)}: ${groupPartIfNeeded(instruction)}"
    }

    // let's over-group for clarity
    override fun shouldGroupInside(container: PetNode) =
        container is Or || super.shouldGroupInside(container)

    override fun precedence() = 6
  }

  data class Custom(val functionName: String, val arguments: List<TypeExpr>) : Instruction() {
    constructor(
        functionName: String,
        vararg arguments: TypeExpr
    ) : this(functionName, arguments.toList())

    override fun visitChildren(v: PetVisitor) = v.visit(arguments)

    override fun toString() = "@$functionName(${arguments.joinToString()})"
  }

  data class Then(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }

    override fun visitChildren(v: PetVisitor) = v.visit(instructions)

    override fun toString() = instructions.joinToString(" THEN ") { groupPartIfNeeded(it) }

    override fun precedence() = 2
  }

  data class Or(val instructions: Set<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toSet())

    init {
      require(instructions.size >= 2)
    }

    override fun toString() = instructions.joinToString(" OR ") { groupPartIfNeeded(it) }
    override fun visitChildren(v: PetVisitor) = v.visit(instructions)

    override fun shouldGroupInside(container: PetNode) =
        container is Then || super.shouldGroupInside(container)

    override fun precedence() = 4
  }

  data class Multi(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }
    override fun visitChildren(v: PetVisitor) = v.visit(instructions)

    override fun toString() = instructions.joinToString { groupPartIfNeeded(it) }

    override fun precedence() = 0
  }

  data class Transform(val instruction: Instruction, override val transform: String) :
      Instruction(), GenericTransform<Instruction> {
    override fun visitChildren(v: PetVisitor) = v.visit(instruction)
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
                From.parser() and
                optional(intensity) map
                { (scal, fro, intens) ->
                  Transmute(fro, scal, intens)
                }

        val perable = transmute or group(transmute) or gain or remove

        val maybePer =
            perable and
                optional(skipChar('/') and sat) map
                { (instr, sat) ->
                  if (sat == null) instr else Per(instr, sat)
                }

        val maybeTransform =
            maybePer or
            (transform(parser()) map { (node, transformName) ->
              Transform(node, transformName)
            })

        val arguments = separatedTerms(typeExpr, char(','), acceptZero = true)
        val custom =
            skipChar('@') and
                _lowerCamelRE and
                group(arguments) map
                { (name, args) ->
                  Custom(name.text, args)
                }
        val atom = group(parser()) or maybeTransform or custom

        val gated =
            optional(Requirement.atomParser() and skipChar(':')) and
                atom map
                { (one, two) ->
                  if (one == null) two else Gated(one, two)
                }

        val orInstr =
            separatedTerms(gated, _or) map
                {
                  val set = it.toSet()
                  if (set.size == 1) set.first() else Or(set)
                }

        val then = separatedTerms(orInstr, _then) map { if (it.size == 1) it.first() else Then(it) }
        commaSeparated(then) map { if (it.size == 1) it.first() else Multi(it) }
      }
    }
  }
}
