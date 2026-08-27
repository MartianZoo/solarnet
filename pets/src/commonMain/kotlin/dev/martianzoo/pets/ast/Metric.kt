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
 * A way of computing a non-negative integer based on a world. Metrics appear after a slash in
 * instructions, and also belong to `Award`s.
 */
public sealed class Metric : PetElement() {
  public companion object {
    /** Returns [inner] scaled by [unit], omitting the meaningless wrapper when [unit] is one. */
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
   * Evaluates this metric using [count] for component counts and [countUnion] for the
   * multiset-union semantics of an [Or].
   *
   * The callbacks supply the world-dependent operations; scaling and maximum behavior are intrinsic
   * to the metric syntax tree.
   */
  public fun evaluate(
      count: (Count) -> Int,
      readProperty: (Property) -> Int,
      countUnion: (Or) -> Int,
  ): Int =
      when (this) {
        is Count -> count(this)
        is Constant -> value
        is Property -> readProperty(this)
        is Scaled -> inner.evaluate(count, readProperty, countUnion) / unit
        is Max ->
            min(
                inner.evaluate(count, readProperty, countUnion),
                maximum.evaluate(count, readProperty, countUnion),
            )
        is Subtract ->
            maxOf(
                minuend.evaluate(count, readProperty, countUnion) -
                    subtrahend.evaluate(count, readProperty, countUnion),
                0,
            )
        is Or -> countUnion(this)
        is Eval -> error("metric property evaluation was not expanded: $this")
        is Transform -> throw ExpressionException("unhandled metric transform: $this")
      }

  /** Includes a concrete Metric property's syntax in the surrounding class effect. */
  public data class Eval(val property: Property) : Metric() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(property)

    override fun toString(): String = "EVAL $property"

    override fun precedence(): Int = 12
  }

  public data class Count(val expression: Expression) : Metric() {
    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(expression)

    override fun toString(): String = "$expression"

    override fun precedence(): Int = 12
  }

  /** A fixed non-negative value. */
  public data class Constant public constructor(val value: Int) : Metric() {
    init {
      require(value >= 0)
    }

    override fun visitChildren(visitor: Visitor): Unit = Unit

    override fun toString(): String = "$value"

    override fun precedence(): Int = 12
  }

  /** Counts one unit for each complete group of [unit] counted by [inner]. */
  @ConsistentCopyVisibility
  public data class Scaled internal constructor(val inner: Metric, val unit: Int) : Metric() {
    init {
      if (unit < 1) throw PetSyntaxException("metric can't be zero")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)

    override fun toString(): String = "$unit ${groupPartIfNeeded(inner)}"

    override fun precedence(): Int = 11
  }

  /** Caps [inner] at the value of [maximum]. */
  public data class Max(val inner: Metric, val maximum: Metric) : Metric() {
    init {
      if (inner is Max) throw PetSyntaxException("what are you even doing")
    }

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner, maximum)

    override fun toString(): String =
        "${groupPartIfNeeded(inner)} MAX ${groupPartIfNeeded(maximum)}"

    override fun precedence(): Int = 10
  }

  /** Subtracts two Metric values, saturating at zero so Metrics remain non-negative. */
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

  @ConsistentCopyVisibility
  public data class Or internal constructor(val metrics: List<Count>) : Metric() {
    init {
      require(metrics.size > 1)
      require(metrics.distinct().size == metrics.size)
    }

    public companion object {
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

        val transform: Parser<Metric> =
            transform(parser()) map { (node, transformName) -> Transform(node, transformName) }

        val eval: Parser<Metric> = skip(_eval) and Property.parser() map ::Eval

        val nonconstant: Parser<Metric> =
            eval or transform or Property.parser() or count or group(parser())

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
