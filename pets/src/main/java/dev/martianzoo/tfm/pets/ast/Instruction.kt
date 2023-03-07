package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.BaseTokenizer
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.util.suf
import dev.martianzoo.util.toSetStrict

public sealed class Instruction : PetNode() {

  public sealed class Change : Instruction() {
    abstract val count: Int
    abstract val gaining: Expression?
    abstract val removing: Expression?
    abstract val intensity: Intensity?
  }

  public data class Gain(
      val scaledEx: ScaledExpression,
      override val intensity: Intensity? = null,
  ) : Change() {
    override val count = scaledEx.scalar
    override val gaining = scaledEx.expression
    override val removing = null

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
    override fun toString() = "$scaledEx${intensity?.symbol ?: ""}"

    init {
      if (count == 0) {
        throw PetException("Can't do zero")
      }
    }
  }

  data class Remove(val scaledEx: ScaledExpression, override val intensity: Intensity? = null) :
      Change() {
    override val count = scaledEx.scalar
    override val gaining = null
    override val removing = scaledEx.expression

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
    override fun toString() = "-$scaledEx${intensity?.symbol ?: ""}"

    init {
      if (count == 0) {
        throw PetException("Can't do zero")
      }
    }
  }

  data class Transmute(
      val from: FromExpression,
      val scalar: Int? = null,
      override val intensity: Intensity? = null,
  ) : Change() {
    override val count = scalar ?: 1
    override val gaining = from.toExpression
    override val removing = from.fromExpression

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

  data class Per(val instruction: Instruction, val metric: Metric) : Instruction() {
    init {
      when (instruction) {
        is Gain,
        is Remove,
        is Transmute -> {}

        else -> throw PetException("Per can only contain gain/remove/transmute") // TODO more
      }
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(metric, instruction)

    override fun precedence() = 8

    override fun toString() = "$instruction / $metric"
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

  data class Custom(val functionName: String, val arguments: List<Expression>) : Instruction() {
    constructor(
        functionName: String,
        vararg arguments: Expression
    ) : this(functionName, arguments.toList())

    override fun visitChildren(visitor: Visitor) = visitor.visit(arguments)

    override fun toString() = "@$functionName(${arguments.joinToString()})"
  }

  sealed class CompositeInstruction : Instruction() {
    abstract val instructions: List<Instruction>
    override fun visitChildren(visitor: Visitor) = visitor.visit(instructions)
    fun toString(connector: String) = instructions.joinToString(connector) { groupPartIfNeeded(it) }
  }

  data class Then(override val instructions: List<Instruction>) : CompositeInstruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }

    override fun precedence() = 2
    override fun toString() = toString(" THEN ")
  }

  data class Or(override val instructions: List<Instruction>) : CompositeInstruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
      if (instructions.distinct().size != instructions.size) {
        throw PetException("duplicates")
      }
    }

    override fun safeToNestIn(container: PetNode) =
        super.safeToNestIn(container) && container !is Then

    override fun precedence() = 4
    override fun toString() = toString(" OR ")
  }

  data class Multi(override val instructions: List<Instruction>) : CompositeInstruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }

    override fun precedence() = 0
    override fun toString() = toString(", ")
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

  companion object : BaseTokenizer() {
    fun instruction(text: String): Instruction = Parsing.parse(parser(), text)

    internal fun parser(): Parser<Instruction> {
      return parser {
        val scaledEx: Parser<ScaledExpression> = ScaledExpression.parser()

        val gain: Parser<Gain> =
            scaledEx and optional(intensity) map { (ste, int) -> Gain(ste, int) }
        val remove: Parser<Remove> =
            skipChar('-') and gain map { Remove(it.scaledEx, it.intensity) }

        val transmute: Parser<Transmute> =
            optional(scalar) and
            FromExpression.parser() and
            optional(intensity) map { (scalar, fro, int) -> Transmute(fro, scalar, int) }

        val perable: Parser<Change> = transmute or group(transmute) or gain or remove

        val maybePer: Parser<Instruction> =
            perable and
            optional(skipChar('/') and Metric.parser()) map { (instr, metric) ->
              if (metric == null) instr else Per(instr, metric)
            }

        val transform: Parser<Transform> =
            transform(parser()) map { (node, tname) -> Transform(node, tname) }
        val maybeTransform: Parser<Instruction> = transform or maybePer

        val arguments = separatedTerms(Expression.parser(), char(','), acceptZero = true)
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
              val set = it.toSetStrict().toList()
              if (set.size == 1) set.first() else Or(set)
            }

        val then = separatedTerms(orInstr, _then) map { if (it.size == 1) it.first() else Then(it) }
        commaSeparated(then) map { if (it.size == 1) it.first() else Multi(it) }
      }
    }
  }
}
