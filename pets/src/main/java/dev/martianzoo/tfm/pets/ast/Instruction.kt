package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.asJust
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.UserException.InvalidReificationException
import dev.martianzoo.tfm.api.UserException.PetSyntaxException
import dev.martianzoo.tfm.pets.PetTokenizer
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.util.Reifiable
import dev.martianzoo.util.toSetStrict

/**
 * A specification of steps that might be taken (or were taken) to alter a game state. Instructions
 * appear as the right-hand side of [Action]s and [Effect]s, on map areas, in the "do this now"
 * section of cards, in an engine's task queues, and so forth.
 */
public sealed class Instruction : PetElement() {
  companion object {
    /** Recursively breaks apart any [Multi] instructions found in [instructions]. */
    fun split(instructions: Iterable<Instruction>): List<Instruction> =
        instructions.flatMap(::split)

    /** Recursively breaks apart any [Multi] instructions found in [instruction]. */
    fun split(instruction: Instruction): List<Instruction> =
        if (instruction is Multi) {
          split(instruction.instructions)
        } else {
          listOf(instruction)
        }

    internal fun parser(): Parser<Instruction> = Parsers.parser()
  }

  /**
   * Returns an instruction that (in essence) does this instruction [factor] times. The [factor]
   * must be non-negative, and if zero, [NoOp] is returned.
   */
  operator fun times(factor: Int): Instruction {
    if (factor == 0) return NoOp
    require(factor > 0)
    return scale(factor)
  }

  protected abstract fun scale(factor: Int): Instruction

  /** An instruction that does nothing. */
  public object NoOp : Instruction() {
    override fun scale(factor: Int) = this

    override fun isAbstract(einfo: ExpressionInfo) = false

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      if (proposed != NoOp) throw InvalidReificationException("not Ok")
    }

    override fun visitChildren(visitor: Visitor) {}
    override fun toString() = "Ok"
  }

  public sealed class Change : Instruction() {
    abstract val count: Scalar

    abstract val gaining: Expression?
    abstract val removing: Expression?
    abstract val intensity: Intensity?

    override fun isAbstract(einfo: ExpressionInfo): Boolean {
      return intensity?.abstract != false ||
          count.abstract ||
          (gaining?.let { einfo.isAbstract(it) } == true) ||
          (removing?.let { einfo.isAbstract(it) } == true)
    }

    val amount: Amount by lazy { Amount(count, intensity) }

    data class Amount(val scalar: Scalar, val intensity: Intensity?) : Reifiable<Amount> {
      override val abstract: Boolean = scalar.abstract || intensity?.abstract != false

      override fun ensureNarrows(that: Amount) {
        intensity!!.ensureNarrows(that.intensity!!)
        if (that.intensity == OPTIONAL && scalar is ActualScalar && that.scalar is ActualScalar) {
          if (scalar.value > that.scalar.value) throw InvalidReificationException("")
        } else {
          scalar.ensureNarrows(that.scalar)
        }
      }
    }

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      if (proposed == NoOp && intensity == OPTIONAL) return
      (proposed as Change).amount.ensureReifies(amount)
      gaining?.let { einfo.ensureReifies(it, proposed.gaining!!) }
      removing?.let { einfo.ensureReifies(it, proposed.removing!!) }
    }
  }

  public data class Gain
  internal constructor(
      val scaledEx: ScaledExpression,
      override val intensity: Intensity?,
  ) : Change() {
    companion object {
      fun gain(scaledEx: ScaledExpression, intensity: Intensity? = MANDATORY): Instruction =
          if (scaledEx.expression == OK.expression) NoOp else Gain(scaledEx, intensity)
    }

    override val count = scaledEx.scalar
    override val gaining = scaledEx.expression
    override val removing = null

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
    override fun scale(factor: Int) = copy(scaledEx = scaledEx * factor)

    override fun toString() = "$scaledEx${intensity?.symbol ?: ""}"

    init {
      checkNonzero(count)
    }
  }

  data class Remove(
      val scaledEx: ScaledExpression,
      override val intensity: Intensity? = MANDATORY,
  ) : Change() {
    override val count = scaledEx.scalar
    override val gaining = null
    override val removing = scaledEx.expression

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
    override fun scale(factor: Int) = copy(scaledEx = scaledEx * factor)

    override fun toString() = "-$scaledEx${intensity?.symbol ?: ""}"

    init {
      checkNonzero(count)
    }
  }

  data class Transmute(
      val fromEx: FromExpression,
      val scalar: Scalar,
      override val intensity: Intensity? = MANDATORY,
  ) : Change() {
    override val count = scalar
    override val gaining = fromEx.toExpression
    override val removing = fromEx.fromExpression

    override fun visitChildren(visitor: Visitor) = visitor.visit(fromEx)
    override fun scale(factor: Int) = copy(scalar = scalar * factor)

    override fun toString(): String {
      val scalText = if (scalar == ActualScalar(1)) "" else "$scalar "
      return "$scalText$fromEx${intensity?.symbol ?: ""}"
    }

    init {
      checkNonzero(count)
    }

    override fun safeToNestIn(container: PetNode) =
        super.safeToNestIn(container) && (fromEx !is SimpleFrom || container !is Or)

    override fun precedence() = if (fromEx is SimpleFrom) 7 else 10

    companion object {
      fun tryMerge(left: Instruction, right: Instruction): Transmute? {
        val gain: Gain = if (left is Gain) left else right as? Gain ?: return null
        val remove: Remove =
            if (left == gain) {
              (right as? Remove)
            } else {
              (left as? Remove)
            }
                ?: return null

        val scalar = gain.scaledEx.scalar

        if (remove.scaledEx.scalar != scalar) return null
        val intensity = setOfNotNull(gain.intensity, remove.intensity).singleOrNull() ?: return null
        return Transmute(
            SimpleFrom(gain.scaledEx.expression, remove.scaledEx.expression), scalar, intensity)
      }
    }
  }

  data class Per(val instruction: Instruction, val metric: Metric) : Instruction() {
    init {
      when (instruction) {
        is Gain,
        is Remove,
        is Transmute, -> {}
        else -> throw PetSyntaxException("Per can only contain gain/remove/transmute for now")
      }
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(metric, instruction)
    override fun scale(factor: Int) = copy(instruction = instruction * factor)

    override fun precedence() = 8

    override fun isAbstract(einfo: ExpressionInfo) = instruction.isAbstract(einfo)

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      proposed as Per
      if (proposed.metric != metric) {
        throw InvalidReificationException("can't change the metric")
      }
      proposed.instruction.ensureReifies(instruction, einfo)
    }

    override fun toString() = "$instruction / $metric"
  }

  data class Gated(val gate: Requirement, val mandatory: Boolean, val instruction: Instruction) :
      Instruction() {
    init {
      if (instruction is Gated) throw PetSyntaxException("You don't gate a gater")
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(gate, instruction)
    override fun scale(factor: Int) = copy(instruction = instruction * factor)

    override fun isAbstract(einfo: ExpressionInfo) = instruction.isAbstract(einfo)

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      proposed as Gated
      if (proposed.gate != gate) {
        throw InvalidReificationException("can't change the condition")
      }
      proposed.instruction.ensureReifies(instruction, einfo)
    }

    override fun toString(): String {
      val connector = if (mandatory) ": " else " ?: "
      return "${groupPartIfNeeded(gate)}$connector${groupPartIfNeeded(instruction)}"
    }

    // let's over-group for clarity
    override fun safeToNestIn(container: PetNode) =
        super.safeToNestIn(container) && container !is Or

    override fun precedence() = 6
  }

  data class Custom(
      val functionName: String,
      val arguments: List<Expression>,
      val multiplier: Int, // TODO support in language so we can roundtrip this
  ) : Instruction() {
    override fun visitChildren(visitor: Visitor) = visitor.visit(arguments)
    override fun scale(factor: Int) = copy(multiplier = multiplier * factor)

    override fun isAbstract(einfo: ExpressionInfo) = arguments.any { einfo.isAbstract(it) }

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      proposed as Custom
      if (proposed.multiplier != multiplier) {
        throw InvalidReificationException("can't change multiplier")
      }
      if (proposed.functionName != functionName) {
        throw InvalidReificationException("can't change function name")
      }
      if (proposed.arguments.size != arguments.size) {
        throw InvalidReificationException("wrong argument count")
      }
      for ((wide, narrow) in arguments.zip(proposed.arguments)) {
        einfo.ensureReifies(wide, narrow)
      }
    }

    override fun toString() = "@$functionName(${arguments.joinToString()})"
  }

  sealed class CompositeInstruction(instrs: List<Instruction>) : Instruction() {
    init {
      require(instrs.size >= 2)
    }

    abstract val instructions: List<Instruction>

    abstract fun copy(instructions: Iterable<Instruction>): Instruction

    final override fun scale(factor: Int) = copy(instructions.map { it * factor })

    override fun visitChildren(visitor: Visitor) = visitor.visit(instructions)

    abstract fun connector(): String

    final override fun toString() = instructions.joinToString(connector()) { groupPartIfNeeded(it) }
  }

  data class Then(override val instructions: List<Instruction>) :
      CompositeInstruction(instructions) {
    override fun copy(instructions: Iterable<Instruction>) =
        copy(instructions = instructions.toList())

    override fun precedence() = 2

    override fun isAbstract(einfo: ExpressionInfo) = instructions.any { it.isAbstract(einfo) }

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      proposed as Then
      for ((wide, narrow) in instructions.zip(proposed.instructions)) {
        narrow.ensureReifies(wide, einfo)
      }
      val maybeXs = this.descendantsOfType<Scalar>()
      if (maybeXs.any { it is XScalar }) {
        val noXs = proposed.descendantsOfType<ActualScalar>()
        require(maybeXs.size == noXs.size) { "$maybeXs / $noXs" }

        val allXValues = mutableSetOf<Int>()
        for ((maybeX, noX) in maybeXs.zip(noXs)) {
          if (maybeX is XScalar) {
            require(noX.value % maybeX.multiple == 0)
            allXValues += noX.value / maybeX.multiple
          }
        }
        if (allXValues.size > 1) {
          throw InvalidReificationException("Can't set different values for X: $allXValues")
        }
      }
    }

    override fun connector() = " THEN "
  }

  data class Or(override val instructions: List<Instruction>) : CompositeInstruction(instructions) {
    init {
      if (instructions.distinct().size != instructions.size) {
        throw PetSyntaxException("duplicates")
      }
    }

    override fun copy(instructions: Iterable<Instruction>) =
        copy(instructions = instructions.toList())

    override fun safeToNestIn(container: PetNode) =
        super.safeToNestIn(container) && container !is Then

    override fun precedence() = 4

    override fun isAbstract(einfo: ExpressionInfo) = true

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      var messages = ""
      for (option in instructions) {
        try { // Just get any one to work
          proposed.ensureReifies(option, einfo)
          return
        } catch (e: InvalidReificationException) {
          messages += "${e.message}\n"
        }
      }
      throw InvalidReificationException(
          "Instruction `$proposed` doesn't reify any arm of the OR instruction:\n$messages")
    }

    override fun connector() = " OR "

    companion object {
      fun create(instructions: List<Instruction>): Instruction {
        require(instructions.any())
        return if (instructions.size == 1) {
          instructions.first()
        } else {
          Or(instructions)
        }
      }

      fun create(first: Instruction, vararg rest: Instruction) =
          if (rest.none()) first else Or(listOf(first) + rest)
    }
  }

  data class Multi(override val instructions: List<Instruction>) :
      CompositeInstruction(instructions) {
    init {
      require(instructions.count { it.descendantsOfType<XScalar>().any() } <= 1)
    }

    override fun copy(instructions: Iterable<Instruction>) =
        copy(instructions = instructions.toList())

    override fun isAbstract(einfo: ExpressionInfo) = error("should have been split by now: $this")

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) =
        error("should have been split by now: $this")

    override fun precedence() = 0

    override fun connector() = ", "

    companion object {
      fun create(instructions: List<Instruction>): Instruction {
        return when (instructions.size) {
          0 -> NoOp
          1 -> instructions.single()
          else -> Multi(instructions)
        }
      }

      fun create(first: Instruction, vararg rest: Instruction) =
          if (rest.none()) first else Multi(listOf(first) + rest)
    }
  }

  data class Transform(val instruction: Instruction, override val transformKind: String) :
      Instruction(), TransformNode<Instruction> {
    override fun visitChildren(visitor: Visitor) = visitor.visit(instruction)

    override fun scale(factor: Int) = copy(instruction = instruction * factor)

    override fun isAbstract(einfo: ExpressionInfo) =
        error("should have been transformed by now: $this")

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) =
        error("should have been transformed by now: $this")

    override fun toString() = "$transformKind[$instruction]"

    override fun extract() = instruction
  }

  override val kind = Instruction::class.simpleName!!

  protected abstract fun isAbstract(einfo: ExpressionInfo): Boolean

  fun ensureReifies(abstractTarget: Instruction, einfo: ExpressionInfo) {
    AsReifiable(this, einfo).ensureReifies(AsReifiable(abstractTarget, einfo))
  }

  data class AsReifiable(
      val instruction: Instruction,
      val einfo: ExpressionInfo,
  ) : Reifiable<AsReifiable> {
    override fun ensureNarrows(that: AsReifiable) {
      val abstractTarget = that.instruction
      if (abstractTarget !is Or &&
          instruction != NoOp &&
          instruction::class != abstractTarget::class) {
        throw InvalidReificationException("`$instruction` can't reify `$abstractTarget`")
      }
      abstractTarget.checkReificationDoNotCall(instruction, einfo) // well WE can call it
    }

    override val abstract = instruction.isAbstract(einfo)
  }

  protected abstract fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo)

  enum class Intensity(val symbol: String, override val abstract: Boolean = false) :
      Reifiable<Intensity> {
    /** The full amount must be gained/removed/transmuted. */
    MANDATORY("!"),

    /** Do "as much as possible" of the amount. */
    AMAP("."),

    /** The player can choose how much of the amount to do, including none of it. */
    OPTIONAL("?", true),
    ;

    override fun ensureNarrows(that: Intensity) {
      if (that != this && that != OPTIONAL) {
        throw InvalidReificationException("")
      }
    }

    companion object {
      fun from(symbol: String) = values().first { it.symbol == symbol }
    }
  }

  private object Parsers : PetTokenizer() {
    internal fun parser(): Parser<Instruction> {
      return parser {
        val gain: Parser<Instruction> =
            ScaledExpression.parser() and
            optional(intensity) map { (ste, int) ->
              Gain.gain(ste, int)
            }

        val remove: Parser<Remove> =
            skipChar('-') and
            ScaledExpression.parser() and
            optional(intensity) map { (ste, int) ->
              Remove(ste, int)
            }

        val transmute: Parser<Transmute> =
            optional(ScaledExpression.scalar()) and
            FromExpression.parser() and
            optional(intensity) map { (scalar, fro, int) ->
              Transmute(fro, scalar ?: ActualScalar(1), int)
            }

        val perable: Parser<Instruction> = transmute or group(transmute) or gain or remove

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
              Custom(name.text, args, 1)
            }
        val atom: Parser<Instruction> = group(parser()) or maybeTransform or custom

        val isMandatory: Parser<Boolean> = (_questionColon asJust false) or (char(':') asJust true)

        val gated: Parser<Instruction> =
            optional(Requirement.atomParser() and isMandatory) and
            atom map { (gate, ins) ->
              if (gate == null) ins else Gated(gate.t1, gate.t2, ins)
            }

        val orInstr: Parser<Instruction> =
            separatedTerms(gated, _or) map {
              val set = it.toSetStrict().toList()
              if (set.size == 1) set.first() else Or(set)
            }

        val then = separatedTerms(orInstr, _then) map { if (it.size == 1) it.first() else Then(it) }

        commaSeparated(then) map { Multi.create(it) }
      }
    }
  }
}
