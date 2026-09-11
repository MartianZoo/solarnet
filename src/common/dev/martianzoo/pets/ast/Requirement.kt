package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/**
 * A yes-or-no query over one game state, for example, `MAX 4 OxygenStep`, as defined by
 * [section 4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements).
 *
 * A requirement only observes; nothing inside one is an open choice for a player to settle ([rule
 * L4-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements)).
 */
public sealed class Requirement : PetElement() {
  public companion object {
    /** Recursively breaks apart the [And] requirements in each element of [requirement]. */
    public fun split(requirement: Iterable<Requirement>): List<Requirement> = requirement.flatMap {
      split(it)
    }

    /** Recursively breaks apart any [And] requirements. */
    public fun split(requirement: Requirement): List<Requirement> =
        if (requirement is And) {
          split(requirement.requirements)
        } else {
          listOf(requirement)
        }

    /**
     * Returns the requirement met only when both [one] and [two] are, or `null` if both are absent.
     * Conjunction is
     * [rule L4-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements)'s
     * comma; a lone argument is returned as-is, and joining a requirement with an equal one yields
     * just the one.
     */
    public fun join(one: Requirement?, two: Requirement?): Requirement? {
      val x = setOfNotNull(one, two)
      return when (x.size) {
        0 -> null
        1 -> x.first()
        else -> And(x.toList())
      }
    }

    internal fun parser(): Parser<Requirement> = Parsers.parser()

    /** Parses one top-level disjunction, leaving a following comma to its container. */
    internal fun disjunctionParser(): Parser<Requirement> = Parsers.disjunctionParser()

    internal fun atomParser(): Parser<Requirement> = Parsers.atomParser()
  }

  override fun safeToNestIn(container: PetNode): Boolean =
      super.safeToNestIn(container) || container is IfTrigger

  /**
   * Evaluates this requirement using [count] for each metric it needs. Per
   * [rule L4-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements),
   * [count] is the only place a game state enters: nothing else about the state is consulted.
   */
  public fun isMetBy(count: (Metric) -> Int): Boolean =
      when (this) {
        is Counting -> {
          val actual = count(metric)
          when (this) {
            is Min -> actual >= target
            is Max -> actual <= target
            is Exact -> actual == target
          }
        }
        is Or -> requirements.any { it.isMetBy(count) }
        is And -> requirements.all { it.isMetBy(count) }
        is Eval -> error("requirement property evaluation was not expanded: $this")
        is Transform -> throw ExpressionException("unhandled requirement transform: $this")
      }

  /**
   * Includes a concrete Requirement property's syntax in the surrounding class effect, per
   * [rule L4-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements).
   * Until elaboration expands it, an `EVAL` has no truth value of its own, and [isMetBy] treats a
   * request for one as a programming error.
   */
  public data class Eval(val property: Property) : Requirement() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(property)

    override fun toString(): String = "EVAL $property"

    override fun precedence(): Int = 12
  }

  /**
   * A requirement comparing [target] with the value of [metric], in one of the three counting forms
   * of
   * [rule L4-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements).
   *
   * A counting requirement takes one metric atom, so a metric union or subtraction must be
   * parenthesized where a requirement counts it — `9 (Plant - Steel)` ([rule
   * L4-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements)).
   */
  public sealed class Counting(
      /**
       * The number [metric]'s value is compared against, independent of any unit scaling inside
       * [metric] itself ([rule
       * L4-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements)).
       */
      public val target: Int,

      /** The metric whose value is compared with [target]. */
      public val metric: Metric,
  ) : Requirement() {
    init {
      require(target >= 0)
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(metric)

    /** The values of [metric] that meet this requirement. */
    public abstract val range: IntRange

    protected fun countingString(prefix: String = "", fullSimpleMetric: Boolean = false): String {
      val countedMetric = metric
      val counted =
          when (countedMetric) {
            is Metric.Count ->
                scaledEx(countedMetric.expression, ActualScalar(target)).let {
                  if (fullSimpleMetric) it.toFullString() else it.toString()
                }
            is Property ->
                if (prefix.isEmpty() && target == 1) "$countedMetric" else "$target $countedMetric"
            is Metric.Transform -> "$target $countedMetric"
            else -> "$target ($countedMetric)"
          }
      return prefix + counted
    }
  }

  /**
   * Met when [countedMetric]'s value is at least [minimum] — the `n M` form of
   * [rule L4-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements).
   * A minimum of zero asks nothing and is rejected by
   * [rule L4-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements);
   * `MAX 0` and `= 0` are the ways to require none.
   */
  public data class Min(public val minimum: Int, public val countedMetric: Metric) :
      Counting(minimum, countedMetric) {
    /** Converts the `n Foo` form, counting components matching the expression. */
    public constructor(
        scaledEx: ScaledExpression
    ) : this(scaledEx.actualScalar(), Metric.Count(scaledEx.expression))

    init {
      Scalar.checkNonzero(ActualScalar(target))
    }

    override fun toString(): String = countingString()

    override val range: IntRange = target..Int.MAX_VALUE
  }

  /**
   * Met when [countedMetric]'s value is at most [maximum] — the `MAX n M` form of
   * [rule L4-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements),
   * and the way to state that something is absent.
   */
  public data class Max(val maximum: Int, val countedMetric: Metric) :
      Counting(maximum, countedMetric) {
    internal constructor(
        scaledEx: ScaledExpression
    ) : this(scaledEx.actualScalar(), Metric.Count(scaledEx.expression))

    override fun toString(): String = countingString("MAX ", fullSimpleMetric = true)

    override val range: IntRange = 0..target
  }

  /**
   * Met when [countedMetric]'s value is exactly [expected] — the `= n M` form of
   * [rule L4-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements).
   */
  public data class Exact(public val expected: Int, public val countedMetric: Metric) :
      Counting(expected, countedMetric) {
    internal constructor(
        scaledEx: ScaledExpression
    ) : this(scaledEx.actualScalar(), Metric.Count(scaledEx.expression))

    override fun toString(): String = countingString("=", fullSimpleMetric = true)

    override val range: IntRange = target..target
  }

  /**
   * Met when at least one of [requirements] is — the `OR` of
   * [rule L4-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements),
   * which binds more tightly than [And]. Alternatives are a set, so repeating one cannot make a
   * requirement easier twice ([rule
   * L4-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements)).
   */
  @ConsistentCopyVisibility
  public data class Or internal constructor(val requirements: Set<Requirement>) : Requirement() {
    internal constructor(
        req1: Requirement,
        req2: Requirement,
        vararg rest: Requirement,
    ) : this(setOf(req1) + req2 + rest)

    init {
      require(requirements.size >= 2)
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirements)

    override fun toString(): String = requirements.joinToString(" OR ") { groupPartIfNeeded(it) }

    override fun precedence(): Int = 3

    public companion object {
      /**
       * Returns the requirement met when any of [requirements] is. Duplicate alternatives collapse,
       * and a single remaining alternative is returned as itself rather than as an `Or` ([rule
       * L4-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements)).
       */
      public fun create(requirements: Collection<Requirement>): Requirement {
        require(requirements.isNotEmpty())
        val distinct = requirements.toSet()
        return if (distinct.size == 1) distinct.single() else Or(distinct)
      }
    }
  }

  /**
   * Met only when every one of [requirements] is — the comma of
   * [rule L4-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements),
   * the lowest-precedence requirement operator. Conjuncts are a sequence, kept as written even when
   * one repeats ([rule
   * L4-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#4-requirements)).
   */
  @ConsistentCopyVisibility
  public data class And internal constructor(val requirements: List<Requirement>) : Requirement() {
    private constructor(
        req1: Requirement,
        req2: Requirement,
        vararg rest: Requirement,
    ) : this(listOf(req1) + req2 + rest)

    init {
      require(requirements.size >= 2)
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirements)

    override fun toString(): String = requirements.joinToString { groupPartIfNeeded(it) }

    override fun precedence(): Int = 1

    override fun safeToNestIn(container: PetNode): Boolean {
      return super.safeToNestIn(container) && container !is IfTrigger
    }

    public companion object {
      /**
       * Returns the requirement met when all of [requirements] are, in the order given. A single
       * conjunct is returned as itself rather than as an `And`.
       */
      public fun create(requirements: Collection<Requirement>): Requirement {
        require(requirements.isNotEmpty())
        return if (requirements.size == 1) requirements.single() else And(requirements.toList())
      }
    }
  }

  /**
   * A [requirement] marked for rewriting by the handler named by [transformKind], as
   * [rule L10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)
   * allows on any requirement. [isMetBy] rejects one that survived to evaluation.
   */
  public data class Transform(val requirement: Requirement, override val transformKind: String) :
      Requirement(), TransformNode<Requirement> {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirement)

    override fun toString(): String = "$transformKind[$requirement]"

    override fun extract(): Requirement = requirement
  }

  override val kind: kotlin.reflect.KClass<out PetNode> = Requirement::class

  private object Parsers : PetTokenizer() {
    fun parser(): Parser<Requirement> {
      return parser {
        commaSeparated(disjunctionParser()) map And.Companion::create
      }
    }

    fun disjunctionParser(): Parser<Requirement> =
        separatedTerms(atomParser(), _or) map { Or.create(it.toSet()) }

    /**
     * A requirement suitable for being nested directly in something else. Used by gated
     * instructions and conditional triggers.
     */
    fun atomParser(): Parser<Requirement> {
      return parser {
        val scaledEx = parser {
          val scalarAndOptionalExpression = rawScalar and optional(Expression.parser())
          val optionalScalarAndExpression = optional(rawScalar) and Expression.parser()

          scalarAndOptionalExpression or
              optionalScalarAndExpression map
              { (scalar, expression) ->
                val resolvedScalar = ActualScalar(scalar ?: 1)
                if (expression == null) ScaledExpression.denominationless(resolvedScalar)
                else scaledEx(expression, resolvedScalar)
              }
        }

        val countedMetric = rawScalar and Metric.atomParser()

        val propertyMin: Parser<Requirement> = Property.parser() map { Min(1, it) }
        val min =
            propertyMin or
                (countedMetric map { (target, metric) -> Min(target, metric) }) or
                (scaledEx map Requirement::Min)
        val max =
            skip(_max) and
                ((countedMetric map { (target, metric) -> Max(target, metric) }) or
                    (scaledEx map Requirement::Max))
        val exact =
            skipChar('=') and
                ((countedMetric map { (target, metric) -> Exact(target, metric) }) or
                    (scaledEx map Requirement::Exact))
        val transform =
            transform(parser()) map { (node, transformName) -> Transform(node, transformName) }
        val eval: Parser<Requirement> = skip(_eval) and Property.parser() map ::Eval
        eval or transform or min or max or exact or group(parser())
      }
    }
  }
}

private fun ScaledExpression.actualScalar(): Int =
    when (val scalar = scalar) {
      is ActualScalar -> scalar.value
      is XScalar -> throw PetSyntaxException("can't use X in requirements (yet?)")
    }
