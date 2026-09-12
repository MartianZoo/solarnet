package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import kotlin.math.min

/**
 * A non-negative integer computed from one game state, as defined by
 * [section 5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics).
 * Metrics appear after the `/` of an instruction, inside counting [Requirement]s, and as
 * class-property values.
 *
 * Non-negativity holds by construction: [Subtract] saturates at zero, and every other operation
 * builds up from counts and constants.
 */
public sealed class Metric : PetElement() {
  public companion object {
    /**
     * Returns [inner] scaled by [unit] per
     * [rule L5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics),
     * omitting the meaningless wrapper when [unit] is one. A unit of zero is rejected.
     */
    public fun scaled(inner: Metric, unit: Int): Metric {
      if (unit < 1) throw PetSyntaxException("metric can't be zero")
      return if (unit == 1) inner else Scaled(inner, unit)
    }

    internal fun parser(): Parser<Metric> = Parsers.parser()

    /** Parses Metric subtraction but leaves a top-level `OR` to the enclosing Pets kind. */
    internal fun subtractionParser(): Parser<Metric> = Parsers.subtractionParser()

    internal fun atomParser(): Parser<Metric> = Parsers.atomParser()
  }

  override val kind: kotlin.reflect.KClass<out PetNode> = Metric::class

  /**
   * Evaluates this metric, using [count] for component counts, [readProperty] for property reads,
   * [countUnion] for the union semantics of an [Or], and [rank] for a [Rank].
   *
   * Per
   * [rule L5-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics),
   * those callbacks are the only world-dependent part: scaling, capping and subtraction are
   * computed from the syntax itself.
   */
  public fun evaluate(
      count: (Count) -> Int,
      readProperty: (Property) -> Int,
      countUnion: (Or) -> Int,
      rank: (Rank) -> Int,
  ): Int =
      when (this) {
        is Count -> count(this)
        is Constant -> value
        is Property -> readProperty(this)
        is Rank -> rank(this)
        is Scaled -> inner.evaluate(count, readProperty, countUnion, rank) / unit
        is Max ->
            min(
                inner.evaluate(count, readProperty, countUnion, rank),
                maximum.evaluate(count, readProperty, countUnion, rank),
            )
        is Subtract ->
            maxOf(
                minuend.evaluate(count, readProperty, countUnion, rank) -
                    subtrahend.evaluate(count, readProperty, countUnion, rank),
                0,
            )
        is Or -> countUnion(this)
        is Eval -> error("metric property evaluation was not expanded: $this")
        is Transform -> throw ExpressionException("unhandled metric transform: $this")
      }

  /**
   * The highest-first competition rank of [candidate] among the live [selector] matches, comparing
   * [metrics] lexicographically — `RANK Selector { m1, m2 }`, per
   * [rule L5-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics).
   * Authored syntax leaves [candidate] null; specializing a selector refinement supplies the
   * concrete candidate whose rank is being tested.
   *
   * This module owns only the syntax and that scoping; ranking a live field is realized where a
   * world is available, and pinned by `engine/RankMetricTest.kt`.
   */
  public data class Rank(
      /** The field being ranked. Its refinement filters that field, but see [selectorName]. */
      public val selector: Expression,

      /** The comparison keys, compared lexicographically. At least one is required. */
      public val metrics: List<Metric>,

      /** Which competitor's position is being asked for, or null in authored syntax. */
      public val candidate: Expression? = null,
  ) : Metric() {
    init {
      if (metrics.isEmpty()) throw PetSyntaxException("RANK needs a metric")
    }

    /**
     * The expression used inside [metrics] to denote each candidate: [selector] without its
     * refinement, since
     * [rule L5-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics)
     * keeps that filter out of the name the metrics use.
     */
    public val selectorName: Expression = selector.copy(refinement = null)

    override fun visitChildren(visitor: Visitor) {
      visitor.visit(selector)
      visitor.visit(metrics)
    }

    override fun toString(): String = "RANK $selector { ${metrics.joinToString(", ")} }"

    override fun precedence(): Int = 12
  }

  /**
   * Includes a concrete Metric property's syntax in the surrounding class effect, per
   * [rule L5-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics).
   * Until elaboration expands it, an `EVAL` has no value of its own, and [evaluate] treats a
   * request for one as a programming error.
   */
  public data class Eval(val property: Property) : Metric() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(property)

    override fun toString(): String = "EVAL $property"

    override fun precedence(): Int = 12
  }

  /**
   * Counts the components matching [expression] — the base case of
   * [rule L5-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics),
   * and the only form an [Or] alternative may take.
   */
  public data class Count(val expression: Expression) : Metric() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(expression)

    override fun toString(): String = "$expression"

    override fun precedence(): Int = 12
  }

  /**
   * A fixed non-negative value: `5` is five, per
   * [rule L5-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics).
   */
  public data class Constant public constructor(val value: Int) : Metric() {
    init {
      require(value >= 0)
    }

    override fun visitChildren(visitor: Visitor): Unit = Unit

    override fun toString(): String = "$value"

    override fun precedence(): Int = 12
  }

  /**
   * Counts one for each complete group of [unit] counted by [inner], per
   * [rule L5-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics):
   * `3 Plant` is 2 at seven plants and also 2 at eight. Construct one through [scaled], which drops
   * a meaningless unit of one.
   */
  @ConsistentCopyVisibility
  public data class Scaled internal constructor(val inner: Metric, val unit: Int) : Metric() {
    init {
      if (unit < 1) throw PetSyntaxException("metric can't be zero")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)

    override fun toString(): String = "$unit ${groupPartIfNeeded(inner)}"

    override fun precedence(): Int = 11
  }

  /**
   * The smaller of [inner] and [maximum], per
   * [rule L5-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics).
   * A cap may not be directly capped again.
   */
  public data class Max(val inner: Metric, val maximum: Metric) : Metric() {
    init {
      if (inner is Max) throw PetSyntaxException("what are you even doing")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, maximum)

    override fun toString(): String =
        "${groupPartIfNeeded(inner)} MAX ${groupPartIfNeeded(maximum)}"

    override fun precedence(): Int = 10
  }

  /**
   * Subtracts [subtrahend] from [minuend], saturating at zero so that a metric never goes negative,
   * per
   * [rule L5-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics).
   * Subtraction is left-associative, so `A - B - C` is `(A - B) - C`.
   */
  public data class Subtract(val minuend: Metric, val subtrahend: Metric) : Metric() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(minuend, subtrahend)

    override fun toString(): String {
      val left =
          when (minuend) {
            is Subtract -> "$minuend"
            else -> groupPartIfNeeded(minuend)
          }
      return "$left - ${groupPartIfNeeded(subtrahend)}"
    }

    override fun precedence(): Int = 9
  }

  /**
   * Counts the union of [metrics] without double-counting a component that matches more than one,
   * per
   * [rule L5-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics).
   * The alternatives must be plain [Count]s, because subtraction discards the component identity a
   * union needs. The parser rejects duplicate authored alternatives; construction and rewriting
   * collapse alternatives that have become equal.
   *
   * `OR` binds least tightly of all metric operators ([rule
   * L5-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics)),
   * so after the `/` of an instruction it must be grouped — a bare `OR` there begins an instruction
   * alternative instead.
   */
  @ConsistentCopyVisibility
  public data class Or internal constructor(val metrics: List<Count>) : Metric() {
    init {
      require(metrics.size > 1)
      require(metrics.distinct().size == metrics.size)
    }

    public companion object {
      /**
       * Returns the union of [metrics], flattening nested unions, or null if [metrics] is empty. A
       * single remaining alternative is returned as itself rather than as an `Or`.
       *
       * This collapses duplicate alternatives rather than rejecting them; it is the parser that
       * enforces
       * [rule L5-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#5-metrics)'s
       * rejection of a duplicate an author actually wrote.
       *
       * @throws PetSyntaxException if any alternative is not a plain [Count]
       */
      public fun create(metrics: Iterable<Metric>): Metric? {
        val flattened = metrics.flatMap { if (it is Or) it.metrics else listOf(it) }
        val counted = flattened.map {
          it as? Count
              ?: throw PetSyntaxException(
                  "OR metric alternatives must identify components, but found: $it"
              )
        }
        val distinct = counted.distinct()
        return when (distinct.size) {
          0 -> null
          1 -> distinct.single()
          else -> Or(distinct)
        }
      }

      private fun create(first: Metric, vararg rest: Metric) =
          if (rest.none()) first else create(listOf(first) + rest)
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(metrics)

    override fun toString(): String = metrics.joinToString(" OR ") { groupPartIfNeeded(it) }

    override fun precedence(): Int = 4
  }

  /**
   * An [inner] metric marked for rewriting by the handler named by [transformKind], as
   * [rule L10-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-transform-blocks)
   * allows on any metric. [evaluate] rejects one that survived to evaluation.
   */
  public data class Transform(val inner: Metric, override val transformKind: String) :
      Metric(), TransformNode<Metric> {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)

    override fun toString(): String = "$transformKind[$inner]"

    override fun extract(): Metric = inner
  }

  private object Parsers : PetTokenizer() {
    fun parser(): Parser<Metric> {
      return parser {
        val subtraction = subtractionParser()
        subtraction and
            zeroOrMore(skip(_or) and subtraction) map
            { (met, addon) ->
              val authored = listOf(met) + addon
              val flattened = authored.flatMap { if (it is Or) it.metrics else listOf(it) }
              if (flattened.distinct().size != flattened.size) {
                throw PetSyntaxException("duplicate metric OR alternative: $flattened")
              }
              if (addon.any()) Or.create(authored)!! else met
            }
      }
    }

    fun subtractionParser(): Parser<Metric> {
      return parser {
        atomParser() and
            zeroOrMore(skipChar('-') and atomParser()) map
            { (first, rest) ->
              rest.fold(first, ::Subtract)
            }
      }
    }

    /** One capped/scaled Metric operand; composites require their own delimiters here. */
    fun atomParser(): Parser<Metric> {
      return parser {
        val count: Parser<Count> = Expression.parser() map Metric::Count

        val rank: Parser<Metric> =
            skip(_rank) and
                Expression.parser(allowDerivedClass = false) and
                skipChar('{') and
                commaSeparated(parser()) and
                skipChar('}') map
                { (selector, metrics) ->
                  Rank(selector, metrics)
                }

        val transform: Parser<Metric> =
            transform(parser()) map { (node, transformName) -> Transform(node, transformName) }

        val eval: Parser<Metric> = skip(_eval) and Property.parser() map ::Eval

        val nonconstant: Parser<Metric> =
            rank or eval or transform or Property.parser() or count or group(parser())

        val scaled: Parser<Metric> =
            rawScalar and nonconstant map { (unit, met) -> scaled(met, unit) }

        val constant: Parser<Metric> = rawScalar map ::Constant

        val primary: Parser<Metric> = scaled or nonconstant or constant

        val max: Parser<Metric> =
            primary and
                optional(skip(_max) and primary) map
                { (met, limit) ->
                  limit?.let { Max(met, it) } ?: met
                }

        max
      }
    }
  }
}
