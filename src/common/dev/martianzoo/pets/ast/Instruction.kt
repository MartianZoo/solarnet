package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.OK
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.util.invoke
import dev.martianzoo.pets.util.toSetStrict

/**
 * A specification of steps that might be taken (or were taken) to alter a world. Instructions
 * appear as the right-hand side of [Action]s and [Effect]s, on map areas, in the "do this now"
 * section of cards, in an engine's task queues, and so forth.
 */
public sealed class Instruction : InstructionTree() {
  internal companion object {
    internal fun parser(): Parser<Instruction> =
        Parsers.parser() map
            {
              it as? Instruction
                  ?: throw PetSyntaxException("Expected one instruction, got group: $it")
            }

    internal fun treeParser(): Parser<InstructionTree> = Parsers.parser()
  }

  /**
   * Returns an instruction that (in essence) does this instruction [factor] times. The [factor]
   * must be non-negative, and if zero, [NoOp] is returned.
   */
  final override operator fun times(factor: Int): Instruction {
    if (factor == 0) return NoOp
    require(factor > 0)
    return scale(factor)
  }

  override val kind: kotlin.reflect.KClass<out PetNode> = Instruction::class

  protected abstract fun scale(factor: Int): Instruction

  /** An instruction that does nothing. */
  public object NoOp : Instruction() {
    override fun scale(factor: Int): Instruction = this

    override fun isAbstract(info: TypeInfo): Boolean = false

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      if (proposed != NoOp) throw NarrowingException("not Ok")
    }

    override fun visitChildren(visitor: Visitor): Unit = Unit

    override fun toString(): String = "Ok"
  }

  public sealed class Change : Instruction() {
    public companion object {
      /** Creates and canonicalizes a gain, removal, or transmutation instruction. */
      public fun change(
          gaining: Expression? = null,
          removing: Expression? = null,
          count: Int = 1,
          intensity: Intensity? = MANDATORY,
      ): Instruction {
        require(count >= 0)
        return when {
          count == 0 -> NoOp
          removing == null -> Gain.gain(gaining!!, count, intensity)
          gaining == null -> Remove.remove(removing, count, intensity)
          else -> Transmute(Full(gaining, removing), ActualScalar(count), intensity)
        }
      }
    }

    public abstract val count: Scalar

    public abstract val gaining: Expression?
    public abstract val removing: Expression?
    // TODO: Rename Intensity to Quantifier throughout.
    public abstract val intensity: Intensity?

    override fun isAbstract(info: TypeInfo): Boolean {
      return count.isAbstract(info) ||
          intensity?.isAbstract(info) != false ||
          (gaining?.isAbstract(info) == true) ||
          (removing?.isAbstract(info) == true)
    }

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      if (proposed == NoOp && intensity == OPTIONAL) return
      proposed as? Change ?: throw NarrowingException("$this  /  $proposed")
      proposed.intensity!!.ensureNarrows(intensity!!, info)
      val proposedCount = proposed.count
      val authoredCount = count
      if (intensity == OPTIONAL && proposedCount is ActualScalar && authoredCount is ActualScalar) {
        if (proposedCount.value > authoredCount.value) throw NarrowingException("")
      } else {
        proposedCount.ensureNarrows(authoredCount, info)
      }
      gaining?.let { proposed.gaining!!.ensureNarrows(it, info) }
      removing?.let { proposed.removing!!.ensureNarrows(it, info) }
    }
  }

  public data class Gain
  public constructor(
      val scaledEx: ScaledExpression,
      override val intensity: Intensity?,
  ) : Change() {
    public companion object {
      /** Creates and canonicalizes a gain of one copy of [expression]. */
      public fun gain(expression: HasExpression): Instruction = gain(expression, 1)

      /** Creates a gain of [scaledEx], or [NoOp] when its expression is `Ok`. */
      public fun gain(
          scaledEx: ScaledExpression,
          intensity: Intensity? = MANDATORY,
      ): Instruction = if (scaledEx.expression == OK.expression) NoOp else Gain(scaledEx, intensity)

      /** Creates and canonicalizes a gain of [count] copies of [expression]. */
      public fun gain(
          expression: HasExpression,
          count: Int = 1,
          intensity: Intensity? = MANDATORY,
      ): Instruction = gain(scaledEx(expression, count), intensity)
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

  @ConsistentCopyVisibility
  public data class Remove
  internal constructor(
      val scaledEx: ScaledExpression,
      override val intensity: Intensity? = MANDATORY,
  ) : Change() {
    public companion object {
      /** Creates and canonicalizes a removal of one copy of [expression]. */
      public fun remove(expression: HasExpression): Instruction = remove(expression, 1)

      /** Creates and canonicalizes a removal of [scaledEx]. */
      public fun remove(
          scaledEx: ScaledExpression,
          intensity: Intensity? = MANDATORY,
      ): Instruction = Remove(scaledEx, intensity)

      /** Creates and canonicalizes a removal of [count] copies of [expression]. */
      public fun remove(
          expression: HasExpression,
          count: Int = 1,
          intensity: Intensity? = MANDATORY,
      ): Instruction = remove(scaledEx(expression, count), intensity)
    }

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

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scalar, fromEx)

    override fun scale(factor: Int): Instruction = copy(scalar = scalar * factor)

    override fun toString(): String {
      val scalText = if (scalar == ActualScalar(1)) "" else "$scalar "
      return "$scalText$fromEx${intensity?.symbol ?: ""}"
    }

    init {
      checkNonzero(count)
    }

    override fun safeToNestIn(container: PetNode): Boolean =
        super.safeToNestIn(container) && (fromEx !is Full || container !is Or)

    override fun precedence(): Int = if (fromEx is Full) 7 else 10

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      super.ensureIsNarrowedBy(proposed, info)
      if (proposed == NoOp) return
      proposed as Transmute
      val variables = typeVariablesFor(info)
      for (variable in
          variables.variables.filter {
            info.isAbstract(variables.expressionOf(it.declaration))
          }) {
        val bindings =
            variables.bindings(gaining, proposed.gaining, variable) +
                variables.bindings(removing, proposed.removing, variable)
        if (bindings.distinct().size > 1) {
          throw NarrowingException(
              "Can't set Type variable $variable differently: ${bindings.toSet()}"
          )
        }
      }
    }
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

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? Per ?: throw NarrowingException("$proposed does not preserve metric $metric")
      if (proposed.metric != metric) {
        throw NarrowingException("can't change the metric")
      }
      proposed.inner.ensureNarrows(inner, info)
    }

    override fun toString(): String = "$inner / ${groupPartIfNeeded(metric)}"
  }

  /** Carries out [inner] as the concrete [actor], independently of who narrows the task. */
  public data class By(val inner: Instruction, val actor: Expression) : Instruction() {
    public companion object {
      /** Creates a performer override. */
      public fun create(inner: Instruction, actor: Expression): Instruction = By(inner, actor)

      /** Creates a performer override, distributing it over independent instructions. */
      public fun createTree(inner: InstructionTree, actor: Expression): InstructionTree =
          when (inner) {
            is InstructionGroup -> InstructionGroup(inner.instructions.map { By(it, actor) })
            is Instruction -> By(inner, actor)
          }
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, actor)

    override fun scale(factor: Int): Instruction = create(inner * factor, actor)

    override fun isAbstract(info: TypeInfo): Boolean =
        inner.isAbstract(info) || actor.isAbstract(info)

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? By ?: throw NarrowingException("$proposed does not preserve performer $actor")
      if (proposed.actor != actor) throw NarrowingException("can't change performer $actor")
      proposed.inner.ensureNarrows(inner, info)
    }

    override fun toString(): String = "${groupPartIfNeeded(inner)} BY $actor"

    override fun precedence(): Int = 9
  }

  @ConsistentCopyVisibility
  public data class Gated internal constructor(val gate: Requirement, val inner: InstructionTree) :
      Instruction() {
    public companion object {
      public fun create(gate: Requirement?, inner: Instruction): Instruction =
          if (gate == null) inner else Gated(gate, inner)

      public fun createTree(gate: Requirement?, inner: InstructionTree): InstructionTree =
          if (gate == null) inner else Gated(gate, inner)
    }

    init {
      if (inner is Gated) throw PetSyntaxException("You don't gate a gater")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(gate, inner)

    override fun scale(factor: Int): Instruction = copy(inner = inner * factor)

    override fun isAbstract(info: TypeInfo): Boolean = inner.isAbstract(info)

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? Gated ?: throw NarrowingException("$proposed does not preserve condition $gate")
      if (proposed.gate != gate) {
        throw NarrowingException("can't change the condition")
      }
      proposed.inner.ensureNarrows(inner, info)
    }

    override fun toString(): String = "${groupPartIfNeeded(gate)}: ${groupPartIfNeeded(inner)}"

    override fun precedence(): Int = 4
  }

  @ConsistentCopyVisibility
  public data class Then
  internal constructor(
      /** Every stage except the final continuation. */
      val stages: List<Instruction>,
      val continuation: InstructionTree,
  ) : Instruction() {
    /** This sequence's stages in source order. */
    public val instructions: List<InstructionTree> = stages + continuation

    public val first: Instruction
      get() = stages.first()

    init {
      require(stages.isNotEmpty())
      if (continuation is Then) {
        throw PetSyntaxException("Nested THEN continuations must be flattened")
      }
      // Every left operand must remain one task and cannot itself contain an enqueue sequence.
      if (
          stages.any {
            it.descendantsOfType<InstructionGroup>().any() || it.descendantsOfType<Then>().any()
          }
      ) {
        throw PetSyntaxException("THEN left operands cannot contain groups or other THENs")
      }
    }

    override fun scale(factor: Int): Instruction =
        withParts(stages.map { it * factor }, continuation * factor)

    override fun visitChildren(visitor: Visitor) {
      visitor.visit(stages)
      visitor.visit(continuation)
    }

    /** Replaces stages while preserving the authored Type variables carried by this `THEN`. */
    public fun withInstructions(instructions: List<InstructionTree>): Then {
      val replacement = createTree(instructions) as? Then ?: error("THEN requires two stages")
      return replacement.withTypeVariables(typeVariables)
    }

    /** Replaces the sequence parts while preserving this `THEN`'s Type variables. */
    internal fun withParts(stages: List<Instruction>, continuation: InstructionTree): Then =
        Then(stages, continuation).withTypeVariables(typeVariables)

    override fun precedence(): Int = 2

    override fun isAbstract(info: TypeInfo): Boolean = instructions.any { it.isAbstract(info) }

    private val hasSharedX: Lazy<Boolean> = lazy {
      instructions.count { it.descendantsOfType<XScalar>().isNotEmpty() } >= 2
    }

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? Then ?: throw NarrowingException("Can't narrow $this to $proposed")
      if (instructions.size != proposed.instructions.size) {
        throw NarrowingException("Can't change the number of THEN stages")
      }
      val specialized = bindTypeVariablesFrom(proposed, info)
      for ((wide, narrow) in specialized.instructions.zip(proposed.instructions)) {
        narrow.ensureNarrows(wide, info)
      }
      if (hasSharedX()) sharedXValue(this, proposed)
    }

    private fun bindTypeVariablesFrom(
        proposed: Then,
        info: TypeInfo,
        fallback: PetTransformer? = null,
    ): Then {
      var specialized = this
      val variables = typeVariablesFor(info)
      for (variable in
          variables.variables.filter {
            info.isAbstract(variables.expressionOf(it.declaration))
          }) {
        val declaration = variables.expressionOf(variable.declaration)
        val bindings =
            variables
                .bindings(this, proposed, variable)
                .filter {
                  it != declaration && narrowsExpression(it, declaration, info)
                }
                .distinct()
        if (bindings.size > 1) {
          throw NarrowingException("Can't bind Type variable $variable differently: $bindings")
        }
        val binding =
            bindings.singleOrNull()
                ?: fallback
                    ?.takeIf { bindings.isEmpty() }
                    ?.transformExpression(declaration)
                    ?.takeIf { it != declaration }
        binding?.let {
          val captured = variable.bound.classTable.resolve(binding.uncomplemented())
          val transformed =
              variables.bind(mapOf(variable to captured)).transformInstruction(specialized)
          specialized =
              transformed as? Then ?: error("expression replacement changed THEN into $transformed")
        }
      }
      return specialized
    }

    private fun narrowsExpression(
        narrow: Expression,
        wide: Expression,
        info: TypeInfo,
    ): Boolean = narrow.narrows(wide, info)

    /** Narrows the first stage and carries every shared choice into later stages. */
    public fun bindFirstStage(
        proposed: Instruction,
        info: TypeInfo,
        loweredBinding: PetTransformer? = null,
    ): Then = replaceFirstStage(proposed, info, loweredBinding, requireBinding = true)

    /** Selects and narrows the first stage, including when no cross-stage type is specialized. */
    public fun selectFirstStage(
        proposed: Instruction,
        info: TypeInfo,
        loweredBinding: PetTransformer? = null,
    ): Then = replaceFirstStage(proposed, info, loweredBinding, requireBinding = false)

    private fun replaceFirstStage(
        proposed: Instruction,
        info: TypeInfo,
        loweredBinding: PetTransformer?,
        requireBinding: Boolean,
    ): Then {
      proposed.ensureNarrows(first, info)
      val partial = withParts(listOf(proposed) + stages.drop(1), continuation)
      val variables = typeVariablesFor(info)
      val authoredBinding =
          PetTransformer.chain(
              variables.variables.mapNotNull { variable ->
                val declaration = variables.expressionOf(variable.declaration)
                if (
                    loweredBinding != null &&
                        loweredBinding.transformExpression(declaration) != declaration
                ) {
                  return@mapNotNull null
                }
                val positionalBindings =
                    variables
                        .bindings(first, proposed, variable)
                        .filter { it != declaration && narrowsExpression(it, declaration, info) }
                        .map { variable.bound.classTable.resolve(it.uncomplemented()) }
                        .distinct()
                val bindings = positionalBindings.ifEmpty {
                  variables.bindingsIn(proposed, variable, info)
                }
                if (bindings.size > 1) {
                  throw NarrowingException(
                      "Can't bind Type variable $variable differently: ${bindings.toSet()}"
                  )
                }
                bindings.singleOrNull()?.let { variables.bind(mapOf(variable to it)) }
              }
          )
      val specialized =
          bindTypeVariablesFrom(
              partial,
              info,
              PetTransformer.chain(loweredBinding, authoredBinding),
          )
      val selectedX = if (hasSharedX()) sharedXValue(first, proposed) else null
      val fullySpecialized =
          selectedX?.let { bindXTo(it).transformInstruction(specialized) as Then } ?: specialized
      if (requireBinding && fullySpecialized == this) {
        throw NarrowingException("The first stage does not bind this THEN's Type variable")
      }
      return fullySpecialized.withParts(
          listOf(proposed) + fullySpecialized.stages.drop(1),
          fullySpecialized.continuation,
      )
    }

    private fun sharedXValue(wide: PetNode, narrow: PetNode): Int? {
      val wideScalars = wide.descendantsOfType<Scalar>()
      val narrowScalars = narrow.descendantsOfType<Scalar>()
      if (wideScalars.none { it is XScalar }) return null
      if (wideScalars.size != narrowScalars.size) {
        throw NarrowingException("Can't match X occurrences in $narrow")
      }
      val xValues =
          wideScalars.zip(narrowScalars).mapNotNull { (wideScalar, narrowScalar) ->
            if (wideScalar !is XScalar) return@mapNotNull null
            narrowScalar as? ActualScalar
                ?: throw NarrowingException("Can't bind X occurrence in $narrow")
            if (narrowScalar.value % wideScalar.multiple != 0) {
              throw NarrowingException(
                  "${narrowScalar.value} isn't a multiple of ${wideScalar.multiple}"
              )
            }
            narrowScalar.value / wideScalar.multiple
          }
      if (xValues.distinct().size > 1) {
        throw NarrowingException("Can't set different values for X: ${xValues.toSet()}")
      }
      return xValues.singleOrNull()
    }

    internal fun keepTogether(isAbstract: ((Expression) -> Boolean)?) =
        hasSharedX() ||
            isAbstract?.let { check ->
              typeVariables.variables.any { variable ->
                check(typeVariables.expressionOf(variable.declaration))
              }
            } == true

    /** Returns the right-associated continuation enqueued after the first stage. */
    public fun continuationAfterFirst(): InstructionGroup =
        InstructionGroup.of(createTree(stages.drop(1) + continuation))

    override fun toString(): String = instructions.joinToString(" THEN ") { groupPartIfNeeded(it) }

    public companion object {
      public fun create(it: List<Instruction>): Instruction = createTree(it) as Instruction

      /** Returns a canonical sequence, collapsing empty and singleton inputs. */
      public fun createTree(it: List<InstructionTree>): InstructionTree =
          it.let { sourceParts ->
                val final = sourceParts.lastOrNull()
                if (final is Then) sourceParts.dropLast(1) + final.instructions else sourceParts
              }
              .let { stages ->
                when (stages.size) {
                  0 -> NoOp
                  1 -> stages.first()
                  else -> {
                    val leading =
                        stages.dropLast(1).map { stage ->
                          stage as? Instruction ?: throw PetSyntaxException("Bad THEN")
                        }
                    Then(leading, stages.last())
                  }
                }
              }
    }
  }

  @ConsistentCopyVisibility
  public data class Or internal constructor(val instructions: List<InstructionTree>) :
      Instruction() {
    init {
      require(instructions.size >= 2)
      if (instructions.distinct().size != instructions.size) {
        throw PetSyntaxException("duplicates")
      }
    }

    override fun scale(factor: Int): Instruction =
        createTree(instructions.map { it * factor }) as Instruction

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(instructions)

    override fun safeToNestIn(container: PetNode): Boolean =
        super.safeToNestIn(container) && container !is Then

    override fun precedence(): Int = 6

    override fun isAbstract(info: TypeInfo): Boolean = true

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      if (proposed is Or) {
        proposed.instructions.forEach { ensureIsNarrowedBy(it, info) }
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

    override fun toString(): String = instructions.joinToString(" OR ") { groupPartIfNeeded(it) }

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

      /** Creates an OR while preserving any grouped options produced by preprocessing. */
      public fun createTree(instructions: Collection<InstructionTree>): InstructionTree {
        require(instructions.any())
        val set = instructions.toSet()
        return if (set.size == 1) {
          set.first()
        } else {
          Or(set.toList())
        }
      }

      private fun create(first: InstructionTree, vararg rest: InstructionTree): InstructionTree =
          createTree(listOf(first) + rest)
    }
  }

  public data class Transform(
      val instruction: InstructionTree,
      override val transformKind: String,
  ) : Instruction(), TransformNode<InstructionTree> {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(instruction)

    override fun scale(factor: Int): Instruction = copy(instruction = instruction * factor)

    override fun isAbstract(info: TypeInfo): Boolean =
        error("should have been transformed by now: $this")

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo): Unit =
        error("should have been transformed by now: $this")

    override fun toString(): String = "$transformKind[$instruction]"

    override fun extract(): InstructionTree = instruction
  }

  public enum class Intensity(public val symbol: String, public val abstract: Boolean = false) :
      Specification<Intensity> {
    /** The full amount must be gained/removed/transmuted. */
    MANDATORY("!"),

    /** Do "as much as possible" of the amount. */
    AMAP("."),

    /** The player can choose how much of the amount to do, including none of it. */
    OPTIONAL("?", true),
    ;

    override fun isAbstract(info: TypeInfo): Boolean = abstract

    override fun ensureNarrows(that: Intensity, info: TypeInfo) {
      if (that != this && that != OPTIONAL) {
        throw NarrowingException("")
      }
    }

    private companion object {
      private fun from(symbol: String) = entries.first { it.symbol == symbol }
    }
  }

  private object Parsers : PetTokenizer() {
    internal fun parser(): Parser<InstructionTree> {
      return parser {
        val gain: Parser<Instruction> =
            ScaledExpression.parser() and
                optional(intensity) map
                { (ste, int) ->
                  Gain.gain(ste, int)
                }

        val remove: Parser<Instruction> =
            skipChar('-') and
                ScaledExpression.parser() and
                optional(intensity) map
                { (ste, int) ->
                  Remove.remove(ste, int)
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
                optional(skipChar('/') and Metric.subtractionParser()) map
                { (instr, metric) ->
                  if (metric == null) instr else Per(instr, metric)
                }

        val transform: Parser<Transform> =
            transform(parser()) map { (node, tname) -> Transform(node, tname) }

        val maybeTransform: Parser<InstructionTree> = transform or maybePer

        val atomBase: Parser<InstructionTree> = maybeTransform or group(parser())

        val atom: Parser<InstructionTree> =
            atomBase and
                optional(skip(_by) and Expression.parser()) map
                { (instruction, actor) ->
                  if (actor == null) instruction else By.createTree(instruction, actor)
                }

        val orInstr: Parser<InstructionTree> =
            separatedTerms(atom, _or) map
                {
                  val set = it.toSetStrict().toList()
                  Or.createTree(set)
                }

        val gated: Parser<InstructionTree> =
            optional(Requirement.atomParser() and skipChar(':')) and
                orInstr map
                { (gate, ins) ->
                  Gated.createTree(gate, ins)
                }

        val then = separatedTerms(gated, _then) map { Then.createTree(it) }

        commaSeparated(then) map { InstructionGroup.createTree(it) }
      }
    }
  }
}
