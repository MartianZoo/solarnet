package dev.martianzoo.pets.ast

import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.OK
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.Companion.checkNonzero
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.types.GroundType
import dev.martianzoo.pets.types.TypeVariable
import dev.martianzoo.pets.util.invoke

/**
 * A transition between a before-state and an after-state, including gain and removal events, as
 * defined by
 * [section 2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)
 * — the only kind of element that denotes one. Instructions appear as the right-hand side of
 * [Action]s and [Effect]s, on map areas, in the "do this now" section of cards, in an engine's task
 * queues, and so forth.
 *
 * Bringing an after-state about is a separate job: what an instruction denotes is stated here,
 * while scheduling, choice presentation and attribution belong to the engine.
 */
public sealed class Instruction : InstructionTree() {
  /**
   * Returns an instruction that (in essence) does this instruction [factor] times. The [factor]
   * must be non-negative, and if zero, [NoOp] is returned.
   */
  final override operator fun times(factor: Int): Instruction {
    if (factor == 0) return NoOp
    require(factor > 0)
    if (factor == 1) return this
    return scale(factor).also { it.sourceLocation = sourceLocation }
  }

  override val kind: kotlin.reflect.KClass<out PetNode> = Instruction::class

  protected abstract fun scale(factor: Int): Instruction

  /**
   * The instruction leaving a state unchanged without events, spelled `Ok` ([rule
   * L2-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   * It vanishes from a group rather than appearing as an empty member, and a group left with
   * nothing in it *is* `Ok`. `Ok` narrows an optional change and nothing else ([rule
   * L3-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
   */
  public object NoOp : Instruction() {
    override fun scale(factor: Int): Instruction = this

    override fun isAbstract(info: TypeInfo): Boolean = false

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      if (proposed != NoOp) throw NarrowingException("`$proposed` does not narrow `Ok`")
    }

    override fun visitChildren(visitor: Visitor): Unit = Unit

    override fun toString(): String = "Ok"
  }

  /**
   * One of the three elementary instructions of
   * [rule L2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions):
   * a [Gain], a [Remove] or a [Transmute]. Each says that the after-state holds some number more,
   * fewer, or differently typed components relative to the before-state.
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
     * L2-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
     * Zero is rejected.
     */
    public abstract val count: Scalar

    /**
     * The gained Type, or null for a pure removal. A transmutation must change its concrete Type.
     */
    public abstract val gaining: Expression?

    /** The removed Type, or null for a pure gain. A transmutation must change its concrete Type. */
    public abstract val removing: Expression?

    /**
     * How much of [count] must happen ([rule
     * L2-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)),
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
      ensureChangeIsNarrowedBy(this, proposed, info)
    }

    protected fun ensureChangeIsNarrowedBy(
        authored: Change,
        proposed: InstructionTree,
        info: TypeInfo,
    ) {
      val quantifier = authored.quantifier
      if (proposed == NoOp && quantifier == OPTIONAL) return
      proposed as? Change
          ?: throw NarrowingException("expected a change narrowing of `$this`, found `$proposed`")
      proposed.quantifier!!.ensureNarrows(quantifier!!, info)
      authored.ensureCountIsNarrowedBy(proposed.count, info)
      authored.gaining?.let { info.ensureSelectionNarrows(it, proposed.gaining!!) }
      authored.removing?.let { info.ensureSelectionNarrows(it, proposed.removing!!) }
    }

    internal fun ensureCountIsNarrowedBy(proposedCount: Scalar, info: TypeInfo) {
      val authoredCount = count
      if (
          quantifier == OPTIONAL && proposedCount is ActualScalar && authoredCount is ActualScalar
      ) {
        if (proposedCount.value > authoredCount.value) {
          throw NarrowingException(
              "change count `${proposedCount.value}` exceeds optional maximum " +
                  "`${authoredCount.value}`"
          )
        }
      } else {
        proposedCount.ensureNarrows(authoredCount, info)
      }
    }
  }

  /**
   * Says the after-state holds [scaledEx]'s count more of its expression — the `n Foo` form of
   * [rule L2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions).
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
   * [rule L2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions).
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
   * [rule L2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions).
   * See [FromExpression] for the compact spelling available when both sides share a class ([rule
   * L2-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   *
   * Either side may explicitly name a choice used by the other. Narrowing must supply a single
   * consistent value for each such type variable ([rule
   * L3-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
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

    override fun scale(factor: Int): Instruction = copy(scalar = scalar * factor)

    override fun toString(): String {
      val scalText = if (scalar == ActualScalar(1)) "" else "$scalar "
      return "$scalText$fromEx${quantifier?.symbol ?: ""}"
    }

    init {
      checkNonzero(count)
    }

    override fun precedence(): Int = if (fromEx is Full) 7 else 10

    public companion object {
      /** Resolves shared named variables across both sides of a constructed transmutation. */
      public fun resolveTypeVariableNames(transmute: Transmute): Transmute {
        return dev.martianzoo.pets.ast.resolveTypeVariableNames(
            transmute,
            transmute.localTypeVariableDeclarations(),
        )
      }
    }

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      if (proposed == NoOp) {
        ensureChangeIsNarrowedBy(this, proposed, info)
        return
      }
      proposed as? Transmute
          ?: throw NarrowingException(
              "expected a transmutation narrowing of `$this`, found `$proposed`"
          )
      val proposedGain = info.classTable.resolve(proposed.gaining)
      if (!proposedGain.abstract && proposedGain == info.classTable.resolve(proposed.removing)) {
        throw NarrowingException("a transmutation must change its type: $proposed")
      }
      (fromEx as? Compact)?.ensureRetainedArgumentsAgree(
          proposed.gaining,
          proposed.removing,
          info,
      )
      val variables = typeVariablesFor(info)
      val selected = mutableMapOf<TypeVariable, GroundType>()
      for (variable in
          variables.variables.filter {
            info.isAbstract(variables.expressionOf(it.declaration))
          }) {
        val classTable = info.classTable
        val bindings =
            variables.bindings(gaining, proposed.gaining, variable, info, classTable) +
                variables.bindings(removing, proposed.removing, variable, info, classTable)
        val declaration = variables.expressionOf(variable.declaration).copy(typeVariableName = null)
        val distinct =
            bindings.filter { it.copy(typeVariableName = null) != declaration }.distinct()
        if (distinct.size > 1) {
          throw NarrowingException(
              "type variable `$variable` has conflicting bindings: `${bindings.toSet()}`"
          )
        }
        distinct.singleOrNull()?.let {
          selected[variable] = classTable.resolve(it).groundType
        }
      }
      val table = info.classTable
      val scoped = copy().withTypeVariables(variables)
      val specialized =
          if (selected.isEmpty()) scoped
          else variables.bind(selected).transformInstruction(scoped) as Transmute
      specialized.typeVariables.ensureChoicesRetained(
          specialized,
          proposed,
          info,
          table,
          specialized.typeVariables.variables.toSet(),
      )
      ensureChangeIsNarrowedBy(specialized, proposed, info)
    }
  }

  /**
   * Scales [inner] by the value of [metric] — `Titanium / 3 EarthTag` grants one titanium per three
   * complete Earth tags ([rule
   * L2-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   * Only an elementary [Change] may be scaled this way. The metric is not a choice: a proposal must
   * reproduce it exactly ([rule
   * L3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
   */
  public data class Per(val inner: Instruction, val metric: Metric) : Instruction() {
    init {
      if (inner !is Change) {
        throw PetSyntaxException("`PER` requires a gain, removal, or transmutation: `$inner`")
      }
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(metric, inner)

    override fun scale(factor: Int): Instruction = copy(inner = inner * factor)

    override fun precedence(): Int = 8

    override fun isAbstract(info: TypeInfo): Boolean = inner.isAbstract(info)

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? Per ?: throw NarrowingException("`$proposed` does not preserve metric `$metric`")
      if (proposed.metric != metric) {
        throw NarrowingException("cannot change metric `$metric` to `${proposed.metric}`")
      }
      proposed.inner.ensureNarrows(inner, info)
    }

    override fun toString(): String = "$inner / ${groupPartIfNeeded(metric)}"
  }

  /**
   * Carries out [inner] as the concrete [actor], independently of who narrows the task ([rule
   * L2-15](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   * Like a gate or a metric, the actor is not a choice: a proposal must reproduce it exactly ([rule
   * L3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
   * Attribution itself is `IDENTITY.md`'s subject.
   */
  public data class By(val inner: Instruction, val actor: Expression) : Instruction() {
    public companion object {
      /** Creates a performer override. */
      public fun create(inner: Instruction, actor: Expression): Instruction = By(inner, actor)

      /**
       * Creates a performer override, distributing it over independent instructions as
       * [rule L2-15](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)
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
      proposed as? By
          ?: throw NarrowingException("`$proposed` does not preserve performer `$actor`")
      if (proposed.actor != actor) {
        throw NarrowingException("cannot change performer `$actor` to `${proposed.actor}`")
      }
      proposed.inner.ensureNarrows(inner, info)
    }

    override fun toString(): String = "${groupPartIfNeeded(inner)} BY $actor"

    override fun precedence(): Int = 9
  }

  /**
   * Makes [inner] available only while [gate] holds ([rule
   * L2-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   * The gate is not a choice: it decides whether the result is available at all, and a proposal
   * must reproduce it exactly ([rule
   * L3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
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
      if (inner is Gated)
          throw PetSyntaxException("a gated instruction cannot contain another gate")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(gate, inner)

    override fun scale(factor: Int): Instruction = copy(inner = inner * factor)

    override fun isAbstract(info: TypeInfo): Boolean = inner.isAbstract(info)

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? Gated
          ?: throw NarrowingException("`$proposed` does not preserve condition `$gate`")
      if (proposed.gate != gate) {
        throw NarrowingException("cannot change condition `$gate` to `${proposed.gate}`")
      }
      proposed.inner.ensureNarrows(inner, info)
    }

    override fun toString(): String = "${groupPartIfNeeded(gate)}: ${groupPartIfNeeded(inner)}"

    override fun precedence(): Int = 4
  }

  /**
   * Fans [body] out over the components matching [selector] in one World snapshot, producing one
   * independent branch per matching component occurrence present ([rule
   * L2-14](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   * In a branch, a marked [selector] exposes that concrete Type to the body. `Me@Player` explicitly
   * rebinds lexical ownership; an unmarked selector preserves the enclosing `Me`.
   *
   * A selector refinement chooses which components take part. An `@` marker exposes the selected
   * component for use in [body]. A gate in [body] behaves like any other gate and fails when its
   * requirement is unmet. Class properties in [body] are evaluated separately after each branch has
   * bound its selection. The body may not be empty and fanouts do not nest.
   *
   * The body need not name the selected component: the selector may serve only as the repetition
   * source. The selector is not a choice: a proposal must reproduce it exactly ([rule
   * L3-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
   * How the live world is enumerated, and when, is `EACH.md`'s subject.
   */
  public data class Each(val selector: Expression, val body: InstructionTree) : Instruction() {
    init {
      if (body == NoOp) throw PetSyntaxException("`EACH` requires a non-`Ok` body")
      // Nesting would make `Anyone` and each selector name ambiguous between two fanouts, and no
      // rule needs it. Banning it keeps one selection in scope at a time.
      if (body.descendantsOfType<Each>().any()) {
        throw PetSyntaxException("`EACH` cannot contain another `EACH`")
      }
    }

    /** Returns this fanout's body with its marked selector occurrences bound to [selected]. */
    public fun bodyFor(selected: Expression): InstructionTree =
        selectorReferenceBinder(selector, selected).transformInstructionTree(body)

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(selector, body)

    override fun scale(factor: Int): Instruction = copy(body = body * factor)

    override fun isAbstract(info: TypeInfo): Boolean = body.isAbstract(info)

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? Each
          ?: throw NarrowingException("`$proposed` does not preserve `EACH $selector`")
      if (proposed.selector != selector) {
        throw NarrowingException(
            "cannot change `EACH` selector `$selector` to `${proposed.selector}`"
        )
      }
      proposed.body.ensureNarrows(body, info)
    }

    override fun toString(): String = "EACH $selector { $body }"
  }

  /**
   * Says that each stage happens before the next ([rule
   * L2-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   * The relation is stated between the changes themselves and is right-associative, so `A THEN B
   * THEN C` is one sequence of three stages rather than nested pairs. Every stage before the last
   * must be a single instruction: a group or another sequence on the left is rejected, because
   * "before" needs one identifiable change to be before.
   *
   * A sequence is also the one place independent instructions may share an `X` ([rule
   * L2-11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)),
   * and narrowing must then give every occurrence one consistent value ([rule
   * L3-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
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
        throw PetSyntaxException("`THEN` continuation cannot contain another `THEN`")
      }
      // Every left operand must remain one task and cannot itself contain an enqueue sequence.
      if (
          stages.any {
            it.descendantsOfType<InstructionGroup>().any() || it.descendantsOfType<Then>().any()
          }
      ) {
        throw PetSyntaxException("`THEN` left operands cannot contain groups or other `THEN`s")
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
      val replacement =
          createTree(instructions) as? Then ?: error("`THEN` requires at least two stages")
      return replacement.withTypeVariables(typeVariables)
    }

    /** Replaces the sequence parts while preserving this `THEN`'s Type variables. */
    internal fun withParts(stages: List<Instruction>, continuation: InstructionTree): Then =
        Then(stages, continuation).withTypeVariables(typeVariables)

    override fun precedence(): Int = 2

    override fun isAbstract(info: TypeInfo): Boolean = instructions.any { it.isAbstract(info) }

    internal val hasSharedX: Lazy<Boolean> = lazy {
      instructions.count { it.descendantsOfType<XScalar>().isNotEmpty() } >= 2
    }

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
      proposed as? Then
          ?: throw NarrowingException("expected a `THEN` narrowing of `$this`, found `$proposed`")
      if (instructions.size != proposed.instructions.size) {
        throw NarrowingException(
            "`THEN` stage count cannot change from `${instructions.size}` to " +
                "`${proposed.instructions.size}`"
        )
      }
      val values = narrowingXValues(proposed, info, hasSharedX()).filterNotNull().toSet()
      if (values.size > 1) throw NarrowingException("`X` has conflicting values: `$values`")
    }

    private fun narrowingXValues(proposed: Then, info: TypeInfo, sharedX: Boolean): Set<Int?> {
      fun firstChoice(wide: PetNode, narrow: PetNode): Pair<Or, InstructionTree>? {
        if (wide is Or) return (narrow as? InstructionTree)?.let { wide to it }
        if (wide::class != narrow::class) return null
        return wide.immediateChildren().zip(narrow.immediateChildren()).firstNotNullOfOrNull {
            (w, n) ->
          firstChoice(w, n)
        }
      }
      fun replacing(root: Then, target: InstructionTree, replacement: InstructionTree): Then =
          object : PetTransformer() {
                override fun transformNode(node: PetNode): PetNode =
                    if (node === target) replacement else transformChildren(node)
              }
              .transformInstruction(root) as Then

      val alternatives = firstChoice(this, proposed)
      if (alternatives != null) {
        val (choice, selected) = alternatives
        val proposals = if (selected is Or) selected.instructions else listOf(selected)
        return proposals
            .flatMap { proposal ->
              val narrower = replacing(proposed, selected, proposal)
              val accepted =
                  choice.instructions.flatMap { arm ->
                    val candidate = replacing(this, choice, arm)
                    try {
                      candidate.narrowingXValues(narrower, info, sharedX)
                    } catch (_: NarrowingException) {
                      emptySet()
                    }
                  }
              if (accepted.isEmpty())
                  throw NarrowingException("no `OR` arm preserves this sequence's shared choices")
              accepted
            }
            .toSet()
      }
      val specialized = bindTypeVariablesFrom(proposed, info)
      val variables = typeVariablesFor(info)
      val table = info.classTable
      val live =
          specialized.typeVariables.variables
              .filter { variable ->
                variables.bindings(this, proposed, variable, info, table).isNotEmpty()
              }
              .toSet()
      specialized.typeVariables.ensureChoicesRetained(specialized, proposed, info, table, live)
      for ((wide, narrow) in specialized.instructions.zip(proposed.instructions)) {
        narrow.ensureNarrows(wide, info)
      }
      return setOf(if (sharedX) sharedXValue(specialized, proposed, info) else null)
    }

    private fun bindTypeVariablesFrom(
        proposed: Then,
        info: TypeInfo,
        fallback: PetTransformer? = null,
    ): Then {
      val variables = typeVariablesFor(info)
      val captures = mutableMapOf<TypeVariable, GroundType>()
      for (variable in
          variables.variables.filter {
            info.isAbstract(variables.expressionOf(it.declaration))
          }) {
        val declaration = variables.expressionOf(variable.declaration)
        val bindings =
            variables
                .bindings(
                    this,
                    proposed,
                    variable,
                    info,
                    info.classTable,
                )
                .filter {
                  !sameAfterNameConsumption(it, declaration)
                }
                .distinct()
        if (bindings.size > 1) {
          throw NarrowingException(
              "type variable `$variable` has conflicting bindings: `$bindings`"
          )
        }
        val lowered = fallback?.transformExpression(declaration)?.takeIf { it != declaration }
        val binding =
            lowered?.takeIf { candidate -> bindings.all { candidate.narrows(it, info) } }
                ?: bindings.singleOrNull()
        binding?.let {
          val captured = info.classTable.resolve(binding).groundType
          captures[variable] = captured
        }
      }
      val scoped = withParts(stages, continuation).withTypeVariables(variables)
      if (captures.isEmpty()) return scoped
      val transformer =
          variables.bind(
              captures,
              info.classTable,
          )
      for ((variable, captured) in captures) {
        if (!captured.abstract) {
          val constraint =
              transformer.transformExpression(
                  variables.expressionOf(variable.declaration).copy(typeVariableName = null)
              )
          captured.expression.ensureNarrows(constraint, info)
        }
      }
      return transformer.transformInstruction(scoped) as Then
    }

    private fun sameAfterNameConsumption(left: Expression, right: Expression): Boolean =
        left.copy(typeVariableName = null) == right.copy(typeVariableName = null)

    /** Selects the first stage after every shared Type it uses has been chosen. */
    public fun selectFirstStage(
        proposed: Instruction,
        info: TypeInfo,
        loweredBinding: PetTransformer? = null,
    ): Then {
      val firstStage = first
      val selectableFirst =
          if (firstStage is Gated) {
            firstStage.inner as Instruction
          } else {
            firstStage
          }
      if (
          proposed.descendantsOfType<Then>().isNotEmpty() ||
              proposed.descendantsOfType<InstructionGroup>().isNotEmpty()
      ) {
        throw NarrowingException("a first-stage selection cannot contain a sequence or group")
      }
      val proposedFirst = if (firstStage is Gated) firstStage.copy(inner = proposed) else proposed
      val partial = withParts(listOf(proposedFirst) + stages.drop(1), continuation)
      val specialized = bindTypeVariablesFrom(partial, info, loweredBinding)
      val selectedFirstStage = specialized.first
      val selectable =
          if (selectedFirstStage is Gated) selectedFirstStage.inner else selectedFirstStage
      proposed.ensureNarrows(selectable, info)
      if (selectedFirstStage is Gated && !info.has(selectedFirstStage.gate)) {
        throw NarrowingException("condition is not met: `${selectedFirstStage.gate}`")
      }
      val selectedX = if (hasSharedX()) sharedXValue(selectableFirst, proposed, info) else null
      val fullySpecialized =
          selectedX?.let { bindXTo(it).transformInstruction(specialized) as Then } ?: specialized
      val variables = typeVariablesFor(info)
      val table = info.classTable
      val live =
          variables.variables
              .filter { variable ->
                variables.bindings(this, partial, variable, info, table).isNotEmpty()
              }
              .toSet()
      fullySpecialized.typeVariables.ensureChoicesRetained(
          fullySpecialized,
          partial,
          info,
          table,
          live,
      )
      val selected =
          fullySpecialized
              .withParts(
                  listOf(proposed) + fullySpecialized.stages.drop(1),
                  fullySpecialized.continuation,
              )
              .withTypeVariables(fullySpecialized.typeVariables.retaining(live))
      if (selected.mustRemainOneTask(info::isAbstract)) {
        throw NarrowingException("first stage still uses an unsettled shared choice")
      }
      return selected
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
          if (narrowNode == wideNode) return unbound
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
      if (xValues.isEmpty()) {
        throw NarrowingException("cannot match `X` occurrences in `$narrow`")
      }
      val concreteValues = xValues.filterNotNull()
      if (concreteValues.isNotEmpty() && narrow.descendantsOfType<XScalar>().isNotEmpty()) {
        throw NarrowingException("a bound `X` must be substituted at every occurrence in `$narrow`")
      }
      if (concreteValues.size > 1) {
        throw NarrowingException("`X` has conflicting values: `$concreteValues`")
      }
      return concreteValues.singleOrNull()
    }

    /** Whether task admission must retain this complete sequence as one pending instruction. */
    public fun mustRemainOneTask(isAbstract: ((Expression) -> Boolean)?): Boolean =
        (hasSharedX() && first.descendantsOfType<XScalar>().isNotEmpty()) ||
            isAbstract?.let { check ->
              typeVariables.variables.any { variable ->
                first.descendantsOfType<Expression>().any {
                  typeVariables.variableAt(it) === variable
                } && check(typeVariables.expressionOf(variable.declaration))
              }
            } == true

    /** Returns the right-associated continuation enqueued after the first stage. */
    public fun continuationAfterFirst(): InstructionGroup {
      val expander = typeVariables.expandNames()
      val remaining = expander.transformInstructionTree(createTree(stages.drop(1) + continuation))
      return InstructionGroup.of(
          if (remaining == NoOp) remaining
          else remaining.withTypeVariables(typeVariables.transformedBy(expander))
      )
    }

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
                          stage as? Instruction
                              ?: throw PetSyntaxException(
                                  "`THEN` left stage must be one instruction: `$stage`"
                              )
                        }
                    Then(leading, stages.last())
                  }
                }
              }

      /** Resolves shared named variables after the complete sequence has been constructed. */
      public fun resolveTypeVariableNames(then: Then): Then {
        val declarations = then.localTypeVariableDeclarations()
        then.descendantsOfType<Transmute>().forEach { transmute ->
          transmute.localTypeVariableDeclarations().forEach { declaration ->
            val identity = declaration.typeVariableName!!.identity
            fun usedOutside(node: PetNode): Boolean {
              if (node === transmute) return false
              if (
                  node is Transmute &&
                      node.localTypeVariableDeclarations().any {
                        it.typeVariableName!!.identity == identity
                      }
              ) {
                return false
              }
              if (
                  node is Expression &&
                      node.typeVariableName !is Expression.TypeVariableName.Declaration &&
                      node.typeVariableName?.identity == identity
              ) {
                return true
              }
              return node.immediateChildren().any(::usedOutside)
            }
            if (usedOutside(then)) {
              throw PetSyntaxException(
                  "type variable `${declaration.typeVariableName!!.authoredSpelling}` cannot be used outside " +
                      "its transmutation"
              )
            }
          }
        }
        return dev.martianzoo.pets.ast.resolveTypeVariableNames(
            then,
            declarations,
        )
      }
    }
  }

  /**
   * Offers a choice among [instructions] ([rule
   * L2-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   * The parser rejects duplicate authored alternatives. Construction and rewriting collapse arms
   * that have become equal; a single remaining outcome is returned without an `Or` wrapper.
   *
   * An `OR` that remains is always abstract ([rule
   * L3-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
   * It is also one of only two exceptions to a narrowing preserving node shape — the other being
   * [NoOp] narrowing an optional change: any instruction narrows an `OR` by narrowing one arm,
   * while a proposed `OR` narrows it only when every arm does ([rules L3-2 and
   * L3-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
   */
  @ConsistentCopyVisibility
  public data class Or internal constructor(val instructions: List<InstructionTree>) :
      Instruction() {
    init {
      require(instructions.size >= 2)
      if (instructions.distinct().size != instructions.size) {
        throw PetSyntaxException("duplicate `OR` alternatives: `$instructions`")
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
          "instruction `$proposed` does not narrow any arm of `$this`:\n$messages",
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
       * [rule L2-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)'s
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
       * [rule L2-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)'s
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
   * L8-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks)).
   * A handler rewrites only inside its own block and must return the same kind of Pets; a block
   * expanding into several independent instructions splices into the surrounding group ([rule
   * L8-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks)).
   * Every other operation here rejects a transform that survived to it.
   */
  public data class Transform(
      val instruction: InstructionTree,
      override val transformKind: String,
  ) : Instruction(), TransformNode<InstructionTree> {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(instruction)

    override fun scale(factor: Int): Instruction = copy(instruction = instruction * factor)

    override fun isAbstract(info: TypeInfo): Boolean =
        throw ExpressionException("unhandled instruction transform: `$this`")

    override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo): Unit =
        throw ExpressionException("unhandled instruction transform: `$this`")

    override fun toString(): String = "$transformKind[$instruction]"

    override fun extract(): InstructionTree = instruction
  }

  /**
   * How much of a [Change]'s count must actually happen ([rule
   * L2-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#2-instructions)).
   * Only [OPTIONAL] leaves anything open; [MANDATORY] and [AMAP] are incompatible with each other,
   * so neither narrows to the other ([rule
   * L3-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
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
        throw NarrowingException("quantifier `${this.symbol}` does not narrow `${that.symbol}`")
      }
    }

    private companion object {
      private fun from(symbol: String) = entries.first { it.symbol == symbol }
    }
  }
}
