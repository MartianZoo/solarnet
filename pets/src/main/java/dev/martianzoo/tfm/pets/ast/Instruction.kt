package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.asJust
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.Exceptions.InvalidReificationException
import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.pets.BaseTokenizer
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.util.suf
import dev.martianzoo.util.toSetStrict

public sealed class Instruction : PetNode() {

  // better pass at least 1
  abstract operator fun times(factor: Int): Instruction

  public sealed class Change : Instruction() {
    abstract val count: Int
    abstract val gaining: Expression?
    abstract val removing: Expression?
    abstract val intensity: Intensity?

    abstract override fun times(factor: Int): Change

    override fun isAbstract(einfo: ExpressionInfo): Boolean {
      return intensity!! == OPTIONAL ||
          (gaining?.let { einfo.isAbstract(it) } == true) ||
          (removing?.let { einfo.isAbstract(it) } == true)
    }

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      if (proposed == nullInstruction && intensity == OPTIONAL) return
      proposed as Change

      gaining?.let { einfo.checkReifies(it, proposed.gaining!!) }
      removing?.let { einfo.checkReifies(it, proposed.removing!!) }
      if (proposed.count > count) {
        throw InvalidReificationException(
            "Can't increase amount from ${count} to ${proposed.count}")
      }
      if (proposed.intensity!! == OPTIONAL) {
        throw InvalidReificationException("Can't execute an optional instruction")
      }
      // Optional to non-optional is otherwise always fine
      if (intensity!! != OPTIONAL) {
        if (proposed.count != count) {
          throw InvalidReificationException(
              "Can't decrease amount from ${count} to ${proposed.count}")
        }
        if (proposed.intensity!! != intensity) {
          throw InvalidReificationException(
              "Can't change the intensity of a non-optional instruction")
        }
      }
    }
  }

  public data class Gain(
      val scaledEx: ScaledExpression,
      override val intensity: Intensity? = null,
  ) : Change() {
    override val count = scaledEx.scalar
    override val gaining = scaledEx.expression
    override val removing = null

    override fun visitChildren(visitor: Visitor) = visitor.visit(scaledEx)
    override fun times(factor: Int) = copy(scaledEx = scaledEx.copy(scalar = count * factor))

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
    override fun times(factor: Int) = copy(scaledEx = scaledEx.copy(scalar = count * factor))

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
    override fun times(factor: Int) =
        if (factor == 1) this else copy(scalar = (scalar ?: 1) * factor)

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
        val scal: Int? = if (scalar == 1) null else scalar
        return Transmute(
            SimpleFrom(gain.scaledEx.expression, remove.scaledEx.expression), scal, intensity)
      }
    }
  }

  data class Per(val instruction: Instruction, val metric: Metric) : Instruction() {
    init {
      when (instruction) {
        is Gain,
        is Remove,
        is Transmute, -> {}
        else -> throw PetException("Per can only contain gain/remove/transmute") // TODO more
      }
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(metric, instruction)
    override fun times(factor: Int) = copy(instruction = instruction * factor)

    override fun precedence() = 8

    override fun isAbstract(einfo: ExpressionInfo) = instruction.isAbstract(einfo)

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      proposed as Per
      if (proposed.metric != metric) {
        throw InvalidReificationException("can't change the metric")
      }
      proposed.instruction.checkReifies(instruction, einfo)
    }

    override fun toString() = "$instruction / $metric"
  }

  data class Gated(val gate: Requirement, val mandatory: Boolean, val instruction: Instruction) :
      Instruction() {
    init {
      if (instruction is Gated) {
        throw PetException("You don't gate a gater") // TODO enable
      }
    }

    override fun visitChildren(visitor: Visitor) = visitor.visit(gate, instruction)
    override fun times(factor: Int) = copy(instruction = instruction * factor)

    override fun isAbstract(einfo: ExpressionInfo) = instruction.isAbstract(einfo)

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      proposed as Gated
      if (proposed.gate != gate) {
        throw InvalidReificationException("can't change the condition")
      }
      proposed.instruction.checkReifies(instruction, einfo)
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

  data class Custom(val functionName: String, val arguments: List<Expression>) : Instruction() {
    constructor(
        functionName: String,
        vararg arguments: Expression,
    ) : this(functionName, arguments.toList())

    override fun visitChildren(visitor: Visitor) = visitor.visit(arguments)
    override fun times(factor: Int) = Multi.create(List(factor) { this })!!

    override fun isAbstract(einfo: ExpressionInfo): Boolean {
      return false // TODO
    }

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      proposed as Custom
      if (proposed.functionName != functionName) {
        throw InvalidReificationException("can't change function name")
      }
      if (proposed.arguments.size != arguments.size) {
        throw InvalidReificationException("wrong argument count")
      }
      for ((yours, mine) in proposed.arguments.zip(arguments)) {
        einfo.checkReifies(mine, yours)
      }
    }

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
    override fun times(factor: Int) = copy(instructions.map { it * factor })

    override fun isAbstract(einfo: ExpressionInfo) = instructions.any { it.isAbstract(einfo) }

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      proposed as Then
      for ((yours, mine) in proposed.instructions.zip(instructions)) {
        yours.checkReifies(mine, einfo)
      }
    }

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
    override fun times(factor: Int) = copy(instructions.map { it * factor })

    override fun isAbstract(einfo: ExpressionInfo) = true

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) {
      var messages = ""
      for (option in instructions) {
        try { // Just get any one to work
          proposed.checkReifies(option, einfo)
          return
        } catch (e: InvalidReificationException) {
          messages += "${e.message}\n"
        }
      }
      throw InvalidReificationException(
          "Instruction `$proposed` doesn't reify any arm of the OR instruction:\n$messages")
    }

    override fun toString() = toString(" OR ")
  }

  data class Multi(override val instructions: List<Instruction>) : CompositeInstruction() {
    constructor(vararg instructions: Instruction) : this(instructions.toList())

    init {
      require(instructions.size >= 2)
    }

    override fun precedence() = 0

    override fun times(factor: Int) = copy(instructions.map { it * factor })

    override fun isAbstract(einfo: ExpressionInfo) = error("should have been split by now")

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) =
        error("should have been split by now")

    override fun toString() = toString(", ")

    companion object {
      fun create(instructions: List<Instruction>): Instruction? {
        return when (instructions.size) {
          0 -> null
          1 -> instructions.single()
          else -> Multi(instructions)
        }
      }
      fun create(first: Instruction, vararg rest: Instruction) =
          if (rest.none()) first else Multi(listOf(first) + rest)
    }
  }

  data class Transform(val instruction: Instruction, override val transformKind: String) :
      Instruction(), GenericTransform<Instruction> {
    override fun visitChildren(visitor: Visitor) = visitor.visit(instruction)
    override fun times(factor: Int) = copy(instruction = instruction * factor)

    override fun isAbstract(einfo: ExpressionInfo) = error("")

    override fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo) = error("")

    override fun toString() = "$transformKind[$instruction]"

    override fun extract() = instruction
  }

  override val kind = Instruction::class.simpleName!!

  protected abstract fun isAbstract(einfo: ExpressionInfo): Boolean

  fun checkReifies(abstractTarget: Instruction, einfo: ExpressionInfo) {
    if (isAbstract(einfo)) {
      throw InvalidReificationException("A reification must be concrete")
    }
    if (this == abstractTarget) return
    if (!abstractTarget.isAbstract(einfo)) {
      throw InvalidReificationException("Already concrete: $abstractTarget")
    }
    if (abstractTarget !is Or && this != nullInstruction && this::class != abstractTarget::class) {
      throw InvalidReificationException(
          "A ${this::class.simpleName} instruction can't reify a" +
              " ${abstractTarget::class.simpleName} instruction")
    }
    abstractTarget.checkReificationDoNotCall(this, einfo) // well WE can call it
  }

  protected abstract fun checkReificationDoNotCall(proposed: Instruction, einfo: ExpressionInfo)

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

  companion object : BaseTokenizer() {
    fun split(instruction: Iterable<Instruction>): List<Instruction> =
        instruction.flatMap { split(it) }

    fun split(instruction: Instruction): List<Instruction> =
        if (instruction is Multi) {
          split(instruction.instructions)
        } else {
          listOf(instruction)
        }

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
                optional(intensity) map
                { (scalar, fro, int) ->
                  Transmute(fro, scalar, int)
                }

        val perable: Parser<Change> = transmute or group(transmute) or gain or remove

        val maybePer: Parser<Instruction> =
            perable and
                optional(skipChar('/') and Metric.parser()) map
                { (instr, metric) ->
                  if (metric == null) instr else Per(instr, metric)
                }

        val transform: Parser<Transform> =
            transform(parser()) map { (node, tname) -> Transform(node, tname) }
        val maybeTransform: Parser<Instruction> = transform or maybePer

        val arguments = separatedTerms(Expression.parser(), char(','), acceptZero = true)
        val custom: Parser<Custom> =
            skipChar('@') and
                _lowerCamelRE and
                group(arguments) map
                { (name, args) ->
                  Custom(name.text, args)
                }
        val atom: Parser<Instruction> = group(parser()) or maybeTransform or custom

        val isMandatory: Parser<Boolean> = (_questionColon asJust false) or (char(':') asJust true)

        val gated: Parser<Instruction> =
            optional(Requirement.atomParser() and isMandatory) and
                atom map
                { (gate, ins) ->
                  if (gate == null) ins else Gated(gate.t1, gate.t2, ins)
                }

        val orInstr: Parser<Instruction> =
            separatedTerms(gated, _or) map
                {
                  val set = it.toSetStrict().toList()
                  if (set.size == 1) set.first() else Or(set)
                }

        val then = separatedTerms(orInstr, _then) map { if (it.size == 1) it.first() else Then(it) }
        commaSeparated(then) map { Multi.create(it)!! }
      }
    }

    public val nullInstruction = Gain(scaledEx(OK.expr), MANDATORY)
  }
}
