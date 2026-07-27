package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.api.SystemClasses.OK
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.util.Reifiable
import dev.martianzoo.util.toSetStrict

/**
 * A specification of steps that might be taken (or were taken) to alter a game state. Instructions
 * appear as the right-hand side of [Action]s and [Effect]s, on map areas, in the "do this now"
 * section of cards, in an engine's task queues, and so forth.
 */
public sealed class Instruction : PetElement() {
  public companion object {
    /** Recursively breaks apart any [Multi] instructions found in [instruction]. */
    public fun split(instruction: Instruction): InstructionGroup =
        InstructionGroup(
            when (instruction) {
              is Multi -> instruction.instructions.flatMap { split(it).instructions }
              is NoOp -> listOf()
              else -> listOf(instruction)
            }
        )

    internal fun parser(): Parser<Instruction> = Parsers.parser()
  }

  /** A flattened list of instructions containing no instances of [NoOp] or [Multi]. */
  public data class InstructionGroup(val instructions: List<Instruction>) {
    public val size: Int by instructions::size

    public fun <T> map(function: (Instruction) -> T): List<T> = instructions.map(function)

    public fun forEach(consumer: (Instruction) -> Unit): Unit = instructions.forEach(consumer)

    internal fun asInstruction() = Multi.create(instructions)

    init {
      require(instructions.all { it !is NoOp && it !is Multi })
    }
  }

  /**
   * Returns an instruction that (in essence) does this instruction [factor] times. The [factor]
   * must be non-negative, and if zero, [NoOp] is returned.
   */
  public operator fun times(factor: Int): Instruction {
    if (factor == 0) return NoOp
    require(factor > 0)
    return scale(factor)
  }

  protected abstract fun scale(factor: Int): Instruction

  /** An instruction that does nothing. */
  public object NoOp : Instruction() {
    override fun scale(factor: Int): Instruction = this

    override fun isAbstract(info: TypeInfo): Boolean = false

    override fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo) {
      if (proposed != NoOp) throw NarrowingException("not Ok")
    }

    override fun visitChildren(visitor: Visitor): Unit = Unit

    override fun toString(): String = "Ok"
  }

  public sealed class Change : Instruction() {
    public companion object {
      public fun change(
          count: Int = 1,
          gaining: Expression? = null,
          removing: Expression? = null,
          intensity: Intensity? = MANDATORY,
      ): Instruction {
        require(count >= 0)
        return when {
          count == 0 -> NoOp
          removing == null -> gain(scaledEx(count, gaining!!), intensity)
          gaining == null -> Remove(scaledEx(count, removing), intensity)
          else -> Transmute(FromExpression(gaining, removing), ActualScalar(count), intensity)
        }
      }
    }

    public abstract val count: Scalar

    public abstract val gaining: Expression?
    public abstract val removing: Expression?
    public abstract val intensity: Intensity?

    override fun isAbstract(info: TypeInfo): Boolean {
      return intensity?.abstract != false ||
          count.abstract ||
          (gaining?.let { info.isAbstract(it) } == true) ||
          (removing?.let { info.isAbstract(it) } == true)
    }

    private val amount: Amount by lazy { Amount(count, intensity) }

    internal data class Amount(val scalar: Scalar, val intensity: Intensity?) : Reifiable<Amount> {
      override val abstract: Boolean = scalar.abstract || intensity?.abstract != false

      override fun ensureNarrows(that: Amount, info: TypeInfo) {
        intensity!!.ensureNarrows(that.intensity!!, info)
        if (that.intensity == OPTIONAL && scalar is ActualScalar && that.scalar is ActualScalar) {
          if (scalar.value > that.scalar.value) throw NarrowingException("")
        } else {
          scalar.ensureNarrows(that.scalar, info)
        }
      }
    }

    override fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo) {
      if (proposed == NoOp && intensity == OPTIONAL) return
      proposed as? Change ?: throw NarrowingException("$this  /  $proposed")
      proposed.amount.ensureNarrows(amount, info)
      gaining?.let { info.ensureNarrows(it, proposed.gaining!!) }
      removing?.let { info.ensureNarrows(it, proposed.removing!!) }
    }
  }

  @ExposedCopyVisibility
  public data class Gain
  internal constructor(
      val scaledEx: ScaledExpression,
      override val intensity: Intensity?,
  ) : Change() {
    public companion object {
      public fun gain(scaledEx: ScaledExpression, intensity: Intensity? = MANDATORY): Instruction =
          if (scaledEx.expression == OK.expression) NoOp else Gain(scaledEx, intensity)
    }

    override val count: Scalar = scaledEx.scalar
    override val gaining: Expression = scaledEx.expression
    override val removing: Expression? = null

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scaledEx)

    override fun scale(factor: Int): Instruction = copy(scaledEx = scaledEx * factor)

    override fun toString(): String = "$scaledEx${intensity?.symbol ?: ""}"

    init {
      checkNonzero(count)
    }
  }

  public data class Remove(
      val scaledEx: ScaledExpression,
      override val intensity: Intensity? = MANDATORY,
  ) : Change() {
    override val count: Scalar = scaledEx.scalar
    override val gaining: Expression? = null
    override val removing: Expression = scaledEx.expression

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scaledEx)

    override fun scale(factor: Int): Instruction = copy(scaledEx = scaledEx * factor)

    override fun toString(): String = "-$scaledEx${intensity?.symbol ?: ""}"

    init {
      checkNonzero(count)
    }
  }

  public data class Transmute(
      val fromEx: FromExpression,
      val scalar: Scalar,
      override val intensity: Intensity? = MANDATORY,
  ) : Change() {
    override val count: Scalar = scalar
    override val gaining: Expression = fromEx.toExpression
    override val removing: Expression = fromEx.fromExpression

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(fromEx)

    override fun scale(factor: Int): Instruction = copy(scalar = scalar * factor)

    override fun toString(): String {
      val scalText = if (scalar == ActualScalar(1)) "" else "$scalar "
      return "$scalText$fromEx${intensity?.symbol ?: ""}"
    }

    init {
      checkNonzero(count)
    }

    override fun safeToNestIn(container: PetNode): Boolean =
        super.safeToNestIn(container) && container !is Or

    override fun precedence(): Int = 7
  }

  public data class Per(val inner: Instruction, val metric: Metric) : Instruction() {
    init {
      if (inner !is Change) {
        throw PetSyntaxException("Per can only contain gain/remove/transmute for now")
      }
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(metric, inner)

    override fun scale(factor: Int): Instruction = copy(inner = inner * factor)

    override fun precedence(): Int = 8

    override fun isAbstract(info: TypeInfo): Boolean = inner.isAbstract(info)

    override fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo) {
      proposed as Per
      if (proposed.metric != metric) {
        throw NarrowingException("can't change the metric")
      }
      proposed.inner.ensureNarrows(inner, info)
    }

    override fun toString(): String = "$inner / $metric"
  }

  public data class Gated(val gate: Requirement, val inner: Instruction) : Instruction() {
    public companion object {
      public fun create(gate: Requirement?, inner: Instruction): Instruction =
          if (gate == null) inner else Gated(gate, inner)
    }

    init {
      if (inner is Gated) throw PetSyntaxException("You don't gate a gater")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(gate, inner)

    override fun scale(factor: Int): Instruction = copy(inner = inner * factor)

    override fun isAbstract(info: TypeInfo): Boolean = inner.isAbstract(info)

    override fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo) {
      proposed as Gated
      if (proposed.gate != gate) {
        throw NarrowingException("can't change the condition")
      }
      proposed.inner.ensureNarrows(inner, info)
    }

    override fun toString(): String = "${groupPartIfNeeded(gate)}: ${groupPartIfNeeded(inner)}"

    // let's over-group for clarity
    override fun safeToNestIn(container: PetNode): Boolean =
        super.safeToNestIn(container) && container !is Or

    override fun precedence(): Int = 6
  }

  public sealed class CompositeInstruction(instrs: List<Instruction>) : Instruction() {
    init {
      require(instrs.size >= 2)
    }

    public abstract val instructions: List<Instruction>

    internal abstract fun copy(instructions: Iterable<Instruction>): Instruction

    final override fun scale(factor: Int): Instruction = copy(instructions.map { it * factor })

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(instructions)

    internal abstract fun connector(): String

    final override fun toString(): String =
        instructions.joinToString(connector()) { groupPartIfNeeded(it) }
  }

  public data class Then(override val instructions: List<Instruction>) :
      CompositeInstruction(instructions) {
    init {
      if (instructions.size < 2) throw PetSyntaxException("")

      // it's okay for the final instruction to have a Multi in it, but not the previous ones
      val allButLast = instructions.subList(0, instructions.size - 1)
      val problem = allButLast.any { it.descendantsOfType<Multi>().any() }
      if (problem) throw PetSyntaxException("Bad THEN")
    }

    override fun copy(instructions: Iterable<Instruction>) =
        copy(instructions = instructions.toList())

    override fun precedence(): Int = 2

    override fun isAbstract(info: TypeInfo): Boolean = instructions.any { it.isAbstract(info) }

    // TODO understand and simplify
    override fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo) {
      proposed as? Then ?: throw NarrowingException("Can't reify $this to $proposed")
      for ((wide, narrow) in instructions.zip(proposed.instructions)) {
        narrow.ensureNarrows(wide, info)
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
          throw NarrowingException("Can't set different values for X: $allXValues")
        }
      }
    }

    internal fun keepLinked() = descendantsOfType<XScalar>().any()

    override fun connector() = " THEN "

    public companion object {
      public fun create(it: List<Instruction>): Instruction =
          when (it.size) {
            0 -> NoOp
            1 -> it.first()
            else -> Then(it)
          }
    }
  }

  public data class Or(override val instructions: List<Instruction>) :
      CompositeInstruction(instructions) {
    init {
      if (instructions.distinct().size != instructions.size) {
        throw PetSyntaxException("duplicates")
      }
    }

    override fun copy(instructions: Iterable<Instruction>) =
        copy(instructions = instructions.toList())

    override fun safeToNestIn(container: PetNode): Boolean =
        super.safeToNestIn(container) && container !is Then

    override fun precedence(): Int = 4

    override fun isAbstract(info: TypeInfo): Boolean = true

    override fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo) {
      if (proposed is Or) {
        proposed.instructions.forEach { ensureIsNarrowedBy_doNotCall(it, info) }
        return
      }
      var messages = ""
      for (option in instructions) {
        try { // Just get any one to work
          proposed.ensureNarrows(option, info)
          return
        } catch (e: NarrowingException) {
          messages += "${e.message}\n"
        }
      }
      throw NarrowingException(
          "Instruction `$proposed` doesn't narrow any arm of `$this`:\n$messages",
      )
    }

    override fun connector() = " OR "

    public companion object {
      public fun create(instructions: Collection<Instruction>): Instruction {
        require(instructions.any())
        val set = instructions.toSet()
        return if (set.size == 1) {
          set.first()
        } else {
          Or(set.toList())
        }
      }

      internal fun create(first: Instruction, vararg rest: Instruction) =
          if (rest.none()) first else Or(listOf(first) + rest)
    }
  }

  public data class Multi(override val instructions: List<Instruction>) :
      CompositeInstruction(instructions) {
    init {
      require(instructions.count { it.descendantsOfType<XScalar>().any() } <= 1)
    }

    override fun copy(instructions: Iterable<Instruction>) =
        copy(instructions = instructions.toList())

    override fun isAbstract(info: TypeInfo): Boolean = instructions.any { it.isAbstract(info) }

    override fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo) {
      if (proposed != this) {
        error("should have been split by now: $this")
      }
    }

    override fun precedence(): Int = 0

    override fun connector() = ", "

    public companion object {
      public fun create(instructions: List<Instruction>): Instruction {
        return when (instructions.size) {
          0 -> NoOp
          1 -> instructions.single()
          else -> Multi(instructions)
        }
      }

      internal fun create(first: Instruction, vararg rest: Instruction) =
          if (rest.none()) first else Multi(listOf(first) + rest)
    }
  }

  public data class Transform(val instruction: Instruction, override val transformKind: String) :
      Instruction(), TransformNode<Instruction> {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(instruction)

    override fun scale(factor: Int): Instruction = copy(instruction = instruction * factor)

    override fun isAbstract(info: TypeInfo): Boolean =
        error("should have been transformed by now: $this")

    override fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo): Unit =
        error("should have been transformed by now: $this")

    override fun toString(): String = "$transformKind[$instruction]"

    override fun extract(): Instruction = instruction
  }

  override val kind: kotlin.reflect.KClass<out PetNode> = Instruction::class

  public abstract fun isAbstract(info: TypeInfo): Boolean

  @Suppress("TooGenericExceptionCaught") // TODO
  public fun narrows(abstractInstr: Instruction, info: TypeInfo): Boolean =
      try {
        ensureNarrows(abstractInstr, info)
        true
      } catch (_: Exception) {
        false
      }

  // This is the entry point into all the ensureNarrows business throughout the codebase
  public fun ensureNarrows(abstractInstr: Instruction, info: TypeInfo) {
    if (abstractInstr !is Or && this != NoOp && this::class != abstractInstr::class) {
      throw NarrowingException("`$this` can't reify `$abstractInstr` (different types)")
    }
    try {
      abstractInstr.ensureIsNarrowedBy_doNotCall(this, info) // well WE can call it
    } catch (e: NarrowingException) {
      throw NarrowingException("$this does not narrow $abstractInstr", e)
    }
  }

  @Suppress("FunctionNaming")
  protected abstract fun ensureIsNarrowedBy_doNotCall(proposed: Instruction, info: TypeInfo)

  public enum class Intensity(internal val symbol: String, override val abstract: Boolean = false) :
      Reifiable<Intensity> {
    /** The full amount must be gained/removed/transmuted. */
    MANDATORY("!"),

    /** Do "as much as possible" of the amount. */
    AMAP("."),

    /** The player can choose how much of the amount to do, including none of it. */
    OPTIONAL("?", true),
    ;

    override fun ensureNarrows(that: Intensity, info: TypeInfo) {
      if (that != this && that != OPTIONAL) {
        throw NarrowingException("")
      }
    }

    internal companion object {
      internal fun from(symbol: String) = entries.first { it.symbol == symbol }
    }
  }

  private object Parsers : PetTokenizer() {
    internal fun parser(): Parser<Instruction> {
      return parser {
        val gain: Parser<Instruction> =
            ScaledExpression.parser() and optional(intensity) map { (ste, int) -> gain(ste, int) }

        val remove: Parser<Remove> =
            skipChar('-') and
                ScaledExpression.parser() and
                optional(intensity) map
                { (ste, int) ->
                  Remove(ste, int)
                }

        val transmute: Parser<Transmute> =
            optional(ScaledExpression.scalar()) and
                FromExpression.parser() and
                optional(intensity) map
                { (scalar, fro, int) ->
                  Transmute(fro, scalar ?: ActualScalar(1), int)
                }

        val perable: Parser<Instruction> = transmute or group(transmute) or gain or remove

        val maybePer: Parser<Instruction> =
            perable and
                optional(skipChar('/') and Metric.parser()) map
                { (instr, metric) ->
                  if (metric == null) instr else Per(instr, metric)
                }

        val transform: Parser<Transform> =
            transform(parser()) map { (node, tname) -> Transform(node, tname) }

        val maybeTransform: Parser<Instruction> = transform or maybePer

        val atom: Parser<Instruction> = group(parser()) or maybeTransform

        val gated: Parser<Instruction> =
            optional(Requirement.atomParser() and skipChar(':')) and
                atom map
                { (gate, ins) ->
                  if (gate == null) ins else Gated(gate, ins)
                }

        val orInstr: Parser<Instruction> =
            separatedTerms(gated, _or) map
                {
                  val set = it.toSetStrict().toList()
                  if (set.size == 1) set.first() else Or(set)
                }

        val then = separatedTerms(orInstr, _then) map { Then.create(it) }

        commaSeparated(then) map { Multi.create(it) }
      }
    }
  }
}
