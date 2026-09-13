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
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.OK
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.util.invoke
import dev.martianzoo.pets.util.toSetStrict

/**
 * A relation between a before-state and an after-state, as defined by
 * [section 6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)
 * — the only kind of element that denotes one. Instructions appear as the right-hand side of
 * [Action]s and [Effect]s, on map areas, in the "do this now" section of cards, in an engine's task
 * queues, and so forth.
 *
 * Bringing an after-state about is a separate job: what an instruction denotes is stated here,
 * while scheduling, choice presentation and attribution belong to the engine.
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

  /**
   * The instruction relating a state to itself, spelled `Ok` ([rule
   * L6-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   * It vanishes from a group rather than appearing as an empty member, and a group left with
   * nothing in it *is* `Ok`. `Ok` narrows an optional change and nothing else ([rule
   * L7-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   */
  public object NoOp : Instruction() {
    override fun scale(factor: Int): Instruction = this

    override fun isAbstract(info: TypeInfo): Boolean = false

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      if (proposed != NoOp) throw NarrowingException("not Ok")
    }

    override fun visitChildren(visitor: Visitor): Unit = Unit

    override fun toString(): String = "Ok"
  }

  /**
   * One of the three elementary instructions of
   * [rule L6-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions):
   * a [Gain], a [Remove] or a [Transmute]. Each says that the after-state holds some number more,
   * fewer, or differently-typed components than the before-state.
   */
  public sealed class Change : Instruction() {
    public companion object {
      /** Creates and canonicalizes a gain, removal, or transmutation instruction. */
      public fun change(
          gaining: Expression? = null,
          removing: Expression? = null,
          count: Int = 1,
          quantifier: Quantifier? = MANDATORY,
      ): Instruction {
        require(count >= 0)
        return when {
          count == 0 -> NoOp
          removing == null -> Gain.gain(gaining!!, count, quantifier)
          gaining == null -> Remove.remove(removing, count, quantifier)
          else -> Transmute(Full(gaining, removing), ActualScalar(count), quantifier)
        }
      }
    }

    /**
     * How many components change: a positive integer or an `X` standing for an amount left open
     * ([rule
     * L6-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
     * Zero is rejected.
     */
    public abstract val count: Scalar

    /** What the after-state holds more of, or null for a pure removal. */
    public abstract val gaining: Expression?

    /** What the after-state holds fewer of, or null for a pure gain. */
    public abstract val removing: Expression?

    /**
     * How much of [count] must happen ([rule
     * L6-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)),
     * or null in authored Pets that has not yet been elaborated — elaboration supplies the class's
     * default ([rule
     * T10-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults)).
     */
    public abstract val quantifier: Quantifier?

    override fun isAbstract(info: TypeInfo): Boolean {
      return count.isAbstract(info) ||
          quantifier?.isAbstract(info) != false ||
          (gaining?.isAbstract(info) == true) ||
          (removing?.isAbstract(info) == true)
    }

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      if (proposed == NoOp && quantifier == OPTIONAL) return
      proposed as? Change ?: throw NarrowingException("$this  /  $proposed")
      proposed.quantifier!!.ensureNarrows(quantifier!!, info)
      val proposedCount = proposed.count
      val authoredCount = count
      if (
          quantifier == OPTIONAL && proposedCount is ActualScalar && authoredCount is ActualScalar
      ) {
        if (proposedCount.value > authoredCount.value) throw NarrowingException("")
      } else {
        proposedCount.ensureNarrows(authoredCount, info)
      }
      gaining?.let { proposed.gaining!!.ensureNarrows(it, info) }
      removing?.let { proposed.removing!!.ensureNarrows(it, info) }
    }
  }

  /**
   * Says the after-state holds [scaledEx]'s count more of its expression — the `n Foo` form of
   * [rule L6-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions).
   */
  public data class Gain
  public constructor(
      val scaledEx: ScaledExpression,
      override val quantifier: Quantifier?,
  ) : Change() {
    public companion object {
      /** Creates and canonicalizes a gain of one copy of [expression]. */
      public fun gain(expression: HasExpression): Instruction = gain(expression, 1)

      /** Creates a gain of [scaledEx], or [NoOp] when its expression is `Ok`. */
      public fun gain(
          scaledEx: ScaledExpression,
          quantifier: Quantifier? = MANDATORY,
      ): Instruction =
          if (scaledEx.expression == OK.expression) NoOp else Gain(scaledEx, quantifier)

      /** Creates and canonicalizes a gain of [count] copies of [expression]. */
      public fun gain(
          expression: HasExpression,
          count: Int = 1,
          quantifier: Quantifier? = MANDATORY,
      ): Instruction = gain(scaledEx(expression, count), quantifier)
    }

    override val count: Scalar = scaledEx.scalar
    override val gaining: Expression = scaledEx.expression
    override val removing: Expression? = null

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scaledEx)

    override fun scale(factor: Int): Instruction = copy(scaledEx = scaledEx * factor)

    override fun toString(): String = "$scaledEx${quantifier?.symbol ?: ""}"

    init {
      checkNonzero(count)
    }
  }

  /**
   * Says the after-state holds [scaledEx]'s count fewer of its expression — the `-n Foo` form of
   * [rule L6-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions).
   */
  @ConsistentCopyVisibility
  public data class Remove
  internal constructor(
      val scaledEx: ScaledExpression,
      override val quantifier: Quantifier? = MANDATORY,
  ) : Change() {
    public companion object {
      /** Creates and canonicalizes a removal of one copy of [expression]. */
      public fun remove(expression: HasExpression): Instruction = remove(expression, 1)

      /** Creates and canonicalizes a removal of [scaledEx]. */
      public fun remove(
          scaledEx: ScaledExpression,
          quantifier: Quantifier? = MANDATORY,
      ): Instruction = Remove(scaledEx, quantifier)

      /** Creates and canonicalizes a removal of [count] copies of [expression]. */
      public fun remove(
          expression: HasExpression,
          count: Int = 1,
          quantifier: Quantifier? = MANDATORY,
      ): Instruction = remove(scaledEx(expression, count), quantifier)
    }

    override val count: Scalar = scaledEx.scalar
    override val gaining: Expression? = null
    override val removing: Expression = scaledEx.expression

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scaledEx)

    override fun scale(factor: Int): Instruction = copy(scaledEx = scaledEx * factor)

    override fun toString(): String = "-$scaledEx${quantifier?.symbol ?: ""}"

    init {
      checkNonzero(count)
    }
  }

  /**
   * Says that [scalar] components of one type have become that many of another — the `n Foo FROM
   * Bar` form of
   * [rule L6-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions).
   * See [FromExpression] for the compact spelling available when both sides share a class ([rule
   * L6-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   *
   * Because both sides may repeat one abstract expression, narrowing a transmutation must supply a
   * single consistent value for each shared type variable ([rule
   * L7-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   */
  public data class Transmute(
      val fromEx: FromExpression,
      val scalar: Scalar,
      override val quantifier: Quantifier? = MANDATORY,
  ) : Change() {
    override val count: Scalar = scalar
    override val gaining: Expression = fromEx.toExpression
    override val removing: Expression = fromEx.fromExpression

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(scalar, fromEx)

    override fun scale(factor: Int): Instruction =
        copy(scalar = scalar * factor).withTypeVariables(typeVariables)

    override fun toString(): String {
      val scalText = if (scalar == ActualScalar(1)) "" else "$scalar "
      return "$scalText$fromEx${quantifier?.symbol ?: ""}"
    }

    init {
      checkNonzero(count)
    }

    // A transmutation written in full needs parentheses inside an OR, where its bare FROM would
    // otherwise be ambiguous; rule L6-13 requires rendering to re-insert that grouping.
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
            variables.bindings(gaining, proposed.gaining, variable, region = 0) +
                variables.bindings(removing, proposed.removing, variable, region = 1)
        var commonBinding = variable.bound
        var selectedRoot = variable.bound.rootClass
        var selectedRefinement = variable.bound.refinement
        var selected = false
        for (binding in bindings) {
          val type =
              (info as? GameReader)?.resolve(binding) ?: variable.bound.classTable.resolve(binding)
          if (
              selected && (type.rootClass != selectedRoot || type.refinement != selectedRefinement)
          ) {
            throw NarrowingException(
                "Can't set Type variable $variable differently: ${bindings.toSet()}"
            )
          }
          selectedRoot = type.rootClass
          selectedRefinement = type.refinement
          selected = true
          commonBinding =
              (commonBinding glb type)
                  ?: throw NarrowingException(
                      "Can't set Type variable $variable differently: ${bindings.toSet()}"
                  )
        }
      }
    }
  }

  /**
   * Scales [inner] by the value of [metric] — `Titanium / 3 EarthTag` grants one titanium per three
   * complete Earth tags ([rule
   * L6-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   * Only an elementary [Change] may be scaled this way. The metric is not a choice: a proposal must
   * reproduce it exactly ([rule
   * L7-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   */
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

  /**
   * Carries out [inner] as the concrete [actor], independently of who narrows the task ([rule
   * L6-11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   * Like a gate or a metric, the actor is not a choice: a proposal must reproduce it exactly ([rule
   * L7-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   * Attribution itself is `IDENTITY.md`'s subject.
   */
  public data class By(val inner: Instruction, val actor: Expression) : Instruction() {
    public companion object {
      /** Creates a performer override. */
      public fun create(inner: Instruction, actor: Expression): Instruction = By(inner, actor)

      /**
       * Creates a performer override, distributing it over independent instructions as
       * [rule L6-11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)
       * requires: `(A, B) BY Player1` is `A BY Player1, B BY Player1`.
       */
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

  /**
   * Makes [inner] available only while [gate] holds ([rule
   * L6-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   * The gate is not a choice: it decides whether the result is available at all, and a proposal
   * must reproduce it exactly ([rule
   * L7-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   * `OR` binds more tightly than a gate, so `3 PlantTag: Plant OR 4 Plant` gates both alternatives;
   * a gate does not directly contain another gate.
   */
  @ConsistentCopyVisibility
  public data class Gated internal constructor(val gate: Requirement, val inner: InstructionTree) :
      Instruction() {
    public companion object {
      /** Returns [inner] gated on [gate], or just [inner] when [gate] is null. */
      public fun create(gate: Requirement?, inner: Instruction): Instruction =
          if (gate == null) inner else Gated(gate, inner)

      /** Returns [inner] gated on [gate], or just [inner] when [gate] is null. */
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

  /**
   * Fans [body] out over the components matching [selector] in one World snapshot, producing one
   * independent branch per distinct concrete Type present ([rule
   * L6-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   * In a branch, the authored [selector] expression denotes that concrete Type, and when the
   * selector is an `Owner`, so does the contextual `Owner`, so an ordinary owned body reads exactly
   * as it does on a card.
   *
   * A selector refinement chooses which components take part, without becoming part of the name the
   * body uses (see [selectorName]). A gate in [body] behaves like any other gate and fails when its
   * requirement is unmet. Class properties in [body] are evaluated separately after each branch has
   * bound its selection. The body may not be empty and fanouts do not nest.
   *
   * The selector is not a choice: a proposal must reproduce it exactly ([rule
   * L7-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   * How the live world is enumerated, and when, is `EACH.md`'s subject.
   */
  public data class Each(val selector: Expression, val body: InstructionTree) : Instruction() {
    init {
      if (body == NoOp) throw PetSyntaxException("EACH needs a body")
      // Nesting would make `Owner` and each selector name ambiguous between two fanouts, and no
      // rule needs it. Banning it keeps one selection in scope at a time.
      if (body.descendantsOfType<Each>().any()) {
        throw PetSyntaxException("EACH can't contain another EACH")
      }
    }

    /**
     * The authored expression a body occurrence must equal in order to denote the selected
     * component: [selector] without its refinement, since
     * [rule L6-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)
     * keeps that filter out of the name the body uses.
     */
    public val selectorName: Expression = selector.copy(refinement = null)

    /** The Class name represented by a `Class<T>` selector, when this is a Class fanout. */
    public val representedSelectorName: Expression? =
        selectorName.arguments.singleOrNull()?.takeIf { selectorName.className == CLASS }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(selector, body)

    override fun scale(factor: Int): Instruction = copy(body = body * factor)

    override fun isAbstract(info: TypeInfo): Boolean = body.isAbstract(info)

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? Each ?: throw NarrowingException("$proposed does not preserve `EACH $selector`")
      if (proposed.selector != selector) {
        throw NarrowingException("can't change the EACH selector")
      }
      proposed.body.ensureNarrows(body, info)
    }

    override fun toString(): String = "EACH $selector { $body }"
  }

  /**
   * Says that each stage happens before the next ([rule
   * L6-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   * The relation is stated between the changes themselves and is right-associative, so `A THEN B
   * THEN C` is one sequence of three stages rather than nested pairs. Every stage before the last
   * must be a single instruction: a group or another sequence on the left is rejected, because
   * "before" needs one identifiable change to be before.
   *
   * A sequence is also the one place independent instructions may share an `X` ([rule
   * L6-14](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)),
   * and narrowing must then give every occurrence one consistent value ([rule
   * L7-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   * What waiting means for pending work is `SEQUENCING.md`'s subject.
   */
  @ConsistentCopyVisibility
  public data class Then
  internal constructor(
      /** Every stage except the final continuation. */
      val stages: List<Instruction>,

      /** The last stage, the only one that may itself be a group. */
      val continuation: InstructionTree,
  ) : Instruction() {
    /** This sequence's stages in source order. */
    public val instructions: List<InstructionTree> = stages + continuation

    /** The stage that happens first. */
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
      if (hasSharedX()) sharedXValue(this, proposed, info)
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
          val captured =
              ((info as? GameReader)?.resolve(binding)
                      ?: variable.bound.classTable.resolve(binding))
                  .groundType
          val transformed =
              variables
                  .bind(
                      mapOf(variable to captured),
                      (info as? GameReader)?.classTable ?: captured.classTable,
                  )
                  .transformInstruction(specialized)
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
      val firstStage = first
      val selectableFirst =
          if (firstStage is Gated) {
            firstStage.inner as Instruction
          } else {
            firstStage
          }
      proposed.ensureNarrows(selectableFirst, info)
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
                        .bindings(selectableFirst, proposed, variable)
                        .filter { it != declaration && narrowsExpression(it, declaration, info) }
                        .map { expression ->
                          ((info as? GameReader)?.resolve(expression)
                                  ?: variable.bound.classTable.resolve(expression))
                              .groundType
                        }
                        .distinct()
                val bindings = positionalBindings.ifEmpty {
                  variables.bindingsIn(proposed, variable, info)
                }
                if (bindings.size > 1) {
                  throw NarrowingException(
                      "Can't bind Type variable $variable differently: ${bindings.toSet()}"
                  )
                }
                bindings.singleOrNull()?.let { binding ->
                  variables.bind(
                      mapOf(variable to binding),
                      (info as? GameReader)?.classTable ?: binding.classTable,
                  )
                }
              }
          )
      val selectionBinding = PetTransformer.chain(loweredBinding, authoredBinding)
      val selectedFirstStage = selectionBinding.transformInstruction(firstStage)
      if (selectedFirstStage is Gated && !info.has(selectedFirstStage.gate)) {
        throw NarrowingException("Condition is not met: ${selectedFirstStage.gate}")
      }
      val specialized =
          bindTypeVariablesFrom(
              partial,
              info,
              selectionBinding,
          )
      val selectedX = if (hasSharedX()) sharedXValue(first, proposed, info) else null
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

    private fun sharedXValue(
        wide: InstructionTree,
        narrow: InstructionTree,
        info: TypeInfo,
    ): Int? {
      val unbound = setOf<Int?>(null)

      fun merge(left: Set<Int?>, right: Set<Int?>): Set<Int?> = buildSet {
        for (leftValue in left) {
          for (rightValue in right) {
            when {
              leftValue == null -> add(rightValue)
              rightValue == null || leftValue == rightValue -> add(leftValue)
            }
          }
        }
      }

      fun bindings(wideNode: PetNode, narrowNode: PetNode): Set<Int?> {
        if (wideNode.descendantsOfType<XScalar>().isEmpty()) return unbound
        if (wideNode is Or) {
          fun bindingsForArm(narrowArm: InstructionTree): Set<Int?> {
            return wideNode.instructions
                .asSequence()
                .filter { narrowArm.narrows(it, info) }
                .flatMap { wideArm -> bindings(wideArm, narrowArm) }
                .toSet()
          }
          val narrowArms =
              if (narrowNode is Or) {
                narrowNode.instructions
              } else {
                listOf(narrowNode as? InstructionTree ?: return emptySet())
              }
          return narrowArms.fold(unbound) { result, narrowArm ->
            merge(result, bindingsForArm(narrowArm))
          }
        }
        if (wideNode is XScalar) {
          val narrowScalar = narrowNode as? ActualScalar ?: return emptySet()
          if (narrowScalar.value % wideNode.multiple != 0) return emptySet()
          return setOf(narrowScalar.value / wideNode.multiple)
        }
        if (narrowNode == NoOp) return unbound

        val wideChildren = wideNode.immediateChildren()
        val narrowChildren = narrowNode.immediateChildren()
        if (wideChildren.size != narrowChildren.size) return emptySet()
        return wideChildren.zip(narrowChildren).fold(unbound) { result, (wideChild, narrowChild) ->
          merge(result, bindings(wideChild, narrowChild))
        }
      }

      val xValues = bindings(wide, narrow)
      if (xValues.isEmpty()) throw NarrowingException("Can't match X occurrences in $narrow")
      val concreteValues = xValues.filterNotNull()
      if (concreteValues.size > 1) {
        throw NarrowingException("Can't set different values for X: $concreteValues")
      }
      return concreteValues.singleOrNull()
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
      /** Returns a canonical sequence of [it], which must not collapse to a group. */
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

  /**
   * Offers a choice among [instructions] ([rule
   * L6-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   * The parser rejects duplicate authored alternatives. Construction and rewriting collapse arms
   * that have become equal; a single remaining outcome is returned without an `Or` wrapper.
   *
   * An `OR` that remains is always abstract ([rule
   * L7-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   * It is also one of only two exceptions to a narrowing preserving node shape — the other being
   * [NoOp] narrowing an optional change: any instruction narrows an `OR` by narrowing one arm,
   * while a proposed `OR` narrows it only when every arm does ([rules L7-2 and
   * L7-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   */
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
      /**
       * Returns a choice among [instructions]. A single alternative is returned as itself rather
       * than as an `Or`.
       *
       * This collapses duplicate alternatives rather than rejecting them; it is the parser that
       * enforces
       * [rule L6-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)'s
       * rejection of a duplicate an author actually wrote.
       */
      public fun create(instructions: Collection<Instruction>): Instruction {
        require(instructions.any())
        val set = instructions.toSet()
        return if (set.size == 1) {
          set.first()
        } else {
          Or(set.toList())
        }
      }

      /**
       * Creates an OR while preserving any grouped options produced by preprocessing. Like
       * [create], this collapses duplicate alternatives rather than applying
       * [rule L6-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)'s
       * rejection.
       */
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

  /**
   * An [instruction] marked for rewriting by the handler named by [transformKind] ([rule
   * L10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)).
   * A handler rewrites only inside its own block and must return the same kind of Pets; a block
   * expanding into several independent instructions splices into the surrounding group ([rule
   * L10-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)).
   * Every other operation here rejects a transform that survived to it.
   */
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

  /**
   * How much of a [Change]'s count must actually happen ([rule
   * L6-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
   * Only [OPTIONAL] leaves anything open; [MANDATORY] and [AMAP] are incompatible with each other,
   * so neither narrows to the other ([rule
   * L7-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
   */
  public enum class Quantifier(public val symbol: String, public val abstract: Boolean = false) :
      Specification<Quantifier> {
    /** The full amount must be gained/removed/transmuted. */
    MANDATORY("!"),

    /**
     * Do "as much as possible" of the amount. What that resolves to against a real state is
     * `QUANTIFIERS.md`'s subject.
     */
    AMAP("."),

    /** The player can choose how much of the amount to do, including none of it. */
    OPTIONAL("?", true),
    ;

    override fun isAbstract(info: TypeInfo): Boolean = abstract

    override fun ensureNarrows(that: Quantifier, info: TypeInfo) {
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
                optional(quantifier) map
                { (ste, int) ->
                  Gain.gain(ste, int)
                }

        val remove: Parser<Instruction> =
            skipChar('-') and
                ScaledExpression.parser() and
                optional(quantifier) map
                { (ste, int) ->
                  Remove.remove(ste, int)
                }

        val transmute: Parser<Transmute> =
            optional(ScaledExpression.scalar()) and
                FromExpression.parser() and
                optional(quantifier) map
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

        val each: Parser<Instruction> =
            skip(_each) and
                Expression.parser(allowDerivedClass = false) and
                skipChar('{') and
                parser() and
                skipChar('}') map
                { (selector, body) ->
                  Each(selector, body)
                }

        val atomBase: Parser<InstructionTree> = each or maybeTransform or group(parser())

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
