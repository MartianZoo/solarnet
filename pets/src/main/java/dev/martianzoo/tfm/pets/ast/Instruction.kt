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
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.util.suf

public sealed class Instruction : PetNode() {

  public sealed class Change : Instruction() {
    abstract val count: Int
    abstract val removing: TypeExpr?
    abstract val gaining: TypeExpr?
    abstract val intensity: Intensity?
  }

  public data class Gain(
      val scaledType: ScaledTypeExpr,
      override val intensity: Intensity? = null,
  ) : Change() {
    override val count = scaledType.scalar
    override val removing = null
    override val gaining = scaledType.typeExpr

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledType)
    override fun toString() = "$scaledType${intensity?.symbol ?: ""}"

    init {
      if (count == 0) {
        throw PetException("Can't do zero")
      }
    }
  }

  data class Remove(val scaledType: ScaledTypeExpr, override val intensity: Intensity? = null) :
      Change() {
    override val count = scaledType.scalar
    override val removing = scaledType.typeExpr
    override val gaining = null

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledType)
    override fun toString() = "-$scaledType${intensity?.symbol ?: ""}"

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

    override fun visitChildren(visitor: Visitor) = visitor.visit(from)
    override fun toString(): String {
      return "${scalar.suf(' ')}$from${intensity?.symbol ?: ""}"
    }

    init {
      if (count == 0) {
        throw PetException("Can't do zero")
      }
    }

    override fun safeToNestIn(container: PetNode) =
        super.safeToNestIn(container) && (from !is SimpleFrom || container !is Or)

    override fun precedence() = if (from is SimpleFrom) 7 else 10
  }

  data class Per(val instruction: Instruction, val scaledType: ScaledTypeExpr) : Instruction() {
    init {
      if (scaledType.scalar == 0) {
        throw PetException("Can't do something 'per' zero")
      }
      when (instruction) {
        is Gain, is Remove, is Transmute -> {}
        else -> throw PetException("Per can only contain gain/remove/transmute") // TODO more
      }
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledType, instruction)

    override fun precedence() = 8

    override fun toString() = "$instruction / ${scaledType.toString(forceType = true)}"
  }

  data class Gated(val gate: Requirement, val instruction: Instruction) : Instruction() {
    init {
      if (instruction is Gated) {
        throw PetException("You don't gate a gater") // TODO enable
      }
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(gate, instruction)

    override fun toString(): String {
      return "${groupPartIfNeeded(gate)}: ${groupPartIfNeeded(instruction)}"
    }

    // let's over-group for clarity
    override fun safeToNestIn(container: PetNode) =
        super.safeToNestIn(container) && container !is Or

    override fun precedence() = 6
  }

  data class Custom(val functionName: String, val arguments: List<TypeExpr>) : Instruction() {
    constructor(
        functionName: String,
        vararg arguments: TypeExpr
    ) : this(functionName, arguments.toList())

    override fun visitChildren(visitor: Visitor) = visitor.visit(arguments)

    override fun toString() = "@$functionName(${arguments.joinToString()})"
  }

  data class Then(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(instructions)

    override fun toString() = instructions.joinToString(" THEN ") { groupPartIfNeeded(it) }

    override fun precedence() = 2
  }

  data class Or(val instructions: Set<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toSet())

    init {
      require(instructions.size >= 2)
    }

    override fun toString() = instructions.joinToString(" OR ") { groupPartIfNeeded(it) }
    override fun visitChildren(visitor: Visitor) = visitor.visit(instructions)

    override fun safeToNestIn(container: PetNode) =
        super.safeToNestIn(container) && container !is Then

    override fun precedence() = 4
  }

  data class Multi(val instructions: List<Instruction>) : Instruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(instructions)

    override fun toString() = instructions.joinToString { groupPartIfNeeded(it) }

    override fun precedence() = 0
  }

  data class Transform(val instruction: Instruction, override val transformKind: String) :
      Instruction(), GenericTransform<Instruction> {
    override fun visitChildren(visitor: Visitor) = visitor.visit(instruction)
    override fun toString() = "$transformKind[$instruction]"

    override fun extract() = instruction
  }

  override val kind = Instruction::class.simpleName!!

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
        val scaledType: Parser<ScaledTypeExpr> = ScaledTypeExpr.parser()

        val gain: Parser<Gain> =
            scaledType and optional(intensity) map { (ste, int) -> Gain(ste, int) }
        val remove: Parser<Remove> =
            skipChar('-') and gain map { Remove(it.scaledType, it.intensity) }

        val transmute: Parser<Transmute> =
            optional(scalar) and
            From.parser() and
            optional(intensity) map { (scalar, fro, int) -> Transmute(fro, scalar, int) }

        val perable: Parser<Change> = transmute or group(transmute) or gain or remove

        val maybePer: Parser<Instruction> =
            perable and
            optional(skipChar('/') and scaledType) map { (instr, sat) ->
              if (sat == null) instr else Per(instr, sat)
            }

        val transform: Parser<Transform> =
            transform(parser()) map { (node, tname) -> Transform(node, tname) }
        val maybeTransform: Parser<Instruction> = maybePer or transform

        val arguments = separatedTerms(TypeExpr.parser(), char(','), acceptZero = true)
        val custom: Parser<Custom> =
            skipChar('@') and
            _lowerCamelRE and
            group(arguments) map { (name, args) ->
              Custom(name.text, args)
            }
        val atom: Parser<Instruction> = group(parser()) or maybeTransform or custom

        val gated: Parser<Instruction> =
            optional(Requirement.atomParser() and skipChar(':')) and
            atom map { (one, two) ->
              if (one == null) two else Gated(one, two)
            }

        val orInstr: Parser<Instruction> =
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
